package com.example.campusskillplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_info")
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long skillId;
    private Long demandId;
    private Long publisherId;
    private Long acceptorId;
    private BigDecimal amount;

    /**
     * 订单状态：0-待支付，1-资金托管，2-服务中，3-待确认，4-已完成，5-已取消
     */
    private Integer status = 0;
    private String payPassword;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String cancelReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}