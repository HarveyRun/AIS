import Link from "next/link";
import type { Metadata } from "next";
import { CountryFlag } from "@/components/country-flag";
import { ProgramCard } from "@/components/program-card";
import { NoticeBox, SectionHeading } from "@/components/notice";
import {
  getSiteMeta,
  listCountries,
  listPrograms,
  listRegions,
} from "@/lib/content";
import { toCardData } from "@/lib/display";

export function generateMetadata(): Metadata {
  const site = getSiteMeta();
  return { title: site.tagline, description: site.description };
}

export default function HomePage() {
  const countries = listCountries();
  const regions = listRegions();
  const programs = listPrograms();

  return (
    <div className="mx-auto max-w-6xl px-4">
      {/* Hero */}
      <section className="py-14">
        <div className="max-w-3xl">
          <p className="text-sm font-medium text-teal-700">
            国家 → 省/州 → 项目，三级看懂移民
          </p>
          <h1 className="mt-3 text-3xl font-bold leading-tight text-slate-900 md:text-5xl">
            移民流程指南
          </h1>
          <p className="mt-4 text-base leading-relaxed text-slate-600 md:text-lg">
            {getSiteMeta().description}
          </p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Link
              href="/basics/"
              className="rounded-xl bg-teal-600 px-5 py-2.5 font-medium text-white transition hover:bg-teal-700"
            >
              先看移民科普
            </Link>
            <Link
              href="#provinces"
              className="rounded-xl border border-slate-300 bg-white px-5 py-2.5 font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-50"
            >
              按省·州浏览
            </Link>
          </div>
        </div>
      </section>

      {/* 国家 → 省/州 */}
      <section className="pb-14">
        <SectionHeading
          title="按国家浏览"
          description="不同国家移民政策不同"
        />
        <div className="grid gap-4 md:grid-cols-3">
          {countries.map((country) => {
            const countryRegions = regions.filter(
              (r) => r.country === country.id
            );
            const count = programs.filter((p) => p.country === country.id).length;
            return (
              <Link
                key={country.id}
                href={`/countries/${country.id}/`}
                className="group rounded-2xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-teal-300 hover:shadow-lg hover:shadow-teal-600/5"
              >
                <div className="flex items-center justify-between">
                  <CountryFlag
                    countryId={country.id}
                    flag={country.flag}
                    className="h-9 w-14 rounded shadow-sm"
                  />
                  <span className="text-xs text-slate-400">
                    {count} 个项目
                  </span>
                </div>
                <h3 className="mt-3 font-bold text-slate-900 group-hover:text-teal-700">
                  {country.name}
                </h3>
                <p className="mt-1 line-clamp-2 text-sm text-slate-500">
                  {country.summary}
                </p>
                <p className="mt-3 text-xs text-slate-400">
                  收录 {countryRegions.length} 个省/州 →
                </p>
              </Link>
            );
          })}
        </div>
      </section>

      {/* 按省/州浏览 */}
      <section id="provinces" className="scroll-mt-20 pb-14">
        <SectionHeading
          title="按省/州浏览"
          description="相同国家，不同的省/州的可行性可能完全不同。"
        />
        <div className="space-y-4">
          {countries.map((country) => {
            const countryRegions = regions.filter(
              (r) => r.country === country.id
            );
            if (countryRegions.length === 0) return null;
            return (
              <div
                key={country.id}
                className="rounded-2xl border border-slate-200 bg-white p-5"
              >
                <div className="flex items-center gap-2">
                  <CountryFlag
                    countryId={country.id}
                    flag={country.flag}
                    className="h-3.5 w-5 rounded-sm"
                  />
                  <Link
                    href={`/countries/${country.id}/`}
                    className="font-bold text-slate-900 hover:text-teal-700"
                  >
                    {country.name}
                  </Link>
                  <span className="text-xs text-slate-400">
                    {countryRegions.length} 个省/州
                  </span>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  {(() => {
                    const fc = programs.filter((x) => x.country === country.id && !x.region).length;
                    if (fc === 0) return null;
                    return (
                      <Link
                        href={`/countries/${country.id}/`}
                        className="rounded-full border border-blue-200 bg-blue-50 px-3 py-1.5 text-sm font-medium text-blue-700 transition hover:bg-blue-100"
                      >
                        联邦项目 {fc}
                      </Link>
                    );
                  })()}
                  {countryRegions.map((r) => {
                    const count = programs.filter(
                      (p) => p.country === country.id && p.region === r.id
                    ).length;
                    return (
                      <Link
                        key={r.id}
                        href={`/countries/${country.id}/${r.id}/`}
                        className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-sm text-slate-600 transition hover:border-teal-300 hover:bg-teal-50 hover:text-teal-700"
                      >
                        {r.name}
                        {count > 0 && (
                          <span className="ml-1 text-xs text-slate-400">
                            {count}
                          </span>
                        )}
                      </Link>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* 使用路径 */}
      <section className="pb-14">
        <SectionHeading
          title="网站使用路径"
          description="三步找到对你有用的信息"
        />
        <div className="grid gap-4 md:grid-cols-3">
          {[
            {
              step: "1",
              title: "选择国家",
              desc: "从国家页了解移民体系：联邦管什么、省/州管什么，联邦与地方的关系决定了你的可选范围。",
            },
            {
              step: "2",
              title: "选择省/州",
              desc: "同一类项目在不同省/州的门槛可能天差地别。逐省查看政策概述，判断自己的条件更匹配哪里。",
            },
            {
              step: "3",
              title: "查看项目流程",
              desc: "项目页提供分步时间线、逐项费用、材料清单、常见拒签原因与官方入口，信息自行核对判断。",
            },
          ].map((item) => (
            <div
              key={item.step}
              className="flex flex-col rounded-2xl border border-slate-200 bg-slate-50/60 p-5"
            >
              <span className="grid h-8 w-8 place-items-center rounded-full bg-teal-600 font-bold text-white">
                {item.step}
              </span>
              <h3 className="mt-3 font-bold text-slate-900">{item.title}</h3>
              <p className="mt-1 flex-1 text-sm leading-relaxed text-slate-600">
                {item.desc}
              </p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
