package com.example.campusskillplatform.controller;

import com.example.campusskillplatform.common.Result;
import com.example.campusskillplatform.entity.Demand;
import com.example.campusskillplatform.entity.OrderInfo;
import com.example.campusskillplatform.entity.Skill;
import com.example.campusskillplatform.service.DemandService;
import com.example.campusskillplatform.service.OrderInfoService;
import com.example.campusskillplatform.service.SkillService;
import com.example.campusskillplatform.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单模块控制器
 * 处理订单创建、支付、服务流转、取消等全生命周期接口
 */
@RestController
@RequestMapping("/api/order")
public class OrderInfoController {

    private static final Logger log = LoggerFactory.getLogger(OrderInfoController.class);

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private SkillService skillService;

    @Autowired
    private DemandService demandService;

    /**
     * 创建技能订单（接技能单）
     * @param request HTTP请求（包含Token）
     * @param skillId 技能ID
     * @param acceptorId 接单方ID（必须与当前登录用户一致）
     * @return 新订单ID
     */
    @PostMapping("/create-skill")
    public Result<Long> createSkillOrder(HttpServletRequest request,
                                         @RequestParam Long skillId,
                                         @RequestParam Long acceptorId) {
        Long loginUserId = tokenUtil.getUserId(request);
        if (loginUserId == null) {
            throw new RuntimeException("未登录");
        }
        if (!loginUserId.equals(acceptorId)) {
            throw new RuntimeException("无权替他人接单");
        }

        Skill skill = skillService.getById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能不存在");
        }
        Long publisherId = skill.getUserId();
        if (publisherId == null) {
            throw new RuntimeException("技能发布者ID为空");
        }
        if (publisherId.equals(acceptorId)) {
            throw new RuntimeException("不能接自己发布的技能单");
        }

        log.info("创建技能订单，发布方：{}，接单方：{}，技能ID：{}", publisherId, acceptorId, skillId);
        Long orderId = orderInfoService.createSkillOrder(skillId, publisherId, acceptorId);
        return Result.success(orderId);
    }

    /**
     * 创建需求订单（接需求单，与创建技能订单对称）
     * @param request HTTP请求（包含Token）
     * @param demandId 需求ID
     * @param acceptorId 接单方ID（必须与当前登录用户一致）
     * @return 新订单ID
     */
    @PostMapping("/create-demand")
    public Result<Long> createDemandOrder(HttpServletRequest request,
                                          @RequestParam Long demandId,
                                          @RequestParam Long acceptorId) {
        Long loginUserId = tokenUtil.getUserId(request);
        if (loginUserId == null) {
            throw new RuntimeException("未登录");
        }
        if (!loginUserId.equals(acceptorId)) {
            throw new RuntimeException("无权替他人接单");
        }

        Demand demand = demandService.getById(demandId);
        if (demand == null) {
            throw new RuntimeException("需求不存在");
        }
        Long publisherId = demand.getUserId();
        if (publisherId == null) {
            throw new RuntimeException("需求发布者ID为空");
        }
        if (publisherId.equals(acceptorId)) {
            throw new RuntimeException("不能接自己发布的需求");
        }

        log.info("创建需求订单，发布方：{}，接单方：{}，需求ID：{}", publisherId, acceptorId, demandId);
        Long orderId = orderInfoService.createDemandOrder(demandId, publisherId, acceptorId);
        return Result.success(orderId);
    }

    /**
     * 支付订单（模拟资金托管）
     * @param request HTTP请求（包含Token）
     * @param orderId 订单ID
     * @param password 支付密码
     * @return 操作结果
     */
    @PostMapping("/pay")
    public Result<String> payOrder(HttpServletRequest request,
                                   @RequestParam Long orderId,
                                   @RequestParam String password) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("支付密码不能为空");
        }
        OrderInfo order = orderInfoService.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        boolean isPayer;
        if (order.getSkillId() != null) {
            isPayer = userId.equals(order.getAcceptorId());
        } else {
            isPayer = userId.equals(order.getPublisherId());
        }
        if (!isPayer) {
            throw new RuntimeException("无权支付此订单");
        }
        boolean success = orderInfoService.payOrder(orderId, password);
        if (!success) {
            throw new RuntimeException("支付失败");
        }
        log.info("订单支付成功，订单ID：{}，用户ID：{}", orderId, userId);
        return Result.success("支付成功");
    }

    /**
     * 开始服务（仅服务提供方可操作）
     * @param request HTTP请求（包含Token）
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PutMapping("/start")
    public Result<String> startService(HttpServletRequest request,
                                       @RequestParam Long orderId) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        OrderInfo order = orderInfoService.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        boolean isProvider;
        if (order.getSkillId() != null) {
            isProvider = userId.equals(order.getPublisherId());
        } else {
            isProvider = userId.equals(order.getAcceptorId());
        }
        if (!isProvider) {
            throw new RuntimeException("仅服务提供方可开始服务");
        }
        boolean success = orderInfoService.startService(orderId, userId);
        if (!success) {
            throw new RuntimeException("服务开始失败");
        }
        return Result.success("服务开始成功");
    }

    /**
     * 结束服务（仅服务提供方可操作）
     * @param request HTTP请求（包含Token）
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PutMapping("/end")
    public Result<String> endService(HttpServletRequest request,
                                     @RequestParam Long orderId) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        OrderInfo order = orderInfoService.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        boolean isProvider;
        if (order.getSkillId() != null) {
            isProvider = userId.equals(order.getPublisherId());
        } else {
            isProvider = userId.equals(order.getAcceptorId());
        }
        if (!isProvider) {
            throw new RuntimeException("仅服务提供方可结束服务");
        }
        boolean success = orderInfoService.endService(orderId, userId);
        if (!success) {
            throw new RuntimeException("结束服务失败");
        }
        return Result.success("服务已结束，等待确认完成");
    }

    /**
     * 确认完成（仅服务购买方可操作）
     * @param request HTTP请求（包含Token）
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PutMapping("/complete")
    public Result<String> confirmComplete(HttpServletRequest request,
                                          @RequestParam Long orderId) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        OrderInfo order = orderInfoService.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        boolean isConsumer;
        if (order.getSkillId() != null) {
            isConsumer = userId.equals(order.getAcceptorId());
        } else {
            isConsumer = userId.equals(order.getPublisherId());
        }
        if (!isConsumer) {
            throw new RuntimeException("仅服务购买方可确认完成");
        }
        boolean success = orderInfoService.confirmComplete(orderId, userId);
        if (!success) {
            throw new RuntimeException("订单完成确认失败");
        }
        return Result.success("订单完成确认成功");
    }

    /**
     * 取消订单（订单双方均可操作，需在服务开始前）
     * @param request HTTP请求（包含Token）
     * @param orderId 订单ID
     * @param reason 取消原因
     * @return 操作结果
     */
    @PutMapping("/cancel")
    public Result<String> cancelOrder(HttpServletRequest request,
                                      @RequestParam Long orderId,
                                      @RequestParam String reason) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("取消原因不能为空");
        }
        if (reason.length() > 200) {
            throw new RuntimeException("取消原因不能超过200字");
        }
        boolean success = orderInfoService.cancelOrder(orderId, userId, reason);
        if (!success) {
            throw new RuntimeException("订单取消失败");
        }
        log.info("订单取消成功，订单ID：{}，用户ID：{}", orderId, userId);
        return Result.success("订单取消成功");
    }

    /**
     * 获取当前用户作为发布方的订单列表
     * @param request HTTP请求（包含Token）
     * @return 订单列表
     */
    @GetMapping("/as-publisher")
    public Result<List<OrderInfo>> getOrdersAsPublisher(HttpServletRequest request) {
        Long publisherId = tokenUtil.getUserId(request);
        if (publisherId == null) {
            throw new RuntimeException("未登录");
        }
        List<OrderInfo> orders = orderInfoService.getOrdersByPublisherId(publisherId);
        log.info("查询发布方订单，用户ID：{}，数量：{}", publisherId, orders.size());
        return Result.success(orders);
    }

    /**
     * 获取当前用户作为接单方的订单列表
     * @param request HTTP请求（包含Token）
     * @return 订单列表
     */
    @GetMapping("/as-acceptor")
    public Result<List<OrderInfo>> getOrdersAsAcceptor(HttpServletRequest request) {
        Long acceptorId = tokenUtil.getUserId(request);
        if (acceptorId == null) {
            throw new RuntimeException("未登录");
        }
        List<OrderInfo> orders = orderInfoService.getOrdersByAcceptorId(acceptorId);
        log.info("查询接单方订单，用户ID：{}，数量：{}", acceptorId, orders.size());
        return Result.success(orders);
    }

    /**
     * 获取订单详情（仅订单双方和管理员可查看）
     * @param request HTTP请求（包含Token）
     * @param orderId 订单ID
     * @return 订单完整信息
     */
    @GetMapping("/detail/{orderId}")
    public Result<OrderInfo> getOrderDetail(HttpServletRequest request,
                                            @PathVariable Long orderId) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        OrderInfo order = orderInfoService.getOrderDetail(orderId, userId);
        return Result.success(order);
    }

    /**
     * 测试接口（开发调试用）
     * @return 服务状态信息
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("订单服务正常");
    }
}
