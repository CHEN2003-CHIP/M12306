package com.jiawa.train.member.service;

import com.jiawa.train.common.req.MemberTicketReq;
import com.jiawa.train.common.resp.PageResp;
import com.jiawa.train.member.req.TicketQueryReq;
import com.jiawa.train.member.resp.TicketQueryResp;

import java.util.List;


public interface TicketService {

    public void save(MemberTicketReq req) throws Exception;

    public PageResp<TicketQueryResp> queryList(TicketQueryReq req);

    public void delete(Long id);

    void batchSave(List<MemberTicketReq> reqList);
}
