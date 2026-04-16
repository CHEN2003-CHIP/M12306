//package com.jiawa.train.business.feign.fallback;
//
//
//import com.jiawa.train.business.feign.MemberFeign;
//import com.jiawa.train.business.service.AfterConfirmOrderService;
//import com.jiawa.train.common.resp.CommonResp;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.cloud.openfeign.FallbackFactory;
//import org.springframework.stereotype.Component;
//
//@Component
//public class MemberFallBackFactroy implements FallbackFactory<MemberFeign> {
//    private static final Logger LOG = LoggerFactory.getLogger(AfterConfirmOrderService.class);
//    @Override
//    public MemberFeign create(Throwable cause) {
//        return req -> {
//            LOG.error("调用保存接口异常", cause);
//            return new CommonResp<>();
//        };
//    }
//
//}
