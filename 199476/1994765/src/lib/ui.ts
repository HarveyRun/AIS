import { difficultyLabels, immigrationTypeLabels } from "./schema";

/**
 * 纯展示辅助：格式化与样式映射。
 * ⚠️ 本文件禁止引入任何 Node API（fs/path）或 @/lib/content，
 * 因为客户端组件会直接 import 此文件。
 */

const currencySymbols: Record<string, string> = {
  CNY: "¥",
  USD: "$",
  CAD: "C$",
  AUD: "A$",
};

export function moneyText(range: {
  min: number;
  max: number;
  currency: string;
}): string {
  const symbol = currencySymbols[range.currency] ?? "";
  const fmt = (v: number): string => {
    if (range.currency === "CNY") {
      return v >= 10000 ? `${trimNum(v / 10000)} 万` : `${v}`;
    }
    return symbol + v.toLocaleString("en-US");
  };
  const unit = range.currency === "CNY" ? "元" : "";
  return `约 ${fmt(range.min)} – ${fmt(range.max)} ${unit}`;
}

export function monthsText(range: { min: number; max: number }): string {
  const fmt = (m: number): string => {
    if (m >= 12) {
      const years = Math.round((m / 12) * 10) / 10;
      return `${years} 年`;
    }
    return `${m} 个月`;
  };
  return range.min === range.max
    ? fmt(range.min)
    : `${fmt(range.min)} – ${fmt(range.max)}`;
}

function trimNum(v: number): number {
  return Math.round(v * 10) / 10;
}

export const difficultyStyles: Record<string, string> = {
  low: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
  mid: "bg-amber-50 text-amber-700 ring-amber-600/20",
  high: "bg-rose-50 text-rose-700 ring-rose-600/20",
};

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

export { difficultyLabels, immigrationTypeLabels };

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
  difficulty: string;
  difficultyLabel: string;
  statusLabel?: string;
  costText: string;
  durationText: string;
  /** 排序/筛选用数值 */
  costMin: number;
  durationMinMonths: number;
  /** 预算门槛档位（无结构化条件时视为 low） */
  budgetTier: "low" | "mid" | "high";
  infoVerifiedAt: string;
}
