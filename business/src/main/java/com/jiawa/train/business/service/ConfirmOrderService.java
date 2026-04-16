package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.*;
import com.jiawa.train.business.dto.ConfirmOrderMQDto;
import com.jiawa.train.business.enums.ConfirmOrderStatusEnum;
import com.jiawa.train.business.enums.SeatColEnum;
import com.jiawa.train.business.enums.SeatTypeEnum;
import com.jiawa.train.business.req.ConfirmOrderTicketReq;
import com.jiawa.train.common.context.LoginMemberContext;
import com.jiawa.train.common.exception.BusinessException;
import com.jiawa.train.common.exception.BusinessExceptionEnum;
import com.jiawa.train.common.resp.PageResp;
import com.jiawa.train.common.util.SnowUtil;
import com.jiawa.train.business.mapper.ConfirmOrderMapper;
import com.jiawa.train.business.req.ConfirmOrderQueryReq;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.resp.ConfirmOrderQueryResp;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jiawa.train.common.exception.BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR;
import static com.jiawa.train.common.exception.BusinessExceptionEnum.CONFIRM_ORDER__ERROR;

@Service
public class ConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Autowired
    private DailyTrainTicketService dailyTrainTicketService;

    @Autowired
    private DailyTrainCarriageService dailyTrainCarriageService;

    @Autowired
    private DailyTrainSeatService dailyTrainSeatService;

    @Autowired
    private AfterConfirmOrderService afterConfirmOrderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SkTokenService skTokenService;

    public void save(ConfirmOrderDoReq req) {
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        } else {
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        PageInfo<ConfirmOrder> pageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<ConfirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }

//    @SentinelResource(value = "DoConfirm",blockHandler = "doConfirmBlockHandler")
    public void doConfirm(ConfirmOrderMQDto req) {


        String lockKey= DateUtil.formatDate(req.getDate())+"."+req.getTrainCode();
        //redisson
        RLock lock = null;
        try{
            lock = redissonClient.getLock(lockKey);
            boolean tryLock=lock.tryLock(3, TimeUnit.SECONDS);
            if(tryLock){
                LOG.info("拿到Lock SUCCESSFULLY!!");
            }
            else{
                LOG.info("未拿到Lock !!");
                //throw new BusinessException(BusinessExceptionEnum.BUSINESS_CONFIRM_ORDER_LOCK_ERROR);
                return;
            }
            while (true){
                ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
                confirmOrderExample.setOrderByClause("id asc");
                confirmOrderExample.createCriteria().andDateEqualTo(req.getDate())
                        .andTrainCodeEqualTo(req.getTrainCode())
                        .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
                PageHelper.startPage(1,5);
                List<ConfirmOrder> list = confirmOrderMapper.selectByExampleWithBLOBs(confirmOrderExample);

                if (CollUtil.isEmpty(list)) {
                    LOG.info("没有需要处理的订单，结束循环");
                    break;
                } else {
                    LOG.info("本次处理{}条订单", list.size());
                }
                list.forEach(confirmOrder ->{
                    try {
                        sell(confirmOrder);
                    } catch (BusinessException e) {
                        if (e.getE().equals(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR)) {
                            LOG.info("本订单余票不足，继续售卖下一个订单");
                            confirmOrder.setStatus(ConfirmOrderStatusEnum.EMPTY.getCode());
                            updateStatus(confirmOrder);
                        } else {
                            throw e;
                        }
                    }
                });
            }
        } catch (InterruptedException e) {
            LOG.error("购票异常",e);
        }
        finally {
            //增加日志打印功能
            LOG.info("释放锁开始：{}", lock);
            if(null != lock && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }


    /**
     * 更新订单状态
     * @param confirmOrder
     */
    private void updateStatus(ConfirmOrder confirmOrder) {
        ConfirmOrder confirmOrder1 = new ConfirmOrder();
        confirmOrder1.setId(confirmOrder.getId());
        confirmOrder1.setStatus(confirmOrder.getStatus());
        confirmOrder1.setUpdateTime(new Date());
        confirmOrderMapper.updateByPrimaryKeySelective(confirmOrder1);
    }

    /**.
     * 售票
     * @param confirmOrder
     */
    private void sell(ConfirmOrder confirmOrder) {

        // 为了演示排队效果，每次出票增加200毫秒延时
//        try {
//            Thread.sleep(200);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        // 构造ConfirmOrderDoReq
        ConfirmOrderDoReq req = new ConfirmOrderDoReq();
        req.setMemberId(confirmOrder.getMemberId());
        req.setDate(confirmOrder.getDate());
        req.setTrainCode(confirmOrder.getTrainCode());
        req.setStart(confirmOrder.getStart());
        req.setEnd(confirmOrder.getEnd());
        req.setDailyTrainTicketId(confirmOrder.getDailyTrainTicketId());
        req.setTickets(JSON.parseArray(confirmOrder.getTickets(), ConfirmOrderTicketReq.class));
        req.setLogId("");
        // 省略业务数据校验，如：车次是否存在，余票是否存在，车次是否在有效期内，tickets条数>0，同乘客同车次是否已买过

        LOG.info("将确认订单更新成处理中，避免重复处理，confirm_order.id: {}", confirmOrder.getId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.PENDING.getCode());
        updateStatus(confirmOrder);
        List<ConfirmOrderTicketReq> tickets = req.getTickets();
            /*// 保存确认订单表，状态初始
            Date date = new Date();
            ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class, "tickets");
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            //confirmOrder.setNumberId(LoginMemberContext.getId());
            confirmOrder.setNumberId(req.getNumberId());
            confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
            confirmOrder.setCreateTime(date);
            confirmOrder.setUpdateTime(date);
            confirmOrder.setTickets(JSON.toJSONString(tickets));
            confirmOrderMapper.insert(confirmOrder);*/

       /* //从数据库中查
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id asc");
        confirmOrderExample.createCriteria().andDateEqualTo(req.getDate())
                .andMemberIdEqualTo(req.getNumberId())
                .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode())
                .andTrainCodeEqualTo(req.getTrainCode());
        List<ConfirmOrder> list = confirmOrderMapper.selectByExampleWithBLOBs(confirmOrderExample);
        ConfirmOrder confirmOrder = null;
        if (CollUtil.isEmpty(list)){
            LOG.info("没有找到对应的订单信息");
        }else {
            LOG.info("本次处理:{}条确认订单", list.size());
            confirmOrder=list.get(0);
        }*/

        // 查出余票记录，需要得到真实的库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(req.getDate(), req.getTrainCode(), req.getStart(), req.getEnd());
        LOG.info("余票信息:{}", dailyTrainTicket.toString());

        // 扣减余票数量，并判断余票是否足够
        reduceTicket(req, dailyTrainTicket);

        //存储最终的购票结果
        ArrayList<DailyTrainSeat> finalSeatList = new ArrayList<>();

        /*1.计算相对第一个座位的偏移值
         * 2.比如选择的是C1，D2,则偏移量为[0.5]
         * 3.比如选择的是A1，B1，C1，则偏移量为[0,1,2]
         */
        ConfirmOrderTicketReq ticketReq0 = tickets.get(0);
        if (StrUtil.isNotBlank(ticketReq0.getSeat())) {
            LOG.info("本次购票有选座");

            //查出本次选座的座位类型都有哪些列，用于计算所选座位与第一个座位的偏移值
            List<SeatColEnum> colsByType = SeatColEnum.getColsByType(ticketReq0.getSeatTypeCode());
            LOG.info("本次选座座位类型包含的列有：{}", colsByType);

            //组成和前端两排选座一样的列表，用于做参照的作为列表，如:referSeatList={A1,C1,D1,F1,A2,C2,D2,F2}
            List<String> referSeatList = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                for (SeatColEnum seatColEnum : colsByType) {
                    referSeatList.add(seatColEnum.getCode() + (i + 1));
                }
            }
            LOG.info("用于作为参照的两排座位列表：{}", referSeatList);

            //先计算绝对偏移值，即参照作为在列表中的位置索引,如：绝对偏移[1,5]相对偏移为[1-1,5-1]=[0,4]
            ArrayList<Integer> absoluteOffSet = new ArrayList<>();
            for (ConfirmOrderTicketReq ticket : tickets) {
                String seatCode = ticket.getSeat();
                int index = referSeatList.indexOf(seatCode);
                absoluteOffSet.add(index);
            }
            LOG.info("计算得到所有的绝对偏移值为：{}", absoluteOffSet);

            //在计算相对偏移值，即第一个座位到所选座位的偏移值
            ArrayList<Integer> relativeOffSet = new ArrayList<>();
            for (Integer data : absoluteOffSet) {
                int index = data - absoluteOffSet.get(0);
                relativeOffSet.add(index);
            }
            LOG.info("计算得到所有的相对偏移值为：{}", relativeOffSet);
            //ticketReq0.getSeat().split("")[0]意思是将‘A1’变为【A,1】后取第一位
            getSeat( req.getDate(), req.getTrainCode(), ticketReq0.getSeatTypeCode(), ticketReq0.getSeat().split("")[0], relativeOffSet, dailyTrainTicket.getStartIndex(), dailyTrainTicket.getEndIndex(),finalSeatList);

        } else {
            LOG.info("本次购票没有有选座");
            for (ConfirmOrderTicketReq ticket : tickets) {
                getSeat(req.getDate(), req.getTrainCode(), ticket.getSeatTypeCode(), null, null, dailyTrainTicket.getStartIndex(), dailyTrainTicket.getEndIndex(),finalSeatList);
            }

        }
        LOG.info("最终的选座结果为：{}", finalSeatList);

        // 选中座位后事务处理：
        // 座位表修改售卖情况sell；
        // 余票详情表修改余票；
        // 为会员增加购票记录
        // 更新确认订单为成功
        try {
            afterConfirmOrderService.afterDoConfirm(dailyTrainTicket, finalSeatList, tickets, confirmOrder);
        } catch (Exception e) {
            LOG.error("购票失败事务回滚", e);
            throw new BusinessException(CONFIRM_ORDER__ERROR);
        }
    }

    /**
     * 取消排队，只有I状态才能取消排队，所以按状态更新
     * @param id
     */
    public Integer cancel(Long id) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.createCriteria().andIdEqualTo(id).andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setStatus(ConfirmOrderStatusEnum.CANCEL.getCode());
        return confirmOrderMapper.updateByExampleSelective(confirmOrder, confirmOrderExample);
    }


    public void doConfirmBlockHandler(ConfirmOrderDoReq req, BlockException ex) {
        LOG.error("购票限流了");
        throw new BusinessException(BusinessExceptionEnum.BUSINESS_CONFIRM_ORDER_LOCK_ERROR);
    }
    /**
     *
     * @param date
     * @param trainCode
     * @param seatType
     * @param column
     * @param offsetList
     */
    //获取座位
    public void getSeat(Date date,String trainCode,String seatType,
                        String column,List<Integer> offsetList,
                        Integer startIndex,Integer endIndex,List<DailyTrainSeat> fianlSeatList){
        //get the carriage level
        List<DailyTrainSeat> getSeatList=new ArrayList<>();
        List<DailyTrainCarriage> dailyTrainCarriages = dailyTrainCarriageService.
                selectBySeatType(date,trainCode,seatType);
        LOG.info("获取车厢的数量为{}", dailyTrainCarriages.size());
        //get the seats in the carriages
        for(DailyTrainCarriage carriage:dailyTrainCarriages){
            getSeatList.clear();
            LOG.info("开始从{}车厢找座位",carriage.getIndex());
            List<DailyTrainSeat> dailyTrainSeats = dailyTrainSeatService
                    .selectByCarriage(date, trainCode, carriage.getIndex());
            LOG.info("车厢{}的座位数是{}",carriage.getIndex(),dailyTrainSeats.size());
            for(DailyTrainSeat seat:dailyTrainSeats){

                Integer seatIndex=seat.getCarriageSeatIndex();
                String col=seat.getCol();

                boolean isAlreadyChoose=false;
                for(int i=0;i<fianlSeatList.size();i++){
                    if(seat.getId().equals(fianlSeatList.get(i).getId())){
                        isAlreadyChoose=true;
                        break;
                    }
                }
                if(isAlreadyChoose){
                    continue;
                }

                if(StrUtil.isBlank(column)){
                    LOG.info("非选座");
                }
                else{
                    if(!col.equals(column)){
                        LOG.info("座位{}列值不对，寻找下一个，当前列{}，期待列{}",seatIndex,col,column);
                        continue;
                    }
                }

                boolean isAvaliable= canSell(seat,startIndex,endIndex);
                if(isAvaliable){
                    LOG.info("选中车座");
                    getSeatList.add(seat);
                }
                else{
                    continue;
                }


                boolean isAllGet=true;
                if(CollUtil.isNotEmpty(offsetList)){
                    LOG.info("有偏移值：{}，校验偏移的座位是否可选",offsetList);
                    for(int i=1;i<offsetList.size();i++){
                        Integer offset=offsetList.get(i);
                        int nextIndex=seatIndex+offset-1;
                        if(nextIndex>=dailyTrainSeats.size()){
                            LOG.info("偏移值以超出车厢座位数，当前车厢无法选择");
                            isAllGet=false;
                            break;
                        }
                        DailyTrainSeat nextSeat=dailyTrainSeats.get(nextIndex);
                        boolean canChoose=canSell(nextSeat,startIndex,endIndex);
                        if(canChoose){
                            LOG.info("下一个座位{}被选中",nextSeat.getCarriageSeatIndex());
                            getSeatList.add(nextSeat);
                        }
                        else{
                            LOG.info("下一个座位{}不可选中",nextSeat.getCarriageSeatIndex());
                            isAllGet=false;
                            break;
                        }
                    }
                }
                if(!isAllGet){
                    //选不出来全部的合法座位
                    getSeatList.clear();
                    continue;
                }
                //save
                fianlSeatList.addAll(getSeatList);
                return;

            }
        }

    }

    /**
     * 查询当前id之前排队的人数
     * @param id
     * @return
     */

    public Integer queryLineCount(Long id) {
        ConfirmOrder confirmOrder = confirmOrderMapper.selectByPrimaryKey(id);
        ConfirmOrderStatusEnum statusEnum = EnumUtil.getBy(ConfirmOrderStatusEnum::getCode, confirmOrder.getStatus());
        int result=switch (statusEnum){
            case PENDING -> 0;
            case SUCCESS -> -1;
            case FAILURE -> -2;
            case EMPTY -> -3;
            case CANCEL -> -4;
            case INIT -> 999 ;
        };
        if (result == 999) {
            // 排在第几位，下面的写法：where a=1 and (b=1 or c=1) 等价于 where (a=1 and b=1) or (a=1 and c=1)
            ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
            confirmOrderExample.or().andDateEqualTo(confirmOrder.getDate())
                    .andTrainCodeEqualTo(confirmOrder.getTrainCode())
                    .andCreateTimeLessThan(confirmOrder.getCreateTime())
                    .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
            confirmOrderExample.or().andDateEqualTo(confirmOrder.getDate())
                    .andTrainCodeEqualTo(confirmOrder.getTrainCode())
                    .andCreateTimeLessThan(confirmOrder.getCreateTime())
                    .andStatusEqualTo(ConfirmOrderStatusEnum.PENDING.getCode());
            return Math.toIntExact(confirmOrderMapper.countByExample(confirmOrderExample));
        } else {
            return result;
        }
    }
    //the seat can be sold ?
    private boolean canSell(DailyTrainSeat seat,Integer start,Integer end){
        String sell=seat.getSell();
        String sellPart=sell.substring(start-1,end-1);
        if(Integer.parseInt(sellPart)>0){
            LOG.info("座位{}在区间{}-{}已售出，无法选中该座位",
                    seat.getCarriageSeatIndex(),
                    start,end);
            return false;
        }
        else{
            LOG.info("座位{}在区间{}-{}未售出，选中该座位",
                    seat.getCarriageSeatIndex(),
                    start,end);
            //current sell
            StringBuilder currentSell=new StringBuilder(sell);
            for(int i=start-1;i<end-1;i++){
                currentSell.setCharAt(i,'1');
            }
            String curSell=currentSell.toString();
            LOG.info("座位{}在区间{}-{}已售出，原来售卖sell信息为{},现在售卖信息为{}。",
                    seat.getCarriageSeatIndex(),
                    start,end,sell,curSell);
            seat.setSell(curSell);
            return true;
        }
    }

    private static void reduceTicket(ConfirmOrderDoReq req, DailyTrainTicket dailyTrainTicket) {
        for(ConfirmOrderTicketReq ticketReq : req.getTickets()) {
            String seatTypdeCode=ticketReq.getSeatTypeCode();
            SeatTypeEnum seatTypeEnum = EnumUtil.getBy(SeatTypeEnum::getCode, seatTypdeCode);
            switch (seatTypeEnum) {
                case YDZ -> {
                    int restTicketCount= dailyTrainTicket.getYdz()-1;
                    if(restTicketCount<0) {
                        throw new BusinessException(CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(restTicketCount);
                }
                case EDZ ->  {
                    int restTicketCount= dailyTrainTicket.getEdz()-1;
                    if(restTicketCount<0) {
                        throw new BusinessException(CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setEdz(restTicketCount);
                }
                case RW -> {
                    int restTicketCount= dailyTrainTicket.getRw()-1;
                    if(restTicketCount<0) {
                        throw new BusinessException(CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setRw(restTicketCount);
                }
                case YW -> {
                    int restTicketCount= dailyTrainTicket.getYw()-1;
                    if(restTicketCount<0) {
                        throw new BusinessException(CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYw(restTicketCount);
                }
            }
        }
    }
}
