package com.example.campusskillplatform.controller;

import com.example.campusskillplatform.common.Result;
import com.example.campusskillplatform.entity.Notification;
import com.example.campusskillplatform.service.NotificationService;
import com.example.campusskillplatform.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知模块控制器
 * 处理站内通知的查询、已读标记等接口
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TokenUtil tokenUtil;

    /**
     * 获取当前用户的全部通知
     * @param request HTTP请求（包含Token）
     * @return 通知列表
     */
    @GetMapping("/list")
    public Result<List<Notification>> getMyNotifications(HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        List<Notification> list = notificationService.getUserNotifications(userId);
        log.info("查询通知列表，用户ID：{}，数量：{}", userId, list.size());
        return Result.success(list);
    }

    /**
     * 获取当前用户的未读通知数量
     * @param request HTTP请求（包含Token）
     * @return 未读数量
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记单条通知为已读
     * @param id 通知ID
     * @param request HTTP请求（包含Token）
     * @return 操作结果
     */
    @PutMapping("/read/{id}")
    public Result<String> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        notificationService.markAsRead(id, userId);
        return Result.success("已标记为已读");
    }

    /**
     * 标记当前用户全部通知为已读
     * @param request HTTP请求（包含Token）
     * @return 操作结果
     */
    @PutMapping("/read-all")
    public Result<String> markAllAsRead(HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        notificationService.markAllAsRead(userId);
        return Result.success("已全部标记为已读");
    }
}
