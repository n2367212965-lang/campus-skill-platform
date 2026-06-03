<template>
  <div class="login-container">
    <el-card class="login-box">
      <h2 style="text-align: center; margin-bottom: 30px">用户登录</h2>
      <el-form
        :model="loginForm"
        :rules="rules"
        ref="loginFormRef"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            style="width: 100%"
            :loading="loading"
            @click="handleLogin"
            >登录</el-button
          >
        </el-form-item>
        <div style="text-align: center; margin-bottom: 10px">
          <router-link to="/forgot-password">忘记密码？</router-link>
        </div>
        <div style="text-align: center; margin-bottom: 10px">
          <router-link to="/register">还没有账号？去注册</router-link>
        </div>
        <div style="text-align: center">
          <router-link to="/" style="color:#999;font-size:13px">返回首页</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { login, getUserInfo } from "@/api";
import { ElMessage } from "element-plus";

const router = useRouter();
const userStore = useUserStore();
const loginFormRef = ref(null);
const loading = ref(false);

const loginForm = reactive({
  username: "",
  password: "",
});

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

const handleLogin = async () => {
  await loginFormRef.value.validate();
  loading.value = true;
  try {
    const token = await login(loginForm);
    userStore.setToken(token);
    const info = await getUserInfo();
    userStore.setUserInfo(info);
    ElMessage.success("登录成功");
    router.push(info.role === 1 ? "/admin" : "/");
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* 【校园清新风优化】登录页 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f9f7e8;
  padding-top: 60px;
}
.login-box {
  width: 450px;
  border-radius: 12px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
  border: 1px solid #e8f5e9 !important;
}

:deep(.el-button) {
  border-radius: 6px !important;
  transition: all 0.2s ease !important;
}
:deep(.el-button:hover) {
  transform: scale(0.98) !important;
}
</style>
