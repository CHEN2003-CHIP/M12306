package com.jiawa.train.business.domain;

import lombok.Data;

import java.util.Date;

@Data
public class SkToken {
    private Long id;

    private Date date;

    private String trainCode;

    private Integer count;

    private Date createTime;

    private Date updateTime;
}
