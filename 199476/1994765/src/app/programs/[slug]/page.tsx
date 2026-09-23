import Link from "next/link";
import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { CountryFlag } from "@/components/country-flag";
import { Badge } from "@/components/program-card";
import { SectionHeading } from "@/components/notice";
import { Timeline } from "@/components/timeline";
import { TermsText } from "@/components/terms-text";
import { ProgramCard } from "@/components/program-card";
import {
  getCountry,
  getGlossary,
  getProgram,
  getRegion,
  listPrograms,
} from "@/lib/content";
import { buildTermIndex } from "@/lib/term-link";
import {
  immigrationTypeLabels,
  statusLabels,
} from "@/lib/schema";
import { toCardData } from "@/lib/display";
import { statusStyles, typeStyles } from "@/lib/ui";

export function generateStaticParams() {
  return listPrograms().map((p) => ({ slug: p.slug }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const p = getProgram(slug);
  if (!p) return { title: "未找到项目" };
  return { title: `${p.name}：条件、费用、流程全解`, description: p.summary };
}

/** 区块标题：teal 竖条 + 加粗标题，视觉上把每个大区分开 */
function BlockHeading({ title }: { title: string }) {
  return (
    <h2 className="flex items-center gap-3 text-xl font-bold text-slate-900 md:text-2xl">
      <span aria-hidden className="h-7 w-1.5 rounded bg-teal-600" />
      {title}
    </h2>
  );
}

export default async function ProgramPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const program = getProgram(slug);
  if (!program) notFound();

  const country = getCountry(program.country);
  const region = program.region
    ? getRegion(program.country, program.region)
    : undefined;
  if (!country) notFound();

  const termIndex = buildTermIndex(getGlossary());
  const cost = program.cost;

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <Breadcrumbs
        items={[
          { label: country.name, href: `/countries/${country.id}/` },
          ...(region
            ? [{ label: region.name, href: `/countries/${country.id}/${region.id}/` }]
            : []),
          { label: program.name },
        ]}
      />

      {/* 头部 */}
      <header className="mt-6">
        <div className="flex flex-wrap items-center gap-1.5">
          <Link
            href={`/countries/${country.id}/`}
            className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600 hover:bg-slate-200"
          >
            <CountryFlag
              countryId={country.id}
              flag={country.flag}
              className="h-2.5 w-3.5"
            />
            {country.name}
            {region ? ` · ${region.name}` : ""}
          </Link>
          {/* 标签顺序：项目类型（联邦/本省）→ 移民方式 */}
          <Badge
            className={
              region
                ? "bg-orange-50 text-orange-700 ring-orange-600/20"
                : "bg-blue-50 text-blue-700 ring-blue-600/20"
            }
          >
            {region ? region.name + "项目" : "联邦项目"}
          </Badge>
          <Badge className={typeStyles[program.type]}>
            {immigrationTypeLabels[program.type]}
          </Badge>
          {program.status !== "active" && (
            <Badge className={statusStyles[program.status]}>
              {statusLabels[program.status]}
            </Badge>
          )}
        </div>
        <h1 className="mt-3 text-3xl font-bold text-slate-900 md:text-4xl">
          {program.name}
        </h1>
        <p className="mt-1 text-sm text-slate-400">{program.nameEn}</p>

        {/* 项目属性 */}
        <section className="mt-8 grid grid-cols-2 gap-3 md:grid-cols-3">
          {[
            { label: "所属层级", value: region ? `${country.name} · ${region.name}` : `${country.name} 联邦级` },
            { label: "移民方式", value: immigrationTypeLabels[program.type] },
            { label: "更新时间", value: program.infoVerifiedAt },
          ].map((item) => (
            <div
              key={item.label}
              className="rounded-2xl border border-slate-200 bg-white p-4"
            >
              <p className="text-xs text-slate-400">{item.label}</p>
              <p className="mt-1 font-bold text-slate-900">{item.value}</p>
            </div>
          ))}
        </section>

        {/* 优缺点：放在「适合谁」上面 */}
        <div className="mt-10 grid max-w-3xl gap-3 md:grid-cols-2">
          <div className="rounded-xl border border-emerald-100 bg-emerald-50/60 p-4">
            <p className="text-sm font-bold text-emerald-900">优点</p>
            <ul className="mt-1.5 space-y-1 text-sm leading-relaxed text-emerald-800">
              {program.pros.map((pro) => (
                <li key={pro}>· <TermsText text={pro} index={termIndex} /></li>
              ))}
            </ul>
          </div>
          <div className="rounded-xl border border-amber-100 bg-amber-50/60 p-4">
            <p className="text-sm font-bold text-amber-900">缺点 / 注意点</p>
            <ul className="mt-1.5 space-y-1 text-sm leading-relaxed text-amber-800">
              {program.cons.map((con) => (
                <li key={con}>· <TermsText text={con} index={termIndex} /></li>
              ))}
            </ul>
          </div>
        </div>
      </header>

    

      <div className="mt-12 grid gap-12 lg:grid-cols-[1fr_320px]">
        {/* 左主栏 */}
        <div className="min-w-0">
          {/* 分步流程 */}
          <section className="border-t border-slate-200 pt-10">
            <BlockHeading title="分步流程" />
            {program.duration.note && (
              <p className="mb-4 mt-3 text-sm text-slate-500">
                <TermsText text={program.duration.note} index={termIndex} />
              </p>
            )}
            <Timeline steps={program.duration.steps} termIndex={termIndex} />
          </section>

          {/* 申请条件 */}
          <section className="mt-12 border-t border-slate-200 pt-10">
            <BlockHeading title="申请条件" />
            <ul className="mt-4 space-y-3">
              {program.requirements.map((req) => (
                <li
                  key={req.title}
                  className="rounded-xl border border-slate-200 bg-white p-4"
                >
                  <p className="font-semibold text-slate-900">
                    ✓ <TermsText text={req.title} index={termIndex} />
                  </p>
                  {req.detail && (
                    <p className="mt-1 text-sm leading-relaxed text-slate-600">
                      <TermsText text={req.detail} index={termIndex} />
                    </p>
                  )}
                </li>
              ))}
            </ul>
          </section>

          {/* 费用明细 */}
          <section className="mt-12 border-t border-slate-200 pt-10">
            <BlockHeading title="费用明细" />
            <p className="mt-3 text-sm text-slate-500">
              {cost.note ?? "以下为官方规费与真实生活开销，以官方最新金额为准。"}
            </p>
            <div className="mt-4 overflow-hidden rounded-2xl border border-slate-200">
              <table className="w-full text-sm">
                <tbody>
                  {cost.breakdown.map((row, i) => (
                    <tr
                      key={row.item}
                      className={i % 2 === 0 ? "bg-white" : "bg-slate-50/60"}
                    >
                      <td className="px-4 py-3 font-medium text-slate-800">
                        {row.item}
                      </td>
                      <td className="px-4 py-3 text-slate-700">{row.amount}</td>
                      <td className="hidden px-4 py-3 text-xs text-slate-400 md:table-cell">
                        {row.note ?? ""}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          {/* 材料清单 */}
          <section className="mt-12 border-t border-slate-200 pt-10">
            <BlockHeading title="材料清单" />
            <ul className="mt-4 grid gap-2 md:grid-cols-2">
              {program.materials.map((m) => (
                <li
                  key={m}
                  className="flex items-start gap-2 rounded-xl border border-slate-200 bg-white p-3 text-sm text-slate-700"
                >
                  <span aria-hidden className="mt-0.5 text-teal-600">
                    ☐
                  </span>
                  <TermsText text={m} index={termIndex} />
                </li>
              ))}
            </ul>
          </section>

          {/* 虚构示例案例：放在材料清单下面 */}
          <section className="mt-12 border-t border-slate-200 pt-10">
            <BlockHeading title="走一遍：虚构案例" />
            <p className="mt-3 text-sm text-slate-500">
              示例说明，非真实案件
            </p>
            <div className="mt-4 rounded-xl bg-white p-4 ring-1 ring-slate-200">
              <p className="text-sm font-semibold text-slate-900">申请人条件</p>
              <p className="mt-1 text-sm leading-relaxed text-slate-600">
                <TermsText text={program.case.profile} index={termIndex} />
              </p>
            </div>
            <ol className="mt-4 space-y-2">
              {program.case.steps.map((step, i) => (
                <li key={step} className="flex gap-3 text-sm leading-relaxed text-slate-600">
                  <span aria-hidden className="grid h-5 w-5 shrink-0 place-items-center rounded-full bg-teal-600 text-[11px] font-bold text-white">
                    {i + 1}
                  </span>
                  <TermsText text={step} index={termIndex} />
                </li>
              ))}
            </ol>
            <p className="mt-4 rounded-xl bg-emerald-50 p-3 text-sm font-medium text-emerald-900">
              <strong>结果：</strong>
              <TermsText text={program.case.outcome} index={termIndex} />
            </p>
          </section>
        </div>

        {/* 右侧栏 */}
        <aside className="space-y-5">
          <div className="rounded-2xl border border-slate-200 bg-white p-5">
            <h3 className="font-bold text-slate-900">项目亮点</h3>
            <ul className="mt-3 space-y-2">
              {program.highlights.map((h) => (
                <li key={h} className="flex items-start gap-2 text-sm text-slate-600">
                  <span aria-hidden className="mt-0.5 text-teal-600">
                    ✦
                  </span>
                  <TermsText text={h} index={termIndex} />
                </li>
              ))}
            </ul>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5">
            <h3 className="font-bold text-slate-900">官方来源</h3>
            <p className="mt-1 text-xs text-slate-400">
              以下链接指向官方机构，政策细节以官方为准。
            </p>
            <ul className="mt-3 space-y-2">
              {program.officialSources.map((s) => (
                <li key={s.url}>
                  <a
                    href={s.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-sm font-medium text-teal-700 underline decoration-teal-200 underline-offset-2 hover:text-teal-800"
                  >
                    {s.label} ↗
                  </a>
                </li>
              ))}
            </ul>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5">
            <h3 className="font-bold text-slate-900">更新时间</h3>
            <p className="mt-2 text-sm text-slate-600">
              本页更新于 <strong>{program.infoVerifiedAt}</strong>
              。移民政策变动频繁，递交申请前请务必核对官方最新要求。
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}
