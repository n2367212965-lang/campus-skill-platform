package com.example.campusskillplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.campusskillplatform.entity.Dispute;
import java.util.List;

public interface DisputeService extends IService<Dispute> {
    boolean createDispute(Long orderId, Long applicantId, Integer disputeType, String reason, String evidence);
    List<Dispute> getUserDisputes(Long userId);
    List<Dispute> getPendingDisputes();
    boolean handleDispute(Long disputeId, Long handlerId, String handleResult, Integer status);
}