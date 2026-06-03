<template>
  <div class="detail-page" v-loading="loading">
    <div class="detail-wrapper">
      <!-- 返回 -->
      <el-page-header @back="goBack" content="返回首页" style="margin-bottom: 16px" />

      <el-empty v-if="!skill && !loading" description="技能不存在或已删除" style="padding:60px 0">
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </el-empty>

      <template v-if="skill">
        <!-- 顶部：标题+状态+价格 -->
        <div class="hero-card">
          <div class="hero-left">
            <h1 class="hero-title">{{ skill.title }}</h1>
            <div class="hero-meta">
              <el-tag :type="SKILL_STATUS[skill.status]?.type" size="small">{{ SKILL_STATUS[skill.status]?.text }}</el-tag>
              <span class="meta-divider">|</span>
              <span>{{ skill.category }}</span>
              <span class="meta-divider">|</span>
              <span>{{ formatTime(skill.createTime) }} 发布</span>
            </div>
          </div>
          <div class="hero-right">
            <div class="hero-price">¥{{ skill.price }}</div>
            <div class="hero-unit">/ {{ skill.duration }}小时</div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-bar" v-if="skill.status === 1">
          <template v-if="userStore.token && Number(skill.userId) !== Number(userStore.userInfo.id)">
            <el-button type="primary" size="large" round @click="handleAccept" :loading="btnLoading" style="padding:0 60px;height:48px;font-size:16px">
              立即预约
            </el-button>
          </template>
          <template v-else-if="userStore.token && Number(skill.userId) === Number(userStore.userInfo.id)">
            <el-tag type="info" size="large" style="padding:8px 24px;font-size:14px">这是你发布的技能</el-tag>
          </template>
          <template v-else>
            <el-tag type="warning" size="large" style="padding:8px 24px;font-size:14px">请先登录后再预约</el-tag>
          </template>
        </div>

        <!-- 详情+发布者 -->
        <el-row :gutter="20">
          <el-col :span="16">
            <el-card class="info-card">
              <template #header><span class="section-title">服务详情</span></template>
              <div class="desc-text">{{ skill.description }}</div>
              <div class="info-grid">
                <div class="info-item"><span class="info-label">服务时长</span><span class="info-value">{{ skill.duration }} 小时</span></div>
                <div class="info-item"><span class="info-label">服务地点</span><span class="info-value">{{ skill.location || "未指定" }}</span></div>
                <div class="info-item" v-if="skill.tags"><span class="info-label">标签</span><span class="info-value">{{ skill.tags }}</span></div>
                <div class="info-item" v-if="skill.contactPhone"><span class="info-label">联系电话</span><span class="info-value" style="color:#52c41a;font-weight:600">{{ skill.contactPhone }}</span></div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="publisher-card">
              <template #header><span class="section-title">发布者</span></template>
              <div class="publisher-inner">
                <el-avatar :size="56" style="margin-bottom:10px">{{ (publisherName || "U").charAt(0) }}</el-avatar>
                <div class="publisher-name" @click="goToUserProfile(skill.userId)">{{ publisherName || "未知用户" }}</div>
                <el-button size="small" link type="primary" @click="goToUserProfile(skill.userId)">查看主页</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getSkillDetail, createSkillOrder, getUserInfo, getUserById } from "@/api";
import { useUserStore } from "@/stores/user";
import { SKILL_STATUS } from "@/constants";
import { ElMessage, ElMessageBox } from "element-plus";
import { formatTime } from "@/utils/format";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const btnLoading = ref(false);
const skill = ref(null);
const publisherName = ref("");

const loadData = async () => {
  loading.value = true;
  try {
    if (userStore.token) {
      try { const info = await getUserInfo(); userStore.setUserInfo(info); } catch {}
    }
    skill.value = await getSkillDetail(route.params.id);
    if (skill.value?.userId) {
      try {
        const u = await getUserById(skill.value.userId);
        publisherName.value = u?.nickname || u?.username || "未知用户";
      } catch {}
    }
  } catch { ElMessage.error("加载失败"); }
  finally { loading.value = false; }
};

const handleAccept = async () => {
  if (!userStore.token) { ElMessage.warning("请先登录"); router.push("/login"); return; }
  try {
    await ElMessageBox.confirm("确认预约该技能吗？", "提示", { type: "warning" });
    btnLoading.value = true;
    await createSkillOrder(skill.value.id, userStore.userInfo.id);
    ElMessage.success("预约成功！");
    router.push("/user/orders");
  } catch (e) { if (e !== "cancel") ElMessage.error("预约失败"); }
  finally { btnLoading.value = false; }
};

const goBack = () => router.push("/");
const goToUserProfile = (id) => { if (id) router.push(`/user-profile/${id}`); };

onMounted(() => loadData());
</script>

<style scoped>
.detail-page { background: #f9f7e8; min-height: 100vh; padding-top: 80px; }
.detail-wrapper { max-width: 960px; margin: 0 auto; padding: 0 20px 40px; }

.hero-card {
  background: #fff; border-radius: 12px; padding: 32px; margin-bottom: 16px;
  display: flex; justify-content: space-between; align-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04); border: 1px solid #e8f5e9;
}
.hero-title { font-size: 24px; font-weight: 700; color: #1a1a1a; margin: 0 0 12px; }
.hero-meta { font-size: 13px; color: #999; display: flex; align-items: center; gap: 8px; }
.meta-divider { color: #ddd; }
.hero-right { text-align: right; }
.hero-price { font-size: 40px; font-weight: 700; color: #e64340; line-height: 1.2; }
.hero-unit { font-size: 13px; color: #999; margin-top: 4px; }

.action-bar { text-align: center; margin-bottom: 20px; }

.info-card, .publisher-card {
  border-radius: 10px; border: 1px solid #e8f5e9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.section-title { font-size: 15px; font-weight: 600; color: #333; }
.desc-text { color: #555; font-size: 15px; line-height: 1.8; margin-bottom: 24px; white-space: pre-wrap; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 12px; color: #999; }
.info-value { font-size: 14px; color: #333; }

.publisher-inner { display: flex; flex-direction: column; align-items: center; }
.publisher-name { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 6px; cursor: pointer; }
.publisher-name:hover { color: #52c41a; }
</style>
