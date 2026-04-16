package com.atustcchen.javaailongchain4j.feign;

import com.atustcchen.javaailongchain4j.req.PassengerQueryReq;
import com.atustcchen.javaailongchain4j.resp.CommonResp;
import com.atustcchen.javaailongchain4j.resp.PageResp;
import com.atustcchen.javaailongchain4j.resp.PassengerQueryResp;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name="member")
public interface queryFeign {

    @GetMapping("member/passenger/query-list")
    public CommonResp<PageResp<PassengerQueryResp>> queryList(@Valid @SpringQueryMap PassengerQueryReq req);
}
