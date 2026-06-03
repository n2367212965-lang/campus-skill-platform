<template>
  <div class="orders-page">
    <el-card>
      <template #header>
        <span style="font-weight: bold;font-size:16px">我的订单</span>
      </template>

      <el-tabs v-model="activeTab" @tab-change="loadData">
        <el-tab-pane label="我是发布方" name="publisher" />
        <el-tab-pane label="我是接单方" name="acceptor" />
      </el-tabs>

      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column label="服务内容" min-width="160">
          <template #default="scope">
            <span v-if="serviceMap[scope.row.id]" style="color:#333;font-weight:500">
              {{ serviceMap[scope.row.id] }}
            </span>
            <span v-else style="color:#ccc">加载中...</span>
          </template>
        </el-table-column>
        <el-table-column label="对方" width="100">
          <template #default="scope">
            {{ partyMap[scope.row.id] || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="scope">¥{{ scope.row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="ORDER_STATUS[scope.row.status]?.type" size="small">
              {{ ORDER_STATUS[scope.row.status]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="scope">
            <el-button size="small" link type="primary" @click="viewDetail(scope.row)">
              详情
            </el-button>
            <template v-if="activeTab === 'publisher'">
              <el-button size="small" type="danger" @click="handleCancel(scope.row)"
                v-if="scope.row.status === 0 || scope.row.status === 1">取消</el-button>
              <el-button size="small" type="primary" @click="handlePay(scope.row)"
                v-if="scope.row.status === 0 && !scope.row.skillId">支付</el-button>
              <el-button size="small" type="primary" @click="handleStart(scope.row)"
                v-if="scope.row.status === 1 && scope.row.skillId">开始服务</el-button>
              <el-button size="small" type="success" @click="handleEnd(scope.row)"
                v-if="scope.row.status === 2 && scope.row.skillId">结束服务</el-button>
              <el-button size="small" type="success" @click="handleConfirm(scope.row)"
                v-if="scope.row.status === 3 && !scope.row.skillId">确认完成</el-button>
            </template>
            <template v-if="activeTab === 'acceptor'">
              <el-button size="small" type="danger" @click="handleCancel(scope.row)"
                v-if="scope.row.status === 0 || scope.row.status === 1">取消</el-button>
              <el-button size="small" type="primary" @click="handlePay(scope.row)"
                v-if="scope.row.status === 0 && scope.row.skillId">支付</el-button>
              <el-button size="small" type="primary" @click="handleStart(scope.row)"
                v-if="scope.row.status === 1 && !scope.row.skillId">开始服务</el-button>
              <el-button size="small" type="success" @click="handleEnd(scope.row)"
                v-if="scope.row.status === 2 && !scope.row.skillId">结束服务</el-button>
              <el-button size="small" type="success" @click="handleConfirm(scope.row)"
                v-if="scope.row.status === 3 && scope.row.skillId">确认完成</el-button>
            </template>
            <!-- 评价/售后（双方通用） -->
            <template v-if="scope.row.status === 4">
              <el-button size="small" type="warning" @click="openRatingDialog(scope.row)"
                v-if="!getOrderRatedStatus(scope.row.id)">评价</el-button>
            </template>
            <el-button size="small" type="danger" @click="openDisputeDialog(scope.row)"
              v-if="(scope.row.status === 2 || scope.row.status === 3 || scope.row.status === 4) && !getOrderDisputeStatus(scope.row.id)">
              售后
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="list.length === 0 && !loading" description="暂无订单" style="margin-top:30px" />
    </el-card>

    <!-- 支付弹窗 -->
    <el-dialog v-model="payDialogVisible" title="确认支付" width="400px" draggable>
      <el-form :model="payForm" :rules="payRules" ref="payFormRef">
        <el-form-item label="订单号"><span>{{ currentOrder?.orderNo }}</span></el-form-item>
        <el-form-item label="金额">
          <span style="color:#e64340;font-size:22px;font-weight:bold">¥{{ currentOrder?.amount }}</span>
        </el-form-item>
        <el-form-item label="支付密码" prop="password">
          <el-input v-model="payForm.password" type="password" placeholder="默认密码 123456" show-password maxlength="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="payLoading" @click="confirmPay">确认支付</el-button>
      </template>
    </el-dialog>

    <!-- 评价弹窗 -->
    <el-dialog v-model="ratingDialogVisible" title="评价" width="480px">
      <el-form :model="ratingForm" label-width="80px">
        <el-form-item label="订单号"><span>{{ currentOrder?.orderNo }}</span></el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="ratingForm.score" :max="5" show-score />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="ratingForm.content" type="textarea" :rows="3" placeholder="请输入评价内容..." maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ratingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ratingLoading" @click="handleSubmitRating">提交评价</el-button>
      </template>
    </el-dialog>

    <!-- 售后弹窗 -->
    <el-dialog v-model="disputeDialogVisible" title="申请售后" width="480px">
      <el-form :model="disputeForm" label-width="80px">
        <el-form-item label="订单号"><span>{{ currentOrder?.orderNo }}</span></el-form-item>
        <el-form-item label="售后类型">
          <el-select v-model="disputeForm.disputeType" placeholder="请选择" style="width:100%">
            <el-option :label="DISPUTE_TYPE[1].text" :value="1" />
            <el-option :label="DISPUTE_TYPE[2].text" :value="2" />
            <el-option :label="DISPUTE_TYPE[3].text" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="disputeForm.reason" type="textarea" :rows="3" placeholder="请描述遇到的问题..." maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disputeDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="disputeLoading" @click="handleSubmitDispute">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import {
  getOrdersAsPublisher, getOrdersAsAcceptor, getUserById,
  getSkillDetail, getDemandDetail,
  payOrder, startService, endService, confirmComplete, cancelOrder,
  submitRating, createDispute, getOrderRatings, getMyDisputes,
} from "@/api";
import { ORDER_STATUS, DISPUTE_TYPE } from "@/constants";
import { formatTime } from "@/utils/format";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();

const activeTab = ref("publisher");
const list = ref([]);
const loading = ref(false);
const serviceMap = ref({});
const partyMap = ref({});
const orderRatedMap = ref({});
const orderDisputeMap = ref({});

// 弹窗
const payDialogVisible = ref(false);
const payLoading = ref(false);
const currentOrder = ref(null);
const payFormRef = ref(null);
const payForm = ref({ password: "123456" });
const payRules = ref({ password: [{ required: true, message: "请输入支付密码", trigger: "blur" }] });

const ratingDialogVisible = ref(false);
const ratingLoading = ref(false);
const ratingForm = ref({ score: 5, content: "" });

const disputeDialogVisible = ref(false);
const disputeLoading = ref(false);
const disputeForm = ref({ disputeType: 1, reason: "" });

// 加载对方昵称
const loadPartyName = async (order) => {
  const targetId = activeTab.value === "publisher" ? order.acceptorId : order.publisherId;
  if (!targetId) return;
  try {
    const user = await getUserById(targetId);
    partyMap.value[order.id] = user?.nickname || user?.username || `用户${targetId}`;
  } catch { partyMap.value[order.id] = "-"; }
};

// 加载服务标题
const loadServiceTitle = async (order) => {
  try {
    if (order.skillId) {
      const skill = await getSkillDetail(order.skillId);
      serviceMap.value[order.id] = skill?.title || "技能服务";
    } else if (order.demandId) {
      const demand = await getDemandDetail(order.demandId);
      serviceMap.value[order.id] = demand?.title || "需求服务";
    }
  } catch { serviceMap.value[order.id] = "-"; }
};

const loadData = async () => {
  if (!userStore.token) { ElMessage.warning("请先登录"); return; }
  loading.value = true;
  serviceMap.value = {};
  partyMap.value = {};
  try {
    const res = activeTab.value === "publisher" ? (await getOrdersAsPublisher()) : (await getOrdersAsAcceptor());
    list.value = res || [];
    // 并发加载附加信息
    await Promise.all(list.value.map(o => Promise.all([loadPartyName(o), loadServiceTitle(o)])));

    const myDisputes = (await getMyDisputes()) || [];
    orderDisputeMap.value = {};
    myDisputes.forEach(d => { orderDisputeMap.value[d.orderId] = true; });

    orderRatedMap.value = {};
    for (const o of list.value) {
      const ratings = await getOrderRatings(o.id);
      orderRatedMap.value[o.id] = (ratings || []).some(r => r.fromUserId === userStore.userInfo.id);
    }
  } catch (e) {
    ElMessage.error("加载订单失败");
    list.value = [];
  } finally { loading.value = false; }
};

const getOrderRatedStatus = (orderId) => orderRatedMap.value[orderId] || false;
const getOrderDisputeStatus = (orderId) => orderDisputeMap.value[orderId] || false;

const handleCancel = async (row) => {
  try {
    const { value: reason } = await ElMessageBox.prompt("请输入取消原因（最多200字）", "取消订单", {
      confirmButtonText: "确定", cancelButtonText: "取消",
      inputValidator: (v) => v.length <= 200,
      inputErrorMessage: "取消原因不能超过200字",
    });
    if (!reason) { ElMessage.warning("取消原因不能为空"); return; }
    await cancelOrder(row.id, reason);
    ElMessage.success("订单已取消");
    loadData();
  } catch (e) { if (e !== "cancel") ElMessage.error("取消失败：" + (e.message || "服务器错误")); }
};

const handlePay = (row) => {
  currentOrder.value = row;
  payForm.value = { password: "123456" };
  payDialogVisible.value = true;
};

const confirmPay = async () => {
  const valid = await payFormRef.value.validate();
  if (!valid) return;
  payLoading.value = true;
  try {
    await payOrder(currentOrder.value.id, payForm.value.password);
    ElMessage.success("支付成功");
    payDialogVisible.value = false;
    loadData();
  } catch (e) {
    ElMessage.error(e.message?.includes("密码") ? "支付密码错误（默认：123456）" : "支付失败：" + (e.message || ""));
  } finally { payLoading.value = false; }
};

const handleStart = async (row) => {
  try {
    await ElMessageBox.confirm("确认开始服务吗？", "提示", { type: "info" });
    await startService(row.id);
    ElMessage.success("服务已开始");
    loadData();
  } catch (e) { if (e !== "cancel") ElMessage.error("操作失败：" + (e.message || "")); }
};

const handleEnd = async (row) => {
  try {
    await ElMessageBox.confirm("确认结束服务吗？结束后等待对方确认", "提示", { type: "warning" });
    await endService(row.id);
    ElMessage.success("服务已结束，等待确认");
    loadData();
  } catch (e) { if (e !== "cancel") ElMessage.error("操作失败：" + (e.message || "")); }
};

const handleConfirm = async (row) => {
  try {
    await ElMessageBox.confirm("确认服务已完成吗？确认后订单完成", "确认完成", { type: "warning" });
    await confirmComplete(row.id);
    ElMessage.success("订单已完成");
    loadData();
  } catch (e) { if (e !== "cancel") ElMessage.error("操作失败：" + (e.message || "")); }
};

const viewDetail = (row) => router.push({ path: `/user/order-detail/${row.id}`, query: { tab: activeTab.value } });

const openRatingDialog = (row) => {
  currentOrder.value = row;
  ratingForm.value = { score: 5, content: "" };
  ratingDialogVisible.value = true;
};

const handleSubmitRating = async () => {
  if (!ratingForm.value.content.trim()) { ElMessage.warning("请输入评价内容"); return; }
  ratingLoading.value = true;
  try {
    const isPublisher = activeTab.value === "publisher";
    await submitRating({
      orderId: currentOrder.value.id,
      toUserId: isPublisher ? currentOrder.value.acceptorId : currentOrder.value.publisherId,
      score: ratingForm.value.score,
      content: ratingForm.value.content,
      ratingType: isPublisher ? 1 : 2,
    });
    ElMessage.success("评价成功");
    ratingDialogVisible.value = false;
    orderRatedMap.value[currentOrder.value.id] = true;
    loadData();
  } catch (e) { ElMessage.error("评价失败：" + (e.message || "")); }
  finally { ratingLoading.value = false; }
};

const openDisputeDialog = (row) => {
  currentOrder.value = row;
  disputeForm.value = { disputeType: 1, reason: "" };
  disputeDialogVisible.value = true;
};

const handleSubmitDispute = async () => {
  if (!disputeForm.value.reason.trim()) { ElMessage.warning("请输入售后原因"); return; }
  try {
    await ElMessageBox.confirm("确认提交售后？管理员将介入处理", "提示", { type: "warning" });
    disputeLoading.value = true;
    await createDispute({ orderId: currentOrder.value.id, disputeType: disputeForm.value.disputeType, reason: disputeForm.value.reason });
    ElMessage.success("售后已提交");
    disputeDialogVisible.value = false;
    orderDisputeMap.value[currentOrder.value.id] = true;
    loadData();
  } catch (e) { if (e !== "cancel") ElMessage.error("提交失败：" + (e.message || "")); }
  finally { disputeLoading.value = false; }
};

onMounted(() => loadData());
</script>

<style scoped>
.orders-page { padding: 20px; background: #f9f7e8; min-height: calc(100vh - 60px); }
</style>
