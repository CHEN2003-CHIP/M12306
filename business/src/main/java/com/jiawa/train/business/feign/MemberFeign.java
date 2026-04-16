package com.jiawa.train.business.feign;



import com.jiawa.train.common.req.MemberTicketReq;
import com.jiawa.train.common.resp.CommonResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

//@FeignClient(value = "Number",url="http://localhost:8080")

//@FeignClient(name = "member",url = "http://127.0.0.1:8001")
@FeignClient(name = "member")
public interface MemberFeign {
    @GetMapping("/member/feign/ticket/save")
    CommonResp<Object> save(@RequestBody MemberTicketReq req);

    @PostMapping("/member/feign/ticket/batch-save")
    CommonResp<Object> batchSave(List<MemberTicketReq> batchMemberTickets);
}
