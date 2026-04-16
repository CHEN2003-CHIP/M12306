package com.jiawa.train.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.common.req.MemberTicketReq;
import com.jiawa.train.common.resp.PageResp;
import com.jiawa.train.common.util.SnowUtil;
import com.jiawa.train.member.domain.Ticket;
import com.jiawa.train.member.domain.TicketExample;
import com.jiawa.train.member.mapper.TicketMapper;
import com.jiawa.train.member.req.TicketQueryReq;
import com.jiawa.train.member.resp.TicketQueryResp;
import com.jiawa.train.member.service.TicketService;
//import io.seata.core.context.RootContext;


import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TicketServiceImpl implements TicketService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketServiceImpl.class);

    @Autowired
    private TicketMapper ticketMapper;

    /**
     * 会员购买车票后新增保存
     * @param req
     */
    //@DeleteCache(cacheName= "Ticket",key = "TicketService")
    public void save(MemberTicketReq req)  {
        LOG.info("seata的全局事务id:{}", RootContext.getXID());//与远程调用的id一致，表示在一个事务中
        DateTime now = DateTime.now();
        Ticket ticket = BeanUtil.copyProperties(req, Ticket.class);
        ticket.setId(SnowUtil.getSnowflakeNextId());
        ticket.setCreateTime(now);
        ticket.setUpdateTime(now);
        ticketMapper.insert(ticket);
    }

    /**
     * 用户购票信息
     * @param req
     * @return
     */
    //@GetCache(cacheName = "Ticket",key="TicketService",expire = 60,timeUnit = TimeUnit.SECONDS)
    public PageResp<TicketQueryResp> queryList(TicketQueryReq req) {
        TicketExample ticketExample = new TicketExample();
        ticketExample.setOrderByClause("id desc");
        TicketExample.Criteria criteria = ticketExample.createCriteria();
        if (ObjectUtil.isNotNull(req.getNumberId())){
            criteria.andNumberIdEqualTo(req.getNumberId());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<Ticket> ticketList = ticketMapper.selectByExample(ticketExample);

        if(CollUtil.isNotEmpty(ticketList)){
            LOG.info("date ::{}",ticketList.get(0).getTrainDate());
        }

        PageInfo<Ticket> pageInfo = new PageInfo<>(ticketList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<TicketQueryResp> list = BeanUtil.copyToList(ticketList, TicketQueryResp.class);

        LOG.info("new date ::{}",list.get(0).getTrainDate());
        PageResp<TicketQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    //@DeleteCache(cacheName= "Ticket",key = "TicketService")
    public void delete(Long id) {
        ticketMapper.deleteByPrimaryKey(id);
    }

    @Override
    public void batchSave(List<MemberTicketReq> reqList) {
        LOG.info("seata全局事务id:{}", RootContext.getXID());
        Date now = new Date();
        List<Ticket> ticketList = new ArrayList<>();

        for (MemberTicketReq req : reqList) {
            Ticket ticket = new Ticket();
            ticket.setId(SnowUtil.getSnowflakeNextId());
            ticket.setMemberId(req.getMemberId());
            ticket.setPassengerId(req.getPassengerId());
            ticket.setPassengerName(req.getPassengerName());
            ticket.setTrainDate(req.getTrainDate());
            ticket.setTrainCode(req.getTrainCode());
            ticket.setCarriageIndex(req.getCarriageIndex());
            ticket.setSeatRow(req.getSeatRow());
            ticket.setSeatCol(req.getSeatCol());
            ticket.setStartStation(req.getStartStation());
            ticket.setStartTime(req.getStartTime());
            ticket.setEndStation(req.getEndStation());
            ticket.setEndTime(req.getEndTime());
            ticket.setSeatType(req.getSeatType());
            ticket.setCreateTime(now);
            ticket.setUpdateTime(now);
            ticketList.add(ticket);
        }

        // 调用批量插入SQL
        ticketMapper.batchInsert(ticketList);
    }
}
