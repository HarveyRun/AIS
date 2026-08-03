export function formatDate(value: string, withTime = true): string {
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    ...(withTime ? { hour: "2-digit", minute: "2-digit" } : {}),
  }).format(new Date(value));
}

export function formatMoney(value: number): string {
  return `¥${(Number(value) || 0).toFixed(2)}`;
}

export function formatBytes(value: number): string {
  if (value < 1024) return `${value} B`;
  if (value < 1_048_576) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1_048_576).toFixed(1)} MB`;
}

export function makeId(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

export function hashText(text: string): string {
  let a = 2_166_136_261;
  let b = 5_381;
  for (let index = 0; index < text.length; index += 1) {
    a = Math.imul(a ^ text.charCodeAt(index), 16_777_619);
    b = Math.imul(b, 33) ^ text.charCodeAt(index);
  }
  return `${(a >>> 0).toString(16)}${(b >>> 0).toString(16)}`;
}

export function makeInviteCode(email: string): string {
  const number = Number.parseInt(hashText(email).slice(0, 8), 16) % 1_000_000;
  return `DC-${String(number).padStart(6, "0")}`;
}

export function isSingleIdea(value: string, minimumLength = 6): boolean {
  const text = value.trim();
  return (
    text.length >= minimumLength &&
    text.length <= 80 &&
    !/(并且|同时|以及|另外|还要|顺便)/.test(text) &&
    !/[。；;！？!?]/.test(text.slice(0, -1))
  );
}

export function statusClass(status: string): string {
  if (["已完成", "已通过", "排队中"].includes(status)) return "green";
  if (["待付款", "沟通中"].includes(status)) return "amber";
  if (["不制作", "未通过"].includes(status)) return "red";
  return "";
}
