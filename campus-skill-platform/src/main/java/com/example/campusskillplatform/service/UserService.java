package com.example.campusskillplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.campusskillplatform.dto.UserUpdateDTO;
import com.example.campusskillplatform.entity.User;

public interface UserService extends IService<User> {

    Long register(String username, String password, String phone);
    User findByUsername(String username);
    String login(String username, String password);
    User getById(Long userId);
    boolean updateUser(Long userId, UserUpdateDTO userUpdateDTO);
    boolean updateAvatar(Long userId, String avatarUrl);
    void forgotPassword(String phone, String code, String newPassword);
}