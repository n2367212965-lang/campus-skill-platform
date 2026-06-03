package com.example.campusskillplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.entity.Skill;
import com.example.campusskillplatform.mapper.SkillMapper;
import com.example.campusskillplatform.service.SkillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 技能服务实现
 * 与 DemandServiceImpl 完全对称
 */
@Service
public class SkillServiceImpl extends ServiceImpl<SkillMapper, Skill> implements SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishSkill(Skill skill, Long userId) {
        skill.setUserId(userId);
        skill.setStatus(Constants.SKILL_STATUS_PENDING);
        boolean success = this.save(skill);
        if (success) {
            log.info("技能发布成功，技能ID：{}，用户ID：{}", skill.getId(), userId);
            return skill.getId();
        }
        return null;
    }

    @Override
    public List<Skill> getSkillsByCategory(String category) {
        QueryWrapper<Skill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category", category)
                .eq("status", Constants.SKILL_STATUS_APPROVED)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public List<Skill> searchSkills(String keyword) {
        QueryWrapper<Skill> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like("title", keyword)
                    .or()
                    .like("description", keyword)
                    .or()
                    .like("tags", keyword));
        }
        queryWrapper.eq("status", Constants.SKILL_STATUS_APPROVED)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public List<Skill> getSkillsByUserId(Long userId) {
        QueryWrapper<Skill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    // ========== 审核（与 Demand 对称，含状态机验证） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSkillStatus(Long skillId, Integer status, String auditRemark) {
        Skill skill = this.getById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能不存在");
        }
        skill.setStatus(status);
        if (auditRemark != null) {
            skill.setAuditRemark(auditRemark);
        }
        boolean updated = this.updateById(skill);
        if (updated) {
            log.info("技能状态更新，技能ID：{}，新状态：{}", skillId, status);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditSkill(Long skillId, Integer status, String auditRemark) {
        Skill skill = this.getById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能不存在");
        }
        if (skill.getStatus() != Constants.SKILL_STATUS_PENDING) {
            throw new RuntimeException("只能审核待审核状态的技能");
        }
        if (status != Constants.SKILL_STATUS_APPROVED && status != Constants.SKILL_STATUS_REJECTED) {
            throw new RuntimeException("审核状态值不合法");
        }
        skill.setStatus(status);
        skill.setAuditRemark(auditRemark);
        boolean updated = this.updateById(skill);
        if (updated) {
            log.info("技能审核完成，技能ID：{}，审核结果：{}", skillId, status);
        }
        return updated;
    }

    @Override
    public List<Skill> getPendingSkills() {
        QueryWrapper<Skill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Constants.SKILL_STATUS_PENDING)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    // ========== 上下架（与需求完全对称） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean offlineSkill(Long skillId, Long userId) {
        Skill skill = this.getById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能不存在");
        }
        if (!skill.getUserId().equals(userId)) {
            throw new RuntimeException("无权下架他人技能");
        }
        if (skill.getStatus() != Constants.SKILL_STATUS_APPROVED) {
            throw new RuntimeException("仅已上架的技能可下架");
        }
        boolean updated = updateSkillStatus(skillId, Constants.SKILL_STATUS_OFFLINE, "用户主动下架");
        if (updated) {
            log.info("技能下架成功，技能ID：{}，用户ID：{}", skillId, userId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean onlineSkill(Long skillId, Long userId) {
        Skill skill = this.getById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能不存在");
        }
        if (!skill.getUserId().equals(userId)) {
            throw new RuntimeException("无权上架他人技能");
        }
        if (skill.getStatus() != Constants.SKILL_STATUS_OFFLINE) {
            throw new RuntimeException("仅已下架的技能可上架");
        }
        boolean updated = updateSkillStatus(skillId, Constants.SKILL_STATUS_APPROVED, "用户主动上架");
        if (updated) {
            log.info("技能上架成功，技能ID：{}，用户ID：{}", skillId, userId);
        }
        return updated;
    }
}
