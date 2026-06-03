<template>
  <div class="order-detail-page">
    <!-- 主卡片 -->
    <el-card class="main-card">
      <!-- 头部：标题+返回 -->
      <template #header>
        <div class="header-wrap">
          <h2 class="page-title">订单详情</h2>
          <el-button size="small" type="primary" @click="goBack">
            返回订单列表
          </el-button>
        </div>
      </template>

      <!-- 加载状态 -->
      <el-skeleton v-if="loading" :rows="6" animated style="margin: 20px" />

      <!-- 空状态 -->
      <el-empty
        v-else-if="!orderDetail"
        description="未查询到该订单信息"
        style="padding: 40px 0"
      />

      <!-- 订单主体 -->
      <div v-else class="detail-content">
        <!-- 1. 订单概览（主流平台顶部核心区） -->
        <div class="order-overview">
          <div class="overview-left">
            <div class="order-no">
              <span class="label">订单编号：</span>
              <span class="value">{{ orderDetail.orderNo }}</span>
            </div>
            <div class="order-time">
              <span class="label">创建时间：</span>
              <span class="value">{{
                formatTime(orderDetail.createTime)
              }}</span>
            </div>
          </div>
          <div class="overview-right">
            <el-tag :type="ORDER_STATUS[orderDetail.status]?.type" size="large">
              {{ ORDER_STATUS[orderDetail.status]?.text }}
            </el-tag>
            <div class="order-amount">
              <span class="label">订单金额：</span>
              <span class="value">¥{{ orderDetail.amount }}</span>
            </div>
          </div>
        </div>

        <!-- 2. 关联服务详情（核心：直接显示服务信息，而非ID） -->
        <div class="section-title">服务详情</div>
        <div class="service-card" v-if="relatedService">
          <div class="service-header">
            <span class="service-type" v-if="relatedService.type === 'skill'">
              技能服务
            </span>
            <span class="service-type" v-if="relatedService.type === 'demand'">
              需求服务
            </span>
            <span class="service-price">¥{{ relatedService.price }}</span>
          </div>
          <div class="service-body">
            <h3 class="service-title">{{ relatedService.title }}</h3>
            <p class="service-desc">
              {{ relatedService.description || "暂无描述" }}
            </p>
            <div class="service-meta" v-if="relatedService.type === 'skill'">
              <span>服务时长：{{ relatedService.duration }}小时</span>
              <span>服务分类：{{ relatedService.category }}</span>
            </div>
            <div class="service-meta" v-if="relatedService.type === 'demand'">
              <span>期望时长：{{ relatedService.expectedDuration }}小时</span>
              <span>服务分类：{{ relatedService.category }}</span>
            </div>
          </div>
        </div>
        <el-empty
          v-else
          description="暂无关联服务信息"
          style="padding: 20px 0"
        />

        <!-- 3. 交易信息（主流平台的交易模块） -->
        <div class="section-title">交易信息</div>
        <el-descriptions :column="2" border class="desc-list">
          <el-descriptions-item label="交易类型">
            {{ orderDetail.skillId ? "技能服务" : "需求服务" }}
          </el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag type="success" v-if="orderDetail.status !== 0"
              >已支付</el-tag
            >
            <el-tag type="warning" v-else>待支付</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发布方">
            {{ publisherName || "未知用户" }}
            <span v-if="publisherPhone" style="color:#999;margin-left:8px">{{ publisherPhone }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="接单方">
            {{ acceptorName || "未知用户" }}
            <span v-if="acceptorPhone" style="color:#999;margin-left:8px">{{ acceptorPhone }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatTime(orderDetail.updateTime) }}
          </el-descriptions-item>
          <el-descriptions-item
          label="取消原因"
          v-if="orderDetail.status === 5"
        >
          {{ orderDetail.cancelReason || "无" }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </el-card>
</div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
// 引入必要接口：订单、用户、技能、需求
import {
  getOrderDetail,
  getUserById,
  getSkillDetail,
  getDemandDetail,
} from "@/api";
import { ORDER_STATUS } from "@/constants";
import { formatTime } from "@/utils/format";

const router = useRouter();
const route = useRoute();

// 基础状态
const loading = ref(true);
const orderDetail = ref(null);
const publisherName = ref("");
const acceptorName = ref("");
const publisherPhone = ref("");
const acceptorPhone = ref("");
const relatedService = ref(null);

// 返回订单列表
const goBack = () => {
  const tab = route.query.tab || "publisher";
  router.push(`/user/orders?tab=${tab}`);
};

// 加载关联服务详情（核心改动：获取技能/需求完整信息）
const loadRelatedService = async () => {
  try {
    // 有技能ID则加载技能详情
    if (orderDetail.value?.skillId) {
      const skill = await getSkillDetail(orderDetail.value.skillId);
      relatedService.value = {
        type: "skill",
        title: skill.title,
        price: skill.price,
        description: skill.description,
        duration: skill.duration,
        category: skill.category,
      };
    }
    // 有需求ID则加载需求详情
    else if (orderDetail.value?.demandId) {
      const demand = await getDemandDetail(orderDetail.value.demandId);
      relatedService.value = {
        type: "demand",
        title: demand.title,
        price: demand.expectedPrice,
        description: demand.description,
        expectedDuration: demand.expectedDuration,
        category: demand.category,
      };
    }
  } catch (e) {
    console.error("加载关联服务失败：", e);
    ElMessage.warning("关联服务信息加载失败");
  }
};

// 加载订单主信息
const loadOrderDetail = async () => {
  const orderId = route.params.id;
  if (!orderId) {
    ElMessage.error("订单ID不能为空");
    loading.value = false;
    return;
  }

  try {
    // 1. 获取订单基础信息
    const orderData = await getOrderDetail(orderId);
    orderDetail.value = orderData;

    // 2. 获取交易双方昵称
    if (orderData.publisherId) {
      const publisher = await getUserById(orderData.publisherId);
      publisherName.value = publisher?.nickname || publisher?.username || "";
      publisherPhone.value = publisher?.phone || "";
    }
    if (orderData.acceptorId) {
      const acceptor = await getUserById(orderData.acceptorId);
      acceptorName.value = acceptor?.nickname || acceptor?.username || "";
      acceptorPhone.value = acceptor?.phone || "";
    }

    // 3. 核心：加载关联服务详情
    await loadRelatedService();
  } catch (e) {
    console.error("加载订单失败：", e);
    ElMessage.error("加载订单详情失败");
    orderDetail.value = null;
  } finally {
    loading.value = false;
  }
};

// 初始化
onMounted(() => {
  loadOrderDetail();
});
</script>

<style scoped>
/* 页面基础样式（匹配毕设清新风格） */
.order-detail-page {
  background-color: #f9f7e8;
  min-height: calc(100vh - 60px);
  padding: 20px;
}

/* 主卡片（参考主流平台圆角+轻阴影） */
.main-card {
  border-radius: 10px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06) !important;
  border: 1px solid #e8f5e9 !important;
}

/* 头部 */
.header-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

/* 详情内容区 */
.detail-content {
  padding: 20px;
}

/* 1. 订单概览（主流平台顶部核心区） */
.order-overview {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  background: #f8ffed;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 24px;
  border: 1px solid #e8f5e9;
}
.order-no,
.order-time {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-right: 20px;
  margin-bottom: 8px;
}
.label {
  color: #666;
  font-size: 14px;
}
.value {
  color: #333;
  font-size: 15px;
  font-weight: 500;
}
.order-amount {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
}
.order-amount .value {
  color: #e64340;
  font-size: 18px;
  font-weight: 600;
}

/* 2. 分区标题（参考主流平台下划线/侧边线） */
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 30px 0 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #eee;
}

/* 3. 关联服务卡片（核心：直接显示服务信息） */
.service-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8f5e9;
  padding: 16px 20px;
  margin-bottom: 20px;
}
.service-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #eee;
}
.service-type {
  color: #82c91e;
  font-size: 14px;
  font-weight: 500;
  background: #f1f8e9;
  padding: 2px 8px;
  border-radius: 4px;
}
.service-price {
  color: #e64340;
  font-size: 16px;
  font-weight: 600;
}
.service-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.service-desc {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.service-meta {
  display: flex;
  gap: 20px;
  color: #999;
  font-size: 13px;
}

/* 描述列表（主流平台表格样式） */
.desc-list {
  margin-bottom: 20px;
}
:deep(.desc-list .el-descriptions__label) {
  font-weight: 500;
  color: #666;
  background: #f8ffed !important;
}
:deep(.desc-list .el-descriptions__content) {
  color: #333;
}

:deep(.el-button) {
  border-radius: 6px !important;
  padding: 8px 24px;
}
</style>
