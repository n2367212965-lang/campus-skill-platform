package com.example.campusskillplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新数据传输对象
 * 包含可更新的字段：昵称、联系方式、邮箱、个性签名
 */
@Data
public class UserUpdateDTO {

    @Size(max = 20, message = "昵称长度不能超过20个字符")
    private String nickname;

    @Size(max = 20, message = "联系方式长度不能超过20个字符")
    private String contact;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 100, message = "个性签名长度不能超过100个字符")
    private String signature;
}
