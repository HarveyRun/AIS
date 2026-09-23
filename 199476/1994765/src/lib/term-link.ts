import type { GlossaryItem } from "./schema";

/**
 * 行内术语匹配：把正文文本按术语关键词切段，命中的段携带术语解释。
 * 纯函数，构建期在服务端执行一次，客户端只渲染结果。
 */

export interface TermLite {
  term: string;
  explanation: string;
}

export interface TermSegment {
  text: string;
  term?: TermLite;
}

export interface TermIndex {
  /** 按长度降序，保证「I-140 / I-485」优先于「I-140」命中 */
  keywords: { keyword: string; lite: TermLite }[];
  regex: RegExp | null;
}

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function keywordsFrom(item: GlossaryItem): string[] {
  const out = new Set<string>();
  const add = (k: string | undefined) => {
    if (!k) return;
    const t = k.trim();
    if (t.length >= 2) out.add(t);
  };
  // 「EOI（意向表达）」→ EOI、意向表达 两个变体
  const m = item.term.match(/^(.+?)（(.+?)）$/);
  if (m) {
    add(m[1]);
    add(m[2]);
  }
  add(item.term);
  for (const part of (item.en ?? "").split("/")) add(part);
  for (const a of item.aliases ?? []) add(a);
  return [...out];
}

export function buildTermIndex(items: GlossaryItem[]): TermIndex {
  const seen = new Map<string, { keyword: string; lite: TermLite }>();
  for (const item of items) {
    const lite = { term: item.term, explanation: item.explanation };
    for (const kw of keywordsFrom(item)) {
      const key = kw.toLowerCase();
      if (!seen.has(key)) seen.set(key, { keyword: kw, lite });
    }
  }
  const keywords = [...seen.values()].sort(
    (a, b) => b.keyword.length - a.keyword.length
  );
  const regex = keywords.length
    ? new RegExp(`(${keywords.map((k) => escapeRegExp(k.keyword)).join("|")})`, "gi")
    : null;
  return { keywords, regex };
}

const ASCII = /^[A-Za-z0-9]/;

export function segmentTerms(text: string, index: TermIndex): TermSegment[] {
  if (!index.regex || !text) return [{ text }];
  const segments: TermSegment[] = [];
  let last = 0;
  for (const match of text.matchAll(index.regex)) {
    const start = match.index ?? 0;
    const kw = match[0];
    const hit = index.keywords.find(
      (k) => k.keyword.toLowerCase() === kw.toLowerCase()
    );
    if (!hit) continue;
    // 纯英数关键词避免命中更长单词的中间（如 CLB 误配 XCLBY）
    if (ASCII.test(kw)) {
      const prev = start > 0 ? text[start - 1] : "";
      const next = start + kw.length < text.length ? text[start + kw.length] : "";
      if ((prev && /[A-Za-z0-9]/.test(prev)) || (next && /[A-Za-z0-9]/.test(next))) {
        continue;
      }
    }
    if (start > last) segments.push({ text: text.slice(last, start) });
    segments.push({ text: kw, term: hit.lite });
    last = start + kw.length;
  }
  if (last < text.length) segments.push({ text: text.slice(last) });
  return segments;
}
