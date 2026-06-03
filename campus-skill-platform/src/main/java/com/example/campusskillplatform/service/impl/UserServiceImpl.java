package com.example.campusskillplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.dto.UserUpdateDTO;
import com.example.campusskillplatform.entity.User;
import com.example.campusskillplatform.mapper.UserMapper;
import com.example.campusskillplatform.service.SmsService;
import com.example.campusskillplatform.service.UserService;
import com.example.campusskillplatform.utils.JwtUtil;
import com.example.campusskillplatform.utils.Md5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SmsService smsService;

    @Override
    public Long register(String username, String password, String phone) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User existingUser = baseMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        queryWrapper.clear();
        queryWrapper.eq("phone", phone);
        existingUser = baseMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            throw new RuntimeException("手机号已注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(Md5Util.md5(password));
        user.setPhone(phone);
        user.setNickname(username);
        user.setRole(Constants.USER_ROLE_STUDENT);
        user.setStatus(Constants.USER_STATUS_NORMAL);
        user.setCreditScore(Constants.DEFAULT_CREDIT_SCORE);
        // 删除校园认证相关字段

        int result = baseMapper.insert(user);
        if (result > 0) {
            log.info("用户注册成功，用户ID：{}，用户名：{}", user.getId(), username);
            return user.getId();
        }
        return null;
    }

    @Override
    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public String login(String username, String password) {
        User user = findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() == Constants.USER_STATUS_DISABLED) {
            throw new RuntimeException("用户已被禁用");
        }

        String encryptedPassword = Md5Util.md5(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user);
        log.info("用户登录成功，用户ID：{}，用户名：{}", user.getId(), username);
        return token;
    }

    @Override
    public User getById(Long userId) {
        return baseMapper.selectById(userId);
    }

    @Override
    public boolean updateUser(Long userId, UserUpdateDTO userUpdateDTO) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (userUpdateDTO.getNickname() != null && !userUpdateDTO.getNickname().trim().isEmpty()) {
            user.setNickname(userUpdateDTO.getNickname().trim());
        }

        if (userUpdateDTO.getContact() != null) {
            user.setContact(userUpdateDTO.getContact().trim());
        }

        if (userUpdateDTO.getEmail() != null) {
            user.setEmail(userUpdateDTO.getEmail().trim());
        }

        if (userUpdateDTO.getSignature() != null) {
            user.setSignature(userUpdateDTO.getSignature().trim());
        }

        int result = baseMapper.updateById(user);
        if (result > 0) {
            log.info("用户信息更新成功，用户ID：{}", userId);
        }
        return result > 0;
    }

    @Override
    public boolean updateAvatar(Long userId, String avatarUrl) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setAvatar(avatarUrl);
        int result = baseMapper.updateById(user);
        if (result > 0) {
            log.info("用户头像更新成功，用户ID：{}，头像URL：{}", userId, avatarUrl);
        }
        return result > 0;
    }

    @Override
    public void forgotPassword(String phone, String code, String newPassword) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        User user = baseMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new RuntimeException("该手机号未注册");
        }

        if (!smsService.verifyCode(phone, code)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        user.setPassword(Md5Util.md5(newPassword));
        int result = baseMapper.updateById(user);
        if (result > 0) {
            log.info("用户密码重置成功，用户ID：{}，手机号：{}", user.getId(), phone);
        } else {
            throw new RuntimeException("密码重置失败");
        }
    }
}
