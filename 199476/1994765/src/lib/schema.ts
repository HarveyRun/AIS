import { z } from "zod";

/**
 * 内容数据模型（国家 → 省/州/区 → 项目 三级结构）
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

/** 移民类型（顺序即展示顺序：按普通人的可行性从高到低排序） */
export const immigrationTypes = [
  "study", // 留学转移民：门槛是学费和时间，普通年轻人最可行的主路
  "employer", // 雇主担保：不限学历年龄，瓶颈在拿到合格 offer
  "skilled", // 技术移民：不花钱，但拼语言/学历/年龄，达标率低
  "family", // 家庭团聚：有合格亲属几乎稳过，但适用面窄
  "business", // 投资 / 创业：需要大额资金
  "talent", // 杰出人才：仅适合极少数顶尖文体人士
] as const;
export const immigrationTypeSchema = z.enum(immigrationTypes);

export const immigrationTypeLabels: Record<
  (typeof immigrationTypes)[number],
  string
> = {
  study: "留学转移民",
  employer: "雇主担保",
  skilled: "技术移民",
  family: "家庭团聚",
  business: "投资创业",
  talent: "杰出人才",
};

/** 项目状态 */
export const statusSchema = z.enum(["active", "paused", "archived"]);
export const statusLabels: Record<z.infer<typeof statusSchema>, string> = {
  active: "正常接受申请",
  paused: "暂停 / 关闭中",
  archived: "已停办（存档）",
};

/* ---------------- 基础结构 ---------------- */

const officialSourceSchema = z
  .object({
    label: z.string().min(1, "官方来源需要名称"),
    url: urlString,
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
    /** 联邦与省/州/区政策关系的重点说明 */
    regionalPolicyNote: z.string().min(1),
    officialSite: urlString,
  })
  .strict();

export const regionSchema = z
  .object({
    id: slugString,
    country: slugString,
    name: z.string().min(1, "省/州/区需要中文名"),
    nameEn: z.string().min(1),
    summary: z.string().min(1),
    /** 该省/州/区移民政策概述（一段话） */
    policyOverview: z.string().min(1),
    /** 总体优点（移民相关） */
    pros: z.array(z.string().min(1)).min(1, "至少一条优点"),
    /** 总体缺点/注意点（移民相关） */
    cons: z.array(z.string().min(1)).min(1, "至少一条注意点"),
    officialSite: urlString.optional(),
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
    /** 项目优点（移民相关，事实性描述） */
    pros: z.array(z.string().min(1)).min(1, '至少一条优点'),
    /** 项目缺点/注意点 */
    cons: z.array(z.string().min(1)).min(1, '至少一条注意点'),
    /** 虚构示例案例：用一个具体人物把流程走一遍 */
    case: z
      .object({
        profile: z.string().min(1, "案例人物条件不能为空"),
        steps: z.array(z.string().min(1)).min(2, "案例至少两步"),
        outcome: z.string().min(1, "案例结果不能为空"),
      })
      .strict(),
    /** 内容最后人工核实的日期，页面会显著展示 */
    infoVerifiedAt: dateString,
  })
  .strict();

export type Program = z.infer<typeof programSchema>;
export type Country = z.infer<typeof countrySchema>;
export type Region = z.infer<typeof regionSchema>;

/* ---------------- 站点级内容 ---------------- */

/** 术语分类 */
export const glossaryCategories = ["general", "business"] as const;
export const glossaryCategorySchema = z.enum(glossaryCategories);
export const glossaryCategoryLabels: Record<
  z.infer<typeof glossaryCategorySchema>,
  string
> = {
  general: "通用术语",
  business: "业务术语",
};

/** 术语重要度：排序依据（必知 > 重要 > 了解） */
export const glossaryImportances = ["must", "important", "optional"] as const;
export const glossaryImportanceSchema = z.enum(glossaryImportances);
export const glossaryImportanceLabels: Record<
  z.infer<typeof glossaryImportanceSchema>,
  string
> = {
  must: "必知",
  important: "重要",
  optional: "了解",
};

export const glossaryItemSchema = z
  .object({
    term: z.string().min(1),
    en: z.string().optional(),
    /** 行内悬浮解释用的额外匹配关键词（正文里的常见变体写法） */
    aliases: z.array(z.string().min(1)).optional(),
    /** 该术语适用的国家 id 列表（通用术语填全部国家） */
    countries: z.array(slugString).min(1, "至少标注一个适用国家"),
    category: glossaryCategorySchema,
    /** 重要度，组内按此排序 */
    importance: glossaryImportanceSchema,
    /** 仅地区特有术语需要时标注：该术语专属的省/州/区 id（如 quebec） */
    regions: z.array(slugString).optional(),
    explanation: z.string().min(1),
  })
  .strict();

/** FAQ 分类（展示顺序即小白进阶顺序：先扫盲 → 再选路 → 后钻研） */
export const faqCategories = ["basics", "path", "project"] as const;
export const faqCategorySchema = z.enum(faqCategories);
export const faqCategoryLabels: Record<
  z.infer<typeof faqCategorySchema>,
  string
> = {
  basics: "常识扫盲",
  path: "路径选择",
  project: "项目细节",
};

export const faqSchema = z
  .object({
    country: slugString,
    category: faqCategorySchema,
    question: z.string().min(1),
    answer: z.string().min(1),
  })
  .strict();

export type FaqItem = z.infer<typeof faqSchema>;

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
