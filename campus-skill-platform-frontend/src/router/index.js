import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";
import { getUserInfo } from "@/api";

const routes = [
  {
    path: "/",
    name: "Home",
    component: () => import("@/views/Home.vue"),
  },
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/Login.vue"),
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("@/views/Register.vue"),
  },
  {
    path: "/forgot-password",
    name: "ForgotPassword",
    component: () => import("@/views/ForgotPassword.vue"),
  },
  {
    path: "/skill/:id",
    name: "SkillDetail",
    component: () => import("@/views/SkillDetail.vue"),
  },
  {
    path: "/demand/:id",
    name: "DemandDetail",
    component: () => import("@/views/DemandDetail.vue"),
  },
  {
    path: "/user-profile/:id",
    name: "UserProfile",
    component: () => import("@/views/UserProfile.vue"),
  },
  {
    path: "/publish/skill",
    name: "PublishSkill",
    component: () => import("@/views/PublishSkill.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/publish/demand",
    name: "PublishDemand",
    component: () => import("@/views/PublishDemand.vue"),
    meta: { requiresAuth: true },
  },
  // 用户中心
  {
    path: "/user",
    name: "UserCenter",
    component: () => import("@/views/user/Layout.vue"),
    meta: { requiresAuth: true },
    redirect: "/user/profile",
    children: [
      { path: "profile", component: () => import("@/views/user/Profile.vue") },
      { path: "orders", component: () => import("@/views/user/Orders.vue") },
      {
        path: "order-detail/:id",
        component: () => import("@/views/user/OrderDetail.vue"),
      },
      {
        path: "my-skills",
        component: () => import("@/views/user/MySkills.vue"),
      },
      {
        path: "my-demands",
        component: () => import("@/views/user/MyDemands.vue"),
      },
      // 新增：我的评价
      {
        path: "my-ratings",
        component: () => import("@/views/user/MyRatings.vue"),
      },
      // 新增：我的售后
      {
        path: "my-disputes",
        component: () => import("@/views/user/MyDisputes.vue"),
      },
      // 新增：我的通知
      {
        path: "notifications",
        component: () => import("@/views/user/Notifications.vue"),
      },
    ],
  },
  // 管理员后台
  {
    path: "/admin",
    name: "Admin",
    component: () => import("@/views/admin/Layout.vue"),
    meta: { requiresAuth: true, requiresAdmin: true },
    redirect: "/admin/skill-audit",
    children: [
      {
        path: "skill-audit",
        name: "SkillAudit",
        component: () => import("@/views/admin/SkillAudit.vue"),
      },
      {
        path: "demand-audit",
        name: "DemandAudit",
        component: () => import("@/views/admin/DemandAudit.vue"),
      },
      {
        path: "user-management",
        name: "UserManagement",
        component: () => import("@/views/admin/UserManagement.vue"),
      },
      // 新增：售后管理
      {
        path: "dispute-management",
        name: "DisputeManagement",
        component: () => import("@/views/admin/DisputeManagement.vue"),
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore();

  if (to.meta.requiresAuth && !userStore.token) {
    next("/login");
    return;
  }

  if (to.meta.requiresAdmin) {
    if (!userStore.userInfo.id && userStore.token) {
      try {
        const info = await getUserInfo();
        userStore.setUserInfo(info);
      } catch (e) {
        userStore.logout();
        next("/login");
        return;
      }
    }

    if (userStore.userInfo.role !== 1) {
      ElMessage.error("无权访问，需要管理员权限");
      next("/");
      return;
    }
  }

  next();
});

export default router;
