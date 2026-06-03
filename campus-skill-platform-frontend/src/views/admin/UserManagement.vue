<template>
  <div
    style="padding: 20px; background: #f9f7e8; min-height: calc(100vh - 60px)"
  >
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px" v-if="!statsLoading">
      <el-col :span="6"
        ><el-card shadow="hover"
          ><el-statistic
            title="总用户数"
            :value="stats.userCount || 0" /></el-card
      ></el-col>
      <el-col :span="6"
        ><el-card shadow="hover"
          ><el-statistic
            title="活跃用户"
            :value="stats.activeUserCount || 0" /></el-card
      ></el-col>
      <el-col :span="6"
        ><el-card shadow="hover"
          ><el-statistic
            title="待审技能"
            :value="stats.pendingSkillCount || 0" /></el-card
      ></el-col>
      <el-col :span="6"
        ><el-card shadow="hover"
          ><el-statistic
            title="待审需求"
            :value="stats.pendingDemandCount || 0" /></el-card
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
          <span>用户管理</span>
          <el-button type="primary" size="small" @click="loadUsers"
            >刷新用户</el-button
          >
        </div>
      </template>

      <el-skeleton v-if="userLoading" :rows="8" animated />
      <el-table
        v-else
        :data="userList"
        border
        stripe
        style="width: 100%"
        max-height="600"
      >
        <el-table-column label="#" width="50" type="index" :index="i => i + 1" />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column prop="nickname" label="昵称" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column
          prop="creditScore"
          label="信誉分"
          width="80"
          align="center"
        >
          <template #default="s">
            <el-tag :type="s.row.creditScore >= 90 ? 'success' : 'warning'">{{
              s.row.creditScore
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="80" align="center">
          <template #default="s">{{
            s.row.role === 1 ? "管理员" : "普通用户"
          }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="s">
            <el-tag :type="s.row.status === 0 ? 'success' : 'danger'">{{
              s.row.status === 0 ? "正常" : "禁用"
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="s">
            <template v-if="s.row.role !== 1">
              <el-button
                :type="s.row.status === 0 ? 'danger' : 'success'"
                size="small"
                @click="toggleUserStatus(s.row)"
              >
                {{ s.row.status === 0 ? "禁用" : "启用" }}
              </el-button>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-if="!userLoading && userList.length === 0"
        description="暂无用户数据"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { getStatistics, getAllUsers, updateUserStatus } from "@/api";
import { ElMessage, ElMessageBox } from "element-plus";

const stats = ref({});
const userList = ref([]);
const statsLoading = ref(false);
const userLoading = ref(false);

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

const loadUsers = async () => {
  userLoading.value = true;
  try {
    userList.value = (await getAllUsers()) || [];
  } catch (e) {
    ElMessage.error("加载用户列表失败");
    console.error(e);
  } finally {
    userLoading.value = false;
  }
};

const toggleUserStatus = async (row) => {
  const newStatus = row.status === 0 ? 1 : 0;
  try {
    await ElMessageBox.confirm(
      `确认${newStatus === 0 ? "启用" : "禁用"}该用户？`,
      "警告",
      { type: "warning" },
    );
    await updateUserStatus(row.id, newStatus);
    ElMessage.success("操作成功");
    loadUsers();
  } catch (e) {
    if (e !== "cancel") {
      ElMessage.error("操作失败");
      console.error(e);
    }
  }
};

onMounted(() => {
  loadStats();
  loadUsers();
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

/* 【校园清新风优化】卡片 */
:deep(.el-card) {
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08) !important;
  border: 1px solid #e8f5e9 !important;
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

/* 【校园清新风优化】标签 */
:deep(.el-tag) {
  border-radius: 4px !important;
}
</style>
