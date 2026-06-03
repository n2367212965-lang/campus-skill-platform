<template>
  <div class="my-ratings-page">
    <el-card>
      <template #header>
        <span style="font-weight: bold">我的评价</span>
      </template>

      <el-tabs v-model="activeTab" @tab-change="loadData">
        <el-tab-pane label="我收到的评价" name="received" />
        <el-tab-pane label="我发布的评价" name="published" />
      </el-tabs>

      <el-skeleton v-if="loading" :rows="6" animated />
      <template v-else>
        <div v-if="list.length === 0" class="empty-container">
          <el-empty
            :description="
              activeTab === 'received' ? '暂无收到的评价' : '暂无发布的评价'
            "
          />
        </div>
        <div v-else class="rating-list">
          <div v-for="item in list" :key="item.id" class="rating-item">
            <div class="rating-header">
              <div class="user-info">
                <el-avatar
                  :size="40"
                  :src="getDisplayAvatar(item)"
                  fit="cover"
                />
                <div class="user-detail">
                  <span class="username">{{ getDisplayName(item) }}</span>
                  <el-rate
                    v-model="item.score"
                    disabled
                    show-score
                    style="margin-left: 10px"
                  />
                </div>
              </div>
              <div class="header-right">
                <span class="order-link">
                  订单：
                  <el-button type="link" @click="goToOrderDetail(item.orderId)">
                    查看详情
                  </el-button>
                </span>
                <span class="time">{{ formatTime(item.createTime) }}</span>
              </div>
            </div>
            <div class="rating-content">{{ item.content }}</div>
            <div v-if="item.reply" class="rating-reply">
              <div class="reply-label">回复：</div>
              <div class="reply-content">{{ item.reply }}</div>
            </div>
            <!-- 只有在「我收到的评价」Tab且未回复时才显示回复按钮 -->
            <div
              v-if="activeTab === 'received' && !item.reply"
              class="rating-actions"
            >
              <el-button
                type="primary"
                size="small"
                @click="openReplyDialog(item)"
              >
                回复评价
              </el-button>
            </div>
          </div>
        </div>
      </template>
    </el-card>

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyDialogVisible" title="回复评价" width="500px">
      <el-form :model="replyForm" label-width="80px">
        <el-form-item label="回复内容">
          <el-input
            v-model="replyForm.reply"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replyLoading" @click="submitReply">
          确认回复
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import {
  getReceivedRatings,
  getPublishedRatings,
  replyRating,
  getUserById,
} from "@/api";
import { useUserStore } from "@/stores/user";
import { getAvatarUrl } from "@/utils/request";
import { formatTime } from "@/utils/format";

const router = useRouter();
const userStore = useUserStore();
const activeTab = ref("received");
const list = ref([]);
const loading = ref(false);
const replyDialogVisible = ref(false);
const replyLoading = ref(false);
const currentRating = ref(null);
const replyForm = ref({ reply: "" });
const userCache = ref({});

const getFullAvatarUrl = (avatar) => getAvatarUrl(avatar);

// 核心：根据Tab调用不同的接口
const loadData = async () => {
  loading.value = true;
  try {
    let res = [];
    if (activeTab.value === "received") {
      // 我收到的评价：查询 to_user_id = 我
      res = await getReceivedRatings(userStore.userInfo.id);
    } else {
      // 我发布的评价：查询 from_user_id = 我
      res = await getPublishedRatings(userStore.userInfo.id);
    }
    list.value = res || [];

    // 预加载所有相关用户信息
    for (const item of list.value) {
      // 加载评价发布者信息
      if (!userCache.value[item.fromUserId]) {
        try {
          const user = await getUserById(item.fromUserId);
          userCache.value[item.fromUserId] = user;
        } catch (e) {
          console.error("加载用户信息失败：", item.fromUserId);
        }
      }
      // 加载被评价者信息
      if (!userCache.value[item.toUserId]) {
        try {
          const user = await getUserById(item.toUserId);
          userCache.value[item.toUserId] = user;
        } catch (e) {
          console.error("加载用户信息失败：", item.toUserId);
        }
      }
    }
  } catch (e) {
    console.error("加载评价失败：", e);
    ElMessage.error("加载评价失败");
    list.value = [];
  } finally {
    loading.value = false;
  }
};

// 核心：根据Tab不同显示不同的头像
const getDisplayAvatar = (item) => {
  if (activeTab.value === "received") {
    // 我收到的评价：显示发布者（fromUserId）的头像
    const avatar = userCache.value[item.fromUserId]?.avatar;
    return getFullAvatarUrl(avatar);
  } else {
    // 我发布的评价：显示被评价者（toUserId）的头像
    const avatar = userCache.value[item.toUserId]?.avatar;
    return getFullAvatarUrl(avatar);
  }
};

// 核心：根据Tab不同显示不同的用户名
const getDisplayName = (item) => {
  if (activeTab.value === "received") {
    // 我收到的评价：显示发布者的名字
    const user = userCache.value[item.fromUserId];
    return user?.username || user?.nickname || "未知用户";
  } else {
    // 我发布的评价：显示被评价者的名字
    const user = userCache.value[item.toUserId];
    return user?.username || user?.nickname || "未知用户";
  }
};

const goToOrderDetail = (orderId) => {
  router.push(`/user/order-detail/${orderId}`);
};

const openReplyDialog = (item) => {
  currentRating.value = item;
  replyForm.value.reply = "";
  replyDialogVisible.value = true;
};

const submitReply = async () => {
  if (!replyForm.value.reply.trim()) {
    ElMessage.warning("请输入回复内容");
    return;
  }
  replyLoading.value = true;
  try {
    await replyRating(currentRating.value.id, replyForm.value.reply);
    ElMessage.success("回复成功");
    replyDialogVisible.value = false;
    loadData();
  } catch (e) {
    console.error("回复失败：", e);
    ElMessage.error("回复失败");
  } finally {
    replyLoading.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.my-ratings-page {
  padding: 20px;
  background-color: #f9f7e8;
  min-height: calc(100vh - 120px);
}

.rating-list {
  margin-top: 20px;
}

.rating-item {
  padding: 20px;
  border: 1px solid #e8f5e9;
  border-radius: 8px;
  margin-bottom: 16px;
  background-color: #fff;
  transition: all 0.2s ease;
}

.rating-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.rating-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-detail {
  display: flex;
  align-items: center;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.order-link {
  font-size: 12px;
}

.time {
  font-size: 12px;
  color: #999;
}

.rating-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 12px;
}

.rating-reply {
  padding: 12px;
  background-color: #f0f9ff;
  border-radius: 6px;
  margin-bottom: 12px;
  border-left: 3px solid #409eff;
}

.reply-label {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.reply-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.rating-actions {
  text-align: right;
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
