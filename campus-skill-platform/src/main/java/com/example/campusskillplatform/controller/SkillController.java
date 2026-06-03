package com.example.campusskillplatform.controller;

import com.example.campusskillplatform.common.Result;
import com.example.campusskillplatform.entity.OrderInfo;
import com.example.campusskillplatform.entity.Skill;
import com.example.campusskillplatform.service.OrderInfoService;
import com.example.campusskillplatform.service.SkillService;
import com.example.campusskillplatform.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 技能模块控制器
 * 处理技能发布、查询、搜索、上下架等接口
 */
@RestController
@RequestMapping("/api/skill")
public class SkillController {

    private static final Logger log = LoggerFactory.getLogger(SkillController.class);

    @Autowired
    private SkillService skillService;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private TokenUtil tokenUtil;

    /**
     * 发布新技能（需登录）
     * @param request HTTP请求（包含Token）
     * @param skill 技能信息（标题、分类、价格、描述等）
     * @return 新技能的ID
     */
    @PostMapping("/publish")
    public Result<Long> publishSkill(HttpServletRequest request,
                                     @RequestBody Skill skill) {
        // 身份校验：必须登录才能发布技能
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }

        // 参数校验：必填项和业务规则检查
        if (skill.getTitle() == null || skill.getTitle().trim().isEmpty()) {
            throw new RuntimeException("技能标题不能为空");
        }
        if (skill.getCategory() == null || skill.getCategory().trim().isEmpty()) {
            throw new RuntimeException("技能分类不能为空");
        }
        if (skill.getPrice() == null || skill.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("价格必须大于0");
        }
        if (skill.getDuration() != null && skill.getDuration() <= 0) {
            throw new RuntimeException("时长必须大于0");
        }

        Long skillId = skillService.publishSkill(skill, userId);
        if (skillId == null) {
            throw new RuntimeException("技能发布失败");
        }
        log.info("技能发布成功，技能ID：{}，用户ID：{}", skillId, userId);
        return Result.success(skillId);
    }

    /**
     * 根据分类获取技能列表
     * @param category 技能分类名称（如"IT技术"、"语言学习"等）
     * @return 该分类下的所有已审核通过的技能
     */
    @GetMapping("/category")
    public Result<List<Skill>> getSkillsByCategory(@RequestParam String category) {
        List<Skill> skills = skillService.getSkillsByCategory(category);
        return Result.success(skills);
    }

    /**
     * 搜索技能（按关键词模糊匹配标题和描述）
     * @param keyword 搜索关键词（可选，不传则返回全部）
     * @return 匹配的技能列表
     */
    @GetMapping("/search")
    public Result<List<Skill>> searchSkills(@RequestParam(required = false) String keyword) {
        List<Skill> skills = skillService.searchSkills(keyword);
        return Result.success(skills);
    }

    /**
     * 获取当前登录用户发布的所有技能
     * @param request HTTP请求（包含Token）
     * @return 当前用户发布的技能列表
     */
    @GetMapping("/my")
    public Result<List<Skill>> getMySkills(HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        List<Skill> skills = skillService.getSkillsByUserId(userId);
        return Result.success(skills);
    }

    /**
     * 获取技能详情
     * @param id 技能ID
     * @return 技能完整信息
     */
    @GetMapping("/{id}")
    public Result<Skill> getSkillDetail(@PathVariable Long id,
                                         HttpServletRequest request) {
        Skill skill = skillService.getById(id);
        if (skill == null) {
            throw new RuntimeException("技能不存在");
        }
        // 联系方式安全处理：仅发布方和关联订单方可查看联系电话
        Long userId = tokenUtil.getUserId(request);
        boolean isOwner = userId != null && userId.equals(skill.getUserId());
        boolean isRelated = false;
        if (userId != null) {
            List<OrderInfo> orders = orderInfoService.getOrdersBySkillId(id);
            isRelated = orders.stream().anyMatch(o ->
                    userId.equals(o.getPublisherId()) || userId.equals(o.getAcceptorId()));
        }
        if (!isOwner && !isRelated) {
            skill.setContactPhone(null);
        }
        return Result.success(skill);
    }

    /**
     * 下架技能（仅发布者可操作，状态变更用PUT）
     * @param request HTTP请求（包含Token）
     * @param skillId 要下架的技能ID
     * @return 操作结果提示
     */
    @PutMapping("/offline/{skillId}")
    public Result<String> offlineSkill(HttpServletRequest request, @PathVariable Long skillId) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        boolean success = skillService.offlineSkill(skillId, userId);
        if (!success) {
            throw new RuntimeException("技能下架失败");
        }
        log.info("技能下架成功 | 技能ID：{} | 用户ID：{}", skillId, userId);
        return Result.success("技能已下架");
    }

    /**
     * 上架技能（仅发布者可操作，状态变更用PUT）
     * @param request HTTP请求（包含Token）
     * @param skillId 要上架的技能ID
     * @return 操作结果提示
     */
    @PutMapping("/online/{skillId}")
    public Result<String> onlineSkill(HttpServletRequest request, @PathVariable Long skillId) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        boolean success = skillService.onlineSkill(skillId, userId);
        if (!success) {
            throw new RuntimeException("技能上架失败");
        }
        log.info("技能上架成功 | 技能ID：{} | 用户ID：{}", skillId, userId);
        return Result.success("技能已上架");
    }

    @PutMapping("/update/{skillId}")
    public Result<String> updateSkill(HttpServletRequest request,
                                       @PathVariable Long skillId,
                                       @RequestBody Skill skill) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        Skill existing = skillService.getById(skillId);
        if (existing == null) {
            throw new RuntimeException("技能不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此技能");
        }
        skill.setId(skillId);
        skill.setUserId(userId);
        Integer currentStatus = existing.getStatus();
        skill.setStatus(currentStatus);
        skillService.updateById(skill);
        log.info("技能更新成功 | 技能ID：{} | 用户ID：{}", skillId, userId);
        return Result.success("更新成功");
    }

    /**
     * 测试接口（开发调试用）
     * @return 服务状态信息
     */
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("技能服务正常");
    }
}
