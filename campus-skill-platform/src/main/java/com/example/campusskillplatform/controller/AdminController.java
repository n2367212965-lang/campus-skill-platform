package com.example.campusskillplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.common.Result;
import com.example.campusskillplatform.entity.Demand;
import com.example.campusskillplatform.entity.Skill;
import com.example.campusskillplatform.entity.User;
import com.example.campusskillplatform.service.DemandService;
import com.example.campusskillplatform.service.NotificationService;
import com.example.campusskillplatform.service.SkillService;
import com.example.campusskillplatform.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员后台控制器
 * 处理技能审核、需求审核、用户管理、数据统计等管理接口
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private SkillService skillService;

    @Autowired
    private UserService userService;

    @Autowired
    private DemandService demandService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取待审核技能列表
     */
    @GetMapping("/skill/pending")
    public Result<List<Skill>> getPendingSkills() {
        List<Skill> skills = skillService.getPendingSkills();
        return Result.success(skills);
    }

    /**
     * 获取待审核需求列表
     */
    @GetMapping("/demand/pending")
    public Result<List<Demand>> getPendingDemands() {
        List<Demand> demands = demandService.getPendingDemands();
        return Result.success(demands);
    }

    /**
     * 审核技能（通过或驳回）
     * @param skillId 技能ID
     * @param status 1=通过，2=驳回
     * @param remark 审核备注（可选）
     * @return 审核结果
     */
    @PostMapping("/skill/audit/{skillId}")
    public Result<String> auditSkill(@PathVariable Long skillId,
                                     @RequestParam Integer status,
                                     @RequestParam(required = false) String remark) {
        boolean success = skillService.auditSkill(skillId, status, remark);
        if (!success) {
            throw new RuntimeException("技能审核失败");
        }
        log.info("技能审核完成，技能ID：{}，审核结果：{}，备注：{}", skillId, status, remark);

        String message = status == Constants.SKILL_STATUS_APPROVED ? "审核通过" : "审核驳回";
        Skill skill = skillService.getById(skillId);
        if (skill != null) {
            notificationService.sendNotification(skill.getUserId(), "技能审核结果",
                    "你的技能「" + skill.getTitle() + "」" + message + (remark != null ? "，备注：" + remark : ""),
                    "SKILL_AUDITED", skillId);
        }
        return Result.success(message);
    }

    /**
     * 审核需求（通过或驳回）
     * @param demandId 需求ID
     * @param status 1=通过，2=驳回
     * @param remark 审核备注（可选）
     * @return 审核结果
     */
    @PostMapping("/demand/audit/{demandId}")
    public Result<String> auditDemand(@PathVariable Long demandId,
                                      @RequestParam Integer status,
                                      @RequestParam(required = false) String remark) {
        boolean success = demandService.auditDemand(demandId, status, remark);
        if (!success) {
            throw new RuntimeException("需求审核失败");
        }
        log.info("需求审核完成，需求ID：{}，审核结果：{}，备注：{}", demandId, status, remark);

        String message = status == Constants.DEMAND_STATUS_APPROVED ? "审核通过" : "审核驳回";
        Demand demand = demandService.getById(demandId);
        if (demand != null) {
            notificationService.sendNotification(demand.getUserId(), "需求审核结果",
                    "你的需求「" + demand.getTitle() + "」" + message + (remark != null ? "，备注：" + remark : ""),
                    "DEMAND_AUDITED", demandId);
        }
        return Result.success(message);
    }

    /**
     * 获取管理后台统计数据
     * @return 用户数、技能数、需求数等统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> data = new HashMap<>();

        Long userCount = userService.count();
        data.put("userCount", userCount);

        Long skillCount = skillService.count();
        data.put("skillCount", skillCount);

        Long pendingSkillCount = skillService.count(
                new QueryWrapper<Skill>().eq("status", Constants.SKILL_STATUS_PENDING));
        data.put("pendingSkillCount", pendingSkillCount);

        Long approvedSkillCount = skillService.count(
                new QueryWrapper<Skill>().eq("status", Constants.SKILL_STATUS_APPROVED));
        data.put("approvedSkillCount", approvedSkillCount);

        Long rejectedSkillCount = skillService.count(
                new QueryWrapper<Skill>().eq("status", Constants.SKILL_STATUS_REJECTED));
        data.put("rejectedSkillCount", rejectedSkillCount);

        Long offlineSkillCount = skillService.count(
                new QueryWrapper<Skill>().eq("status", Constants.SKILL_STATUS_OFFLINE));
        data.put("offlineSkillCount", offlineSkillCount);

        Long demandCount = demandService.count();
        data.put("demandCount", demandCount);

        Long pendingDemandCount = demandService.count(
                new QueryWrapper<Demand>().eq("status", Constants.DEMAND_STATUS_PENDING));
        data.put("pendingDemandCount", pendingDemandCount);

        Long approvedDemandCount = demandService.count(
                new QueryWrapper<Demand>().eq("status", Constants.DEMAND_STATUS_APPROVED));
        data.put("approvedDemandCount", approvedDemandCount);

        Long rejectedDemandCount = demandService.count(
                new QueryWrapper<Demand>().eq("status", Constants.DEMAND_STATUS_REJECTED));
        data.put("rejectedDemandCount", rejectedDemandCount);

        Long offlineDemandCount = demandService.count(
                new QueryWrapper<Demand>().eq("status", Constants.DEMAND_STATUS_OFFLINE));
        data.put("offlineDemandCount", offlineDemandCount);

        Long activeUserCount = userService.count(
                new QueryWrapper<User>().eq("status", Constants.USER_STATUS_NORMAL));
        data.put("activeUserCount", activeUserCount);

        return Result.success(data);
    }

    /**
     * 获取所有用户列表（密码已脱敏）
     */
    @GetMapping("/users")
    public Result<List<User>> getAllUsers() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        List<User> users = userService.list(queryWrapper);
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    /**
     * 更新用户状态（禁用或启用）
     * RESTful规范：状态更新操作使用PUT方法
     * @param userId 用户ID
     * @param status 0=正常，1=禁用
     * @return 操作结果
     */
    @PutMapping("/user/status/{userId}")
    public Result<String> updateUserStatus(@PathVariable Long userId,
                                           @RequestParam Integer status) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getRole() != null && user.getRole() == Constants.USER_ROLE_ADMIN) {
            throw new RuntimeException("不能禁用管理员账号");
        }
        if (status != Constants.USER_STATUS_NORMAL && status != Constants.USER_STATUS_DISABLED) {
            throw new RuntimeException("状态值不合法");
        }
        user.setStatus(status);
        userService.updateById(user);
        log.info("用户状态更新，用户ID：{}，新状态：{}", userId, status);
        String message = status == Constants.USER_STATUS_NORMAL ? "用户已启用" : "用户已禁用";
        return Result.success(message);
    }
}
