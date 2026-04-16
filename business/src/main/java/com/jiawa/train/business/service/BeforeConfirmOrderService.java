package com.jiawa.train.business.service;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.jiawa.train.business.domain.ConfirmOrder;
import com.jiawa.train.business.dto.ConfirmOrderMQDto;
import com.jiawa.train.business.enums.ConfirmOrderStatusEnum;
import com.jiawa.train.business.enums.RocketMQTopicEnum;
import com.jiawa.train.business.mapper.ConfirmOrderMapper;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.common.context.LoginMemberContext;
import com.jiawa.train.common.exception.BusinessException;
import com.jiawa.train.common.exception.BusinessExceptionEnum;
import com.jiawa.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.json.Json;
import java.util.concurrent.TimeUnit;

@Service
public class BeforeConfirmOrderService {
    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Autowired
    public RocketMQTemplate rocketMQTemplate;

    @Autowired
    private SkTokenService skTokenService;

    @Autowired
    private RedissonClient redissonClient;

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

//    @SentinelResource(value = "BeforeDoConfirm",blockHandler = "beforeDoConfirmBlockHandler")
    public Long beforeDoConfirmOrder(ConfirmOrderDoReq req)  {
        req.setMemberId(LoginMemberContext.getId());
        //检验令牌
        boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getId());
        if(!validSkToken){
            LOG.info("令牌校验不通过");
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_CONFIRM_ORDER_SKTOKEN_ERROR);
        }
        else{
            LOG.info("令牌校验通过");
        }
        // 保存确认订单表，状态初始
        DateTime now= DateTime.now();
        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setId(SnowUtil.getSnowflakeNextId());
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrder.setMemberId(LoginMemberContext.getId());
        confirmOrder.setDate(req.getDate());
        confirmOrder.setTrainCode(req.getTrainCode());
        confirmOrder.setStart(req.getStart());
        confirmOrder.setEnd(req.getEnd());
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));

        confirmOrderMapper.insert(confirmOrder);

        //发送MQ排队购票消息
        ConfirmOrderMQDto dto=new ConfirmOrderMQDto();
        dto.setLogId(MDC.get("LOG_ID"));
        dto.setDate(req.getDate());
        dto.setTrainCode(req.getTrainCode());
        String reqJson= JSON.toJSONString(dto);
        LOG.info("排队购票消息: " + reqJson);
        rocketMQTemplate.convertAndSend(RocketMQTopicEnum.CONFIRM_ORDER.getCode(), reqJson);
        LOG.info("排队购票消息发送成功: " + reqJson);
        return confirmOrder.getId();



    }

    public void beforeDoConfirmBlockHandler(ConfirmOrderDoReq req, BlockException ex) {
        LOG.error("购票限流了");
        throw new BusinessException(BusinessExceptionEnum.BUSINESS_CONFIRM_ORDER_LOCK_ERROR);
    }
}
