import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { SectionHeading } from "@/components/notice";
import {
  getSiteMeta,
  listCountries,
  listPrograms,
  listRegions,
} from "@/lib/content";

export const metadata: Metadata = {
  title: "关于本站",
  description: "网站定位、内容维护方式与免责声明。",
};

export default function AboutPage() {
  const site = getSiteMeta();
  const countries = listCountries();
  const regions = listRegions();
  const programs = listPrograms();

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Breadcrumbs items={[{ label: "关于" }]} />
      <h1 className="mt-6 text-3xl font-bold text-slate-900">关于 {site.name}</h1>

      <section className="mt-8">
        <SectionHeading title="这个网站是做什么的" />
        <div className="rounded-2xl border border-slate-200 bg-white p-6 text-sm leading-7 text-slate-600">
          <p>{site.description}</p>
          <p className="mt-3">
            我们把内容按「<strong>国家 → 省/州 → 项目</strong>」三级组织，因为大量移民国家的政策在地方层面与联邦并不一致：
            加拿大除魁北克外各省有独立的省提名标准，澳大利亚的 190/491 由各州自行筛选，美国虽无州级移民审批、但登陆后的生活成本与资源差异显著。
            选对省份，和技术移民选对国家同样重要。
          </p>
        </div>
      </section>

      <section className="mt-10">
        <SectionHeading title="内容如何维护" />
        <div className="rounded-2xl border border-slate-200 bg-white p-6 text-sm leading-7 text-slate-600">
          <ul className="list-disc space-y-2 pl-5">
            <li>每个项目页都标注「信息核实日期」，政策类内容定期人工复核。</li>
            <li>所有关键信息附官方来源链接，请以官方最新发布为准。</li>
            <li>
              当前收录：{countries.length} 个国家/地区、{regions.length} 个省/州、
              {programs.length} 个项目、{site.maintainer} 持续维护中。
            </li>
            <li>内容以结构化数据存储，新增或修订项目只需编辑对应 JSON 文件并通过校验。</li>
          </ul>
        </div>
      </section>

      <section className="mt-10 pb-16">
        <SectionHeading title="免责声明" />
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-6 text-sm leading-7 text-amber-900">
          {site.disclaimer}
        </div>
      </section>
    </div>
  );
}
