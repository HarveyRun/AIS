import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { NoticeBox } from "@/components/notice";
import { getCountry, getRegion, listCountries, listPrograms } from "@/lib/content";
import { immigrationTypeLabels } from "@/lib/schema";
import { AssessmentClient, type AssessmentProgramInfo } from "./assessment-client";

export const metadata: Metadata = {
  title: "移民方式评估工具",
  description: "回答 8 个问题，按结构化申请条件匹配最适合你的移民路径，并给出差距分析。",
};

export default function AssessmentPage() {
  const countries = listCountries().map((c) => ({ id: c.id, name: c.name, flag: c.flag }));

  const programs: AssessmentProgramInfo[] = listPrograms()
    .filter((p) => p.status === "active")
    .map((p) => {
      const country = getCountry(p.country);
      const region = p.region ? getRegion(p.country, p.region) : undefined;
      return {
        slug: p.slug,
        name: p.name,
        country: p.country,
        type: p.type,
        countryName: country?.name ?? p.country,
        countryFlag: country?.flag ?? "🌐",
        regionName: region?.name,
        typeLabel: immigrationTypeLabels[p.type],
        difficulty: p.difficulty,
        eligibility: p.eligibility,
      };
    });

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Breadcrumbs items={[{ label: "评估工具" }]} />
      <h1 className="mt-6 text-3xl font-bold text-slate-900">测测我适合哪条路</h1>
      <p className="mt-2 text-slate-600">
        4 步、8 个问题。评估按每个项目页的结构化申请条件（学历、语言、预算、雇主、年龄）打分，并告诉你差在哪里。
      </p>
      <div className="mt-6">
        <AssessmentClient countries={countries} programs={programs} />
      </div>
      <div className="mt-8">
        <NoticeBox>
          评估不收集也不上传任何信息，全部计算在你的浏览器内完成。
        </NoticeBox>
      </div>
    </div>
  );
}
