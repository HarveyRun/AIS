import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { CountryFlag } from "@/components/country-flag";
import { ProgramCard } from "@/components/program-card";
import {
  getCountry,
  getGlossary,
  federalPrograms,
  getRegion,
  listRegions,
  programsByRegion,
} from "@/lib/content";
import { buildTermIndex } from "@/lib/term-link";
import { TermsText } from "@/components/terms-text";
import { toCardData } from "@/lib/display";
import { immigrationTypeLabels } from "@/lib/schema";

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
  if (!r) return { title: "未找到省/州/区" };
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

  const own = programsByRegion(countryId, regionId);
  const federal = federalPrograms(countryId);
  const all = [...own, ...federal];
  const termIndex = buildTermIndex(getGlossary());

  // 按移民方式归类（本省 + 联邦一起分）
  const groups = Object.entries(immigrationTypeLabels)
    .map(([type, label]) => ({
      type,
      label,
      items: all.filter((p) => p.type === type),
    }))
    .filter((g) => g.items.length > 0);

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
      </header>


      {/* 页面最后：优缺点 */}
      <section className="mt-10 pb-5">
        <div className="grid gap-4 md:grid-cols-2">
          <div className="rounded-2xl border border-emerald-100 bg-emerald-50/50 p-5">
            <h3 className="font-bold text-emerald-900">优点</h3>
            <ul className="mt-2 space-y-1.5 text-sm leading-relaxed text-emerald-800">
              {region.pros.map((pro) => (
                <li key={pro}>· <TermsText text={pro} index={termIndex} /></li>
              ))}
            </ul>
          </div>
          <div className="rounded-2xl border border-amber-100 bg-amber-50/50 p-5">
            <h3 className="font-bold text-amber-900">缺点 / 注意点</h3>
            <ul className="mt-2 space-y-1.5 text-sm leading-relaxed text-amber-800">
              {region.cons.map((con) => (
                <li key={con}>· <TermsText text={con} index={termIndex} /></li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {/* 按移民方式归类：本省 + 联邦一起列出 */}
      <section className="mt-12 pb-10">
        <div className="space-y-10">
          {groups.map(({ type, label, items }) => {
            const ownCount = items.filter((p) => p.region).length;
            const fedCount = items.length - ownCount;
            return (
              <div key={type}>
                <h3 className="text-lg font-bold text-slate-900">
                  {label}
                  <span className="ml-2 text-sm font-normal text-slate-400">
                    {items.length} 个项目（本省 {ownCount} · 联邦 {fedCount}）
                  </span>
                </h3>
                <div className="mt-3 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                  {items.map((p) => (
                    <ProgramCard
                      key={p.slug}
                      data={toCardData(p)}
                      showLocation={false}
                      showScope="all"
                      scopeText={p.region ? "本省项目" : undefined}
                      showType={false}
                    />
                  ))}
                </div>
              </div>
            );
          })}
          {groups.length === 0 && (
            <div className="rounded-2xl border border-dashed border-slate-300 p-10 text-center text-slate-500">
              该省/州/区暂无可申请的项目。
            </div>
          )}
        </div>
      </section>
      {/* 页面最后：政策概述 */}
      <section className="pb-5">
        <div className="mt-3 rounded-2xl border border-slate-200 bg-white p-6">
          <p className="leading-7 text-slate-600">
            政策概述：<TermsText text={region.policyOverview} index={termIndex} />
          </p>
        </div>
      </section>
    </div>
  );
}
