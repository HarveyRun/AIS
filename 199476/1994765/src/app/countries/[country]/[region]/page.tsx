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
  getRegion,
  listRegions,
  programsByRegion,
} from "@/lib/content";
import { toCardData } from "@/lib/display";

export function generateStaticParams() {
  return listRegions().map((r) => ({
    country: r.country,
    region: r.id,
  }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ country: string; region: string }>;
}): Promise<Metadata> {
  const { country, region } = await params;
  const r = getRegion(country, region);
  if (!r) return { title: "未找到省/州" };
  return {
    title: `${r.name}移民政策与项目`,
    description: r.summary,
  };
}

export default async function RegionPage({
  params,
}: {
  params: Promise<{ country: string; region: string }>;
}) {
  const { country: countryId, region: regionId } = await params;
  const country = getCountry(countryId);
  const region = getRegion(countryId, regionId);
  if (!country || !region) notFound();

  const programs = programsByRegion(countryId, regionId);
  const federal = federalPrograms(countryId);
  const siblings = listRegions(countryId).filter((r) => r.id !== regionId);

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <Breadcrumbs
        items={[
          { label: country.name, href: `/countries/${country.id}/` },
          { label: region.name },
        ]}
      />

      <header className="mt-6">
        <p className="flex items-center gap-1.5 text-sm text-slate-400">
          <CountryFlag
            countryId={country.id}
            flag={country.flag}
            className="h-3 w-4"
          />
          {country.name} · {region.nameEn}
        </p>
        <h1 className="mt-1 text-3xl font-bold text-slate-900 md:text-4xl">
          {region.name}
        </h1>
        <p className="mt-2 max-w-3xl text-slate-600">{region.summary}</p>
        <a
          href={region.officialSite}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-2 inline-block text-sm font-medium text-teal-700 hover:text-teal-800"
        >
          {region.name}官方移民网站 ↗
        </a>
      </header>

      <section className="mt-10">
        <SectionHeading title="省/州政策概述" />
        <div className="rounded-2xl border border-slate-200 bg-white p-6">
          <p className="whitespace-pre-line text-sm leading-7 text-slate-600">
            {region.policyOverview}
          </p>
        </div>
      </section>

      <section className="mt-10">
        <NoticeBox tone="warn">
          省/州项目通常与联邦政策联动（如获提名后仍需走联邦审批），建议结合
          <Link
            href={`/countries/${country.id}/`}
            className="mx-1 font-medium text-teal-700 underline underline-offset-2"
          >
            {country.name}联邦层面政策
          </Link>
          一起阅读。
        </NoticeBox>
      </section>

      {programs.length > 0 ? (
        <section className="mt-12">
          <SectionHeading
            title={`${region.name}的项目（${programs.length}）`}
          />
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {programs.map((p) => (
              <ProgramCard key={p.slug} data={toCardData(p)} />
            ))}
          </div>
        </section>
      ) : (
        <section className="mt-12">
          <NoticeBox>
            {region.name}暂未收录独立的省级项目，移民通常通过联邦/全国性通道完成——可在下方查看，或查看该国其他省/州。
          </NoticeBox>
        </section>
      )}

      {federal.length > 0 && (
        <section className="mt-12">
          <SectionHeading
            title="联邦级项目（同样适用于本省/州）"
          />
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {federal.map((p) => (
              <ProgramCard key={p.slug} data={toCardData(p)} />
            ))}
          </div>
        </section>
      )}

      {siblings.length > 0 && (
        <section className="mt-12 pb-16">
          <SectionHeading title="该国其他省/州" />
          <div className="flex flex-wrap gap-2">
            {siblings.map((r) => (
              <Link
                key={r.id}
                href={`/countries/${country.id}/${r.id}/`}
                className="rounded-full border border-slate-200 bg-white px-4 py-1.5 text-sm text-slate-600 transition hover:border-teal-300 hover:text-teal-700"
              >
                {r.name}
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
