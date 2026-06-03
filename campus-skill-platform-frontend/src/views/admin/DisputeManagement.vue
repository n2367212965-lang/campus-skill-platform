<template>
  <div class="dispute-management-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span style="font-weight: bold">售后管理</span>
          <el-button type="primary" size="small" @click="loadData"
            >刷新列表</el-button
          >
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="8" animated />
      <template v-else>
        <div v-if="list.length === 0" class="empty-container">
          <el-empty description="暂无待处理售后" />
        </div>
        <el-table
          v-else
          :data="list"
          border
          stripe
          style="width: 100%"
          max-height="600"
        >
          <el-table-column label="订单信息" min-width="220">
            <template #default="scope">
              <div class="order-info-cell">
                <span class="order-no">{{ getOrderNo(scope.row.orderId) }}</span>
                <span class="order-amount"
                  >¥{{ getOrderAmount(scope.row.orderId) }}</span
                >
              </div>
            </template>
          </el-table-column>
          <el-table-column label="交易双方" width="160">
            <template #default="scope">
              <div class="party-cell">
                <span>发布：{{ getUserName(scope.row, 'publisher') }}</span>
                <span>接单：{{ getUserName(scope.row, 'acceptor') }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="disputeType" label="售后类型" width="120">
            <template #default="scope">
              <el-tag :type="DISPUTE_TYPE[scope.row.disputeType]?.type">
                {{ DISPUTE_TYPE[scope.row.disputeType]?.text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="reason"
            label="售后原因"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="scope">
              <el-tag :type="DISPUTE_STATUS[scope.row.status]?.type">
                {{ DISPUTE_STATUS[scope.row.status]?.text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="150">
            <template #default="scope">{{
              formatTime(scope.row.createTime)
            }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <template v-if="scope.row.status === 0 || scope.row.status === 1">
                <el-button
                  type="primary"
                  size="small"
                  @click="openHandleDialog(scope.row)"
                  >处理</el-button
                >
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>

    <!-- 处理弹窗 -->
    <el-dialog v-model="handleDialogVisible" title="处理售后" width="650px">
      <div v-if="currentDispute" class="handle-dialog-content">
        <div class="dialog-section">
          <h4>订单信息</h4>
          <div class="dialog-info-grid">
            <div class="info-item"><span class="label">订单编号：</span><span class="value order-no-text">{{ getOrderNo(currentDispute.orderId) }}</span></div>
            <div class="info-item"><span class="label">订单金额：</span><span class="value amount-text">¥{{ getOrderAmount(currentDispute.orderId) }}</span></div>
            <div class="info-item"><span class="label">发布方：</span><span class="value">{{ currentOrderDetail?.publisherName || '-' }} <span v-if="currentOrderDetail?.publisherPhone" style="color:#999;margin-left:4px">{{ currentOrderDetail.publisherPhone }}</span></span></div>
            <div class="info-item"><span class="label">接单方：</span><span class="value">{{ currentOrderDetail?.acceptorName || '-' }} <span v-if="currentOrderDetail?.acceptorPhone" style="color:#999;margin-left:4px">{{ currentOrderDetail.acceptorPhone }}</span></span></div>
          </div>
        </div>
        <div class="dialog-section">
          <h4>售后信息</h4>
          <div class="dialog-info-grid">
            <div class="info-item"><span class="label">售后类型：</span><span class="value"><el-tag :type="DISPUTE_TYPE[currentDispute.disputeType]?.type" size="small">{{ DISPUTE_TYPE[currentDispute.disputeType]?.text }}</el-tag></span></div>
            <div class="info-item"><span class="label">当前状态：</span><span class="value"><el-tag :type="DISPUTE_STATUS[currentDispute.status]?.type" size="small">{{ DISPUTE_STATUS[currentDispute.status]?.text }}</el-tag></span></div>
          </div>
          <div class="info-item" style="margin-top:10px"><span class="label">售后原因：</span><span class="value reason-text">{{ currentDispute.reason }}</span></div>
          <div v-if="currentDispute.evidence" class="info-item" style="margin-top:6px"><span class="label">凭证：</span><span class="value">{{ currentDispute.evidence }}</span></div>
        </div>
        <el-form :model="handleForm" label-width="90px" style="margin-top:16px">
          <el-form-item label="处理结果">
            <el-radio-group v-model="handleForm.status">
              <el-radio :label="2">已解决</el-radio>
              <el-radio :label="3">已关闭</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理说明">
            <el-input
              v-model="handleForm.handleResult"
              type="textarea"
              :rows="3"
              placeholder="请输入处理结果..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="submitHandle"
          >确认处理</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getPendingDisputes, handleDispute, getOrderDetail, getUserById } from "@/api";
import { formatTime } from "@/utils/format";
import { DISPUTE_TYPE, DISPUTE_STATUS } from "@/constants";

const list = ref([]);
const loading = ref(false);
const handleDialogVisible = ref(false);
const handleLoading = ref(false);
const currentDispute = ref(null);
const handleForm = ref({ status: 2, handleResult: "" });
const orderCache = ref({});
const userCache = ref({});
const currentOrderDetail = ref(null);

const loadOrderInfo = async (orderId) => {
  if (orderCache.value[orderId]) return orderCache.value[orderId];
  try {
    const order = await getOrderDetail(orderId);
    let publisherName = "", publisherPhone = "";
    let acceptorName = "", acceptorPhone = "";
    if (order.publisherId) {
      const u = await loadUserInfo(order.publisherId);
      publisherName = u.name; publisherPhone = u.phone;
    }
    if (order.acceptorId) {
      const u = await loadUserInfo(order.acceptorId);
      acceptorName = u.name; acceptorPhone = u.phone;
    }
    const info = { ...order, publisherName, acceptorName, publisherPhone, acceptorPhone };
    orderCache.value[orderId] = info;
    return info;
  } catch (e) {
    console.error(`获取订单${orderId}详情失败`, e);
    return null;
  }
};

const loadUserInfo = async (userId) => {
  if (!userId) return { name: "-", phone: "" };
  if (userCache.value[userId]) return userCache.value[userId];
  try {
    const user = await getUserById(userId);
    const result = {
      name: user?.nickname || user?.username || `用户${userId}`,
      phone: user?.phone || ""
    };
    userCache.value[userId] = result;
    return result;
  } catch (e) {
    return `用户${userId}`;
  }
};

const getOrderNo = (orderId) => {
  const order = orderCache.value[orderId];
  return order?.orderNo || "-";
};

const getOrderAmount = (orderId) => {
  const order = orderCache.value[orderId];
  return order?.amount || "0.00";
};

const getUserName = (row, role) => {
  const order = orderCache.value[row.orderId];
  if (!order) return "-";
  return role === "publisher" ? (order.publisherName || "-") : (order.acceptorName || "-");
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getPendingDisputes();
    list.value = res || [];
    orderCache.value = {};
    userCache.value = {};
    for (const dispute of list.value) {
      await loadOrderInfo(dispute.orderId);
    }
  } catch (e) {
    console.error("加载售后失败：", e);
    ElMessage.error("加载售后失败");
    list.value = [];
  } finally {
    loading.value = false;
  }
};

const openHandleDialog = async (item) => {
  currentDispute.value = item;
  handleForm.value = { status: 2, handleResult: "" };
  currentOrderDetail.value = orderCache.value[item.orderId] || null;
  handleDialogVisible.value = true;
};

const submitHandle = async () => {
  if (!handleForm.value.handleResult.trim()) {
    ElMessage.warning("请输入处理结果");
    return;
  }
  try {
    await ElMessageBox.confirm("确认处理该售后？", "系统提示", {
      type: "warning",
    });
    handleLoading.value = true;
    await handleDispute(
      currentDispute.value.id,
      handleForm.value.handleResult,
      handleForm.value.status,
    );
    ElMessage.success("处理成功");
    handleDialogVisible.value = false;
    loadData();
  } catch (e) {
    if (e !== "cancel") {
      console.error("处理失败：", e);
      ElMessage.error("处理失败");
    }
  } finally {
    handleLoading.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.dispute-management-page {
  padding: 20px;
  background-color: #f9f7e8;
  min-height: calc(100vh - 120px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 18px;
}

.empty-container {
  padding: 40px 0;
}

:deep(.el-card) {
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08) !important;
  border: 1px solid #e8f5e9 !important;
}

:deep(.el-table) {
  border-radius: 8px !important;
  overflow: hidden !important;
}

:deep(.el-table__header) {
  background-color: #e8f5e9 !important;
}

:deep(.el-button) {
  border-radius: 6px !important;
  transition: all 0.2s ease !important;
}

:deep(.el-button:hover) {
  transform: scale(0.98) !important;
}

:deep(.el-dialog) {
  border-radius: 10px !important;
}

:deep(.el-dialog__header) {
  background-color: #e8f5e9 !important;
}

.order-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-no {
  font-size: 13px;
  color: #333;
  font-weight: 600;
  font-family: "Courier New", monospace;
}

.order-amount {
  font-size: 13px;
  color: #e64340;
  font-weight: 600;
}

.party-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #666;
}

.handle-dialog-content {
  padding: 0 10px;
}

.dialog-section {
  margin-bottom: 12px;
}

.dialog-section h4 {
  margin: 0 0 10px 0;
  font-size: 15px;
  color: #333;
  padding-bottom: 6px;
  border-bottom: 1px solid #eee;
}

.dialog-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 20px;
}

.info-item {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.info-item .label {
  color: #999;
  white-space: nowrap;
  min-width: 70px;
}

.info-item .value {
  color: #333;
}

.order-no-text {
  font-family: "Courier New", monospace;
  font-weight: 600;
}

.amount-text {
  color: #e64340;
  font-weight: 600;
  font-size: 15px;
}

.reason-text {
  color: #666;
  line-height: 1.5;
}
</style>
