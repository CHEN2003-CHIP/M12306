package com.jiawa.train.business.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ConfirmOrderMQDto {
    /**
     * 日志流程号，用于同转异时，用同一个流水号
     */
    private String logId;

    /**
     * 日期
     */
    private Date date;

    /**
     * 车次编号
     */
    private String trainCode;

}
