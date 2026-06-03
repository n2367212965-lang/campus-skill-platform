<template>
  <div class="login-container">
    <el-card class="login-box">
      <h2 style="text-align: center; margin-bottom: 30px">忘记密码</h2>
      <el-form
        :model="forgotForm"
        :rules="rules"
        ref="forgotFormRef"
        label-width="80px"
      >
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="forgotForm.phone"
            placeholder="请输入注册时的手机号"
          />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div style="display: flex; gap: 10px">
            <el-input v-model="forgotForm.code" placeholder="请输入验证码" />
            <el-button
              type="primary"
              :disabled="countdown > 0"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? countdown + "秒后重试" : "发送验证码" }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="forgotForm.newPassword"
            type="password"
            placeholder="请输入新密码"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="forgotForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            @keyup.enter="handleForgotPassword"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            style="width: 100%"
            :loading="loading"
            @click="handleForgotPassword"
            >重置密码</el-button
          >
        </el-form-item>
        <div style="text-align: center; margin-bottom: 10px">
          <router-link to="/login">想起密码了？去登录</router-link>
        </div>
        <div style="text-align: center">
          <router-link to="/" style="color: #999; font-size: 13px"
            >返回首页</router-link
          >
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { forgotPassword, sendCode } from "@/api";

const router = useRouter();
const forgotFormRef = ref(null);
const loading = ref(false);
const countdown = ref(0);
let timer = null;

const forgotForm = reactive({
  phone: "",
  code: "",
  newPassword: "",
  confirmPassword: "",
});

const rules = {
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
  code: [{ required: true, message: "请输入验证码", trigger: "blur" }],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    {
      min: 6,
      max: 20,
      message: "密码长度必须在6-20个字符之间",
      trigger: "blur",
    },
  ],
  confirmPassword: [
    { required: true, message: "请再次输入新密码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== forgotForm.newPassword) {
          callback(new Error("两次输入的密码不一致"));
        } else callback();
      },
      trigger: "blur",
    },
  ],
};

const handleSendCode = async () => {
  if (!forgotForm.phone) {
    ElMessage.warning("请先输入手机号");
    return;
  }
  try {
    await sendCode(forgotForm.phone);
    ElMessage.success("验证码已发送");
    countdown.value = 60;
    timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) clearInterval(timer);
    }, 1000);
  } catch (error) {
    console.error(error);
  }
};

const handleForgotPassword = async () => {
  await forgotFormRef.value.validate();
  loading.value = true;
  try {
    await forgotPassword({
      phone: forgotForm.phone,
      code: forgotForm.code,
      newPassword: forgotForm.newPassword,
      confirmPassword: forgotForm.confirmPassword,
    });
    ElMessage.success("密码重置成功，请登录");
    router.push("/login");
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
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
