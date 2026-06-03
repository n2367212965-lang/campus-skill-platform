package com.example.campusskillplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.campusskillplatform.entity.Skill;

import java.util.List;

public interface SkillService extends IService<Skill> {
    Long publishSkill(Skill skill, Long userId);
    List<Skill> getSkillsByCategory(String category);
    List<Skill> searchSkills(String keyword);
    List<Skill> getSkillsByUserId(Long userId);
    boolean updateSkillStatus(Long skillId, Integer status, String auditRemark);

    // 审核（与 DemandService 对称）
    boolean auditSkill(Long skillId, Integer status, String auditRemark);
    List<Skill> getPendingSkills();

    // 上下架
    boolean offlineSkill(Long skillId, Long userId);
    boolean onlineSkill(Long skillId, Long userId);
}