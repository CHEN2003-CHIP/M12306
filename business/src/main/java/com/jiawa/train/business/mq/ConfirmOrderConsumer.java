package com.jiawa.train.business.mq;

import com.alibaba.fastjson.JSON;
import com.jiawa.train.business.dto.ConfirmOrderMQDto;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.service.ConfirmOrderService;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(consumerGroup = "TRAIN_BUSINESS",topic="CONFIRM_ORDER")
public class ConfirmOrderConsumer implements RocketMQListener<MessageExt> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderConsumer.class);

    @Autowired
    private ConfirmOrderService confirmOrderService;
    @Override
    public void onMessage(MessageExt messageExt) {
        byte[] body = messageExt.getBody();
        //ConfirmOrderDoReq req = JSON.parseObject(body, ConfirmOrderDoReq.class);
        ConfirmOrderMQDto req = JSON.parseObject(body, ConfirmOrderMQDto.class);
        MDC.put("LOG_ID",req.getLogId());
        LOG.info("收到消息: " +new String( body));
        confirmOrderService.doConfirm(req);
    }
}
