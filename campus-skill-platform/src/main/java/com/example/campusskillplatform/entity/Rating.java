package com.example.campusskillplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rating")
public class Rating {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long fromUserId;
    private Long toUserId;

    private Integer score;          // 评分（1-5分）
    private String content;
    private String reply;

    private Integer ratingType;     // 评价类型：1-发布方评价接单方，2-接单方评价发布方

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}