<template>
  <div class="profile-container">
    <el-main class="profile-content">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>我发布的需求</span>
            <el-button type="primary" @click="$router.push('/publish/demand')"
              >发布新需求</el-button
            >
          </div>
        </template>

        <el-skeleton v-if="loading" :rows="5" animated />

        <el-empty
          v-else-if="demandList.length === 0"
          description="你还没有发布任何需求~"
        >
          <el-button type="primary" @click="$router.push('/publish/demand')"
            >去发布</el-button
          >
        </el-empty>

        <el-row :gutter="20" v-else>
          <el-col :span="8" v-for="demand in demandList" :key="demand.id">
            <el-card shadow="hover" class="demand-card">
              <template #header>
                <div class="demand-title">
                  <span>{{ demand.title }}</span>
                  <el-tag :type="getStatusType(demand.status)" size="small">
                    {{ getStatusText(demand.status) }}
                  </el-tag>
                </div>
              </template>

              <div class="demand-info">
                <p class="desc">{{ demand.description || "无描述" }}</p>
                <div class="meta">
                  <span>分类：{{ demand.category }}</span>
                  <span>期望价格：¥{{ demand.expectedPrice || "面议" }}</span>
                  <span>期望时长：{{ demand.expectedDuration || "未指定" }}小时</span>
                </div>
              </div>

              <template #footer>
                <div class="card-footer">
                  <span class="time"
                    >发布时间：{{ formatTime(demand.createTime) }}</span
                  >
                  <el-button
                    v-if="demand.status === 1"
                    type="danger"
                    size="small"
                    @click="handleOffline(demand)"
                    >下架</el-button
                  >
                  <el-button
                    v-else-if="demand.status === 3"
                    type="success"
                    size="small"
                    @click="handleOnline(demand)"
                    >上架</el-button
                  >
                </div>
              </template>
            </el-card>
          </el-col>
        </el-row>
      </el-card>
    </el-main>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { getMyDemands, offlineDemand, onlineDemand } from "@/api";
import { ElMessage, ElMessageBox } from "element-plus";
import { formatTime } from "@/utils/format";

const router = useRouter();
const demandList = ref([]);
const loading = ref(false);

const getStatusType = (status) => {
  const map = {
    0: "warning",
    1: "success",
    2: "danger",
    3: "info",
  };
  return map[status] || "info";
};

const getStatusText = (status) => {
  const map = {
    0: "待审核",
    1: "已上架",
    2: "已驳回",
    3: "已下架",
  };
  return map[status] || "未知状态";
};

const loadMyDemands = async () => {
  loading.value = true;
  try {
    const res = await getMyDemands();
    demandList.value = res || [];
  } catch (error) {
    console.error("加载需求失败：", error);
    ElMessage.error("获取需求列表失败，请重试");
  } finally {
    loading.value = false;
  }
};

const handleOffline = async (demand) => {
  try {
    await ElMessageBox.confirm("确认下架该需求？下架后将不再展示", "提示", {
      type: "warning",
    });
    await offlineDemand(demand.id);
    ElMessage.success("需求已下架");
    loadMyDemands();
  } catch (e) {
    if (e !== "cancel") ElMessage.error("操作失败");
  }
};

const handleOnline = async (demand) => {
  try {
    await ElMessageBox.confirm("确认上架该需求？", "提示", { type: "warning" });
    await onlineDemand(demand.id);
    ElMessage.success("需求已上架");
    loadMyDemands();
  } catch (e) {
    if (e !== "cancel") ElMessage.error("操作失败");
  }
};

onMounted(() => loadMyDemands());
</script>

<style scoped>
.profile-container {
  display: flex;
  min-height: calc(100vh - 60px);
  background: #f9f7e8;
  margin-top: 60px;
}

.profile-content {
  padding: 20px;
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 18px;
}

.demand-card {
  margin-bottom: 20px;
  height: 100%;
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08) !important;
  border: 1px solid #e8f5e9 !important;
  transition: all 0.2s ease !important;
}
.demand-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1) !important;
}

.demand-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.desc {
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin: 10px 0;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 12px;
  color: #999;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}
</style>
