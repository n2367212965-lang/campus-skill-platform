<template>
  <div id="app">
    <Navbar />
    <router-view />
  </div>
</template>

<script setup>
import Navbar from "@/components/Navbar.vue";
</script>

<style>
/* 【优化】全局基础样式 - 校园清新风 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  /* 优化字体：微软雅黑，字间距0.5px，行高1.55 */
  font-family: "Microsoft YaHei", "Inter", sans-serif;
  letter-spacing: 0.5px;
  line-height: 1.55;
}

/* 【优化】全局背景 - 校园清新浅米色 */
body {
  background-color: #f9f7e8; /* 主色调：浅黄色 */
}

#app {
  font-family: "Microsoft YaHei", "Inter", sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* 【优化】全局Element Plus组件基础样式重置 - 校园清新风 */
:root {
  --el-color-primary: #82c91e; /* 调整主色为清新浅绿色系 */
  --el-color-primary-light-9: #e8f5e9; /* 浅绿色背景 */
  --el-color-success: #67c23a;
  --el-color-warning: #e6a23c;
  --el-color-danger: #f56c6c;
  --el-color-info: #909399;
  --el-border-radius-base: 8px; /* 柔和圆角 */
  --el-border-radius-small: 6px;
  --el-shadow-base: 0 1px 3px rgba(0, 0, 0, 0.08); /* 自然柔和阴影 */
}

/* 【优化】全局滚动条样式 - 清新柔和 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 4px;
}
::-webkit-scrollbar-thumb {
  background: #e8f5e9;
  border-radius: 4px;
  border: 1px solid #f9f7e8;
}
::-webkit-scrollbar-thumb:hover {
  background: #d4e6d4;
}

/* 全局组件统一样式（避免各页面重复定义） */
.el-card {
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08) !important;
  border: 1px solid #e8f5e9 !important;
}
.el-button {
  border-radius: 6px !important;
  transition: all 0.2s ease !important;
}
.el-button:hover {
  transform: scale(0.98) !important;
}
.el-dialog {
  border-radius: 10px !important;
}
.el-dialog__header {
  background-color: #e8f5e9 !important;
}
.el-table {
  border-radius: 8px !important;
  overflow: hidden !important;
}
.el-table__header {
  background-color: #e8f5e9 !important;
}
</style>
