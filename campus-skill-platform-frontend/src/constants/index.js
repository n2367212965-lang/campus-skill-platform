// 技能状态
export const SKILL_STATUS = {
  0: { text: "待审核", type: "warning" },
  1: { text: "审核通过", type: "success" },
  2: { text: "审核驳回", type: "danger" },
  3: { text: "已下架", type: "info" },
};

// 需求状态（与技能状态完全对称）
export const DEMAND_STATUS = {
  0: { text: "待审核", type: "warning" },
  1: { text: "审核通过", type: "success" },
  2: { text: "审核驳回", type: "danger" },
  3: { text: "已下架", type: "info" },
};

// 订单状态
export const ORDER_STATUS = {
  0: { text: "待支付", type: "warning" },
  1: { text: "资金托管", type: "primary" },
  2: { text: "服务中", type: "success" },
  3: { text: "待确认", type: "info" },
  4: { text: "已完成", type: "success" },
  5: { text: "已取消", type: "danger" },
};

// 售后类型
export const DISPUTE_TYPE = {
  1: { text: "订单取消", type: "warning" },
  2: { text: "服务质量", type: "danger" },
  3: { text: "支付问题", type: "info" },
};

// 售后状态
export const DISPUTE_STATUS = {
  0: { text: "待处理", type: "warning" },
  1: { text: "处理中", type: "primary" },
  2: { text: "已解决", type: "success" },
  3: { text: "已关闭", type: "info" },
};

// 用户角色
export const USER_ROLE = {
  0: "普通用户",
  1: "管理员",
};
