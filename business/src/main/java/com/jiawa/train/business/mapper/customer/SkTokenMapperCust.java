package com.jiawa.train.business.mapper.customer;

import org.apache.ibatis.annotations.Mapper;

import java.util.Date;

@Mapper
public interface SkTokenMapperCust {

    int decrease(Date date, String trainCode, int decreaseCount);
}
