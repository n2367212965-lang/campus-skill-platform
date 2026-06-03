<template>
  <el-header class="navbar">
    <div class="nav-container">
      <!-- Logo区域 -->
      <div class="logo" @click="$router.push('/')">校园技能互助平台</div>

      <!-- 原生导航菜单：移除我的订单/技能/需求，新增个人中心 -->
      <div class="main-menu">
        <div class="menu-list">
          <!-- 首页大厅 -->
          <div
            class="menu-item"
            :class="{ active: activePath === '/' }"
            @click="$router.push('/')"
          >
            首页大厅
          </div>

          <template v-if="userStore.token">
            <!-- 用户中心 -->
            <div
              class="menu-item"
              :class="{ active: activePath.startsWith('/user') }"
              @click="$router.push('/user/profile')"
            >
              用户中心
            </div>
            <!-- 管理员专属：管理中心 -->
            <div
              v-if="userStore.userInfo.role === 1"
              class="menu-item"
              :class="{ active: activePath.startsWith('/admin') }"
              @click="$router.push('/admin/skill-audit')"
            >
              管理中心
            </div>
          </template>
        </div>
      </div>

      <!-- 用户操作区：头像下拉仅保留退出登录 -->
      <div class="user-actions">
        <template v-if="userStore.token">
          <div class="user-info">
            <el-tag
              v-if="userStore.userInfo.role === 1"
              type="danger"
              size="small"
              style="margin-right: 10px"
              >管理员</el-tag
            >
            <span class="credit"
              >信誉分: {{ userStore.userInfo.creditScore || 100 }}</span
            >

            <!-- 通知铃铛 -->
            <el-badge
              :value="userStore.unreadCount"
              :hidden="userStore.unreadCount === 0"
              :max="99"
              class="notification-badge"
            >
              <el-icon
                class="notification-bell"
                @click="$router.push('/user/notifications')"
              >
                <Bell />
              </el-icon>
            </el-badge>

            <!-- 头像点击跳转个人中心 -->
            <el-avatar
              :size="30"
              style="cursor: pointer"
              :src="fullAvatarUrl"
              fit="cover"
              @click="$router.push('/user/profile')"
            >
              <!-- 头像加载失败时显示首字母兜底 -->
              <template #error>
                {{
                  userStore.userInfo.nickname
                    ? userStore.userInfo.nickname.charAt(0)
                    : "U"
                }}
              </template>
            </el-avatar>

            <!-- 退出登录按钮 -->
            <el-button type="text" size="small" class="logout-btn" @click="handleLogout">
              退出登录
            </el-button>
          </div>
        </template>
        <template v-else>
          <el-button type="primary" size="small" @click="$router.push('/login')"
            >登录</el-button
          >
        </template>
      </div>
    </div>
  </el-header>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { getUserInfo, getUnreadCount } from "@/api";
import { getAvatarUrl } from "@/utils/request";
import { ElMessage } from "element-plus";
import { Bell } from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const fullAvatarUrl = computed(() => getAvatarUrl(userStore.userInfo?.avatar));

let pollTimer = null;

const fetchUnreadCount = async () => {
  if (!userStore.token) return;
  try {
    const data = await getUnreadCount();
    userStore.setUnreadCount(data || 0);
  } catch (e) {
    // ignore
  }
};

// 核心：手动控制激活态（个人中心匹配/user开头的路径）
const activePath = computed(() => route.path);

// 退出处理（保留原有逻辑）
const handleLogout = () => {
  userStore.logout();
  router.push("/login");
  ElMessage.success("退出成功");
};

onMounted(async () => {
  if (userStore.token && !userStore.userInfo.id) {
    try {
      const userFullInfo = await getUserInfo();
      userStore.setUserInfo(userFullInfo);
    } catch (e) {
      console.error("获取用户信息失败：", e);
      userStore.logout();
      ElMessage.error("登录状态失效，请重新登录");
    }
  }
  fetchUnreadCount();
  pollTimer = setInterval(fetchUnreadCount, 30000);
});

watch(
  () => userStore.token,
  (newToken) => {
    if (newToken) {
      fetchUnreadCount();
    } else {
      userStore.setUnreadCount(0);
    }
  },
);

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
});
</script>

<style scoped>
/* 【优化】导航栏整体样式 - 校园清新风 */
.navbar {
  background-color: #f9f7e8; /* 替换深色为浅黄色主色调 */
  padding: 0 20px;
  height: 60px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  /* 柔和阴影：模糊3px，偏移1px，自然不突兀 */
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid #e8f5e9; /* 浅绿色边框增强清新感 */
}

.nav-container {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  gap: 20px;
}

/* 【优化】Logo样式 - 清新配色+字体优化 */
.logo {
  color: #52c41a; /* 清新绿色文字 */
  font-size: 20px;
  font-weight: 600; /* 适度加粗，不夸张 */
  cursor: pointer;
  width: 200px;
  flex-shrink: 0;
  /* 轻微hover效果 */
  transition: all 0.2s ease;
}
.logo:hover {
  transform: scale(0.98); /* 轻微缩放，细腻交互 */
  color: #67c23a;
}

/* 原生导航菜单容器 */
.main-menu {
  flex: 1;
  height: 100%;
  display: flex;
  align-items: center;
}

/* 菜单列表：横向排列所有项 */
.menu-list {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 0; /* 无间距，和原样式一致 */
}

/* 【优化】单个菜单项 - 清新风样式调整 */
.menu-item {
  height: 100%;
  line-height: 60px;
  padding: 0 15px;
  font-size: 14px;
  color: #666; /* 柔和灰色文字 */
  cursor: pointer;
  box-sizing: border-box;
  transition: all 0.2s ease; /* 顺滑过渡 */
  position: relative;
}

/* 【优化】激活态/hover态 - 清新配色+细腻效果 */
.menu-item.active,
.menu-item:hover {
  color: #52c41a; /* 清新绿色 */
  background-color: #e8f5e9; /* 浅绿色背景 */
  /* 底部小线条标识激活态，更细腻 */
}
.menu-item.active::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background-color: #52c41a;
  border-radius: 1px;
}

/* 【优化】用户操作区 - 布局+样式优化 */
.user-actions {
  display: flex;
  align-items: center;
  width: 320px;
  justify-content: flex-end;
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  margin-right: 15px;
}

/* 【优化】信誉分样式 - 清新配色 */
.credit {
  color: #faad14; /* 柔和黄色，贴合主色调 */
  font-size: 12px;
  margin-right: 10px;
  font-weight: 500;
}

/* 【优化】下拉菜单样式 - 校园清新风 */
:deep(.el-dropdown-menu) {
  padding: 8px 0;
  border-radius: 8px; /* 柔和圆角 */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08); /* 自然阴影 */
  border: 1px solid #e8f5e9; /* 浅绿色边框 */
  background-color: #ffffff;
}

:deep(.el-dropdown-item) {
  padding: 10px 20px;
  transition: all 0.2s ease;
}

:deep(.el-dropdown-item:hover) {
  background-color: #e8f5e9; /* 浅绿色hover背景 */
  color: #52c41a;
  transform: scale(0.98); /* 轻微缩放 */
}

/* 【优化】登录按钮样式 - 贴合清新风 */
:deep(.el-button--primary) {
  background-color: #e8f5e9;
  border-color: #52c41a;
  color: #52c41a;
  border-radius: 6px;
  transition: all 0.2s ease;
}
:deep(.el-button--primary:hover) {
  background-color: #52c41a;
  border-color: #52c41a;
  color: #ffffff;
  transform: scale(0.98);
}

/* 【优化】管理员标签样式 - 柔和配色 */
:deep(.el-tag--danger) {
  background-color: #fff1f0;
  border-color: #ffa39e;
  color: #f56c6c;
  border-radius: 4px;
}

.notification-badge {
  margin-right: 10px;
  cursor: pointer;
}
.notification-bell {
  font-size: 20px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}
.notification-bell:hover {
  color: #52c41a;
}

.logout-btn {
  color: #f56c6c;
  font-size: 13px;
  padding: 0 4px;
  margin-left: 8px;
  border-radius: 4px;
}
.logout-btn:hover {
  background-color: #fff1f0;
}
</style>
