import axios from "axios";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import router from "@/router";

/**
 * 后端服务基础地址
 * 开发环境使用localhost:8080
 * 生产环境可改为实际部署地址
 */
export const BASE_URL = "http://localhost:8080";

/**
 * 创建axios实例
 * 配置：超时时间10秒，自动携带Token
 */
const request = axios.create({
  baseURL: BASE_URL,
  timeout: 10000, // 请求超时时间（毫秒）
});

/**
 * 请求拦截器
 * 功能：在每次请求前自动添加JWT Token到请求头
 */
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers["Authorization"] = "Bearer " + userStore.token;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

/**
 * 响应拦截器
 * 功能：
 * 1. 统一处理业务错误（code !== 200）
 * 2. 统一处理HTTP状态码错误（401/404/500等）
 * 3. 自动跳转登录页（Token过期时）
 */
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    // 业务逻辑错误处理（后端返回的code不为200）
    if (res.code !== 200) {
      ElMessage.error(res.message || "请求失败");
      // Token失效或未授权，自动登出并跳转登录页
      if (res.code === 401) {
        const userStore = useUserStore();
        userStore.logout();
        router.push("/login");
      }
      return Promise.reject(new Error(res.message || "请求失败"));
    } else {
      // 成功响应，直接返回data部分（去掉code和message包装）
      return res.data;
    }
  },
  (error) => {
    console.error("请求异常详情：", error);
    // HTTP层面错误处理（网络异常、服务器错误等）
    let errorMsg = "网络错误，请检查网络连接";
    if (error.response) {
      const status = error.response.status;
      if (status === 404) {
        errorMsg = `接口不存在（404）：${error.response.config.url}`;
      } else if (status === 500) {
        errorMsg = "服务器内部错误（500），请联系管理员";
      } else if (status === 401) {
        errorMsg = "登录状态失效，请重新登录";
        const userStore = useUserStore();
        userStore.logout();
        router.push("/login");
      } else {
        errorMsg = `请求失败（${status}）：${error.message}`;
      }
    }
    ElMessage.error(errorMsg);
    return Promise.reject(error);
  },
);

export default request;

/**
 * 统一的头像URL处理函数
 * 所有组件必须使用此函数处理头像地址，确保显示一致
 * @param {string} avatar - 头像路径（可能为空、相对路径或完整URL）
 * @returns {string} 完整的头像URL，无头像时返回空字符串
 */
export function getAvatarUrl(avatar) {
  if (!avatar) return "";
  if (avatar.startsWith("http")) return avatar;
  return `${BASE_URL}${avatar}`;
}
