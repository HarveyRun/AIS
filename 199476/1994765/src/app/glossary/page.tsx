import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { NoticeBox } from "@/components/notice";
import { getGlossary, listCountries, listRegions } from "@/lib/content";
import { GlossaryClient, type GlossaryTerm } from "./glossary-client";

export const metadata: Metadata = {
  title: "移民术语表",
  description: "不同国家术语差异较大",
};

export default function GlossaryPage() {
  const terms: GlossaryTerm[] = getGlossary().map((t) => ({
    term: t.term,
    en: t.en,
    countries: t.countries,
    category: t.category,
    importance: t.importance,
    regions: t.regions,
    explanation: t.explanation,
  }));
  const countries = listCountries().map((c) => ({
    id: c.id,
    name: c.name,
    flag: c.flag,
  }));
  const regions = listRegions().map((r) => ({
    countryId: r.country,
    id: r.id,
    name: r.name,
  }));

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Breadcrumbs items={[{ label: "术语表" }]} />
      <h1 className="mt-6 text-3xl font-bold text-slate-900">移民术语表</h1>
      <p className="mt-2 text-slate-600">
        不同国家术语差异较大
      </p>
      <div className="mt-6">
        <GlossaryClient terms={terms} countries={countries} regions={regions} />
      </div>
    </div>
  );
}
