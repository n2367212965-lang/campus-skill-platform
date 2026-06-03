package com.example.campusskillplatform.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 头像上传请求参数
 */
@Data
public class AvatarUploadDTO {
    private MultipartFile file;
}