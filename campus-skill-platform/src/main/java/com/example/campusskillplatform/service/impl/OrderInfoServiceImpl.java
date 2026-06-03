package com.example.campusskillplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.entity.Demand;
import com.example.campusskillplatform.entity.OrderInfo;
import com.example.campusskillplatform.entity.Skill;
import com.example.campusskillplatform.entity.User;
import com.example.campusskillplatform.mapper.OrderInfoMapper;
import com.example.campusskillplatform.service.DemandService;
import com.example.campusskillplatform.service.NotificationService;
import com.example.campusskillplatform.service.OrderInfoService;
import com.example.campusskillplatform.service.SkillService;
import com.example.campusskillplatform.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    private static final Logger log = LoggerFactory.getLogger(OrderInfoServiceImpl.class);

    @Autowired
    private SkillService skillService;

    @Autowired
    private DemandService demandService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Value("${order.default-demand-amount}")
    private BigDecimal defaultDemandAmount;

    @Override
    public OrderInfo getOrderDetail(Long orderId, Long userId) {
        OrderInfo order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        User user = userService.getById(userId);
        boolean isAdmin = user != null && user.getRole() != null && user.getRole() == 1;

        if (!isAdmin && !userId.equals(order.getPublisherId()) && !userId.equals(order.getAcceptorId())) {
            throw new RuntimeException("无权查看此订单");
        }

        User publisher = userService.getById(order.getPublisherId());
        User acceptor = userService.getById(order.getAcceptorId());
        log.info("订单详情查询 | 订单ID：{} | 发布方：{}({}) | 接单方：{}({}) | 查询人：{}({})",
                orderId,
                order.getPublisherId(), publisher != null ? publisher.getNickname() : "未知",
                order.getAcceptorId(), acceptor != null ? acceptor.getNickname() : "未知",
                userId, isAdmin ? "管理员" : "普通用户");

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSkillOrder(Long skillId, Long publisherId, Long acceptorId) {
        if (publisherId == null || acceptorId == null || skillId == null) {
            throw new RuntimeException("参数不能为空");
        }
        if (publisherId.equals(acceptorId)) {
            throw new RuntimeException("不能接自己的技能单");
        }

        Skill skill = skillService.getById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能不存在");
        }
        if (skill.getStatus() != Constants.SKILL_STATUS_APPROVED) {
            throw new RuntimeException("技能未审核或已下架，无法创建订单");
        }
        if (!skill.getUserId().equals(publisherId)) {
            throw new RuntimeException("技能发布者ID与传入的发布方ID不一致");
        }

        QueryWrapper<OrderInfo> orderQuery = new QueryWrapper<>();
        orderQuery.eq("skill_id", skillId)
                .in("status", Constants.ORDER_STATUS_PENDING,
                        Constants.ORDER_STATUS_PAID,
                        Constants.ORDER_STATUS_SERVING,
                        Constants.ORDER_STATUS_CONFIRMING);
        Long existingOrderCount = this.count(orderQuery);
        if (existingOrderCount > 0) {
            throw new RuntimeException("该技能已有进行中的订单，无法重复接单");
        }

        String orderNo = generateOrderNo();
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setSkillId(skillId);
        order.setPublisherId(publisherId);
        order.setAcceptorId(acceptorId);
        order.setAmount(skill.getPrice());
        order.setStatus(Constants.ORDER_STATUS_PENDING);
        order.setPayPassword(null);

        boolean saved = this.save(order);
        if (!saved) {
            throw new RuntimeException("创建订单失败");
        }

        skillService.updateSkillStatus(skillId, Constants.SKILL_STATUS_OFFLINE, "接单后自动下架");
        log.info("技能接单后自动下架 | 技能ID：{} | 订单ID：{}", skillId, order.getId());

        log.info("技能订单创建成功 | 订单ID：{} | 技能ID：{} | 发布方：{} | 接单方：{}",
                order.getId(), skillId, publisherId, acceptorId);

        User acceptor = userService.getById(acceptorId);
        String acceptorName = acceptor != null ? acceptor.getNickname() : "用户";
        notificationService.sendNotification(publisherId, "有人预约了你的技能",
                acceptorName + "预约了你的技能「" + skill.getTitle() + "」",
                "ORDER_CREATED", order.getId());
        notificationService.sendNotification(acceptorId, "预约成功",
                "你成功预约了技能「" + skill.getTitle() + "」，请及时支付",
                "ORDER_CREATED", order.getId());

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDemandOrder(Long demandId, Long publisherId, Long acceptorId) {
        if (publisherId == null || acceptorId == null || demandId == null) {
            throw new RuntimeException("参数不能为空");
        }
        if (publisherId.equals(acceptorId)) {
            throw new RuntimeException("不能接自己发布的需求");
        }

        Demand demand = demandService.getById(demandId);
        if (demand == null) {
            throw new RuntimeException("需求不存在");
        }
        if (demand.getStatus() != Constants.DEMAND_STATUS_APPROVED) {
            throw new RuntimeException("需求未上架，无法创建订单");
        }

        // 检查是否已有进行中的订单（与技能对称）
        QueryWrapper<OrderInfo> existingQuery = new QueryWrapper<>();
        existingQuery.eq("demand_id", demandId)
                .in("status", Constants.ORDER_STATUS_PENDING,
                        Constants.ORDER_STATUS_PAID,
                        Constants.ORDER_STATUS_SERVING,
                        Constants.ORDER_STATUS_CONFIRMING);
        Long existingCount = this.count(existingQuery);
        if (existingCount > 0) {
            throw new RuntimeException("该需求已有进行中的订单，无法重复接单");
        }

        String orderNo = generateOrderNo();
        BigDecimal amount = demand.getExpectedPrice() != null &&
                demand.getExpectedPrice().compareTo(BigDecimal.ZERO) > 0
                ? demand.getExpectedPrice()
                : defaultDemandAmount;

        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setDemandId(demandId);
        order.setPublisherId(publisherId);
        order.setAcceptorId(acceptorId);
        order.setAmount(amount);
        order.setStatus(Constants.ORDER_STATUS_PENDING);
        order.setPayPassword(null);

        boolean saved = this.save(order);
        if (!saved) {
            throw new RuntimeException("创建需求订单失败");
        }

        // 接单后自动下架需求（与技能对称，走Service层）
        demand.setStatus(Constants.DEMAND_STATUS_OFFLINE);
        demandService.updateById(demand);
        log.info("需求接单后自动下架 | 需求ID：{} | 订单ID：{}", demandId, order.getId());

        log.info("需求订单创建成功 | 订单ID：{} | 需求ID：{} | 发布方：{} | 接单方：{}",
                order.getId(), demandId, publisherId, acceptorId);

        User acceptor2 = userService.getById(acceptorId);
        String acceptorName2 = acceptor2 != null ? acceptor2.getNickname() : "用户";
        notificationService.sendNotification(publisherId, "有人接了你的需求",
                acceptorName2 + "接了你的需求「" + demand.getTitle() + "」，请及时支付",
                "ORDER_CREATED", order.getId());
        notificationService.sendNotification(acceptorId, "接单成功",
                "你成功接了需求「" + demand.getTitle() + "」，等待发布方支付",
                "ORDER_CREATED", order.getId());

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(Long orderId, String password) {
        if (orderId == null || password == null || password.trim().isEmpty()) {
            throw new RuntimeException("订单ID和支付密码不能为空");
        }

        OrderInfo order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() != Constants.ORDER_STATUS_PENDING) {
            throw new RuntimeException(String.format(
                    "订单状态不正确（当前状态：%d），仅待支付状态可支付",
                    order.getStatus()));
        }

        if (!Constants.DEFAULT_PAY_PASSWORD.equals(password)) {
            log.warn("支付密码错误 | 订单ID：{} | 输入密码：{}", orderId, password);
            throw new RuntimeException("支付密码错误");
        }

        order.setStatus(Constants.ORDER_STATUS_PAID);
        order.setPayPassword(password);

        boolean updated = this.updateById(order);
        if (updated) {
            log.info("订单支付成功 | 订单ID：{} | 金额：{}", orderId, order.getAmount());
            Long notifyTarget = (order.getSkillId() != null)
                    ? order.getPublisherId() : order.getAcceptorId();
            notificationService.sendNotification(notifyTarget, "订单已支付",
                    "订单" + order.getOrderNo() + "已支付，你可以开始服务了",
                    "ORDER_PAID", orderId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startService(Long orderId, Long operatorId) {
        if (orderId == null || operatorId == null) {
            throw new RuntimeException("订单ID和操作者ID不能为空");
        }

        OrderInfo order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        boolean isProvider = (order.getSkillId() != null)
                ? operatorId.equals(order.getPublisherId())
                : operatorId.equals(order.getAcceptorId());
        if (!isProvider) {
            throw new RuntimeException("无权操作：仅服务提供方可启动服务");
        }

        if (order.getStatus() != Constants.ORDER_STATUS_PAID) {
            throw new RuntimeException(String.format(
                    "订单状态不正确（当前状态：%d），仅已支付状态可启动服务",
                    order.getStatus()));
        }

        order.setStatus(Constants.ORDER_STATUS_SERVING);
        order.setStartTime(java.time.LocalDateTime.now());
        boolean updated = this.updateById(order);

        if (updated) {
            log.info("服务启动成功 | 订单ID：{} | 操作者ID：{}", orderId, operatorId);
            Long notifyTarget = (order.getSkillId() != null)
                    ? order.getAcceptorId() : order.getPublisherId();
            notificationService.sendNotification(notifyTarget, "服务已开始",
                    "订单" + order.getOrderNo() + "的服务已开始",
                    "ORDER_STARTED", orderId);
        } else {
            log.error("服务启动失败 | 订单ID：{} | 操作者ID：{}", orderId, operatorId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean endService(Long orderId, Long operatorId) {
        if (orderId == null || operatorId == null) {
            throw new RuntimeException("订单ID和操作者ID不能为空");
        }

        OrderInfo order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        boolean isProvider = (order.getSkillId() != null)
                ? operatorId.equals(order.getPublisherId())
                : operatorId.equals(order.getAcceptorId());
        if (!isProvider) {
            throw new RuntimeException("无权操作：仅服务提供方可结束服务");
        }

        if (order.getStatus() != Constants.ORDER_STATUS_SERVING) {
            throw new RuntimeException(String.format(
                    "订单状态不正确（当前状态：%d），仅服务中状态可结束服务",
                    order.getStatus()));
        }

        order.setStatus(Constants.ORDER_STATUS_CONFIRMING);
        order.setEndTime(java.time.LocalDateTime.now());
        boolean updated = this.updateById(order);

        if (updated) {
            log.info("服务结束，等待确认完成 | 订单ID：{} | 操作者ID：{}", orderId, operatorId);
            Long notifyTarget2 = (order.getSkillId() != null)
                    ? order.getAcceptorId() : order.getPublisherId();
            notificationService.sendNotification(notifyTarget2, "服务已完成，请确认",
                    "订单" + order.getOrderNo() + "的服务已完成，请确认并完成订单",
                    "ORDER_ENDED", orderId);
        } else {
            log.error("服务结束失败 | 订单ID：{} | 操作者ID：{}", orderId, operatorId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmComplete(Long orderId, Long operatorId) {
        if (orderId == null || operatorId == null) {
            throw new RuntimeException("订单ID和操作者ID不能为空");
        }

        OrderInfo order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        boolean isConsumer = (order.getSkillId() != null)
                ? operatorId.equals(order.getAcceptorId())
                : operatorId.equals(order.getPublisherId());
        if (!isConsumer) {
            throw new RuntimeException("无权操作：仅服务购买方可确认完成");
        }

        if (order.getStatus() != Constants.ORDER_STATUS_CONFIRMING) {
            throw new RuntimeException(String.format(
                    "订单状态不正确（当前状态：%d），仅待确认状态可确认完成",
                    order.getStatus()));
        }

        order.setStatus(Constants.ORDER_STATUS_COMPLETED);
        boolean updated = this.updateById(order);

        if (updated) {
            log.info("订单确认完成成功 | 订单ID：{} | 操作者ID：{}", orderId, operatorId);
            Long notifyTarget3 = (order.getSkillId() != null)
                    ? order.getPublisherId() : order.getAcceptorId();
            notificationService.sendNotification(notifyTarget3, "订单已完成",
                    "订单" + order.getOrderNo() + "已被确认完成",
                    "ORDER_COMPLETED", orderId);
        } else {
            log.error("订单确认完成失败 | 订单ID：{} | 操作者ID：{}", orderId, operatorId);
        }
        return updated;
    }

    /**
     * 取消订单权限和状态限制
     * 1. 订单双方均可取消订单
     * 2. 仅待支付(0)、资金托管(1)的订单可取消，服务中及之后的订单必须走售后
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId, String reason) {
        if (orderId == null || userId == null) {
            throw new RuntimeException("订单ID和用户ID不能为空");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("取消原因不能为空");
        }

        OrderInfo order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 权限校验：仅订单双方可取消订单
        boolean isPublisher = userId.equals(order.getPublisherId());
        boolean isAcceptor = userId.equals(order.getAcceptorId());
        if (!isPublisher && !isAcceptor) {
            throw new RuntimeException("无权操作：仅订单双方可取消订单");
        }

        // 接单方仅能在服务开始前取消（待支付、资金托管状态）
        if (isAcceptor && order.getStatus() != Constants.ORDER_STATUS_PENDING && order.getStatus() != Constants.ORDER_STATUS_PAID) {
            throw new RuntimeException("接单方仅可在服务开始前取消订单，服务中请发起售后申请");
        }

        // 发布方在服务开始后也不能直接取消，需走售后
        if (isPublisher && order.getStatus() != Constants.ORDER_STATUS_PENDING && order.getStatus() != Constants.ORDER_STATUS_PAID) {
            throw new RuntimeException("服务已开始，无法直接取消订单，请发起售后申请");
        }

        // 订单取消后，技能恢复上架（仅下架状态可恢复）
        if (order.getSkillId() != null) {
            Skill skill = skillService.getById(order.getSkillId());
            if (skill != null && skill.getStatus() == Constants.SKILL_STATUS_OFFLINE) {
                skillService.updateSkillStatus(order.getSkillId(), Constants.SKILL_STATUS_APPROVED, "订单取消后恢复上架");
                log.info("技能订单取消后恢复上架 | 技能ID：{} | 订单ID：{}", order.getSkillId(), orderId);
            }
        }

        // 订单取消后，需求恢复上架（与技能对称）
        if (order.getDemandId() != null) {
            Demand cancelDemand = demandService.getById(order.getDemandId());
            if (cancelDemand != null && cancelDemand.getStatus() == Constants.DEMAND_STATUS_OFFLINE) {
                cancelDemand.setStatus(Constants.DEMAND_STATUS_APPROVED);
                demandService.updateById(cancelDemand);
                log.info("需求订单取消后恢复上架 | 需求ID：{} | 订单ID：{}", order.getDemandId(), orderId);
            }
        }

        order.setStatus(Constants.ORDER_STATUS_CANCELLED);
        order.setCancelReason(reason);
        boolean updated = this.updateById(order);

        if (updated) {
            log.info("订单取消成功 | 订单ID：{} | 操作人ID：{} | 取消原因：{}", orderId, userId, reason);
            Long notifyUserId = isPublisher ? order.getAcceptorId() : order.getPublisherId();
            notificationService.sendNotification(notifyUserId, "订单已取消",
                    "订单" + order.getOrderNo() + "已被取消，原因：" + reason,
                    "ORDER_CANCELLED", orderId);
        } else {
            log.error("订单取消失败 | 订单ID：{} | 操作人ID：{} | 取消原因：{}", orderId, userId, reason);
        }
        return updated;
    }

    @Override
    public List<OrderInfo> getOrdersByPublisherId(Long publisherId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("publisher_id", publisherId)
                .orderByDesc("create_time");
        List<OrderInfo> orders = this.list(queryWrapper);
        log.info("【查询发布方订单】publisherId：{} | 订单数量：{}", publisherId, orders.size());
        return orders;
    }

    @Override
    public List<OrderInfo> getOrdersByAcceptorId(Long acceptorId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("acceptor_id", acceptorId)
                .orderByDesc("create_time");
        List<OrderInfo> orders = this.list(queryWrapper);
        log.info("【查询接单方订单】acceptorId：{} | 订单数量：{}", acceptorId, orders.size());
        return orders;
    }

    @Override
    public List<OrderInfo> getOrdersBySkillId(Long skillId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("skill_id", skillId)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    private String generateOrderNo() {
        String timePart = String.valueOf(System.currentTimeMillis());
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "ORD" + timePart + randomPart;
    }
}
