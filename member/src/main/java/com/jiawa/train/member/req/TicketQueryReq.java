package com.jiawa.train.member.req;


import com.jiawa.train.common.req.PageReq;

public class TicketQueryReq extends PageReq {

    private Long memberId;

    public Long getNumberId() {
        return memberId;
    }

    public void setNumberId(Long numberId)
    {
        this.memberId = numberId;
    }

    @Override
    public String toString() {
        return "TicketQueryReq{" +
                "} " + super.toString();
    }
}
