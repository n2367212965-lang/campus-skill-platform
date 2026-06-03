package com.example.campusskillplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String phone;
    private String nickname;
    private String avatar;
    private String email;
    private String signature;
    private String contact;

    private Integer role;           // 角色：0-学生，1-管理员
    private Integer status;         // 状态：0-正常，1-禁用
    private Integer creditScore;    // 信誉分（默认100）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}