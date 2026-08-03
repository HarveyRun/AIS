export const MOCK_API_STORAGE_KEY = "diancheng-mock-api-v1";
export const SESSION_STORAGE_KEY = "one-thing-session-v1";
export const API_TOKEN_STORAGE_KEY = "diancheng-api-token-v1";
export const PENDING_LOGIN_KEY = "one-thing-pending-login-v1";
export const ADMIN_EMAIL = "timeline.1994.1976@gmail.com";
export const MASTER_INVITE = "DC-199476";

export const FEEDBACK_CATEGORIES = ["建议", "功能问题", "使用问题"] as const;
export const IDEA_STATUSES = [
  "待评估",
  "待付款",
  "排队中",
  "制作中",
  "已完成",
  "不制作",
] as const;
type IdeaStatusValue = (typeof IDEA_STATUSES)[number];
export const IDEA_STATUS_TRANSITIONS: Record<
  IdeaStatusValue,
  readonly IdeaStatusValue[]
> = {
  待评估: [],
  待付款: ["不制作"],
  排队中: ["制作中"],
  制作中: ["已完成"],
  已完成: [],
  不制作: [],
};
export const TEAM_STATUSES = ["待审核", "沟通中", "已通过", "未通过"] as const;

export const PACKAGES = [
  {
    id: "standard",
    name: "标准套餐",
    levelRange: "LEVEL 1—3",
    price: 25,
    durationDays: 30,
    projectQuota: 2,
    iterationQuota: 4,
    benefits: [
      "每月 2 个微型项目",
      "每月 4 次迭代调整",
      "支持完整源码交付",
      "不支持模块概述梳理、细节定制",
      "不支持对接第三方与需求方",
      "最终成品交付标准由平台审核确认",
    ],
  },
  {
    id: "upgrade",
    name: "升级套餐",
    levelRange: "LEVEL 4—6",
    price: 99,
    durationDays: 30,
    projectQuota: 2,
    iterationQuota: 15,
    featured: true,
    benefits: [
      "每月 2 个小型项目",
      "每月 15 次迭代调整",
      "支持完整源码交付",
      "仅支持模块概述梳理，不支持细节定制",
      "不支持对接第三方与需求方",
      "最终成品交付标准由平台审核确认",
    ],
  },
] as const;

export type PackageId = (typeof PACKAGES)[number]["id"];
