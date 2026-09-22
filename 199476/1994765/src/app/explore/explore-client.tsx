"use client";

import { useMemo, useState } from "react";
import { ProgramCard } from "@/components/program-card";
import type { ProgramCardData } from "@/lib/ui";

type SortKey = "verified" | "cost" | "duration";

interface ExploreClientProps {
  cards: ProgramCardData[];
  countries: { id: string; name: string; flag: string }[];
  regions: { countryId: string; id: string; name: string }[];
  types: { id: string; label: string }[];
}

const budgetOptions = [
  { value: "", label: "不限预算" },
  { value: "low", label: "20 万以内" },
  { value: "mid", label: "20–80 万" },
  { value: "high", label: "80 万以上" },
];

export function ExploreClient({ cards, countries, regions, types }: ExploreClientProps) {
  const [country, setCountry] = useState("");
  const [region, setRegion] = useState("");
  const [type, setType] = useState("");
  const [budget, setBudget] = useState("");
  const [sort, setSort] = useState<SortKey>("verified");

  const regionOptions = useMemo(
    () => regions.filter((r) => !country || r.countryId === country),
    [regions, country]
  );

  const filtered = useMemo(() => {
    const budgetOrder = { low: 0, mid: 1, high: 2 } as const;
    let list = cards.filter(
      (c) =>
        (!country || c.countryId === country) &&
        (!region || c.regionId === region) &&
        (!type || c.type === type) &&
        (!budget || budgetOrder[c.budgetTier] <= budgetOrder[budget as "low" | "mid" | "high"])
    );
    list = [...list].sort((a, b) => {
      if (sort === "cost") return a.costMin - b.costMin;
      if (sort === "duration") return a.durationMinMonths - b.durationMinMonths;
      return b.infoVerifiedAt.localeCompare(a.infoVerifiedAt);
    });
    return list;
  }, [cards, country, region, type, budget, sort]);

  const selectCls =
    "rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-700 focus:border-teal-500 focus:outline-none";

  return (
    <div>
      {/* 筛选栏 */}
      <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50/60 p-3">
        <select
          aria-label="国家筛选"
          className={selectCls}
          value={country}
          onChange={(e) => {
            setCountry(e.target.value);
            setRegion("");
          }}
        >
          <option value="">全部国家</option>
          {countries.map((c) => (
            <option key={c.id} value={c.id}>
              {c.flag} {c.name}
            </option>
          ))}
        </select>

        <select
          aria-label="省州筛选"
          className={selectCls}
          value={region}
          onChange={(e) => setRegion(e.target.value)}
          disabled={!country && regionOptions.length === 0}
        >
          <option value="">全部省/州（含联邦）</option>
          {regionOptions.map((r) => (
            <option key={r.id} value={r.id}>
              {r.name}
            </option>
          ))}
        </select>

        <select
          aria-label="预算筛选"
          className={selectCls}
          value={budget}
          onChange={(e) => setBudget(e.target.value)}
        >
          {budgetOptions.map((b) => (
            <option key={b.value} value={b.value}>
              {b.label}
            </option>
          ))}
        </select>

        <select
          aria-label="排序"
          className={selectCls}
          value={sort}
          onChange={(e) => setSort(e.target.value as SortKey)}
        >
          <option value="verified">按核实日期排序</option>
          <option value="cost">按费用从低到高</option>
          <option value="duration">按周期从短到长</option>
        </select>

        {/* 类型快捷筛选 */}
        <div className="flex w-full flex-wrap gap-1.5">
          <button
            onClick={() => setType("")}
            className={`rounded-full px-3 py-1 text-xs font-medium transition ${
              type === ""
                ? "bg-teal-600 text-white"
                : "bg-white text-slate-600 ring-1 ring-inset ring-slate-200 hover:bg-slate-100"
            }`}
          >
            全部方式
          </button>
          {types.map((t) => (
            <button
              key={t.id}
              onClick={() => setType(type === t.id ? "" : t.id)}
              className={`rounded-full px-3 py-1 text-xs font-medium transition ${
                type === t.id
                  ? "bg-teal-600 text-white"
                  : "bg-white text-slate-600 ring-1 ring-inset ring-slate-200 hover:bg-slate-100"
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <p className="mt-4 text-sm text-slate-500">
        共 <strong className="text-slate-800">{filtered.length}</strong> 个项目
        {filtered.length !== cards.length && `（全部 ${cards.length} 个）`}
      </p>

      {filtered.length > 0 ? (
        <div className="mt-4 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filtered.map((c) => (
            <ProgramCard key={c.slug} data={c} />
          ))}
        </div>
      ) : (
        <div className="mt-8 rounded-2xl border border-dashed border-slate-300 p-10 text-center text-slate-500">
          没有符合条件的项目，试试放宽筛选。
        </div>
      )}
    </div>
  );
}
