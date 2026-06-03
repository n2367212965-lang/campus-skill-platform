import { defineStore } from "pinia";
import { ref } from "vue";

export const useUserStore = defineStore("user", () => {
  const token = ref(localStorage.getItem("token") || "");
  const userInfo = ref(
    JSON.parse(
      localStorage.getItem("userInfo") || '{"role":0,"creditScore":100}',
    ),
  );
  const unreadCount = ref(0);

  function setToken(newToken) {
    token.value = newToken;
    localStorage.setItem("token", newToken);
  }

  function setUserInfo(info) {
    userInfo.value = { ...userInfo.value, ...info };
    localStorage.setItem("userInfo", JSON.stringify(userInfo.value));
  }

  function setUnreadCount(count) {
    unreadCount.value = count;
  }

  function decrementUnread() {
    unreadCount.value = Math.max(0, unreadCount.value - 1);
  }

  function clearUnread() {
    unreadCount.value = 0;
  }

  function logout() {
    token.value = "";
    userInfo.value = { role: 0, creditScore: 100 };
    unreadCount.value = 0;
    localStorage.removeItem("token");
    localStorage.removeItem("userInfo");
  }

  return { token, userInfo, unreadCount, setToken, setUserInfo, setUnreadCount, decrementUnread, clearUnread, logout };
});
