package com.example.campusskillplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.campusskillplatform.entity.OrderInfo;
import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {

    Long createSkillOrder(Long skillId, Long publisherId, Long acceptorId);
    Long createDemandOrder(Long demandId, Long publisherId, Long acceptorId);
    boolean payOrder(Long orderId, String password);
    boolean startService(Long orderId, Long acceptorId);
    boolean endService(Long orderId, Long acceptorId);
    boolean confirmComplete(Long orderId, Long publisherId);
    boolean cancelOrder(Long orderId, Long userId, String reason);
    List<OrderInfo> getOrdersByPublisherId(Long publisherId);
    List<OrderInfo> getOrdersByAcceptorId(Long acceptorId);
    List<OrderInfo> getOrdersBySkillId(Long skillId);
    OrderInfo getOrderDetail(Long orderId, Long userId);
}