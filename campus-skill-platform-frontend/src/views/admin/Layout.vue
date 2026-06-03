<template>
  <div class="admin-layout">
    <el-container>
      <el-aside width="200px">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/admin/skill-audit">
            <el-icon><MagicStick /></el-icon>
            <template #title>技能审核</template>
          </el-menu-item>
          <el-menu-item index="/admin/demand-audit">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>需求审核</template>
          </el-menu-item>
          <el-menu-item index="/admin/user-management">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <!-- 新增：售后管理 -->
          <el-menu-item index="/admin/dispute-management">
            <el-icon><Warning /></el-icon>
            <template #title>售后管理</template>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <div class="header-bar">
          <h2>管理后台</h2>
          <el-button @click="$router.push('/')">返回首页</el-button>
        </div>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import {
  MagicStick,
  ChatDotRound,
  User,
  Warning,
} from "@element-plus/icons-vue";

const route = useRoute();
const activeMenu = computed(() => route.path);
</script>

<style scoped>
.admin-layout {
  margin-top: 60px;
  min-height: calc(100vh - 60px);
}

.el-aside {
  background-color: #e8f5e9 !important;
  min-height: calc(100vh - 60px);
  border-right: 1px solid #d4e6d4;
}

:deep(.el-menu) {
  background-color: #e8f5e9 !important;
  border-right: none;
}
:deep(.el-menu-item) {
  color: #555 !important;
  transition: all 0.2s ease;
  border-radius: 6px;
  margin: 4px 8px;
}
:deep(.el-menu-item.is-active) {
  background-color: #82c91e !important;
  color: #fff !important;
}
:deep(.el-menu-item:hover) {
  background-color: #d4e6d4 !important;
  color: #333 !important;
  transform: scale(0.98);
}

.el-main {
  background-color: #f9f7e8 !important;
  padding: 20px;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: #fff;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8f5e9;
}
.header-bar h2 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}
:deep(.header-bar .el-button) {
  border-radius: 6px;
  transition: all 0.2s ease;
}
:deep(.header-bar .el-button:hover) {
  transform: scale(0.98);
}
</style>
