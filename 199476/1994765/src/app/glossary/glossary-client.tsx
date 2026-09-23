"use client";

import { useMemo, useState } from "react";
import { CountryFlag } from "@/components/country-flag";
import {
  glossaryCategoryLabels,
  glossaryCategories,
  glossaryImportanceLabels,
} from "@/lib/schema";

export interface GlossaryTerm {
  term: string;
  en?: string;
  countries: string[];
  category: string;
  importance: "must" | "important" | "optional";
  regions?: string[];
  explanation: string;
}

interface GlossaryClientProps {
  terms: GlossaryTerm[];
  countries: { id: string; name: string; flag: string }[];
  regions: { countryId: string; id: string; name: string }[];
}

const IMPORTANCE_ORDER = { must: 0, important: 1, optional: 2 } as const;

const importanceBadge: Record<string, string> = {
  must: "bg-amber-100 text-amber-800",
  important: "bg-slate-100 text-slate-600",
};

export function GlossaryClient({ terms, countries, regions }: GlossaryClientProps) {
  const [country, setCountry] = useState<string>(countries[0]?.id ?? "");
  const [region, setRegion] = useState<string>("");
  // 三个分类维度：默认全开，可勾选关闭
  const [cats, setCats] = useState<Record<string, boolean>>({
    general: true,
    business: true,
  });

  const regionOptions = useMemo(
    () => regions.filter((r) => r.countryId === country),
    [regions, country]
  );

  const filtered = useMemo(() => {
    let list = terms.filter(
      (t) =>
        (!country || t.countries.includes(country)) &&
        // 省/州筛选：未标注省州的术语全国适用；标注了省州的只保留命中的
        (!region || !t.regions || t.regions.includes(region)) &&
        cats[t.category]
    );
    // 组内按重要度排序：必知 > 重要 > 了解
    list = [...list].sort(
      (a, b) => IMPORTANCE_ORDER[a.importance] - IMPORTANCE_ORDER[b.importance]
    );
    return list;
  }, [terms, country, region, cats]);

  const byCategory = useMemo(
    () =>
      glossaryCategories
        .map((cat) => ({
          category: cat,
          label: glossaryCategoryLabels[cat],
          items: filtered.filter((t) => t.category === cat),
        }))
        .filter((g) => cats[g.category]),
    [filtered, cats]
  );

  const chipCls = (active: boolean) =>
    `rounded-full px-4 py-2 text-sm font-medium transition ${
      active
        ? "bg-teal-600 text-white"
        : "bg-white text-slate-600 ring-1 ring-inset ring-slate-200 hover:bg-slate-100"
    }`;

  const selectCls =
    "rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-700 focus:border-teal-500 focus:outline-none";

  return (
    <div>
      {/* 国家筛选 */}
      <div className="flex flex-wrap items-center gap-2">
        {countries.map((c) => (
          <button
            key={c.id}
            onClick={() => { setCountry(c.id); setRegion(""); }}
            className={chipCls(country === c.id)}
          >
            {c.flag} {c.name}
          </button>
        ))}
      </div>

      {/* 省/州筛选 + 分类筛选 */}
      <div className="mt-3 flex flex-wrap items-center gap-2">
        <span className="text-sm text-slate-400">术语分类：</span>
        {glossaryCategories.map((cat) => (
          <button
            key={cat}
            onClick={() => setCats({ ...cats, [cat]: !cats[cat] })}
            className={`rounded-full px-3 py-1 text-xs font-medium transition ${
              cats[cat]
                ? "bg-slate-800 text-white"
                : "bg-white text-slate-400 ring-1 ring-inset ring-slate-200"
            }`}
          >
            {glossaryCategoryLabels[cat]}
          </button>
        ))}

        <span className="ml-auto text-sm text-slate-400">
          {filtered.length} 条术语
        </span>
      </div>

      {/* 分类分组 */}
      <div className="mt-8 space-y-10">
        {byCategory.map(({ category, label, items }) => (
          <section key={category}>
            <h2 className="text-lg font-bold text-slate-900">
              {label}
              <span className="ml-2 text-sm font-normal text-slate-400">
                {items.length} 条
              </span>
            </h2>
            <div className="mt-3 space-y-3">
              {items.map((t) => {
                const isUniversal = t.countries.length === countries.length;
                return (
                <article
                  key={t.term}
                  id={encodeURIComponent(t.term)}
                  className="scroll-mt-20 rounded-2xl border border-slate-200 bg-white p-5"
                >
                  <h3 className="flex flex-wrap items-center gap-2 font-bold text-slate-900">
                    {t.term}
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-medium ${importanceBadge[t.importance]}`}
                    >
                      {glossaryImportanceLabels[t.importance]}
                    </span>
                    {!isUniversal &&
                      t.countries.map((cid) => {
                        const c = countries.find((x) => x.id === cid);
                        const termRegions = (t.regions ?? [])
                          .map((rid) => regionOptions.find((r) => r.id === rid))
                          .filter(Boolean);
                        return (
                          <span
                            key={cid}
                            className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-normal text-slate-500"
                          >
                            <CountryFlag countryId={c!.id} flag={c!.flag} className="h-2.5 w-3.5" />
                            {termRegions.length > 0
                              ? termRegions.map((r) => r!.name).join(" / ")
                              : c!.name}
                          </span>
                        );
                      })}
                  </h3>
                  <p className="mt-1.5 text-sm leading-relaxed text-slate-600">
                    {t.explanation}
                  </p>
                </article>
                );
              })}
            </div>
          </section>
        ))}
        {filtered.length === 0 && (
          <div className="rounded-2xl border border-dashed border-slate-300 p-10 text-center text-slate-500">
            该筛选下暂无术语。
          </div>
        )}
      </div>
    </div>
  );
}
