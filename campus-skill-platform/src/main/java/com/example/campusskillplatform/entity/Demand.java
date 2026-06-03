package com.example.campusskillplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("demand")
public class Demand {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String category;
    private String tags;
    private String description;
    private BigDecimal expectedPrice;
    private Integer expectedDuration;
    private String expectedLocation;
    private String contactPhone;

    /**
     * 需求状态（与技能状态完全对称）：
     * 0-待审核（新发布），1-审核通过（已上架/可展示），2-审核驳回，3-已下架
     */
    private Integer status = 0;
    private String auditRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}