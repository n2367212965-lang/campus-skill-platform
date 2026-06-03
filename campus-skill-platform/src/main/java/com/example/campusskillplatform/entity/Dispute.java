package com.example.campusskillplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dispute")
public class Dispute {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long applicantId;
    private Integer disputeType;    // 纠纷类型：1-订单取消纠纷，2-服务质量纠纷，3-支付纠纷
    private String reason;
    private String evidence;

    private Integer status;         // 状态：0-待处理，1-处理中，2-已解决，3-已关闭
    private String handleResult;
    private Long handlerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    private LocalDateTime handleTime;

    @TableLogic
    private Integer deleted;
}