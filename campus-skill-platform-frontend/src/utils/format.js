/**
 * 日期时间格式化工具
 * 统一处理后端LocalDateTime格式（ISO 8601，含T分隔符）
 */
export function formatTime(time) {
  if (!time) return "暂无";
  try {
    const formattedTime = time.replace("T", " ");
    const d = new Date(formattedTime);
    if (isNaN(d.getTime())) return "暂无";
    const year = d.getFullYear();
    const month = (d.getMonth() + 1).toString().padStart(2, "0");
    const day = d.getDate().toString().padStart(2, "0");
    const hours = d.getHours().toString().padStart(2, "0");
    const minutes = d.getMinutes().toString().padStart(2, "0");
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  } catch {
    return "暂无";
  }
}
