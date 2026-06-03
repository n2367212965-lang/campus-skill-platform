<template>
  <div class="profile-container">
    <el-main class="profile-content">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>我发布的技能</span>
            <el-button type="primary" @click="$router.push('/publish/skill')"
              >发布新技能</el-button
            >
          </div>
        </template>

        <!-- 加载状态 -->
        <el-skeleton v-if="loading" :rows="5" animated />

        <!-- 空状态 -->
        <el-empty
          v-else-if="skillList.length === 0"
          description="你还没有发布任何技能~"
        >
          <el-button type="primary" @click="$router.push('/publish/skill')"
            >去发布</el-button
          >
        </el-empty>

        <!-- 技能列表 -->
        <el-row :gutter="20" v-else>
          <el-col :span="8" v-for="skill in skillList" :key="skill.id">
            <el-card shadow="hover" class="skill-card">
              <template #header>
                <div class="skill-title">
                  <span>{{ skill.title }}</span>
                  <el-tag :type="getStatusType(skill.status)" size="small">
                    {{ getStatusText(skill.status) }}
                  </el-tag>
                </div>
              </template>

              <div class="skill-info">
                <p class="desc">{{ skill.description || "无描述" }}</p>
                <div class="meta">
                  <span>分类：{{ skill.category }}</span>
                  <span>价格：¥{{ skill.price }}</span>
                  <span>时长：{{ skill.duration }}小时</span>
                </div>
              </div>

              <template #footer>
                <div class="card-footer">
                  <span class="time"
                    >发布时间：{{ formatTime(skill.createTime) }}</span
                  >
                  <el-button
                    v-if="skill.status === 1"
                    type="danger"
                    size="small"
                    @click="handleOffline(skill)"
                    >下架</el-button
                  >
                  <el-button
                    v-else-if="skill.status === 3"
                    type="success"
                    size="small"
                    @click="handleOnline(skill)"
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
import { getMySkills, offlineSkill, onlineSkill } from "@/api";
import { ElMessage, ElMessageBox } from "element-plus";
import { formatTime } from "@/utils/format";

const router = useRouter();
const skillList = ref([]);
const loading = ref(false);

// 状态类型映射
const getStatusType = (status) => {
  const map = {
    0: "warning", // 待审核
    1: "success", // 已上架
    2: "danger", // 已驳回
    3: "info", // 已下架
  };
  return map[status] || "info";
};

// 状态文本映射
const getStatusText = (status) => {
  const map = {
    0: "待审核",
    1: "已上架",
    2: "已驳回",
    3: "已下架",
  };
  return map[status] || "未知状态";
};

// 加载我的技能
const loadMySkills = async () => {
  loading.value = true;
  try {
    const res = await getMySkills();
    skillList.value = res || [];
  } catch (error) {
    console.error("加载技能失败：", error);
    ElMessage.error("获取技能列表失败，请重试");
  } finally {
    loading.value = false;
  }
};

// 下架技能
const handleOffline = async (skill) => {
  try {
    await ElMessageBox.confirm("确认下架该技能？下架后将不再展示", "提示", {
      type: "warning",
    });
    await offlineSkill(skill.id);
    ElMessage.success("技能已下架");
    loadMySkills();
  } catch (e) {
    if (e !== "cancel") ElMessage.error("操作失败");
  }
};

// 上架技能
const handleOnline = async (skill) => {
  try {
    await ElMessageBox.confirm("确认上架该技能？", "提示", { type: "warning" });
    await onlineSkill(skill.id);
    ElMessage.success("技能已上架");
    loadMySkills();
  } catch (e) {
    if (e !== "cancel") ElMessage.error("操作失败");
  }
};

onMounted(() => loadMySkills());
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

.skill-card {
  margin-bottom: 20px;
  height: 100%;
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08) !important;
  border: 1px solid #e8f5e9 !important;
  transition: all 0.2s ease !important;
}
.skill-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1) !important;
}

.skill-title {
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

:deep(.el-button) {
  border-radius: 6px !important;
  transition: all 0.2s ease !important;
}
:deep(.el-button:hover) {
  transform: scale(0.98) !important;
}
</style>
