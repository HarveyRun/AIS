import { z } from "zod";

/**
 * 内容数据模型（国家 → 省/州 → 项目 三级结构）
 *
 * 设计原则：
 * - 全部 .strict()：内容文件里出现拼写错误的字段名会直接校验失败，而不是被静默忽略
 * - 内容在构建期 / 启动期统一校验，坏数据快速失败并给出文件名与字段级错误信息
 */

/** 校验 YYYY-MM-DD 日期 */
const dateString = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "日期格式必须是 YYYY-MM-DD");

const urlString = z.string().url("必须是合法 URL（含 https://）");

const slugPattern = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const slugString = z
  .string()
  .regex(slugPattern, "slug 只能包含小写字母、数字和中划线（如 ca-on-oinp）");

/* ---------------- 枚举 ---------------- */

/** 移民类型 */
export const immigrationTypes = [
  "skilled", // 技术移民
  "employer", // 雇主担保
  "business", // 投资 / 创业
  "talent", // 杰出人才
  "family", // 家庭团聚
  "study", // 留学转移民
] as const;
export const immigrationTypeSchema = z.enum(immigrationTypes);

export const immigrationTypeLabels: Record<
  (typeof immigrationTypes)[number],
  string
> = {
  skilled: "技术移民",
  employer: "雇主担保",
  business: "投资创业",
  talent: "杰出人才",
  family: "家庭团聚",
  study: "留学转移民",
};

/** 申请难度 */
export const difficultySchema = z.enum(["low", "mid", "high"]);
export const difficultyLabels: Record<
  z.infer<typeof difficultySchema>,
  string
> = {
  low: "门槛较低",
  mid: "难度中等",
  high: "门槛较高",
};

/** 项目状态 */
export const statusSchema = z.enum(["active", "paused", "archived"]);
export const statusLabels: Record<z.infer<typeof statusSchema>, string> = {
  active: "正常接受申请",
  paused: "暂停 / 关闭中",
  archived: "已停办（存档）",
};

/** 预算档位（用于筛选与评估打分，人民币口径） */
export const budgetTiers = ["low", "mid", "high"] as const;
export const budgetTierSchema = z.enum(budgetTiers);
export const budgetTierLabels: Record<z.infer<typeof budgetTierSchema>, string> =
  {
    low: "20 万以内",
    mid: "20–80 万",
    high: "80 万以上",
  };

/* ---------------- 基础结构 ---------------- */

const moneyRangeSchema = z
  .object({
    min: z.number().nonnegative(),
    max: z.number().positive(),
    /** 币种代码，如 CNY / CAD / AUD / USD */
    currency: z.string().min(3).max(3),
  })
  .strict();

const officialSourceSchema = z
  .object({
    label: z.string().min(1, "官方来源需要名称"),
    url: urlString,
  })
  .strict();

/**
 * 机器可读的申请条件摘要，供评估工具打分使用。
 * 与详情页的 requirements 相互独立：这里只保留可量化维度。
 */
export const eligibilitySchema = z
  .object({
    /** 最低学历：1 高中及以下 2 大专 3 本科 4 硕士 5 博士 */
    minEducation: z.number().int().min(1).max(5).optional(),
    /** 最低语言水平：0 零基础 1 入门 2 中等(约雅思5.5-6.5) 3 流利(约雅思7+) */
    minLanguage: z.number().int().min(0).max(3).optional(),
    /** 预算档位下限 */
    minBudgetTier: budgetTierSchema.optional(),
    /** 是否必须先拿到当地雇主 offer */
    needsJobOffer: z.boolean().optional(),
    /** 适合年龄区间 */
    ageRange: z
      .object({ min: z.number().int(), max: z.number().int() })
      .strict()
      .optional(),
    /** 额外说明，会展示在评估结果里 */
    note: z.string().optional(),
  })
  .strict();

/* ---------------- 国家 / 省州 / 项目 ---------------- */

export const countrySchema = z
  .object({
    id: slugString,
    name: z.string().min(1, "国家需要中文名"),
    nameEn: z.string().min(1),
    /** 国旗 emoji，如 🇨🇦 */
    flag: z.string().min(1).max(8),
    /** 一句话简介 */
    summary: z.string().min(1),
    /** 移民体系概述（联邦 vs 地方） */
    systemOverview: z.string().min(1),
    /** 联邦与省/州政策关系的重点说明 */
    regionalPolicyNote: z.string().min(1),
    officialSite: urlString,
  })
  .strict();

export const regionSchema = z
  .object({
    id: slugString,
    country: slugString,
    name: z.string().min(1, "省/州需要中文名"),
    nameEn: z.string().min(1),
    summary: z.string().min(1),
    /** 该省/州移民政策概述与倾向 */
    policyOverview: z.string().min(1),
    officialSite: urlString,
  })
  .strict();

export const programSchema = z
  .object({
    slug: slugString,
    name: z.string().min(1, "项目需要名称"),
    nameEn: z.string().min(1),
    country: slugString,
    /** 省州级项目填写所属省州 id；联邦级项目省略此字段 */
    region: slugString.optional(),
    type: immigrationTypeSchema,
    status: statusSchema.default("active"),
    /** 一句话概括（列表页展示） */
    summary: z.string().min(1),
    /** 适合什么样的人 */
    suitableFor: z.string().min(1),
    difficulty: difficultySchema,
    /** 2-4 个亮点短句 */
    highlights: z.array(z.string().min(1)).min(1).max(6),
    requirements: z
      .array(
        z
          .object({
            title: z.string().min(1),
            detail: z.string().optional(),
          })
          .strict()
      )
      .min(1, "至少填写一条申请条件"),
    cost: z
      .object({
        totalEstimate: moneyRangeSchema,
        /** 费用口径说明，如“以三口之家估算” */
        note: z.string().optional(),
        breakdown: z
          .array(
            z
              .object({
                item: z.string().min(1),
                /** 费用金额用字符串描述，兼容区间与多币种 */
                amount: z.string().min(1),
                note: z.string().optional(),
              })
              .strict()
          )
          .min(1, "至少填写一条费用明细"),
      })
      .strict(),
    duration: z
      .object({
        totalMonths: z
          .object({ min: z.number().positive(), max: z.number().positive() })
          .strict(),
        note: z.string().optional(),
        steps: z
          .array(
            z
              .object({
                title: z.string().min(1),
                detail: z.string().optional(),
                durationHint: z.string().optional(),
              })
              .strict()
          )
          .min(2, "流程至少需要两步"),
      })
      .strict(),
    materials: z.array(z.string().min(1)).min(1, "至少填写一条材料"),
    officialSources: z.array(officialSourceSchema).min(1, "至少一个官方来源"),
    commonRejections: z.array(z.string().min(1)).optional(),
    risks: z.array(z.string().min(1)).optional(),
    eligibility: eligibilitySchema.optional(),
    /** 内容最后人工核实的日期，页面会显著展示 */
    infoVerifiedAt: dateString,
  })
  .strict();

export type Program = z.infer<typeof programSchema>;
export type Country = z.infer<typeof countrySchema>;
export type Region = z.infer<typeof regionSchema>;
export type Eligibility = z.infer<typeof eligibilitySchema>;

/* ---------------- 站点级内容 ---------------- */

export const glossaryItemSchema = z
  .object({
    term: z.string().min(1),
    en: z.string().optional(),
    explanation: z.string().min(1),
  })
  .strict();

export const siteMetaSchema = z
  .object({
    name: z.string().min(1),
    tagline: z.string().min(1),
    description: z.string().min(1),
    /** 全站免责声明 */
    disclaimer: z.string().min(1),
    /** 站点联系/维护者展示名 */
    maintainer: z.string().min(1),
  })
  .strict();

export type GlossaryItem = z.infer<typeof glossaryItemSchema>;
export type SiteMeta = z.infer<typeof siteMetaSchema>;
