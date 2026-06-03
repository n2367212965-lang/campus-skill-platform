package com.example.campusskillplatform.controller;

import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.common.Result;
import com.example.campusskillplatform.entity.Dispute;
import com.example.campusskillplatform.entity.User;
import com.example.campusskillplatform.service.DisputeService;
import com.example.campusskillplatform.service.UserService;
import com.example.campusskillplatform.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 售后纠纷模块控制器
 * 处理售后申请、查询、处理等接口
 */
@RestController
@RequestMapping("/api/dispute")
public class DisputeController {

    private static final Logger log = LoggerFactory.getLogger(DisputeController.class);

    @Autowired
    private DisputeService disputeService;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenUtil tokenUtil;

    /**
     * 发起售后申请（订单双方均可发起）
     */
    @PostMapping("/create")
    public Result<String> createDispute(HttpServletRequest request,
                                        @RequestParam Long orderId,
                                        @RequestParam Integer disputeType,
                                        @RequestParam String reason,
                                        @RequestParam(required = false) String evidence) {
        Long applicantId = tokenUtil.getUserId(request);
        if (applicantId == null) {
            throw new RuntimeException("未登录");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("纠纷原因不能为空");
        }
        if (disputeType < Constants.DISPUTE_TYPE_CANCEL || disputeType > Constants.DISPUTE_TYPE_PAYMENT) {
            throw new RuntimeException("纠纷类型不合法");
        }
        boolean success = disputeService.createDispute(orderId, applicantId, disputeType, reason, evidence);
        if (!success) {
            throw new RuntimeException("售后申请提交失败");
        }
        log.info("售后申请提交成功，订单ID：{}，申请人：{}", orderId, applicantId);
        return Result.success("售后申请提交成功");
    }

    /**
     * 【核心修正】查询当前用户作为订单双方的所有售后记录
     */
    @GetMapping("/my")
    public Result<List<Dispute>> getMyDisputes(HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        List<Dispute> disputes = disputeService.getUserDisputes(userId);
        return Result.success(disputes);
    }

    @GetMapping("/pending")
    public Result<List<Dispute>> getPendingDisputes(HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        User user = userService.getById(userId);
        if (user == null || !user.getRole().equals(Constants.USER_ROLE_ADMIN)) {
            throw new RuntimeException("无权访问，需要管理员权限");
        }
        List<Dispute> disputes = disputeService.getPendingDisputes();
        return Result.success(disputes);
    }

    @PostMapping("/handle/{disputeId}")
    public Result<String> handleDispute(HttpServletRequest request,
                                        @PathVariable Long disputeId,
                                        @RequestParam String handleResult,
                                        @RequestParam Integer status) {
        Long handlerId = tokenUtil.getUserId(request);
        if (handlerId == null) {
            throw new RuntimeException("未登录");
        }
        User user = userService.getById(handlerId);
        if (user == null || !user.getRole().equals(Constants.USER_ROLE_ADMIN)) {
            throw new RuntimeException("无权访问，需要管理员权限");
        }
        if (handleResult == null || handleResult.trim().isEmpty()) {
            throw new RuntimeException("处理结果不能为空");
        }
        if (status < Constants.DISPUTE_STATUS_RESOLVED || status > Constants.DISPUTE_STATUS_CLOSED) {
            throw new RuntimeException("处理状态不合法");
        }
        boolean success = disputeService.handleDispute(disputeId, handlerId, handleResult, status);
        if (!success) {
            throw new RuntimeException("纠纷处理失败");
        }
        log.info("纠纷处理完成，纠纷ID：{}，处理人：{}", disputeId, handlerId);
        return Result.success("纠纷处理成功");
    }
}
