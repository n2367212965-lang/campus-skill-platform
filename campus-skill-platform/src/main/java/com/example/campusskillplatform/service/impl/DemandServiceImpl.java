package com.example.campusskillplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.entity.Demand;
import com.example.campusskillplatform.entity.OrderInfo;
import com.example.campusskillplatform.mapper.DemandMapper;
import com.example.campusskillplatform.mapper.OrderInfoMapper;
import com.example.campusskillplatform.service.DemandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 需求服务实现
 * 与 SkillServiceImpl 完全对称
 */
@Service
public class DemandServiceImpl extends ServiceImpl<DemandMapper, Demand> implements DemandService {

    private static final Logger log = LoggerFactory.getLogger(DemandServiceImpl.class);

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishDemand(Demand demand, Long userId) {
        demand.setUserId(userId);
        demand.setStatus(Constants.DEMAND_STATUS_PENDING);
        boolean saved = this.save(demand);
        if (saved) {
            log.info("需求发布成功，需求ID：{}，用户ID：{}", demand.getId(), userId);
            return demand.getId();
        }
        return null;
    }

    @Override
    public List<Demand> getDemandList(Integer page, Integer size) {
        Page<Demand> pageInfo = new Page<>(page, size);
        QueryWrapper<Demand> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Constants.DEMAND_STATUS_APPROVED)
                .orderByDesc("create_time");
        Page<Demand> result = this.page(pageInfo, queryWrapper);
        return result.getRecords();
    }

    @Override
    public Demand getDemandById(Long id) {
        return this.getById(id);
    }

    @Override
    public List<Demand> getDemandsByUserId(Long userId) {
        QueryWrapper<Demand> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public List<Demand> searchDemands(String keyword, String category) {
        QueryWrapper<Demand> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Constants.DEMAND_STATUS_APPROVED);
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like("title", keyword)
                    .or()
                    .like("description", keyword)
                    .or()
                    .like("tags", keyword));
        }
        if (category != null && !category.trim().isEmpty()) {
            queryWrapper.eq("category", category);
        }
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    // ========== 上下架（与技能完全对称） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean offlineDemand(Long demandId, Long userId) {
        Demand demand = this.getById(demandId);
        if (demand == null) {
            throw new RuntimeException("需求不存在");
        }
        if (!demand.getUserId().equals(userId)) {
            throw new RuntimeException("无权下架他人需求");
        }
        if (demand.getStatus() != Constants.DEMAND_STATUS_APPROVED) {
            throw new RuntimeException("仅已上架的需求可下架");
        }
        boolean updated = updateStatus(demandId, Constants.DEMAND_STATUS_OFFLINE, "用户主动下架");
        if (updated) {
            log.info("需求下架成功，需求ID：{}，用户ID：{}", demandId, userId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean onlineDemand(Long demandId, Long userId) {
        Demand demand = this.getById(demandId);
        if (demand == null) {
            throw new RuntimeException("需求不存在");
        }
        if (!demand.getUserId().equals(userId)) {
            throw new RuntimeException("无权上架他人需求");
        }
        if (demand.getStatus() != Constants.DEMAND_STATUS_OFFLINE) {
            throw new RuntimeException("仅已下架的需求可上架");
        }
        boolean updated = updateStatus(demandId, Constants.DEMAND_STATUS_APPROVED, "用户主动上架");
        if (updated) {
            log.info("需求上架成功，需求ID：{}，用户ID：{}", demandId, userId);
        }
        return updated;
    }

    // ========== 审核 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditDemand(Long demandId, Integer status, String auditRemark) {
        Demand demand = this.getById(demandId);
        if (demand == null) {
            throw new RuntimeException("需求不存在");
        }
        if (demand.getStatus() != Constants.DEMAND_STATUS_PENDING) {
            throw new RuntimeException("只能审核待审核的需求");
        }
        if (status != Constants.DEMAND_STATUS_APPROVED && status != Constants.DEMAND_STATUS_REJECTED) {
            throw new RuntimeException("审核状态值不合法");
        }
        demand.setStatus(status);
        demand.setAuditRemark(auditRemark);
        boolean updated = this.updateById(demand);
        if (updated) {
            log.info("需求审核完成，需求ID：{}，审核结果：{}", demandId, status);
        }
        return updated;
    }

    @Override
    public List<Demand> getPendingDemands() {
        QueryWrapper<Demand> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Constants.DEMAND_STATUS_PENDING)
                .orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    // ========== 订单关联 ==========

    @Override
    public List<OrderInfo> getDemandOrders(Long demandId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("demand_id", demandId)
                .orderByDesc("create_time");
        return orderInfoMapper.selectList(queryWrapper);
    }

    // ========== 高级搜索 ==========

    @Override
    public List<Demand> searchDemandsAdvanced(String keyword, String category,
                                              BigDecimal minPrice, BigDecimal maxPrice,
                                              Integer status) {
        QueryWrapper<Demand> queryWrapper = new QueryWrapper<>();
        if (status != null) {
            queryWrapper.eq("status", status);
        } else {
            queryWrapper.eq("status", Constants.DEMAND_STATUS_APPROVED);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like("title", keyword)
                    .or()
                    .like("description", keyword)
                    .or()
                    .like("tags", keyword));
        }
        if (category != null && !category.trim().isEmpty()) {
            queryWrapper.eq("category", category);
        }
        if (minPrice != null) {
            queryWrapper.ge("expected_price", minPrice);
        }
        if (maxPrice != null) {
            queryWrapper.le("expected_price", maxPrice);
        }
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    // ========== 更新 ==========

    @Override
    public boolean updateDemand(Demand demand) {
        return this.updateById(demand);
    }

    // ========== 内部工具（与 SkillServiceImpl 对称） ==========

    private boolean updateStatus(Long demandId, Integer status, String auditRemark) {
        Demand demand = this.getById(demandId);
        if (demand == null) {
            return false;
        }
        demand.setStatus(status);
        if (auditRemark != null) {
            demand.setAuditRemark(auditRemark);
        }
        return this.updateById(demand);
    }
}
