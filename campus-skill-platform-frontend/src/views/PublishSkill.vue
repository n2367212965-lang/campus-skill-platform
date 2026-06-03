<template>
  <div class="publish-container">
    <el-card>
      <template #header>
        <span style="font-weight: bold; font-size: 16px">发布新技能</span>
      </template>
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="120px"
        style="max-width: 800px; margin: 0 auto"
      >
        <el-form-item label="技能标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入技能标题（如：Python编程辅导）"
          />
        </el-form-item>
        <el-form-item label="技能分类" prop="category">
          <el-select
            v-model="form.category"
            placeholder="请选择分类"
            style="width: 100%"
          >
            <el-option label="家教辅导" value="家教辅导" />
            <el-option label="IT技术" value="IT技术" />
            <el-option label="语言学习" value="语言学习" />
            <el-option label="艺术设计" value="艺术设计" />
            <el-option label="运动健身" value="运动健身" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="技能标签" prop="tags">
          <el-input v-model="form.tags" placeholder="标签用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="服务价格 (元)" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0.01"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="服务时长 (小时)" prop="duration">
          <el-input-number
            v-model="form.duration"
            :min="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="服务地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入服务地点" />
        </el-form-item>
        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            placeholder="请输入详细描述，至少10字"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading"
            >提交发布</el-button
          >
          <el-button @click="$router.push('/')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { publishSkill } from "@/api";
import { ElMessage } from "element-plus";

const router = useRouter();
const formRef = ref(null);
const loading = ref(false);

const form = reactive({
  title: "",
  category: "",
  tags: "",
  price: null,
  duration: 1,
  location: "",
  description: "",
});

// ✅ 全中文提示语 + 字数改为至少10字
const rules = {
  title: [
    { required: true, message: "请输入技能标题", trigger: "blur" },
    { min: 2, max: 50, message: "标题长度在2-50个字符", trigger: "blur" },
  ],
  category: [{ required: true, message: "请选择技能分类", trigger: "change" }],
  tags: [
    { required: true, message: "请输入技能标签", trigger: "blur" },
    { min: 2, max: 100, message: "标签长度在2-100个字符", trigger: "blur" },
  ],
  price: [{ required: true, message: "请输入服务价格", trigger: "blur" }],
  duration: [{ required: true, message: "请输入服务时长", trigger: "blur" }],
  location: [
    { required: true, message: "请输入服务地点", trigger: "blur" },
    { min: 2, max: 100, message: "地点长度在2-100个字符", trigger: "blur" },
  ],
  description: [
    { required: true, message: "请输入详细描述", trigger: "blur" },
    {
      min: 10,
      max: 500,
      message: "详细描述至少10个字，最多500个字",
      trigger: "blur",
    },
  ],
};

const handleSubmit = async () => {
  try {
    await formRef.value.validate();
    loading.value = true;
    await publishSkill(form);
    ElMessage.success("发布成功，等待审核");
    router.push("/");
  } catch (error) {
    ElMessage.error("发布失败");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.publish-container {
  max-width: 900px;
  margin: 80px auto 0;
  padding: 20px;
  background-color: #f9f7e8;
}

:deep(.el-card) {
  border-radius: 8px !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08) !important;
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
