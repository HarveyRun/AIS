import { difficultyLabels, immigrationTypeLabels, statusLabels } from "./schema";
import type { Program } from "./schema";
import { getCountry, getRegion } from "./content";
import { moneyText, monthsText, type ProgramCardData } from "./ui";

/**
 * 服务端数据组装层：把内容层的 Program 转换为可序列化的展示数据。
 * ⚠️ 依赖文件系统的 content 层，只能在服务端组件中使用；
 * 客户端组件请直接 import @/lib/ui（纯函数）。
 */

export { moneyText, monthsText };
export type { ProgramCardData };

/** Program → 卡片展示数据（服务端组装，客户端只做渲染） */
export function toCardData(p: Program): ProgramCardData {
  const country = getCountry(p.country);
  const region = p.region ? getRegion(p.country, p.region) : undefined;
  return {
    slug: p.slug,
    name: p.name,
    nameEn: p.nameEn,
    summary: p.summary,
    countryId: p.country,
    countryName: country?.name ?? p.country,
    countryFlag: country?.flag ?? "🌐",
    regionId: p.region,
    regionName: region?.name,
    type: p.type,
    typeLabel: immigrationTypeLabels[p.type],
    difficulty: p.difficulty,
    difficultyLabel: difficultyLabels[p.difficulty],
    statusLabel: p.status !== "active" ? statusLabels[p.status] : undefined,
    costText: moneyText(p.cost.totalEstimate),
    durationText: monthsText(p.duration.totalMonths),
    costMin: p.cost.totalEstimate.min,
    durationMinMonths: p.duration.totalMonths.min,
    budgetTier: p.eligibility?.minBudgetTier ?? "low",
    infoVerifiedAt: p.infoVerifiedAt,
  };
}
