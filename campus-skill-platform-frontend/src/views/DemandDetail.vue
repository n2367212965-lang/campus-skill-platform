<template>
  <div class="detail-page" v-loading="loading">
    <div class="detail-wrapper">
      <el-page-header @back="goBack" content="返回首页" style="margin-bottom: 16px" />

      <el-empty v-if="!demand && !loading" description="需求不存在或已删除" style="padding:60px 0">
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </el-empty>

      <template v-if="demand">
        <div class="hero-card">
          <div class="hero-left">
            <h1 class="hero-title">{{ demand.title }}</h1>
            <div class="hero-meta">
              <el-tag :type="getStatusType(demand.status)" size="small">{{ getStatusText(demand.status) }}</el-tag>
              <span class="meta-divider">|</span>
              <span>{{ demand.category }}</span>
              <span class="meta-divider">|</span>
              <span>{{ formatTime(demand.createTime) }} 发布</span>
            </div>
          </div>
          <div class="hero-right">
            <div class="hero-price">¥{{ demand.expectedPrice || "面议" }}</div>
            <div class="hero-unit">期望 {{ demand.expectedDuration || "?" }}小时</div>
          </div>
        </div>

        <div class="action-bar" v-if="demand.status === 1">
          <template v-if="userStore.token && Number(demand.userId) !== Number(userStore.userInfo.id)">
            <el-button type="primary" size="large" round @click="handleAccept" :loading="btnLoading" style="padding:0 60px;height:48px;font-size:16px">
              立即接单
            </el-button>
          </template>
          <template v-else-if="userStore.token && Number(demand.userId) === Number(userStore.userInfo.id)">
            <el-tag type="info" size="large" style="padding:8px 24px;font-size:14px">这是你发布的需求</el-tag>
          </template>
          <template v-else>
            <el-tag type="warning" size="large" style="padding:8px 24px;font-size:14px">请先登录后接单</el-tag>
          </template>
        </div>

        <el-row :gutter="20">
          <el-col :span="16">
            <el-card class="info-card">
              <template #header><span class="section-title">需求详情</span></template>
              <div class="desc-text">{{ demand.description }}</div>
              <div class="info-grid">
                <div class="info-item"><span class="info-label">期望时长</span><span class="info-value">{{ demand.expectedDuration || "未指定" }} 小时</span></div>
                <div class="info-item"><span class="info-label">期望地点</span><span class="info-value">{{ demand.expectedLocation || "未指定" }}</span></div>
                <div class="info-item" v-if="demand.tags"><span class="info-label">标签</span><span class="info-value">{{ demand.tags }}</span></div>
                <div class="info-item" v-if="demand.contactPhone"><span class="info-label">联系电话</span><span class="info-value" style="color:#52c41a;font-weight:600">{{ demand.contactPhone }}</span></div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="publisher-card">
              <template #header><span class="section-title">发布者</span></template>
              <div class="publisher-inner">
                <el-avatar :size="56" style="margin-bottom:10px">{{ (publisherName || "U").charAt(0) }}</el-avatar>
                <div class="publisher-name" @click="goToUserProfile(demand.userId)">{{ publisherName || "未知用户" }}</div>
                <el-button size="small" link type="primary" @click="goToUserProfile(demand.userId)">查看主页</el-button>
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
import { getDemandDetail, createDemandOrder, getUserInfo, getUserById } from "@/api";
import { useUserStore } from "@/stores/user";
import { ElMessage, ElMessageBox } from "element-plus";
import { formatTime } from "@/utils/format";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const btnLoading = ref(false);
const demand = ref(null);
const publisherName = ref("");

const getStatusType = (s) => ({ 0: "warning", 1: "success", 2: "danger", 3: "info" }[s] || "info");
const getStatusText = (s) => ({ 0: "待审核", 1: "已上架", 2: "已驳回", 3: "已下架" }[s] || "未知");

const loadData = async () => {
  loading.value = true;
  try {
    if (userStore.token) {
      try { const info = await getUserInfo(); userStore.setUserInfo(info); } catch {}
    }
    demand.value = await getDemandDetail(route.params.id);
    if (demand.value?.userId) {
      try {
        const u = await getUserById(demand.value.userId);
        publisherName.value = u?.nickname || u?.username || "未知用户";
      } catch {}
    }
  } catch { ElMessage.error("加载失败"); }
  finally { loading.value = false; }
};

const handleAccept = async () => {
  if (!userStore.token) { ElMessage.warning("请先登录"); router.push("/login"); return; }
  try {
    await ElMessageBox.confirm("确认接该需求吗？", "提示", { type: "warning" });
    btnLoading.value = true;
    await createDemandOrder(demand.value.id, userStore.userInfo.id);
    ElMessage.success("接单成功！");
    router.push("/user/orders");
  } catch (e) { if (e !== "cancel") ElMessage.error("接单失败"); }
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
