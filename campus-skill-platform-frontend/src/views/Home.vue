<template>
  <div class="home">
    <div class="search-section">
      <div class="search-wrapper">
        <el-input
          v-model="keyword"
          placeholder="搜索技能或需求"
          style="width: 500px"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
      </div>
    </div>

    <div class="list-section">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="技能广场" name="skill">
          <div class="action-bar">
            <el-button type="primary" size="large" round @click="$router.push('/publish/skill')" class="publish-btn">
              <el-icon style="margin-right:6px"><Plus /></el-icon>发布新技能
            </el-button>
          </div>
          <el-row :gutter="20">
            <el-col :span="6" v-for="item in skillList" :key="item.id">
              <el-card
                class="card-item"
                shadow="hover"
                @click="$router.push(`/skill/${item.id}`)"
              >
                <template #header>
                  <div class="card-header">
                    <span class="title">{{ item.title }}</span>
                    <el-tag type="success" size="small"
                      >¥{{ item.price }}</el-tag
                    >
                  </div>
                </template>
                <div class="content">
                  <p class="desc">{{ item.description }}</p>
                  <div class="footer">
                    <span>{{ item.location || "未指定地点" }}</span>
                    <span>{{ item.category }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
          <el-empty v-if="skillList.length === 0 && !loading" description="暂无技能" style="margin-top:30px" />
        </el-tab-pane>

        <el-tab-pane label="需求广场" name="demand">
          <div class="action-bar">
            <el-button type="warning" size="large" round @click="$router.push('/publish/demand')" class="publish-btn">
              <el-icon style="margin-right:6px"><Plus /></el-icon>发布新需求
            </el-button>
          </div>
          <el-row :gutter="20">
            <el-col :span="6" v-for="item in demandList" :key="item.id">
              <el-card
                class="card-item"
                shadow="hover"
                @click="$router.push(`/demand/${item.id}`)"
              >
                <template #header>
                  <div class="card-header">
                    <span class="title">{{ item.title }}</span>
                    <el-tag type="warning" size="small">¥{{ item.expectedPrice || "面议" }}</el-tag>
                  </div>
                </template>
                <div class="content">
                  <p class="desc">{{ item.description }}</p>
                  <div class="footer">
                    <span>{{ item.expectedLocation || "未指定地点" }}</span>
                    <span>{{ item.category }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
          <el-empty v-if="demandList.length === 0 && !loading" description="暂无需求" style="margin-top:30px" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { Search, Plus } from "@element-plus/icons-vue";
import { searchSkills, getDemandList, searchDemands } from "@/api";

const keyword = ref("");
const activeTab = ref("skill");
const skillList = ref([]);
const demandList = ref([]);
const loading = ref(false);

const loadSkills = async () => {
  loading.value = true;
  try {
    const data = await searchSkills(keyword.value);
    skillList.value = data || [];
  } catch (e) {
    skillList.value = [];
  } finally { loading.value = false; }
};

const loadDemands = async () => {
  loading.value = true;
  try {
    if (keyword.value) {
      const data = await searchDemands(keyword.value);
      demandList.value = data || [];
    } else {
      const data = await getDemandList({ page: 1, size: 10 });
      demandList.value = data || [];
    }
  } catch (e) {
    demandList.value = [];
  } finally { loading.value = false; }
};

const handleSearch = () => {
  if (activeTab.value === "skill") loadSkills();
  else loadDemands();
};

onMounted(() => {
  loadSkills();
  loadDemands();
});
</script>

<style scoped>
.home {
  padding-top: 80px;
  min-height: 100vh;
  background-color: #f9f7e8;
}
.search-section {
  background: #fff;
  padding: 30px 0;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid #e8f5e9;
}
.search-wrapper {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  justify-content: center;
}
.list-section {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8f5e9;
}

.card-item {
  margin-bottom: 20px;
  cursor: pointer;
  height: 200px;
  border-radius: 8px !important;
  border: 1px solid #e8f5e9 !important;
  transition: all 0.2s ease;
}
.card-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}
.card-header {
  display: flex;
  justify-content: space-between;
  font-weight: bold;
}
.title {
  width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.desc {
  color: #666;
  font-size: 13px;
  height: 60px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  margin: 10px 0;
}
.footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  margin-top: 10px;
}

:deep(.el-button) {
  border-radius: 6px !important;
  transition: all 0.2s ease !important;
}
:deep(.el-button:hover) {
  transform: scale(0.98) !important;
}
.publish-btn {
  padding: 12px 32px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1) !important;
}
.publish-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15) !important;
}
:deep(.el-tabs__header) {
  margin-bottom: 15px !important;
}
</style>
