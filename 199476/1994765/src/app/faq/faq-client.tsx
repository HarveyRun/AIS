"use client";

import { useMemo, useState } from "react";
import { CountryFlag } from "@/components/country-flag";

export interface FaqItemLite {
  country: string;
  question: string;
  answer: string;
}

export function FaqClient({
  faqs,
  countries,
}: {
  faqs: FaqItemLite[];
  countries: { id: string; name: string; flag: string }[];
}) {
  const [country, setCountry] = useState<string>(countries[0]?.id ?? "");

  const filtered = useMemo(
    () => (country ? faqs.filter((f) => f.country === country) : faqs),
    [faqs, country]
  );

  const tabCls = (active: boolean) =>
    `rounded-full px-4 py-2 text-sm font-medium transition ${
      active
        ? "bg-teal-600 text-white"
        : "bg-white text-slate-600 ring-1 ring-inset ring-slate-200 hover:bg-slate-100"
    }`;

  return (
    <div>
      <div className="flex flex-wrap items-center gap-2">
        {countries.map((c) => (
          <button
            key={c.id}
            onClick={() => setCountry(country === c.id ? "" : c.id)}
            className={tabCls(country === c.id)}
          >
            {c.flag} {c.name}
          </button>
        ))}
        <span className="ml-auto text-sm text-slate-400">
          {filtered.length} 个问题
        </span>
      </div>

      {countries.map((c) => {
        const items = filtered.filter((f) => f.country === c.id);
        if (items.length === 0) return null;
        return (
          <section key={c.id} className="mt-10">
            <h2 className="flex items-center gap-2 text-xl font-bold text-slate-900">
              <CountryFlag countryId={c.id} flag={c.flag} className="h-5 w-7 rounded-sm" />
              {c.name}
              <span className="text-sm font-normal text-slate-400">
                {items.length} 个问题
              </span>
            </h2>
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
        <div className="mt-10 rounded-2xl border border-dashed border-slate-300 p-10 text-center text-slate-500">
          该筛选下暂无问题。
        </div>
      )}
    </div>
  );
}
