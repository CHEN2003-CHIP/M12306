package com.jiawa.train.member.controller;


import com.jiawa.train.common.context.LoginMemberContext;
import com.jiawa.train.common.resp.CommonResp;
import com.jiawa.train.common.resp.PageResp;
import com.jiawa.train.member.req.TicketQueryReq;
import com.jiawa.train.member.resp.TicketQueryResp;
import com.jiawa.train.member.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member/ticket")
public class TicketController {
    @Autowired
    private TicketService ticketService;

    /**
     * 查询用户购票信息
     * @param req
     * @return
     */
    @GetMapping("/query-list")
    public CommonResp<PageResp<TicketQueryResp>>query(@Valid TicketQueryReq req){
        CommonResp<PageResp<TicketQueryResp>> commonResp=new CommonResp<>();
        req.setNumberId(LoginMemberContext.getId());
        PageResp<TicketQueryResp> pageResp = ticketService.queryList(req);
        commonResp.setContent(pageResp);
        return commonResp;
    }
}
