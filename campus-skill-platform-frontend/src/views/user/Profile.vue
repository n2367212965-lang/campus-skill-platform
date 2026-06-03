<template>
  <div class="profile-container">
    <!-- 移除左侧aside菜单 -->
    <el-main class="profile-content">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>个人资料</span>
          </div>
        </template>

        <el-form
          :model="userForm"
          :rules="formRules"
          ref="formRef"
          label-width="100px"
        >
          <el-form-item label="头像">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="handleAvatarUpload"
            >
              <el-avatar :size="80" :src="fullAvatarUrl" fit="cover">
                <!-- 头像加载失败时显示加号图标 -->
                <template #error>
                  <el-icon class="avatar-uploader-icon" :size="30"
                    ><Plus
                  /></el-icon>
                </template>
              </el-avatar>
            </el-upload>
          </el-form-item>

          <el-form-item label="用户名">
            <el-input v-model="userForm.username" disabled />
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="userForm.nickname" placeholder="请输入昵称" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userForm.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item label="联系方式" prop="phone">
            <el-input v-model="userForm.phone" placeholder="请输入手机号" />
          </el-form-item>

          <el-form-item label="个性签名" prop="signature">
            <el-input
              v-model="userForm.signature"
              type="textarea"
              :rows="3"
              placeholder="请输入个性签名"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              @click="handleSave"
              :loading="submitLoading"
              >保存修改</el-button
            >
          </el-form-item>
        </el-form>
      </el-card>
    </el-main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { getUserInfo, updateUserInfo, uploadAvatar } from "@/api";
import { getAvatarUrl } from "@/utils/request";
import { ElMessage } from "element-plus";
import { Plus } from "@element-plus/icons-vue";

const router = useRouter();
const userStore = useUserStore();
const formRef = ref(null);
const submitLoading = ref(false);

const userForm = reactive({
  username: "",
  nickname: "",
  phone: "",
  email: "",
  signature: "",
  avatar: "",
});

const fullAvatarUrl = computed(() => getAvatarUrl(userForm.avatar));

const formRules = {
  nickname: [{ required: true, message: "请输入昵称", trigger: "blur" }],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号",
      trigger: "blur",
    },
  ],
  email: [{ type: "email", message: "请输入正确的邮箱格式", trigger: "blur" }],
};

const initUserForm = async () => {
  try {
    const userData = await getUserInfo();
    userStore.setUserInfo(userData);
    userForm.username = userData.username || "";
    userForm.nickname = userData.nickname || userData.username;
    userForm.phone = userData.phone || "";
    userForm.email = userData.email || "";
    userForm.signature = userData.signature || "";
    userForm.avatar = userData.avatar || "";
  } catch (error) {
    console.error("初始化用户信息失败：", error);
    ElMessage.error("获取个人信息失败，请刷新重试");
  }
};

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith("image/");
  const isLt5M = file.size / 1024 / 1024 < 5;
  if (!isImage) {
    ElMessage.error("只能上传图片文件！");
    return false;
  }
  if (!isLt5M) {
    ElMessage.error("图片大小不能超过5MB！");
    return false;
  }
  return true;
};

const handleAvatarUpload = async (options) => {
  try {
    const avatarUrl = await uploadAvatar(options.file);
    userForm.avatar = avatarUrl;
    userStore.setUserInfo({ avatar: avatarUrl });
    ElMessage.success("头像上传成功");
  } catch (error) {
    console.error("头像上传失败：", error);
    ElMessage.error("头像上传失败，请重试");
  }
};

const handleSave = async () => {
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    await updateUserInfo({
      nickname: userForm.nickname.trim(),
      phone: userForm.phone.trim(),
      email: userForm.email.trim(),
      signature: userForm.signature.trim(),
    });
    userStore.setUserInfo({
      nickname: userForm.nickname,
      phone: userForm.phone,
      email: userForm.email,
      signature: userForm.signature,
    });
    ElMessage.success("保存成功");
  } catch (error) {
    console.error("保存信息失败：", error);
    ElMessage.error("保存失败，请重试");
  } finally {
    submitLoading.value = false;
  }
};

onMounted(() => {
  if (!userStore.token) {
    router.push("/login");
  } else {
    initUserForm();
  }
});
</script>

<style scoped>
/* 【校园清新风优化】外层容器 */
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

/* 【校园清新风优化】卡片头部 */
.card-header {
  font-weight: bold;
  font-size: 18px;
}

/* 【校园清新风优化】卡片 */
:deep(.el-card) {
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08) !important;
  border: 1px solid #e8f5e9 !important;
}

.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader .el-upload:hover {
  border-color: #82c91e;
}

.el-icon.avatar-uploader-icon {
  color: #8c939d;
  width: 80px;
  height: 80px;
  line-height: 80px;
  text-align: center;
}

:deep(.el-button) {
  border-radius: 6px !important;
  transition: all 0.2s ease !important;
}
:deep(.el-button:hover) {
  transform: scale(0.98) !important;
}
</style>
