package com.example.campusskillplatform.common;

/**
 * 常量类：集中管理所有常量，消除硬编码
 */
public class Constants {

    // ========== 技能状态 ==========
    public static final Integer SKILL_STATUS_PENDING = 0;    // 待审核
    public static final Integer SKILL_STATUS_APPROVED = 1;   // 审核通过
    public static final Integer SKILL_STATUS_REJECTED = 2;   // 审核驳回
    public static final Integer SKILL_STATUS_OFFLINE = 3;    // 已下架

    // ========== 需求状态（与技能状态完全对称） ==========
    public static final Integer DEMAND_STATUS_PENDING = 0;   // 待审核
    public static final Integer DEMAND_STATUS_APPROVED = 1;  // 审核通过（已上架）
    public static final Integer DEMAND_STATUS_REJECTED = 2;  // 审核驳回
    public static final Integer DEMAND_STATUS_OFFLINE = 3;   // 已下架

    // ========== 订单状态 ==========
    public static final Integer ORDER_STATUS_PENDING = 0;    // 待支付
    public static final Integer ORDER_STATUS_PAID = 1;       // 资金托管
    public static final Integer ORDER_STATUS_SERVING = 2;    // 服务中
    public static final Integer ORDER_STATUS_CONFIRMING = 3; // 待确认（已完成）
    public static final Integer ORDER_STATUS_COMPLETED = 4;  // 已完成
    public static final Integer ORDER_STATUS_CANCELLED = 5;  // 已取消

    // ========== 用户状态 ==========
    public static final Integer USER_STATUS_NORMAL = 0;      // 正常
    public static final Integer USER_STATUS_DISABLED = 1;    // 禁用

    // ========== 用户角色 ==========
    public static final Integer USER_ROLE_STUDENT = 0;       // 学生
    public static final Integer USER_ROLE_ADMIN = 1;         // 管理员

    // ========== 评价类型 ==========
    public static final Integer RATING_TYPE_PUBLISHER_TO_ACCEPTOR = 1; // 发布方评价接单方
    public static final Integer RATING_TYPE_ACCEPTOR_TO_PUBLISHER = 2; // 接单方评价发布方

    // ========== 纠纷类型 ==========
    public static final Integer DISPUTE_TYPE_CANCEL = 1;     // 订单取消纠纷
    public static final Integer DISPUTE_TYPE_SERVICE = 2;    // 服务质量纠纷
    public static final Integer DISPUTE_TYPE_PAYMENT = 3;    // 支付纠纷

    // ========== 纠纷状态 ==========
    public static final Integer DISPUTE_STATUS_PENDING = 0;  // 待处理
    public static final Integer DISPUTE_STATUS_HANDLING = 1; // 处理中
    public static final Integer DISPUTE_STATUS_RESOLVED = 2; // 已解决
    public static final Integer DISPUTE_STATUS_CLOSED = 3;   // 已关闭

    // ========== 默认值（保持原样） ==========
    public static final String DEFAULT_PAY_PASSWORD = "123456";
    public static final String DEFAULT_VERIFICATION_CODE = "123456";
    public static final Integer DEFAULT_CREDIT_SCORE = 100;

    // ========== 文件上传限制 ==========
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final String[] ALLOWED_IMAGE_TYPES = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};

    // ========== 分页默认值 ==========
    public static final Integer DEFAULT_PAGE = 1;
    public static final Integer DEFAULT_PAGE_SIZE = 10;
}