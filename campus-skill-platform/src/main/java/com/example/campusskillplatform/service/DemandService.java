package com.example.campusskillplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.campusskillplatform.entity.Demand;
import com.example.campusskillplatform.entity.OrderInfo;
import java.math.BigDecimal;
import java.util.List;

/**
 * 需求服务接口
 * 与 SkillService 完全对称：都有发布、查询、上下架、审核功能
 */
public interface DemandService extends IService<Demand> {
    Long publishDemand(Demand demand, Long userId);
    List<Demand> getDemandList(Integer page, Integer size);
    Demand getDemandById(Long id);
    List<Demand> getDemandsByUserId(Long userId);
    List<Demand> searchDemands(String keyword, String category);

    // 上下架（与技能对称）
    boolean offlineDemand(Long demandId, Long userId);
    boolean onlineDemand(Long demandId, Long userId);

    // 审核
    boolean auditDemand(Long demandId, Integer status, String auditRemark);
    List<Demand> getPendingDemands();

    // 订单关联
    List<OrderInfo> getDemandOrders(Long demandId);

    // 高级搜索
    List<Demand> searchDemandsAdvanced(String keyword, String category,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       Integer status);

    // 更新
    boolean updateDemand(Demand demand);
}
