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
} from "@/lib/content";
import { buildTermIndex } from "@/lib/term-link";
import { TermsText } from "@/components/terms-text";
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

  const all = listPrograms().filter((p) => p.country === country.id);
  const federal = all.filter((p) => !p.region);
  const ownPrograms = all.filter((p) => p.region);
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
          <strong>联邦 vs 省/州/区：</strong>
          <TermsText text={country.regionalPolicyNote} index={termIndex} />
        </NoticeBox>
      </section>

      {/* 联邦项目（全国通用） */}
      {federal.length > 0 && (
        <section className="mt-12 border-t border-slate-200 pt-12 pb-5">
          <SectionHeading
            title={`联邦项目（${federal.length}）`}
            description="由联邦政府统一运作，全国通用，不限省/州/区"
          />
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {federal.map((p) => (
              <ProgramCard
                key={p.slug}
                data={toCardData(p)}
                showLocation={false}
                showScope="none"
              />
            ))}
          </div>
        </section>
      )}

      {/* 省/州/区项目（第一位） */}
      {ownPrograms.length > 0 && (
        <section className="mt-12">
          <SectionHeading
            title={`省/州/区项目（${ownPrograms.length}）`}
            description="由各省/州/区自己运作的提名/通道"
          />
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {ownPrograms.map((p) => (
              <ProgramCard key={p.slug} data={toCardData(p)} />
            ))}
          </div>
        </section>
      )}

      
    </div>
  );
}
