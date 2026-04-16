package com.jiawa.train.business.mapper.customer;

import com.jiawa.train.business.domain.DailyTrainTicket;
import com.jiawa.train.business.domain.DailyTrainTicketExample;
import com.jiawa.train.business.service.AfterConfirmOrderService;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface DailyTrainTicketCustomerMapper {
    void updateCountBySell(
            Date date,
            String trainCode,
            String seatTypeCode,
            Integer minStartIndex,
            Integer maxStartIndex,
            Integer minEndIndex,
            Integer maxEndIndex
    );


    void batchUpdateCountBySell(List<AfterConfirmOrderService.DailyTrainTicketBatchDTO> batchReduceTickets);
}
