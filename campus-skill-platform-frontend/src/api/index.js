import request from "@/utils/request";

// ==================== 用户模块 ====================
export function getUserById(userId) {
  return request({ url: `/api/user/${userId}`, method: "get" });
}
export function login(data) {
  return request({ url: "/api/user/login", method: "post", data });
}
export function register(data) {
  return request({ url: "/api/user/register", method: "post", data });
}
export function forgotPassword(data) {
  return request({ url: "/api/user/forgot-password", method: "post", data });
}
export function sendCode(phone) {
  return request({
    url: "/api/user/send-code",
    method: "post",
    params: { phone },
  });
}
export function getUserInfo() {
  return request({ url: "/api/user/info", method: "get" });
}
export function updateUserInfo(data) {
  return request({ url: "/api/user/update", method: "put", data });
}
export function uploadAvatar(file) {
  const formData = new FormData();
  formData.append("file", file);
  return request({
    url: "/api/user/upload-avatar",
    method: "post",
    data: formData,
    headers: { "Content-Type": "multipart/form-data" },
  });
}

// ==================== 技能模块 ====================
export function publishSkill(data) {
  return request({ url: "/api/skill/publish", method: "post", data });
}
export function searchSkills(keyword) {
  return request({
    url: "/api/skill/search",
    method: "get",
    params: { keyword },
  });
}
export function getSkillDetail(id) {
  return request({ url: `/api/skill/${id}`, method: "get" });
}
export function getMySkills() {
  return request({ url: "/api/skill/my", method: "get" });
}
export function offlineSkill(skillId) {
  return request({ url: `/api/skill/offline/${skillId}`, method: "put" });
}
export function onlineSkill(skillId) {
  return request({ url: `/api/skill/online/${skillId}`, method: "put" });
}

// ==================== 需求模块（与技能模块完全对称） ====================
export function publishDemand(data) {
  return request({ url: "/api/demand/publish", method: "post", data });
}
export function getDemandList(params) {
  return request({ url: "/api/demand/list", method: "get", params });
}
export function searchDemands(keyword, category) {
  return request({
    url: "/api/demand/search",
    method: "get",
    params: { keyword, category },
  });
}
export function getDemandDetail(id) {
  return request({ url: `/api/demand/${id}`, method: "get" });
}
export function getMyDemands() {
  return request({ url: "/api/demand/my", method: "get" });
}
export function offlineDemand(demandId) {
  return request({ url: `/api/demand/offline/${demandId}`, method: "put" });
}
export function onlineDemand(demandId) {
  return request({ url: `/api/demand/online/${demandId}`, method: "put" });
}

// ==================== 订单模块 ====================
export function createSkillOrder(skillId, acceptorId) {
  return request({
    url: "/api/order/create-skill",
    method: "post",
    params: { skillId, acceptorId },
  });
}
export function createDemandOrder(demandId, acceptorId) {
  return request({
    url: "/api/order/create-demand",
    method: "post",
    params: { demandId, acceptorId },
  });
}
export function payOrder(orderId, password) {
  return request({
    url: "/api/order/pay",
    method: "post",
    params: { orderId, password },
  });
}
export function startService(orderId) {
  return request({
    url: "/api/order/start",
    method: "put",
    params: { orderId },
  });
}
export function endService(orderId) {
  return request({
    url: "/api/order/end",
    method: "put",
    params: { orderId },
  });
}
export function confirmComplete(orderId) {
  return request({
    url: "/api/order/complete",
    method: "put",
    params: { orderId },
  });
}
export function cancelOrder(orderId, reason) {
  return request({
    url: "/api/order/cancel",
    method: "put",
    params: { orderId, reason },
  });
}
export function getOrdersAsPublisher() {
  return request({ url: "/api/order/as-publisher", method: "get" });
}
export function getOrdersAsAcceptor() {
  return request({ url: "/api/order/as-acceptor", method: "get" });
}
export function getOrderDetail(orderId) {
  return request({ url: `/api/order/detail/${orderId}`, method: "get" });
}

// ==================== 评价模块 ====================
export function submitRating(data) {
  return request({ url: "/api/rating/submit", method: "post", params: data });
}
export function getReceivedRatings(userId) {
  return request({ url: `/api/rating/received/${userId}`, method: "get" });
}
export function getPublishedRatings(userId) {
  return request({ url: `/api/rating/published/${userId}`, method: "get" });
}
export function replyRating(ratingId, reply) {
  return request({
    url: `/api/rating/reply/${ratingId}`,
    method: "post",
    params: { reply },
  });
}
export function getOrderRatings(orderId) {
  return request({ url: `/api/rating/order/${orderId}`, method: "get" });
}
export function getUserRatingStats(userId) {
  return request({ url: `/api/rating/stats/${userId}`, method: "get" });
}

// ==================== 售后模块 ====================
export function createDispute(data) {
  return request({ url: "/api/dispute/create", method: "post", params: data });
}
export function getMyDisputes() {
  return request({ url: "/api/dispute/my", method: "get" });
}
export function getPendingDisputes() {
  return request({ url: "/api/dispute/pending", method: "get" });
}
export function handleDispute(disputeId, handleResult, status) {
  return request({
    url: `/api/dispute/handle/${disputeId}`,
    method: "post",
    params: { handleResult, status },
  });
}

// ==================== 管理员模块 ====================
export function getStatistics() {
  return request({ url: "/api/admin/statistics", method: "get" });
}
export function getPendingSkills() {
  return request({ url: "/api/admin/skill/pending", method: "get" });
}
export function auditSkill(skillId, status, remark) {
  return request({
    url: `/api/admin/skill/audit/${skillId}`,
    method: "post",
    params: { status, remark },
  });
}
export function getPendingDemands() {
  return request({ url: "/api/admin/demand/pending", method: "get" });
}
export function auditDemand(demandId, status, remark) {
  return request({
    url: `/api/admin/demand/audit/${demandId}`,
    method: "post",
    params: { status, remark },
  });
}
export function getAllUsers() {
  return request({ url: "/api/admin/users", method: "get" });
}
export function updateUserStatus(userId, status) {
  return request({
    url: `/api/admin/user/status/${userId}`,
    method: "put",
    params: { status },
  });
}

// ==================== 通知模块 ====================
export function getNotifications() {
  return request({ url: "/api/notification/list", method: "get" });
}
export function getUnreadCount() {
  return request({ url: "/api/notification/unread-count", method: "get" });
}
export function markNotificationRead(id) {
  return request({ url: `/api/notification/read/${id}`, method: "put" });
}
export function markAllNotificationsRead() {
  return request({ url: "/api/notification/read-all", method: "put" });
}
