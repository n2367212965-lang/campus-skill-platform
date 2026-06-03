<template>
  <div
    style="padding: 20px; background: #f9f7e8; min-height: calc(100vh - 60px)"
  >
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px" v-if="!statsLoading">
      <el-col :span="4"
        ><el-card shadow="hover"
          ><el-statistic
            title="总用户数"
            :value="stats.userCount || 0" /></el-card
      ></el-col>
      <el-col :span="4"
        ><el-card shadow="hover"
          ><el-statistic
            title="需求总数"
            :value="stats.demandCount || 0" /></el-card
      ></el-col>
      <el-col :span="4"
        ><el-card shadow="hover"
          ><el-statistic title="待审需求" :value="stats.pendingDemandCount || 0"
            ><template #suffix
              ><el-tag type="danger">待处理</el-tag></template
            ></el-statistic
          ></el-card
        ></el-col
      >
      <el-col :span="4"
        ><el-card shadow="hover"
          ><el-statistic
            title="通过需求"
            :value="stats.approvedDemandCount || 0" /></el-card
      ></el-col>
      <el-col :span="4"
        ><el-card shadow="hover"
          ><el-statistic
            title="活跃用户"
            :value="stats.activeUserCount || 0" /></el-card
      ></el-col>
    </el-row>

    <el-skeleton
      v-if="statsLoading"
      :rows="6"
      animated
      style="margin-bottom: 20px"
    />

    <el-card>
      <template #header>
        <div class="card-header">
          <span>需求审核</span>
          <el-button type="primary" size="small" @click="loadDemands"
            >刷新列表</el-button
          >
        </div>
      </template>

      <el-skeleton v-if="demandLoading" :rows="8" animated />
      <el-table
        v-else
        :data="demandList"
        border
        stripe
        style="width: 100%"
        max-height="600"
      >
        <el-table-column label="#" width="50" type="index" :index="i => i + 1" />
        <el-table-column prop="title" label="需求标题" width="180" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="expectedPrice" label="预算" width="90"
          ><template #default="s"
            >¥{{ s.row.expectedPrice }}</template
          ></el-table-column
        >
        <el-table-column prop="expectedDuration" label="时长" width="80" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="expectedLocation" label="地点" width="120" />
        <el-table-column
          prop="description"
          label="详情"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="s">
            <el-tag v-if="s.row.status === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="s.row.status === 1" type="success"
              >已通过</el-tag
            >
            <el-tag v-else type="danger">已驳回</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="s">
            <template v-if="s.row.status === 0">
              <el-button
                type="success"
                size="small"
                @click="handleAudit('demand', s.row, 1, '通过')"
                >通过</el-button
              >
              <el-button
                type="danger"
                size="small"
                @click="openReject('demand', s.row)"
                >驳回</el-button
              >
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-if="!demandLoading && demandList.length === 0"
        description="暂无待审核需求"
      />
    </el-card>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="审核驳回" width="500px">
      <el-form label-width="80px">
        <el-form-item label="驳回原因">
          <el-input
            v-model="rejectForm.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReject" :loading="submitLoading"
          >确认驳回</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { getStatistics, getPendingDemands, auditDemand } from "@/api";
import { ElMessage, ElMessageBox } from "element-plus";

const stats = ref({});
const demandList = ref([]);
const statsLoading = ref(false);
const demandLoading = ref(false);
const rejectDialogVisible = ref(false);
const submitLoading = ref(false);
const currentAuditType = ref("");
const currentAuditRow = ref(null);
const rejectForm = reactive({ remark: "" });

const loadStats = async () => {
  statsLoading.value = true;
  try {
    stats.value = (await getStatistics()) || {};
  } catch (e) {
    ElMessage.error("加载统计数据失败");
    console.error(e);
  } finally {
    statsLoading.value = false;
  }
};

const loadDemands = async () => {
  demandLoading.value = true;
  try {
    demandList.value = (await getPendingDemands()) || [];
  } catch (e) {
    ElMessage.error("加载需求列表失败");
    console.error(e);
  } finally {
    demandLoading.value = false;
  }
};

const handleAudit = async (type, row, status, remark) => {
  try {
    await ElMessageBox.confirm(
      `确认${status === 1 ? "通过" : "操作"}该内容？`,
      "系统提示",
      { type: "warning" },
    );
    if (type === "demand") {
      await auditDemand(row.id, status, remark);
    }
    ElMessage.success("操作成功");
    loadStats();
    loadDemands();
  } catch (e) {
    if (e !== "cancel") console.error(e);
  }
};

const openReject = (type, row) => {
  currentAuditType.value = type;
  currentAuditRow.value = row;
  rejectForm.remark = "";
  rejectDialogVisible.value = true;
};

const submitReject = async () => {
  if (!rejectForm.remark) return ElMessage.warning("请输入驳回原因");
  submitLoading.value = true;
  try {
    const params = {
      id: currentAuditRow.value.id,
      status: 2,
      remark: rejectForm.remark,
    };
    await auditDemand(params.id, params.status, params.remark);
    ElMessage.success("已驳回");
    rejectDialogVisible.value = false;
    loadStats();
    loadDemands();
  } catch (e) {
    ElMessage.error("驳回操作失败");
    console.error(e);
  } finally {
    submitLoading.value = false;
  }
};

onMounted(() => {
  loadStats();
  loadDemands();
});
</script>

<style scoped>
/* 【校园清新风优化】卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 18px;
}

/* 【校园清新风优化】页面背景 */
:deep(div[style*="background: #f0f2f5"]) {
  background: #f9f7e8 !important;
}

/* 【校园清新风优化】卡片、表格、按钮、弹窗（同技能审核） */
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
</style>
