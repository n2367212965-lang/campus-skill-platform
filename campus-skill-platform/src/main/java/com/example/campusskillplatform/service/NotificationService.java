package com.example.campusskillplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.campusskillplatform.entity.Notification;
import java.util.List;

public interface NotificationService extends IService<Notification> {
    void sendNotification(Long userId, String title, String content, String type, Long relatedId);
    List<Notification> getUserNotifications(Long userId);
    Long getUnreadCount(Long userId);
    boolean markAsRead(Long notificationId, Long userId);
    boolean markAllAsRead(Long userId);
}
