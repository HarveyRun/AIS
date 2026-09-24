"use client";

import { useMemo, useState } from "react";
import { faqCategories, faqCategoryLabels } from "@/lib/schema";

export interface FaqItemLite {
  country: string;
  category: string;
  question: string;
  answer: string;
}

/** 各分类的一句话定位（展示用，id 对应 schema 的 faqCategories） */
const CATEGORY_DESC: Record<string, string> = {
  basics: "零基础先看懂游戏规则：都是大白话，没有专业名词",
  path: "对号入座：不会英语、条件普通、年龄偏大，各能走哪条路",
  project: "进阶钻研：具体签证的区别、转永居条件、担保义务",
};

/** 分类展示顺序：常识 → 路径 → 细节 */
const CATEGORY_ORDER: Record<string, number> = { basics: 0, path: 1, project: 2 };

export function FaqClient({
  faqs,
  countries,
}: {
  faqs: FaqItemLite[];
  countries: { id: string; name: string; flag: string }[];
}) {
  // 默认按第一个国家筛选
  const [country, setCountry] = useState<string>(countries[0]?.id ?? "");
  // 分类筛选：默认全开，可点击开关（同术语表）
  const [cats, setCats] = useState<Record<string, boolean>>({
    basics: true,
    path: true,
    project: true,
  });

  const filtered = useMemo(
    () =>
      faqs
        .filter(
          (f) => (!country || f.country === country) && cats[f.category]
        )
        .sort(
          (a, b) => CATEGORY_ORDER[a.category] - CATEGORY_ORDER[b.category]
        ),
    [faqs, country, cats]
  );

  const tabCls = (active: boolean) =>
    `rounded-full px-4 py-2 text-sm font-medium transition ${
      active
        ? "bg-teal-600 text-white"
        : "bg-white text-slate-600 ring-1 ring-inset ring-slate-200 hover:bg-slate-100"
    }`;

  return (
    <div>
      {/* 国家筛选：默认选中第一个国家 */}
      <div className="flex flex-wrap items-center gap-2">
        {countries.map((c) => (
          <button
            key={c.id}
            onClick={() => setCountry(c.id)}
            className={tabCls(country === c.id)}
          >
            {c.flag} {c.name}
          </button>
        ))}
        <span className="ml-auto text-sm text-slate-400">
          {filtered.length} 个问题
        </span>
      </div>

      {/* 分类筛选（可开关） */}
      <div className="mt-3 flex flex-wrap items-center gap-2">
        <span className="text-sm text-slate-400">问题分类：</span>
        {faqCategories.map((cat) => (
          <button
            key={cat}
            onClick={() => setCats({ ...cats, [cat]: !cats[cat] })}
            className={`rounded-full px-3 py-1 text-xs font-medium transition ${
              cats[cat]
                ? "bg-slate-800 text-white"
                : "bg-white text-slate-400 ring-1 ring-inset ring-slate-200"
            }`}
          >
            {faqCategoryLabels[cat]}
          </button>
        ))}
      </div>

      {/* 按分类分组展示（保持 常识 → 路径 → 细节 的进阶顺序） */}
      <div className="mt-8 space-y-10">
        {faqCategories.map((cat) => {
          if (!cats[cat]) return null;
          const items = filtered.filter((f) => f.category === cat);
          if (items.length === 0) return null;
          return (
            <section key={cat}>
              <h2 className="text-lg font-bold text-slate-900">
                {faqCategoryLabels[cat]}
                <span className="ml-2 text-sm font-normal text-slate-400">
                  {items.length} 个问题
                </span>
              </h2>
              <p className="mt-1 text-sm text-slate-500">{CATEGORY_DESC[cat]}</p>
              <div className="mt-4 space-y-3">
                {items.map((f) => (
                  <details
                    key={f.question}
                    className="group rounded-2xl border border-slate-200 bg-white p-5 open:shadow-md"
                  >
                    <summary className="cursor-pointer list-none font-semibold text-slate-900 marker:hidden">
                      <span className="mr-2 text-teal-600">Q</span>
                      {f.question}
                    </summary>
                    <p className="mt-3 border-t border-slate-100 pt-3 text-sm leading-7 text-slate-600">
                      {f.answer}
                    </p>
                  </details>
                ))}
              </div>
            </section>
          );
        })}
        {filtered.length === 0 && (
          <div className="rounded-2xl border border-dashed border-slate-300 p-10 text-center text-slate-500">
            该筛选下暂无问题。
          </div>
        )}
      </div>
    </div>
  );
}
