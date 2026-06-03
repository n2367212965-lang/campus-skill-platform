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
            title="技能总数"
            :value="stats.skillCount || 0" /></el-card
      ></el-col>
      <el-col :span="4"
        ><el-card shadow="hover"
          ><el-statistic title="待审技能" :value="stats.pendingSkillCount || 0"
            ><template #suffix
              ><el-tag type="danger">待处理</el-tag></template
            ></el-statistic
          ></el-card
        ></el-col
      >
      <el-col :span="4"
        ><el-card shadow="hover"
          ><el-statistic
            title="通过技能"
            :value="stats.approvedSkillCount || 0" /></el-card
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
          <span>技能审核</span>
          <el-button type="primary" size="small" @click="loadSkills"
            >刷新列表</el-button
          >
        </div>
      </template>

      <el-skeleton v-if="skillLoading" :rows="8" animated />
      <el-table
        v-else
        :data="skillList"
        border
        stripe
        style="width: 100%"
        max-height="600"
      >
        <el-table-column label="#" width="50" type="index" :index="i => i + 1" />
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="price" label="价格" width="90"
          ><template #default="s">¥{{ s.row.price }}</template></el-table-column
        >
        <el-table-column prop="duration" label="时长" width="80" />
        <el-table-column
          prop="description"
          label="描述"
          min-width="200"
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
                @click="handleAudit('skill', s.row, 1, '通过')"
                >通过</el-button
              >
              <el-button
                type="danger"
                size="small"
                @click="openReject('skill', s.row)"
                >驳回</el-button
              >
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-if="!skillLoading && skillList.length === 0"
        description="暂无待审核技能"
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
import { getStatistics, getPendingSkills, auditSkill } from "@/api";
import { ElMessage, ElMessageBox } from "element-plus";

const stats = ref({});
const skillList = ref([]);
const statsLoading = ref(false);
const skillLoading = ref(false);
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

const loadSkills = async () => {
  skillLoading.value = true;
  try {
    skillList.value = (await getPendingSkills()) || [];
  } catch (e) {
    ElMessage.error("加载技能列表失败");
    console.error(e);
  } finally {
    skillLoading.value = false;
  }
};

const handleAudit = async (type, row, status, remark) => {
  try {
    await ElMessageBox.confirm(
      `确认${status === 1 ? "通过" : "操作"}该内容？`,
      "系统提示",
      { type: "warning" },
    );
    if (type === "skill") {
      await auditSkill(row.id, status, remark);
    }
    ElMessage.success("操作成功");
    loadStats();
    loadSkills();
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
    await auditSkill(params.id, params.status, params.remark);
    ElMessage.success("已驳回");
    rejectDialogVisible.value = false;
    loadStats();
    loadSkills();
  } catch (e) {
    ElMessage.error("驳回操作失败");
    console.error(e);
  } finally {
    submitLoading.value = false;
  }
};

onMounted(() => {
  loadStats();
  loadSkills();
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

/* 【校园清新风优化】全局页面 */
:deep(.publish-container),
:deep(div[style*="background: #f0f2f5"]) {
  background: #f9f7e8 !important;
}

/* 【校园清新风优化】卡片 */
:deep(.el-card) {
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08) !important;
  border: 1px solid #e8f5e9 !important;
}
/* 【校园清新风优化】统计卡片 */
:deep(.el-card[shadow="hover"]) {
  transition: all 0.2s ease !important;
}
:deep(.el-card[shadow="hover"]:hover) {
  transform: translateY(-2px) !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1) !important;
}

/* 【校园清新风优化】表格 */
:deep(.el-table) {
  border-radius: 8px !important;
  overflow: hidden !important;
}
:deep(.el-table__header) {
  background-color: #e8f5e9 !important;
}
:deep(.el-table--striped .el-table__row--striped) {
  background-color: #fdfdfb !important;
}

/* 【校园清新风优化】按钮 */
:deep(.el-button) {
  border-radius: 6px !important;
  transition: all 0.2s ease !important;
}
:deep(.el-button:hover) {
  transform: scale(0.98) !important;
}

/* 【校园清新风优化】弹窗 */
:deep(.el-dialog) {
  border-radius: 10px !important;
}
:deep(.el-dialog__header) {
  background-color: #e8f5e9 !important;
  border-radius: 10px 10px 0 0 !important;
}
</style>
