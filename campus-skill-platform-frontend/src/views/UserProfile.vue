<template>
  <div class="profile-container" v-loading="loading">
    <el-page-header @back="goBack" content="返回" style="margin-bottom: 20px" />

    <div v-if="notFound" class="empty-container">
      <el-empty description="用户不存在" />
    </div>

    <!-- 用户基本信息卡片 -->
    <el-card v-if="user" class="info-card">
      <div class="user-header">
        <el-avatar :size="80" :src="getFullAvatarUrl(user.avatar)" fit="cover" />
        <div class="user-info-text">
          <h2 class="nickname">{{ user.nickname || user.username }}</h2>
          <p class="signature">{{ user.signature || "这个人很懒，什么都没写~" }}</p>
          <div class="user-meta">
            <el-tag type="success" size="small">信誉分: {{ user.creditScore }}</el-tag>
            <span class="join-time">加入于 {{ formatTime(user.createTime) }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 评价统计 -->
    <el-card v-if="ratingStats" class="stats-card">
      <template #header>
        <span style="font-weight: bold">评价统计</span>
      </template>
      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-value">{{ ratingStats.averageScore || "0.0" }}</span>
          <span class="stat-label">平均评分</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ ratingStats.totalRatings || 0 }}</span>
          <span class="stat-label">评价总数</span>
        </div>
        <div class="stat-item">
          <el-rate v-model="averageScore" disabled show-score style="margin-top: 4px" />
        </div>
      </div>
    </el-card>

    <!-- 收到的评价列表 -->
    <el-card class="ratings-card">
      <template #header>
        <span style="font-weight: bold">收到的评价</span>
      </template>

      <div v-if="ratings.length === 0" class="empty-container">
        <el-empty description="暂无评价" />
      </div>

      <div v-else class="rating-list">
        <div v-for="item in ratings" :key="item.id" class="rating-item">
          <div class="rating-header">
            <div class="rating-user">
              <el-avatar :size="36" :src="getRaterAvatar(item)" fit="cover" />
              <span class="rater-name">{{ getRaterName(item) }}</span>
              <el-rate v-model="item.score" disabled show-score style="margin-left: 10px" />
            </div>
            <span class="time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="rating-content">{{ item.content }}</div>
          <div v-if="item.reply" class="rating-reply">
            <span class="reply-label">回复：</span>{{ item.reply }}
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getUserById, getReceivedRatings, getUserRatingStats } from "@/api";
import { getAvatarUrl } from "@/utils/request";
import { ElMessage } from "element-plus";
import { formatTime } from "@/utils/format";

const route = useRoute();
const router = useRouter();
const loading = ref(true);
const user = ref(null);
const ratings = ref([]);
const ratingStats = ref(null);
const userCache = ref({});
const averageScore = ref(0);
const notFound = ref(false);

const getFullAvatarUrl = (avatar) => getAvatarUrl(avatar);

const getRaterAvatar = (item) => {
  const avatar = userCache.value[item.fromUserId]?.avatar;
  return getFullAvatarUrl(avatar);
};

const getRaterName = (item) => {
  const userInfo = userCache.value[item.fromUserId];
  return userInfo?.nickname || userInfo?.username || "未知用户";
};

const loadData = async () => {
  const userId = route.params.id;

  if (!userId) {
    ElMessage.error("用户ID不存在");
    loading.value = false;
    return;
  }

  try {
    const [userRes, ratingsRes, statsRes] = await Promise.all([
      getUserById(userId),
      getReceivedRatings(userId),
      getUserRatingStats(userId),
    ]);

    if (!userRes) {
      notFound.value = true;
      return;
    }

    user.value = userRes;
    ratings.value = ratingsRes || [];
    ratingStats.value = statsRes || {};

    const score = ratingStats.value.averageScore;
    averageScore.value = score !== undefined && score !== null ? parseFloat(score) : 0;

    for (const item of ratings.value) {
      if (!userCache.value[item.fromUserId]) {
        try {
          const rater = await getUserById(item.fromUserId);
          userCache.value[item.fromUserId] = rater;
        } catch (e) {
          console.error("加载评价者信息失败：", item.fromUserId);
        }
      }
    }
  } catch (e) {
    console.error("加载用户主页失败：", e);
    ElMessage.error("加载用户主页失败");
  } finally {
    loading.value = false;
  }
};

const goBack = () => router.back();

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.profile-container {
  max-width: 900px;
  margin: 80px auto 0;
  padding: 20px;
  background-color: #f9f7e8;
  min-height: calc(100vh - 60px);
}

.user-header {
  display: flex;
  align-items: center;
  gap: 24px;
}

.user-info-text {
  flex: 1;
}

.nickname {
  margin: 0;
  font-size: 22px;
  color: #333;
}

.signature {
  margin: 8px 0;
  font-size: 14px;
  color: #999;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.join-time {
  font-size: 12px;
  color: #bbb;
}

.stats-row {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.rating-list {
  margin-top: 8px;
}

.rating-item {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.rating-item:last-child {
  border-bottom: none;
}

.rating-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.rating-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rater-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.time {
  font-size: 12px;
  color: #999;
}

.rating-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 8px;
}

.rating-reply {
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
}

.reply-label {
  font-weight: 600;
  color: #409eff;
}

.empty-container {
  padding: 30px 0;
}

:deep(.el-card) {
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08) !important;
  border: 1px solid #e8f5e9 !important;
  margin-bottom: 16px;
}
</style>
