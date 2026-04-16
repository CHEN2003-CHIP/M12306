package com.jiawa.train.business.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.service.BeforeConfirmOrderService;
import com.jiawa.train.business.service.ConfirmOrderService;
import com.jiawa.train.business.service.DailyTrainService;
import com.jiawa.train.common.exception.BusinessException;
import com.jiawa.train.common.exception.BusinessExceptionEnum;
import com.jiawa.train.common.resp.CommonResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderController.class);

    @Resource
    private ConfirmOrderService confirmOrderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BeforeConfirmOrderService beforeConfirmOrderService;

    @Value("{spring.profiles.active}")
    private String env;

    @GetMapping("/query-line-count/{id}")
    public CommonResp<Integer> queryLineCount(@PathVariable Long id) {
        Integer count = confirmOrderService.queryLineCount(id);
        CommonResp<Integer> commonResp = new CommonResp<>();
        commonResp.setContent(count);
        return commonResp;
    }

    @SentinelResource(value = "confirm-order-do",blockHandler = "doConfirmBlockHandler")
    @PostMapping("/do")
    public CommonResp<Object> doConfirm(@Valid @RequestBody ConfirmOrderDoReq req) {
        if(!"dev".equalsIgnoreCase(env)){
            //验证码校验
            String imageCodeToken = req.getImageCodeToken();
            String imageCode = req.getImageCode();
            String imageCodeInRedis = stringRedisTemplate.opsForValue().get(imageCodeToken);
            LOG.info("验证码从redis：：{}", imageCodeInRedis);
            if(ObjectUtil.isEmpty(imageCodeInRedis)){
                return new CommonResp<>(false, "验证码已过期",null);
            }
            if(!imageCodeInRedis.equalsIgnoreCase(imageCode)){
                return new CommonResp<>(false, "验证码不正确",null);
            }
            else{
                stringRedisTemplate.delete(imageCodeToken);
            }
        }
        //confirmOrderService.doConfirm(req);
        Long id=beforeConfirmOrderService.beforeDoConfirmOrder(req);
        return new CommonResp<>(String.valueOf( id));
    }

    @GetMapping("/cancel/{id}")
    public CommonResp<Integer> cancel(@PathVariable Long id){
        Integer cancelCount = confirmOrderService.cancel(id);
        return new CommonResp<>(cancelCount);
    }

    public CommonResp<Object> doConfirmBlockHandler(ConfirmOrderDoReq req, BlockException ex) {

        CommonResp<Object> commonResp = new CommonResp<>();
        commonResp.setSuccess(false);
        commonResp.setMessage("购票限流了");//方便测试
        //commonResp.setMessage(BusinessExceptionEnum.BUSINESS_CONFIRM_ORDER_LOCK_ERROR.getDesc());
        return commonResp;
    }

}
