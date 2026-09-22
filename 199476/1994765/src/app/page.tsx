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
  recentlyVerified,
} from "@/lib/content";
import { toCardData } from "@/lib/display";
import { immigrationTypeLabels } from "@/lib/schema";

export function generateMetadata(): Metadata {
  const site = getSiteMeta();
  return { title: site.tagline, description: site.description };
}

export default function HomePage() {
  const countries = listCountries();
  const regions = listRegions();
  const programs = listPrograms();
  const latest = recentlyVerified(6);

  const typeCounts = Object.entries(immigrationTypeLabels).map(
    ([type, label]) => ({
      type,
      label,
      count: programs.filter((p) => p.type === type).length,
    })
  );

  return (
    <div className="mx-auto max-w-6xl px-4">
      {/* Hero */}
      <section className="py-14 md:py-20">
        <div className="max-w-3xl">
          <p className="text-sm font-medium text-teal-700">
            国家 → 省/州 → 项目，三级看清每条路
          </p>
          <h1 className="mt-3 text-3xl font-bold leading-tight text-slate-900 md:text-5xl">
            搞清楚每一条移民路：
            <br />
            方式、流程、费用与时间线
          </h1>
          <p className="mt-4 text-base leading-relaxed text-slate-600 md:text-lg">
            {getSiteMeta().description} 全部信息结构化整理，标注核实日期与官方来源。
          </p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Link
              href="/assessment/"
              className="rounded-xl bg-teal-600 px-5 py-2.5 font-medium text-white transition hover:bg-teal-700"
            >
              2 分钟测测我适合哪条路
            </Link>
            <Link
              href="/explore/"
              className="rounded-xl border border-slate-300 bg-white px-5 py-2.5 font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-50"
            >
              浏览全部 {programs.length} 个项目
            </Link>
          </div>
        </div>
      </section>

      {/* 国家 → 省/州 */}
      <section className="pb-14">
        <SectionHeading
          title="按国家浏览，看清省/州差异"
          description="很多国家的移民政策在省/州层面与联邦并不一致——同一个条件，不同省的可行性可能完全不同。"
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
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {countryRegions.map((r) => (
                    <span
                      key={r.id}
                      className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600 group-hover:bg-teal-50 group-hover:text-teal-700"
                    >
                      {r.name}
                    </span>
                  ))}
                  {countryRegions.length === 0 && (
                    <span className="text-xs text-slate-400">
                      以联邦政策为主
                    </span>
                  )}
                </div>
              </Link>
            );
          })}
        </div>
      </section>

      {/* 按类型 */}
      <section className="pb-14">
        <SectionHeading title="按移民方式浏览" />
        <div className="grid grid-cols-2 gap-3 md:grid-cols-6">
          {typeCounts.map(({ type, label, count }) => (
            <Link
              key={type}
              href={`/explore/?type=${type}`}
              className="rounded-xl border border-slate-200 bg-white p-4 text-center transition hover:border-teal-300 hover:bg-teal-50/40"
            >
              <p className="font-bold text-slate-900">{label}</p>
              <p className="mt-0.5 text-xs text-slate-400">{count} 个项目</p>
            </Link>
          ))}
        </div>
      </section>

      {/* 最新核实 */}
      <section className="pb-14">
        <SectionHeading
          title="最近核实的内容"
          description="移民政策变动频繁，我们为每个项目标注最后人工核实日期。"
          more={{ href: "/explore/", label: "查看全部" }}
        />
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {latest.map((p) => (
            <ProgramCard key={p.slug} data={toCardData(p)} />
          ))}
        </div>
      </section>

      {/* 使用路径 */}
      <section className="pb-14">
        <SectionHeading
          title="怎么用好这个网站"
          description="三步：先评估匹配，再深入对比，最后按流程推进。"
        />
        <div className="grid gap-4 md:grid-cols-3">
          {[
            {
              step: "1",
              title: "评估匹配",
              desc: "回答 8 个问题（国家、学历、语言、预算等），按结构化条件匹配最适合你的 2-3 条路径，并给出差距分析。",
              href: "/assessment/",
              cta: "开始评估",
            },
            {
              step: "2",
              title: "深入对比",
              desc: "在浏览页按国家、省/州、类型、预算筛选项目，横向对比费用、周期与门槛。",
              href: "/explore/",
              cta: "去对比",
            },
            {
              step: "3",
              title: "按流程推进",
              desc: "每个项目页提供分步时间线、材料清单、费用明细、常见拒签原因与官方入口。",
              href: "/explore/",
              cta: "看示例流程",
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
              <Link
                href={item.href}
                className="mt-3 text-sm font-medium text-teal-700 hover:text-teal-800"
              >
                {item.cta} →
              </Link>
            </div>
          ))}
        </div>
      </section>

      <section className="pb-16">
        <NoticeBox>
          {getSiteMeta().disclaimer} 每个项目页都附有官方来源链接，请以官方最新信息为准。
        </NoticeBox>
      </section>
    </div>
  );
}
