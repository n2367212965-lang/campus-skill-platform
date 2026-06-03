package com.example.campusskillplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.entity.Dispute;
import com.example.campusskillplatform.entity.OrderInfo;
import com.example.campusskillplatform.mapper.DisputeMapper;
import com.example.campusskillplatform.mapper.OrderInfoMapper;
import com.example.campusskillplatform.service.DisputeService;
import com.example.campusskillplatform.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisputeServiceImpl extends ServiceImpl<DisputeMapper, Dispute> implements DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeServiceImpl.class);

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createDispute(Long orderId, Long applicantId, Integer disputeType,
                                 String reason, String evidence) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 校验：仅订单双方（发布方或接单方）可发起售后申请
        if (!applicantId.equals(order.getPublisherId()) && !applicantId.equals(order.getAcceptorId())) {
            throw new RuntimeException("仅订单双方可发起售后申请");
        }

        // 校验订单状态：仅服务中、待确认、已完成的订单可发起售后
        if (order.getStatus() != Constants.ORDER_STATUS_SERVING &&
                order.getStatus() != Constants.ORDER_STATUS_CONFIRMING &&
                order.getStatus() != Constants.ORDER_STATUS_COMPLETED) {
            throw new RuntimeException("仅服务中、待确认、已完成的订单可发起售后申请");
        }

        // 校验是否已有进行中的售后
        QueryWrapper<Dispute> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId)
                .in("status", Constants.DISPUTE_STATUS_PENDING, Constants.DISPUTE_STATUS_HANDLING);
        Long existingDisputeCount = this.count(queryWrapper);
        if (existingDisputeCount > 0) {
            throw new RuntimeException("该订单已有进行中的售后申请");
        }

        Dispute dispute = new Dispute();
        dispute.setOrderId(orderId);
        dispute.setApplicantId(applicantId);
        dispute.setDisputeType(disputeType);
        dispute.setReason(reason);
        dispute.setEvidence(evidence);
        dispute.setStatus(Constants.DISPUTE_STATUS_PENDING);
        boolean saved = this.save(dispute);
        if (saved) {
            log.info("售后创建成功，售后ID：{}，订单ID：{}，申请人：{}", dispute.getId(), orderId, applicantId);
            Long notifyUserId = applicantId.equals(order.getPublisherId()) ? order.getAcceptorId() : order.getPublisherId();
            notificationService.sendNotification(notifyUserId, "收到售后申请",
                    "订单" + order.getOrderNo() + "的对方发起了售后申请",
                    "DISPUTE_CREATED", dispute.getId());
        }
        return saved;
    }

    @Override
    public List<Dispute> getUserDisputes(Long userId) {
        // 【核心修正2】查询用户作为发布方/接单方的所有订单，再查询这些订单关联的所有售后
        // 1. 先查该用户参与的所有订单
        QueryWrapper<OrderInfo> orderQuery = new QueryWrapper<>();
        orderQuery.eq("publisher_id", userId).or().eq("acceptor_id", userId);
        List<OrderInfo> userOrders = orderInfoMapper.selectList(orderQuery);
        if (userOrders.isEmpty()) {
            return List.of();
        }

        // 2. 提取订单ID列表
        List<Long> orderIds = userOrders.stream().map(OrderInfo::getId).collect(Collectors.toList());

        // 3. 查询这些订单的所有售后记录
        QueryWrapper<Dispute> disputeQuery = new QueryWrapper<>();
        disputeQuery.in("order_id", orderIds).orderByDesc("create_time");
        return this.list(disputeQuery);
    }

    @Override
    public List<Dispute> getPendingDisputes() {
        QueryWrapper<Dispute> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("status", Constants.DISPUTE_STATUS_PENDING, Constants.DISPUTE_STATUS_HANDLING)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleDispute(Long disputeId, Long handlerId, String handleResult, Integer status) {
        Dispute dispute = this.getById(disputeId);
        if (dispute == null) {
            throw new RuntimeException("纠纷不存在");
        }

        if (dispute.getStatus().equals(Constants.DISPUTE_STATUS_RESOLVED) ||
                dispute.getStatus().equals(Constants.DISPUTE_STATUS_CLOSED)) {
            throw new RuntimeException("纠纷已处理完成");
        }

        dispute.setStatus(status);
        dispute.setHandlerId(handlerId);
        dispute.setHandleResult(handleResult);
        dispute.setHandleTime(LocalDateTime.now());
        boolean updated = this.updateById(dispute);
        if (updated) {
            log.info("纠纷处理完成，纠纷ID：{}，处理人：{}，状态：{}", disputeId, handlerId, status);
            notificationService.sendNotification(dispute.getApplicantId(), "售后已处理",
                    "你的售后申请已处理，处理结果：" + handleResult,
                    "DISPUTE_RESOLVED", disputeId);
        }
        return updated;
    }
}
