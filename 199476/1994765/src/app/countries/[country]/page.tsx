import Link from "next/link";
import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { CountryFlag } from "@/components/country-flag";
import { NoticeBox, SectionHeading } from "@/components/notice";
import { ProgramCard } from "@/components/program-card";
import {
  getCountry,
  getGlossary,
  listCountries,
  listPrograms,
  listRegions,
} from "@/lib/content";
import { buildTermIndex } from "@/lib/term-link";
import { TermsText } from "@/components/terms-text";
import { toCardData } from "@/lib/display";
import { immigrationTypeLabels } from "@/lib/schema";

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
  const all = listPrograms().filter((p) => p.country === country.id);
  const termIndex = buildTermIndex(getGlossary());

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
            <TermsText text={country.systemOverview} index={termIndex} />
          </p>
        </div>
        <NoticeBox tone="warn">
          <strong>联邦 vs 省/州：</strong>
          <TermsText text={country.regionalPolicyNote} index={termIndex} />
        </NoticeBox>
      </section>

      {/* 省/州特有项目（第一位；同时保留在下方移民方式归类中） */}
      {(() => {
        const ownPrograms = all.filter((p) => p.region);
        if (ownPrograms.length === 0) return null;
        return (
          <section className="mt-12">
            <SectionHeading
              title={`特有项目（${ownPrograms.length}）`}
              description="由各省/州自己运作的提名/通道"
            />
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {ownPrograms.map((p) => (
                <ProgramCard key={p.slug} data={toCardData(p)} />
              ))}
            </div>
          </section>
        );
      })()}

      {/* 按移民方式归类 */}
      <section className="mt-12 border-t border-slate-200 pt-12 pb-5">
        <div className="space-y-10">
          {Object.entries(immigrationTypeLabels).map(([type, label]) => {
            const group = all.filter((p) => p.type === type);
            if (group.length === 0) return null;
            const federalCount = group.filter((p) => !p.region).length;
            return (
              <div key={type}>
                <h3 className="text-lg font-bold text-slate-900">
                  {label}
                  <span className="ml-2 text-sm font-normal text-slate-400">
                    {group.length} 个项目（联邦 {federalCount} · 省/州 {group.length - federalCount}）
                  </span>
                </h3>
                <div className="mt-3 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                  {group.map((p) => (
                    <ProgramCard key={p.slug} data={toCardData(p)} />
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      </section>

    </div>
  );
}
