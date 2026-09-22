import type { Eligibility } from "./schema";

/**
 * 智能评估：问卷定义 + 规则打分。
 * 纯函数、无副作用，方便单元测试与后续替换为更复杂的打分模型。
 *
 * 打分依据项目数据里的 eligibility 字段（机器可读申请条件），
 * 因此新增项目时只要填写 eligibility，评估工具自动纳入推荐。
 */

export interface AssessmentAnswers {
  /** 意向国家 id；["any"] 表示未定 */
  targetCountries: string[];
  /** 学历 1-5：1 高中及以下 2 大专 3 本科 4 硕士 5 博士 */
  education: 1 | 2 | 3 | 4 | 5;
  /** 语言 0-3：0 零基础 1 入门 2 中等 3 流利 */
  language: 0 | 1 | 2 | 3;
  /** 预算档位 */
  budgetTier: "low" | "mid" | "high";
  /** 全职工作经验（年） */
  workYears: 0 | 1 | 2 | 3; // 0:<1年 1:1-3年 2:4-6年 3:7年以上
  age: number | null;
  /** 是否已拿到当地雇主 offer */
  hasJobOffer: boolean;
  /** 是否有海外亲属（配偶/父母/子女为公民或永居） */
  hasFamilyAbroad: boolean;
}

export interface MatchGap {
  dimension: string;
  detail: string;
}

export interface ProgramMatch {
  slug: string;
  score: number; // 0-100
  matched: string[];
  gaps: MatchGap[];
}

export const assessmentQuestionsMeta = {
  educationLevels: ["高中及以下", "大专", "本科", "硕士", "博士"],
  languageLevels: ["零基础", "入门（约雅思4-5）", "中等（约雅思5.5-6.5）", "流利（雅思7+）"],
  budgetLevels: ["20 万以内", "20–80 万", "80 万以上"],
  workYearLevels: ["不足 1 年", "1–3 年", "4–6 年", "7 年以上"],
} as const;

const BUDGET_ORDER = { low: 0, mid: 1, high: 2 } as const;

/** 工作经验加分：项目未声明则作为软性参考 */
function workExperienceScore(yearsBucket: 0 | 1 | 2 | 3): number {
  return [0, 5, 8, 10][yearsBucket];
}

/**
 * 对单个项目打分。
 * 规则：基础分 40，条件满足加分，条件不足按差距扣分并记录 gap。
 */
export function scoreProgram(
  program: {
    slug: string;
    country: string;
    type: string;
    difficulty: "low" | "mid" | "high";
    eligibility?: Eligibility;
  },
  answers: AssessmentAnswers
): ProgramMatch | null {
  const matched: string[] = [];
  const gaps: MatchGap[] = [];
  let score = 40;

  const el = program.eligibility;

  // 国家意向：明确排除的直接不推荐
  const specified = answers.targetCountries.length > 0 && !answers.targetCountries.includes("any");
  if (specified) {
    if (!answers.targetCountries.includes(program.country)) {
      return null; // 用户明确不意向该国
    }
    score += 15;
    matched.push("符合你的意向国家");
  } else {
    score += 5;
  }

  if (!el) {
    // 没有机器可读条件的项目只做弱推荐，避免误导
    return {
      slug: program.slug,
      score: Math.min(score, 55),
      matched: matched.length ? matched : ["暂无量化条件数据，仅供参考"],
      gaps: [{ dimension: "数据", detail: "该项目尚未录入结构化条件，请查看详情页人工核对" }],
    };
  }

  // 学历
  if (el.minEducation != null) {
    if (answers.education >= el.minEducation) {
      score += 10;
      matched.push("学历达标");
    } else {
      const deficit = el.minEducation - answers.education;
      score -= deficit * 12;
      gaps.push({
        dimension: "学历",
        detail: deficit >= 2 ? "与要求差距较大，通常需要提升学历或走其他路径" : "略低于常见要求，部分项目可用工作经验弥补",
      });
    }
  }

  // 语言
  if (el.minLanguage != null) {
    if (answers.language >= el.minLanguage) {
      score += 10;
      matched.push("语言水平达标");
    } else {
      const deficit = el.minLanguage - answers.language;
      score -= deficit * 12;
      gaps.push({
        dimension: "语言",
        detail: "语言是多数技术类路径的硬门槛，建议先考出成绩再评估",
      });
    }
  }

  // 预算
  if (el.minBudgetTier) {
    if (BUDGET_ORDER[answers.budgetTier] >= BUDGET_ORDER[el.minBudgetTier]) {
      score += 10;
      matched.push("预算充足");
    } else {
      score -= 25;
      gaps.push({ dimension: "预算", detail: "该项目预算要求高于你的选择，可查看费用明细确认真实开销" });
    }
  }

  // 雇主 offer
  if (el.needsJobOffer) {
    if (answers.hasJobOffer) {
      score += 10;
      matched.push("已有雇主 offer");
    } else {
      score -= 30;
      gaps.push({ dimension: "雇主", detail: "该路径通常需要先找到当地雇主担保（offer 是硬前提）" });
    }
  }

  // 年龄
  if (el.ageRange && answers.age != null) {
    const { min, max } = el.ageRange;
    if (answers.age >= min && answers.age <= max) {
      score += 8;
      matched.push(`年龄处于优势区间（${min}-${max} 岁）`);
    } else {
      const distance = answers.age < min ? min - answers.age : answers.age - max;
      score -= distance <= 5 ? 5 : 20;
      if (distance > 0) {
        gaps.push({ dimension: "年龄", detail: `该项目打分对 ${min}-${max} 岁更友好，当前年龄会损失部分分数` });
      }
    }
  }

  // 工作经验软性加分
  score += workExperienceScore(answers.workYears);

  // 家庭纽带：家庭团聚类路径强相关
  if (program.type === "family") {
    if (answers.hasFamilyAbroad) {
      score += 25;
      matched.push("有海外亲属符合担保条件");
    } else {
      score -= 60;
      gaps.push({ dimension: "亲属", detail: "家庭团聚需要先有符合资格的海外亲属作为担保人" });
    }
  }

  // 难度微调：门槛低的项目在同分时优先展示
  score += program.difficulty === "low" ? 3 : program.difficulty === "high" ? -3 : 0;

  return {
    slug: program.slug,
    score: Math.max(0, Math.min(100, Math.round(score))),
    matched,
    gaps,
  };
}

export function rankPrograms(
  programs: Parameters<typeof scoreProgram>[0][],
  answers: AssessmentAnswers,
  limit = 5
): ProgramMatch[] {
  return programs
    .map((p) => scoreProgram(p, answers))
    .filter((m): m is ProgramMatch => m !== null)
    .sort((a, b) => b.score - a.score)
    .slice(0, limit);
}
