package com.jiawa.train.member.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
@Data
public class TicketSaveReq {

    /**
     * id
     */
    private Long id;

    /**
     * 会员id
     */
    @NotNull(message = "【会员id】不能为空")
    private Long numberId;

    /**
     * 乘客id
     */
    @NotNull(message = "【乘客id】不能为空")
    private Long passengerId;

    /**
     * 乘客姓名
     */
    private String passengerName;

    /**
     * 日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @NotNull(message = "【日期】不能为空")
    private Date trainData;

    /**
     * 车次编号
     */
    @NotBlank(message = "【车次编号】不能为空")
    private String trainCode;

    /**
     * 厢序
     */
    @NotNull(message = "【厢序】不能为空")
    private Integer carriageIndex;

    /**
     * 排号|01，02
     */
    @NotBlank(message = "【排号】不能为空")
    private String seatRow;

    /**
     * 列号|枚举[SeatColEnum]
     */
    @NotBlank(message = "【列号】不能为空")
    private String seatCol;

    /**
     * 出发站
     */
    @NotBlank(message = "【出发站】不能为空")
    private String startStation;

    /**
     * 出发时间
     */
    @JsonFormat(pattern = "HH:mm:ss",timezone = "GMT+8")
    @NotNull(message = "【出发时间】不能为空")
    private Date startTime;

    /**
     * 到达站
     */
    @NotBlank(message = "【到达站】不能为空")
    private String endStation;

    /**
     * 到达时间
     */
    @JsonFormat(pattern = "HH:mm:ss",timezone = "GMT+8")
    @NotNull(message = "【到达时间】不能为空")
    private Date endTime;

    /**
     * 座位类型|枚举[SeatTypeEnum]
     */
    @NotBlank(message = "【座位类型】不能为空")
    private String seatType;

    /**
     * 新增时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;

    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date updateTime;

}
