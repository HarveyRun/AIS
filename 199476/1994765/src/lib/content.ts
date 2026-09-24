import fs from "node:fs";
import path from "node:path";
import type { ZodTypeAny, z } from "zod";
import {
  countrySchema,
  faqSchema,
  glossaryItemSchema,
  programSchema,
  regionSchema,
  siteMetaSchema,
  type Country,
  type FaqItem,
  type GlossaryItem,
  type Program,
  type Region,
  type SiteMeta,
} from "./schema";

/**
 * 内容加载与校验层。
 *
 * - 纯 Node 实现（不依赖 Next），构建脚本与页面共用同一份逻辑
 * - 所有内容先过 zod schema，再校验交叉引用（country/region/slug 一致性）
 * - 校验失败时一次性报告所有问题（带文件路径），快速失败、易于修复
 */

const CONTENT_DIR = path.resolve(process.cwd(), "content");

export class ContentValidationError extends Error {
  constructor(public issues: string[]) {
    super(
      `内容校验失败，共 ${issues.length} 个问题：\n` +
        issues.map((i) => `  - ${i}`).join("\n")
    );
    this.name = "ContentValidationError";
  }
}

function readJson(filePath: string): unknown {
  try {
    return JSON.parse(fs.readFileSync(filePath, "utf-8"));
  } catch (err) {
    throw new Error(
      `JSON 解析失败: ${path.relative(process.cwd(), filePath)}\n  ${
        err instanceof Error ? err.message : String(err)
      }`
    );
  }
}

/** 校验单个文件，失败返回错误信息（不抛出），供批量收集 */
function tryParse<S extends ZodTypeAny>(
  filePath: string,
  schema: S
): [z.output<S>, null] | [null, string] {
  const rel = path.relative(process.cwd(), filePath);
  let data: unknown;
  try {
    data = readJson(filePath);
  } catch (err) {
    return [null, err instanceof Error ? err.message : String(err)];
  }
  const result = schema.safeParse(data);
  if (!result.success) {
    const details = result
      .error!.issues.map((i) => `${i.path.join(".") || "(根字段)"}: ${i.message}`)
      .join("; ");
    return [null, `${rel}\n      ${details}`];
  }
  return [result.data as z.output<S>, null];
}

function listJsonFiles(dir: string): string[] {
  if (!fs.existsSync(dir)) return [];
  return fs
    .readdirSync(dir)
    .filter((f) => f.endsWith(".json"))
    .map((f) => path.join(dir, f))
    .sort();
}

export interface ContentBundle {
  site: SiteMeta;
  glossary: GlossaryItem[];
  faqs: FaqItem[];
  countries: Country[];
  regions: Region[];
  programs: Program[];
}

/** 加载并校验全部内容；有问题时抛出 ContentValidationError（含全部问题清单） */
export function loadContent(): ContentBundle {
  const issues: string[] = [];

  /* 站点元信息 */
  const sitePath = path.join(CONTENT_DIR, "site.json");
  if (!fs.existsSync(sitePath)) {
    throw new ContentValidationError([`缺少站点配置文件: content/site.json`]);
  }
  const [site, siteErr] = tryParse(sitePath, siteMetaSchema);
  if (!site) issues.push(siteErr!);

  /* 术语表（可选） */
  let glossary: GlossaryItem[] = [];
  const glossaryPath = path.join(CONTENT_DIR, "glossary.json");
  if (fs.existsSync(glossaryPath)) {
    const raw = readJson(glossaryPath) as unknown;
    if (!Array.isArray(raw)) {
      issues.push("content/glossary.json\n      (根字段): 必须是对象数组");
    } else {
      glossary = [];
      raw.forEach((item, idx) => {
        const r = glossaryItemSchema.safeParse(item);
        if (!r.success) {
          const details = r.error.issues
            .map((i) => `${i.path.join(".") || "(根字段)"}: ${i.message}`)
            .join("; ");
          issues.push(`content/glossary.json 第 ${idx + 1} 条\n      ${details}`);
        } else {
          glossary.push(r.data);
        }
      });
    }
  }

  /* 常见问题 */
  const faqs: FaqItem[] = [];
  const faqsPath = path.join(CONTENT_DIR, "faqs.json");
  if (!fs.existsSync(faqsPath)) {
    issues.push("缺少常见问题文件: content/faqs.json");
  } else {
    const rawFaqs = readJson(faqsPath) as unknown;
    if (!Array.isArray(rawFaqs)) {
      issues.push("content/faqs.json\n      (根字段): 必须是对象数组");
    } else {
      rawFaqs.forEach((item, idx) => {
        const r = faqSchema.safeParse(item);
        if (!r.success) {
          const details = r.error.issues
            .map((i) => `${i.path.join(".") || "(根字段)"}: ${i.message}`)
            .join("; ");
          issues.push(`content/faqs.json 第 ${idx + 1} 条\n      ${details}`);
        } else {
          faqs.push(r.data);
        }
      });
    }
  }
  /* 国家：content/countries/<id>/country.json */
  const countries: Country[] = [];
  const countryBase = path.join(CONTENT_DIR, "countries");
  for (const entry of fs.existsSync(countryBase) ? fs.readdirSync(countryBase, { withFileTypes: true }) : []) {
    if (!entry.isDirectory()) continue;
    const filePath = path.join(countryBase, entry.name, "country.json");
    if (!fs.existsSync(filePath)) {
      issues.push(`目录 content/countries/${entry.name}/ 缺少 country.json`);
      continue;
    }
    const [country, err] = tryParse(filePath, countrySchema);
    if (!country) issues.push(err!);
    else countries.push(country);
  }

  /* 省州与项目：位于 countries/<id>/regions|programs/ */
  const regions: Region[] = [];
  const programs: Program[] = [];
  for (const country of countries) {
    const base = path.join(CONTENT_DIR, "countries", country.id);
    for (const filePath of listJsonFiles(path.join(base, "regions"))) {
      const [region, err] = tryParse(filePath, regionSchema);
      if (!region) issues.push(err!);
      else regions.push(region);
    }
    for (const filePath of listJsonFiles(path.join(base, "programs"))) {
      const [program, err] = tryParse(filePath, programSchema);
      if (!program) issues.push(err!);
      else programs.push(program);
    }
  }

  /* 交叉引用校验 */
  const countryIds = new Set(countries.map((c) => c.id));
  const regionIds = new Set(regions.map((r) => `${r.country}/${r.id}`));

  for (const f of faqs) {
    if (!countryIds.has(f.country)) {
      issues.push(`常见问题「${f.question}」标注了不存在的国家 id "${f.country}"`);
    }
  }

  for (const g of glossary) {
    for (const cid of g.countries) {
      if (!countryIds.has(cid)) {
        issues.push(
          `术语「${g.term}」标注了不存在的国家 id "${cid}"（content/glossary.json）`
        );
      }
    }
    for (const rid of g.regions ?? []) {
      const owners = regions.filter((r) => r.id === rid);
      if (owners.length === 0) {
        issues.push(`术语「${g.term}」标注了不存在的省/州/区 id "${rid}"`);
      } else if (owners.length > 1) {
        issues.push(
          `术语「${g.term}」标注的省/州/区 id "${rid}" 在多个国家下存在（${owners
            .map((o) => o.country)
            .join("、")}），请改用全局唯一的 region id`
        );
      } else if (!g.countries.includes(owners[0].country)) {
        issues.push(
          `术语「${g.term}」标注的省/州/区 "${rid}" 属于 ${owners[0].country}，但 countries 未包含该国`
        );
      }
    }
  }

  for (const c of countries) {
    // 国家 id 与目录名一致性
    if (!fs.existsSync(path.join(CONTENT_DIR, "countries", c.id))) {
      issues.push(
        `国家 "${c.name}" (id=${c.id}) 缺少目录 content/countries/${c.id}/`
      );
    }
  }

  for (const r of regions) {
    if (!countryIds.has(r.country)) {
      issues.push(
        `省/州/区 ${r.id} (content/countries/${r.country}/regions/) 引用了不存在的国家 id "${r.country}"`
      );
    }
  }

  for (const p of programs) {
    if (!countryIds.has(p.country)) {
      issues.push(
        `项目 ${p.slug} 引用了不存在的国家 id "${p.country}"`
      );
    }
    if (p.region) {
      const key = `${p.country}/${p.region}`;
      if (!regionIds.has(key)) {
        issues.push(
          `项目 ${p.slug} 引用了不存在的省/州/区 "${p.region}"（国家 ${p.country} 下未找到）`
        );
      }
    }
  }

  /* slug 全局唯一 + 文件名一致性 */
  const slugSeen = new Map<string, string>();
  for (const country of countries) {
    const dir = path.join(CONTENT_DIR, "countries", country.id, "programs");
    for (const filePath of listJsonFiles(dir)) {
      const slug = path.basename(filePath, ".json");
      if (slugSeen.has(slug)) {
        issues.push(
          `项目 slug "${slug}" 重复：${slugSeen.get(slug)} 与 ${path.relative(process.cwd(), filePath)}`
        );
      } else {
        slugSeen.set(slug, path.relative(process.cwd(), filePath));
      }
      // 文件内容里的 slug 字段若填写，需与文件名一致
      const data = readJson(filePath) as { slug?: string };
      if (data.slug && data.slug !== slug) {
        issues.push(
          `项目文件 ${path.relative(process.cwd(), filePath)} 内 slug="${data.slug}" 与文件名不一致`
        );
      }
    }
  }

  if (issues.length > 0) throw new ContentValidationError(issues);

  return {
    site: site!,
    glossary,
    faqs,
    countries,
    regions,
    programs,
  };
}

/* ---------------- 查询辅助（带缓存） ---------------- */

let cache: ContentBundle | null = null;

export function getContent(): ContentBundle {
  // 开发模式下每次调用都重读内容文件，改 content/ 立即生效（无 HMR 事件可依赖）；
  // 生产构建保持单例缓存，内容在构建期固化。
  if (!cache || process.env.NODE_ENV === "development") {
    cache = loadContent();
  }
  return cache;
}

export function getSiteMeta(): SiteMeta {
  return getContent().site;
}

export function getGlossary(): GlossaryItem[] {
  return getContent().glossary;
}

export function getFaqs(): FaqItem[] {
  return getContent().faqs;
}

export function listCountries(): Country[] {
  return getContent().countries;
}

export function getCountry(id: string): Country | undefined {
  return getContent().countries.find((c) => c.id === id);
}

export function listRegions(countryId?: string): Region[] {
  const all = getContent().regions;
  return countryId ? all.filter((r) => r.country === countryId) : all;
}

export function getRegion(
  countryId: string,
  regionId: string
): Region | undefined {
  return getContent().regions.find(
    (r) => r.country === countryId && r.id === regionId
  );
}

export function listPrograms(): Program[] {
  return getContent().programs;
}

export function getProgram(slug: string): Program | undefined {
  return getContent().programs.find((p) => p.slug === slug);
}

export function programsByCountry(countryId: string): Program[] {
  return getContent().programs.filter((p) => p.country === countryId);
}

export function programsByRegion(
  countryId: string,
  regionId: string
): Program[] {
  return getContent().programs.filter(
    (p) => p.country === countryId && p.region === regionId
  );
}

export function federalPrograms(countryId: string): Program[] {
  return getContent().programs.filter(
    (p) => p.country === countryId && !p.region
  );
}

/** 近期核实的内容（首页“最新更新”用） */

/** 同国家/同省州的相关项目（详情页底部用） */
export function relatedPrograms(program: Program, limit = 3): Program[] {
  const pool = getContent().programs.filter((p) => p.slug !== program.slug);
  const sameRegion = pool.filter(
    (p) => p.region && p.region === program.region && p.country === program.country
  );
  const sameCountry = pool.filter(
    (p) => p.country === program.country && !sameRegion.includes(p)
  );
  const others = pool.filter((p) => !sameRegion.includes(p) && !sameCountry.includes(p));
  return [...sameRegion, ...sameCountry, ...others].slice(0, limit);
}

/** 校验内容但不抛异常（供 content:check 脚本使用） */
export function checkContent(): { ok: true; bundle: ContentBundle } | { ok: false; issues: string[] } {
  try {
    return { ok: true, bundle: loadContent() };
  } catch (err) {
    if (err instanceof ContentValidationError) return { ok: false, issues: err.issues };
    return { ok: false, issues: [err instanceof Error ? err.message : String(err)] };
  }
}
