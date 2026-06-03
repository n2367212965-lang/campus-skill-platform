package com.example.campusskillplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录数据传输对象
 * 包含用户名和密码
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
