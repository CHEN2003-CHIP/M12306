package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.*;
import com.jiawa.train.business.enums.ConfirmOrderStatusEnum;
import com.jiawa.train.business.enums.SeatColEnum;
import com.jiawa.train.business.enums.SeatTypeEnum;
import com.jiawa.train.business.feign.MemberFeign;
import com.jiawa.train.business.mapper.ConfirmOrderMapper;
import com.jiawa.train.business.mapper.DailyTrainSeatMapper;
import com.jiawa.train.business.mapper.customer.DailyTrainTicketCustomerMapper;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.req.ConfirmOrderQueryReq;
import com.jiawa.train.business.req.ConfirmOrderTicketReq;
import com.jiawa.train.business.resp.ConfirmOrderQueryResp;
import com.jiawa.train.common.context.LoginMemberContext;
import com.jiawa.train.common.exception.BusinessException;
import com.jiawa.train.common.req.MemberTicketReq;
import com.jiawa.train.common.resp.CommonResp;
import com.jiawa.train.common.resp.PageResp;
import com.jiawa.train.common.util.SnowUtil;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jiawa.train.common.exception.BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR;

@Service
public class AfterConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(AfterConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Autowired
    private DailyTrainTicketService dailyTrainTicketService;

    @Autowired
    private DailyTrainCarriageService dailyTrainCarriageService;

    @Autowired
    private DailyTrainSeatService dailyTrainSeatService;
    @Autowired
    private DailyTrainSeatMapper dailyTrainSeatMapper;
    @Autowired
    private DailyTrainTicketCustomerMapper dailyTrainTicketCustomerMapper;
    @Autowired
    private MemberFeign memberFeign;


//    //@Transactional
//    @GlobalTransactional
//    public void afterDoConfirm(DailyTrainTicket dailyTrainTicket,List<DailyTrainSeat> finalSeatList,
//                               List<ConfirmOrderTicketReq> tickets,
//                               ConfirmOrder confirmOrder) throws Exception {
//        LOG.info("开启全局事务{}", RootContext.getXID());
//        for(int j=0;j<finalSeatList.size();j++) {
//            DailyTrainSeat seat=finalSeatList.get(j);
//            DailyTrainSeat finalSeat = new DailyTrainSeat();
//            finalSeat.setId(seat.getId());
//            finalSeat.setSell(seat.getSell());
//            finalSeat.setUpdateTime(new Date());
//            dailyTrainSeatMapper.updateByPrimaryKeySelective(finalSeat);
//
//            //扣减受影响区间的车票
//            Integer startIndex=dailyTrainTicket.getStartIndex();
//            Integer endIndex=dailyTrainTicket.getEndIndex();
//            char[] chars=seat.getSell().toCharArray();
//            Integer maxStartIndex=endIndex-1;
//            Integer minEndIndex=startIndex+1;
//            Integer minStartIndex=0;
//            for(int i=startIndex-1;i>=0;i--){
//                char aChar=chars[i];
//                if(aChar=='1'){
//                    minStartIndex=i+1;
//                    break;
//                }
//            }
//            LOG.info("影响区间start min{}-max{}",minStartIndex,maxStartIndex);
//            Integer maxEndIndex=seat.getSell().length();
//            for(int i=endIndex;i<seat.getSell().length();i++){
//                char aChar=chars[i];
//                if(aChar=='1'){
//                    maxEndIndex=i;
//                    break;
//                }
//            }
//            LOG.info("影响区间minend{}-maxend{}",minEndIndex,maxEndIndex);
//            //UPDATE
//            dailyTrainTicketCustomerMapper.updateCountBySell(
//                    seat.getDate(),
//                    seat.getTrainCode(),
//                    seat.getSeatType(),
//                    minStartIndex,
//                    maxStartIndex,
//                    minEndIndex,
//                    maxEndIndex
//            );
//            //save ticket record
//            MemberTicketReq memberTicketReq = new MemberTicketReq();
//            //numberTicketReq.setNumberId(LoginMemberContext.getId());
//            memberTicketReq.setMemberId(LoginMemberContext.getId());
//            memberTicketReq.setPassengerId(tickets.get(j).getPassengerId());
//            memberTicketReq.setPassengerName(tickets.get(j).getPassengerName());
//            memberTicketReq.setTrainDate(dailyTrainTicket.getDate());
//            memberTicketReq.setTrainCode(dailyTrainTicket.getTrainCode());
//            memberTicketReq.setCarriageIndex(seat.getCarriageIndex());
//            memberTicketReq.setSeatRow(seat.getRow());
//            memberTicketReq.setSeatCol(seat.getCol());
//            memberTicketReq.setStartStation(dailyTrainTicket.getStart());
//            memberTicketReq.setStartTime(dailyTrainTicket.getStartTime());
//            memberTicketReq.setEndStation(dailyTrainTicket.getEnd());
//            memberTicketReq.setEndTime(dailyTrainTicket.getEndTime());
//            memberTicketReq.setSeatType(seat.getSeatType());
//            CommonResp<Object> commonResp = memberFeign.save(memberTicketReq);
//            LOG.info("调用number接口，返回：{}",commonResp);
//
//            ConfirmOrder updateOrder = new ConfirmOrder();
//            updateOrder.setId(confirmOrder.getId());
//            updateOrder.setUpdateTime(new Date());
//            updateOrder.setStatus(ConfirmOrderStatusEnum.SUCCESS.getCode());
//            confirmOrderMapper.updateByPrimaryKeySelective(updateOrder);
//
//
//        }
//    }


 //分布式事务保留，确保批量操作原子性
    @GlobalTransactional
    public void afterDoConfirm(DailyTrainTicket dailyTrainTicket, List<DailyTrainSeat> finalSeatList,
                               List<ConfirmOrderTicketReq> tickets, ConfirmOrder confirmOrder) throws Exception {
        LOG.info("开启全局事务{}", RootContext.getXID());

        // ========== 优化1：批量更新座位售卖状态 ==========
        List<DailyTrainSeat> batchUpdateSeats = new ArrayList<>();
        for (DailyTrainSeat seat : finalSeatList) {
            DailyTrainSeat updateSeat = new DailyTrainSeat();
            updateSeat.setId(seat.getId());
            updateSeat.setSell(seat.getSell());
            updateSeat.setUpdateTime(new Date());
            batchUpdateSeats.add(updateSeat);
        }
        // 调用批量更新SQL（需新增Mapper方法）
        if (CollUtil.isNotEmpty(batchUpdateSeats)) {
            dailyTrainSeatMapper.batchUpdateByPrimaryKeySelective(batchUpdateSeats);
        }

        // ========== 优化2：批量扣减余票（按座位类型匹配字段） ==========
        List<DailyTrainTicketBatchDTO> batchReduceTickets = new ArrayList<>();
        for (DailyTrainSeat seat : finalSeatList) {
            // 计算影响区间（保留原有逻辑）
            Integer startIndex = dailyTrainTicket.getStartIndex();
            Integer endIndex = dailyTrainTicket.getEndIndex();
            char[] chars = seat.getSell().toCharArray();
            Integer maxStartIndex = endIndex - 1;
            Integer minEndIndex = startIndex + 1;
            Integer minStartIndex = 0;
            for (int i = startIndex - 1; i >= 0; i--) {
                char aChar = chars[i];
                if (aChar == '1') {
                    minStartIndex = i + 1;
                    break;
                }
            }
            LOG.info("影响区间start min{}-max{}", minStartIndex, maxStartIndex);
            Integer maxEndIndex = seat.getSell().length()+1;
            for (int i = endIndex; i < seat.getSell().length(); i++) {
                char aChar = chars[i];
                if (aChar == '1') {
                    maxEndIndex = i;
                    break;
                }
            }
            LOG.info("影响区间minend{}-maxend{}", minEndIndex, maxEndIndex);

            // 封装批量扣减参数（seatType对应seatTypeCode）
            DailyTrainTicketBatchDTO dto = new DailyTrainTicketBatchDTO();
            dto.setDate(seat.getDate());
            dto.setTrainCode(seat.getTrainCode());
            dto.setSeatTypeCode(seat.getSeatType()); // 座位类型（1=ydz/2=edz/3=rw/4=yw）
            dto.setMinStartIndex(minStartIndex);
            dto.setMaxStartIndex(maxStartIndex);
            dto.setMinEndIndex(minEndIndex);
            dto.setMaxEndIndex(maxEndIndex);
            batchReduceTickets.add(dto);
        }
        // 调用批量扣减SQL（需新增Mapper方法）
        if (CollUtil.isNotEmpty(batchReduceTickets)) {
            dailyTrainTicketCustomerMapper.batchUpdateCountBySell(batchReduceTickets);
        }

        // ========== 优化3：批量调用会员接口 ==========
        List<MemberTicketReq> batchMemberTickets = new ArrayList<>();
        for (int j = 0; j < finalSeatList.size(); j++) {
            DailyTrainSeat seat = finalSeatList.get(j);
            ConfirmOrderTicketReq ticketReq = tickets.get(j);

            MemberTicketReq req = new MemberTicketReq();
            req.setMemberId(confirmOrder.getMemberId());
            req.setPassengerId(ticketReq.getPassengerId());
            req.setPassengerName(ticketReq.getPassengerName());
            req.setTrainDate(dailyTrainTicket.getDate());
            req.setTrainCode(dailyTrainTicket.getTrainCode());
            req.setCarriageIndex(seat.getCarriageIndex());
            req.setSeatRow(seat.getRow());
            req.setSeatCol(seat.getCol());
            req.setStartStation(dailyTrainTicket.getStart());
            req.setStartTime(dailyTrainTicket.getStartTime());
            req.setEndStation(dailyTrainTicket.getEnd());
            req.setEndTime(dailyTrainTicket.getEndTime());
            req.setSeatType(seat.getSeatType());
            batchMemberTickets.add(req);
        }
        // 调用会员批量保存接口（需改造Feign）
        CommonResp<Object> commonResp = memberFeign.batchSave(batchMemberTickets);
        LOG.info("调用会员批量接口，返回：{}", commonResp);

        // ========== 优化4：统一更新订单状态（一次更新即可） ==========
        ConfirmOrder updateOrder = new ConfirmOrder();
        updateOrder.setId(confirmOrder.getId());
        updateOrder.setUpdateTime(new Date());
        updateOrder.setStatus(ConfirmOrderStatusEnum.SUCCESS.getCode());
        confirmOrderMapper.updateByPrimaryKeySelective(updateOrder);
    }

    // 新增：批量扣减余票DTO（封装参数）
    @Data
    public static class DailyTrainTicketBatchDTO {
        private Date date;
        private String trainCode;
        private String seatTypeCode; // 1=ydz/2=edz/3=rw/4=yw
        private Integer minStartIndex;
        private Integer maxStartIndex;
        private Integer minEndIndex;
        private Integer maxEndIndex;
    }


}
