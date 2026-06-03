<template>
  <div class="my-disputes-page">
    <el-card>
      <template #header>
        <span style="font-weight: bold">我的售后</span>
      </template>

      <el-skeleton v-if="loading" :rows="6" animated />
      <template v-else>
        <div v-if="list.length === 0" class="empty-container">
          <el-empty description="暂无售后数据" />
        </div>
        <div v-else class="dispute-list">
          <div v-for="item in list" :key="item.id" class="dispute-item">
            <div class="dispute-header">
              <div class="dispute-title">
                <span class="order-no">
                  订单号：
                  <el-button type="link" @click="goToOrderDetail(item.orderId)">
                    <!-- 核心修改：显示订单业务编号，兜底显示订单ID -->
                    {{ orderNoMap[item.orderId] || item.orderId }}
                  </el-button>
                </span>
                <el-tag
                  :type="DISPUTE_TYPE[item.disputeType]?.type"
                  size="small"
                >
                  {{ DISPUTE_TYPE[item.disputeType]?.text }}
                </el-tag>
                <el-tag
                  type="info"
                  size="small"
                  v-if="item.applicantId === userStore.userInfo.id"
                >
                  我发起的
                </el-tag>
                <el-tag type="warning" size="small" v-else> 对方发起的 </el-tag>
              </div>
              <el-tag
                :type="DISPUTE_STATUS[item.status]?.type"
                size="large"
              >
                {{ DISPUTE_STATUS[item.status]?.text }}
              </el-tag>
            </div>
            <div class="dispute-content">
              <div class="content-item">
                <span class="label">售后原因：</span>
                <span class="value">{{ item.reason }}</span>
              </div>
              <div v-if="item.evidence" class="content-item">
                <span class="label">凭证：</span>
                <span class="value">{{ item.evidence }}</span>
              </div>
              <div v-if="item.handleResult" class="content-item">
                <span class="label">处理结果：</span>
                <span class="value">{{ item.handleResult }}</span>
              </div>
            </div>
            <div class="dispute-footer">
              <span class="time"
                >申请时间：{{ formatTime(item.createTime) }}</span
              >
              <span v-if="item.handleTime" class="time"
                >处理时间：{{ formatTime(item.handleTime) }}</span
              >
            </div>
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { getMyDisputes, getOrderDetail } from "@/api";
import { useUserStore } from "@/stores/user";
import { formatTime } from "@/utils/format";
import { DISPUTE_TYPE, DISPUTE_STATUS } from "@/constants";

const router = useRouter();
const userStore = useUserStore();
const list = ref([]);
const loading = ref(false);
// 新增：存储订单ID对应的订单业务编号
const orderNoMap = ref({});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getMyDisputes();
    list.value = res || [];

    // 核心新增：循环查询每个售后关联的订单，获取订单业务编号
    orderNoMap.value = {};
    for (const dispute of list.value) {
      try {
        const orderDetail = await getOrderDetail(dispute.orderId);
        orderNoMap.value[dispute.orderId] = orderDetail.orderNo;
      } catch (e) {
        console.error(`获取订单${dispute.orderId}详情失败`, e);
      }
    }
  } catch (e) {
    console.error("加载售后失败：", e);
    ElMessage.error("加载售后失败");
    list.value = [];
  } finally {
    loading.value = false;
  }
};

const goToOrderDetail = (orderId) => {
  router.push(`/user/order-detail/${orderId}`);
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.my-disputes-page {
  padding: 20px;
  background-color: #f9f7e8;
  min-height: calc(100vh - 120px);
}

.dispute-list {
  margin-top: 20px;
}

.dispute-item {
  padding: 20px;
  border: 1px solid #e8f5e9;
  border-radius: 8px;
  margin-bottom: 16px;
  background-color: #fff;
  transition: all 0.2s ease;
}

.dispute-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.dispute-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.dispute-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-no {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  font-family: "Courier New", monospace;
}

.dispute-content {
  margin-bottom: 16px;
}

.content-item {
  display: flex;
  margin-bottom: 8px;
  line-height: 1.6;
}

.label {
  font-size: 14px;
  font-weight: 600;
  color: #666;
  min-width: 80px;
}

.value {
  font-size: 14px;
  color: #333;
  flex: 1;
}

.dispute-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.time {
  font-size: 12px;
  color: #999;
}

.empty-container {
  padding: 40px 0;
}

:deep(.el-card) {
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08) !important;
  border: 1px solid #e8f5e9 !important;
}
</style>
