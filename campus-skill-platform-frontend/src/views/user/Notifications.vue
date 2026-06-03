<template>
  <div class="notifications-page">
    <div class="page-header">
      <h3>我的通知</h3>
      <el-button type="primary" size="small" @click="handleMarkAllRead" :disabled="localUnreadCount === 0">
        全部已读
      </el-button>
    </div>

    <el-empty v-if="notifications.length === 0" description="暂无通知" />

    <div v-else class="notification-list">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: item.isRead === 0 }"
        @click="handleClick(item)"
      >
        <div class="notification-dot" v-if="item.isRead === 0"></div>
        <div class="notification-content">
          <div class="notification-title">{{ item.title }}</div>
          <div class="notification-text">{{ item.content }}</div>
          <div class="notification-time">{{ formatTime(item.createTime) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import {
  getNotifications,
  markNotificationRead,
  markAllNotificationsRead,
} from "@/api";
import { ElMessage } from "element-plus";
import { formatTime } from "@/utils/format";

const router = useRouter();
const userStore = useUserStore();
const notifications = ref([]);
const localUnreadCount = ref(0);

const loadData = async () => {
  try {
    const data = await getNotifications();
    notifications.value = data || [];
    localUnreadCount.value = notifications.value.filter((n) => n.isRead === 0).length;
    userStore.setUnreadCount(localUnreadCount.value);
  } catch (e) {
    ElMessage.error("加载通知失败");
  }
};

const handleClick = async (item) => {
  if (item.isRead === 0) {
    try {
      await markNotificationRead(item.id);
      item.isRead = 1;
      localUnreadCount.value = Math.max(0, localUnreadCount.value - 1);
      userStore.decrementUnread();
    } catch (e) {
      // ignore
    }
  }
  if (item.relatedId) {
    // 通过通知内容语义判断当前用户视角：含"你成功"表示接单方视角
    const isAcceptorView = item.content.includes("你成功");
    const tab = isAcceptorView ? "acceptor" : "publisher";
    if (
      item.type === "ORDER_CREATED" ||
      item.type === "ORDER_PAID" ||
      item.type === "ORDER_STARTED" ||
      item.type === "ORDER_ENDED" ||
      item.type === "ORDER_COMPLETED" ||
      item.type === "ORDER_CANCELLED"
    ) {
      router.push(`/user/order-detail/${item.relatedId}?tab=${tab}`);
    } else if (item.type === "DISPUTE_CREATED" || item.type === "DISPUTE_RESOLVED") {
      router.push("/user/my-disputes");
    } else if (item.type === "SKILL_AUDITED") {
      router.push("/user/my-skills");
    } else if (item.type === "DEMAND_AUDITED") {
      router.push("/user/my-demands");
    }
  }
};

const handleMarkAllRead = async () => {
  try {
    await markAllNotificationsRead();
    notifications.value.forEach((n) => (n.isRead = 1));
    localUnreadCount.value = 0;
    userStore.clearUnread();
    ElMessage.success("已全部标记为已读");
  } catch (e) {
    ElMessage.error("操作失败");
  }
};

onMounted(loadData);
</script>

<style scoped>
.notifications-page {
  max-width: 800px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.notification-list {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8f5e9;
  overflow: hidden;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background-color: #f9f7e8;
}

.notification-item.unread {
  background-color: #f0f9eb;
}

.notification-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #52c41a;
  margin-top: 6px;
  margin-right: 12px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.notification-text {
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
  line-height: 1.5;
}

.notification-time {
  font-size: 12px;
  color: #999;
}
</style>
