package com.example.campusskillplatform.controller;

import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.common.Result;
import com.example.campusskillplatform.entity.Demand;
import com.example.campusskillplatform.entity.OrderInfo;
import com.example.campusskillplatform.service.DemandService;
import com.example.campusskillplatform.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 需求模块控制器
 * 与 SkillController 完全对称：都有发布、查询、上下架、更新接口
 */
@RestController
@RequestMapping("/api/demand")
public class DemandController {

    private static final Logger log = LoggerFactory.getLogger(DemandController.class);

    @Autowired
    private DemandService demandService;

    @Autowired
    private TokenUtil tokenUtil;

    /**
     * 发布新需求（需登录）
     */
    @PostMapping("/publish")
    public Result<Long> publishDemand(HttpServletRequest request,
                                        @RequestBody Demand demand) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }

        if (demand.getTitle() == null || demand.getTitle().trim().isEmpty()) {
            throw new RuntimeException("需求标题不能为空");
        }
        if (demand.getCategory() == null || demand.getCategory().trim().isEmpty()) {
            throw new RuntimeException("需求分类不能为空");
        }
        if (demand.getDescription() == null || demand.getDescription().trim().isEmpty()) {
            throw new RuntimeException("需求描述不能为空");
        }
        if (demand.getExpectedPrice() != null && demand.getExpectedPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("期望价格不能为负数");
        }
        if (demand.getExpectedDuration() != null && demand.getExpectedDuration() <= 0) {
            throw new RuntimeException("期望时长必须大于0");
        }

        Long demandId = demandService.publishDemand(demand, userId);
        if (demandId == null) {
            throw new RuntimeException("需求发布失败");
        }
        log.info("需求发布成功，需求ID：{}，用户ID：{}", demandId, userId);
        return Result.success(demandId);
    }

    /**
     * 获取当前登录用户发布的所有需求
     */
    @GetMapping("/my")
    public Result<List<Demand>> getMyDemands(HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        List<Demand> demands = demandService.getDemandsByUserId(userId);
        return Result.success(demands);
    }

    /**
     * 分页获取已审核通过的需求列表（首页展示用）
     */
    @GetMapping("/list")
    public Result<List<Demand>> getDemandList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<Demand> demands = demandService.getDemandList(page, size);
        return Result.success(demands);
    }

    /**
     * 获取需求详情（含联系方式安全处理）
     */
    @GetMapping("/{id}")
    public Result<Demand> getDemandDetail(@PathVariable Long id,
                                          HttpServletRequest request) {
        Demand demand = demandService.getDemandById(id);
        if (demand == null) {
            throw new RuntimeException("需求不存在");
        }
        // 联系方式安全处理：仅发布方和关联订单方可查看联系电话
        Long userId = tokenUtil.getUserId(request);
        boolean isOwner = userId != null && userId.equals(demand.getUserId());
        boolean isRelated = false;
        if (userId != null) {
            List<OrderInfo> orders = demandService.getDemandOrders(demand.getId());
            isRelated = orders.stream().anyMatch(o ->
                    userId.equals(o.getPublisherId()) || userId.equals(o.getAcceptorId()));
        }
        if (!isOwner && !isRelated) {
            demand.setContactPhone(null);
        }
        return Result.success(demand);
    }

    /**
     * 下架需求（仅发布者可操作，与技能对称）
     * RESTful规范：状态变更操作使用PUT方法
     */
    @PutMapping("/offline/{id}")
    public Result<String> offlineDemand(@PathVariable Long id,
                                        HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        boolean success = demandService.offlineDemand(id, userId);
        if (!success) {
            throw new RuntimeException("需求下架失败");
        }
        log.info("需求下架成功，需求ID：{}，用户ID：{}", id, userId);
        return Result.success("需求已下架");
    }

    /**
     * 上架需求（仅发布者可操作，与技能对称）
     * RESTful规范：状态变更操作使用PUT方法
     */
    @PutMapping("/online/{id}")
    public Result<String> onlineDemand(@PathVariable Long id,
                                       HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        boolean success = demandService.onlineDemand(id, userId);
        if (!success) {
            throw new RuntimeException("需求上架失败");
        }
        log.info("需求上架成功，需求ID：{}，用户ID：{}", id, userId);
        return Result.success("需求已上架");
    }

    /**
     * 搜索需求（按关键词和分类模糊匹配）
     */
    @GetMapping("/search")
    public Result<List<Demand>> searchDemands(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        List<Demand> demands = demandService.searchDemands(keyword, category);
        return Result.success(demands);
    }

    /**
     * 高级搜索需求（多条件组合筛选）
     */
    @GetMapping("/search/advanced")
    public Result<List<Demand>> searchDemandsAdvanced(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer status) {
        List<Demand> demands = demandService.searchDemandsAdvanced(
                keyword, category, minPrice, maxPrice, status);
        return Result.success(demands);
    }

    /**
     * 更新需求信息（仅发布者可操作，保留当前状态不变）
     */
    @PutMapping("/update/{id}")
    public Result<String> updateDemand(HttpServletRequest request,
                                       @PathVariable Long id,
                                       @RequestBody Demand demand) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        Demand existing = demandService.getDemandById(id);
        if (existing == null) {
            throw new RuntimeException("需求不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此需求");
        }
        demand.setId(id);
        demand.setUserId(userId);
        demand.setStatus(existing.getStatus());
        demandService.updateDemand(demand);
        log.info("需求更新成功，需求ID：{}，用户ID：{}", id, userId);
        return Result.success("更新成功");
    }
}
