import Link from "next/link";
import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { CountryFlag } from "@/components/country-flag";
import { NoticeBox, SectionHeading } from "@/components/notice";
import { ProgramCard } from "@/components/program-card";
import {
  federalPrograms,
  getCountry,
  listCountries,
  listPrograms,
  listRegions,
} from "@/lib/content";
import { toCardData } from "@/lib/display";

export function generateStaticParams() {
  return listCountries().map((c) => ({ country: c.id }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ country: string }>;
}): Promise<Metadata> {
  const { country } = await params;
  const c = getCountry(country);
  if (!c) return { title: "未找到国家" };
  return {
    title: `${c.name}移民方式与流程指南`,
    description: c.summary,
  };
}

export default async function CountryPage({
  params,
}: {
  params: Promise<{ country: string }>;
}) {
  const { country: countryId } = await params;
  const country = getCountry(countryId);
  if (!country) notFound();

  const regions = listRegions(country.id);
  const federal = federalPrograms(country.id);
  const all = listPrograms().filter((p) => p.country === country.id);

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <Breadcrumbs items={[{ label: country.name }]} />

      {/* 头部 */}
      <header className="mt-6 flex items-start gap-4">
        <CountryFlag
          countryId={country.id}
          flag={country.flag}
          className="mt-1 h-12 w-[4.5rem] rounded shadow-sm md:h-16 md:w-24"
        />
        <div>
          <h1 className="text-3xl font-bold text-slate-900 md:text-4xl">
            {country.name}
            <span className="ml-3 text-lg font-normal text-slate-400">
              {country.nameEn}
            </span>
          </h1>
          <p className="mt-2 max-w-3xl text-slate-600">{country.summary}</p>
          <a
            href={country.officialSite}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-2 inline-block text-sm font-medium text-teal-700 hover:text-teal-800"
          >
            官方移民机构 ↗
          </a>
        </div>
      </header>

      {/* 移民体系 */}
      <section className="mt-10 grid gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="font-bold text-slate-900">移民体系概览</h2>
          <p className="mt-2 text-sm leading-relaxed text-slate-600">
            {country.systemOverview}
          </p>
        </div>
        <NoticeBox tone="warn">
          <strong>联邦 vs 省/州：</strong>
          {country.regionalPolicyNote}
        </NoticeBox>
      </section>

      {/* 省/州列表 */}
      {regions.length > 0 && (
        <section className="mt-12">
          <SectionHeading
            title={`省/州政策（${regions.length}）`}
            description="点击查看该省/州的政策概述与本地项目。"
          />
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {regions.map((region) => {
              const count = all.filter((p) => p.region === region.id).length;
              return (
                <Link
                  key={region.id}
                  href={`/countries/${country.id}/${region.id}/`}
                  className="group rounded-2xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-teal-300 hover:shadow-lg hover:shadow-teal-600/5"
                >
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-slate-900 group-hover:text-teal-700">
                      {region.name}
                    </h3>
                    <span className="text-xs text-slate-400">{count} 个项目</span>
                  </div>
                  <p className="mt-0.5 text-xs text-slate-400">{region.nameEn}</p>
                  <p className="mt-2 line-clamp-2 text-sm text-slate-500">
                    {region.summary}
                  </p>
                </Link>
              );
            })}
          </div>
        </section>
      )}

      {/* 联邦级项目 */}
      {federal.length > 0 && (
        <section className="mt-12">
          <SectionHeading
            title="联邦级项目"
            description="由中央移民机构统一管理、全国适用的路径。"
          />
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {federal.map((p) => (
              <ProgramCard key={p.slug} data={toCardData(p)} />
            ))}
          </div>
        </section>
      )}

      {/* 全部项目 */}
      <section className="mt-12 pb-16">
        <SectionHeading
          title={`全部 ${all.length} 个项目`}
          description="含联邦与省/州级，可进入详情页对比流程与费用。"
        />
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {all.map((p) => (
            <ProgramCard key={p.slug} data={toCardData(p)} />
          ))}
        </div>
      </section>
    </div>
  );
}
