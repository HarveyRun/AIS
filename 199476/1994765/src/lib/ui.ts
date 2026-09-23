import { immigrationTypeLabels } from "./schema";

/**
 * 纯展示辅助：样式映射。
 * ⚠️ 本文件禁止引入任何 Node API（fs/path）或 @/lib/content，
 * 因为客户端组件会直接 import 此文件。
 */

export { immigrationTypeLabels };

export const typeStyles: Record<string, string> = {
  skilled: "bg-sky-50 text-sky-700 ring-sky-600/20",
  employer: "bg-indigo-50 text-indigo-700 ring-indigo-600/20",
  business: "bg-violet-50 text-violet-700 ring-violet-600/20",
  talent: "bg-fuchsia-50 text-fuchsia-700 ring-fuchsia-600/20",
  family: "bg-teal-50 text-teal-700 ring-teal-600/20",
  study: "bg-cyan-50 text-cyan-700 ring-cyan-600/20",
};

export const statusStyles: Record<string, string> = {
  active: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
  paused: "bg-amber-50 text-amber-700 ring-amber-600/20",
  archived: "bg-slate-100 text-slate-500 ring-slate-500/20",
};

/* 卡片用的标准化数据（可序列化，供客户端组件使用） */

export interface ProgramCardData {
  slug: string;
  name: string;
  nameEn: string;
  summary: string;
  countryId: string;
  countryName: string;
  countryFlag: string;
  regionId?: string;
  regionName?: string;
  type: string;
  typeLabel: string;
  statusLabel?: string;
  /** 所属类型：federal 联邦项目 / regional 本省（州）项目 */
  scope: "federal" | "regional";
  scopeLabel: string;
  infoVerifiedAt: string;
}
