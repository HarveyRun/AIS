import Link from "next/link";
import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { CountryFlag } from "@/components/country-flag";
import { Badge } from "@/components/program-card";
import { NoticeBox, SectionHeading } from "@/components/notice";
import { Timeline } from "@/components/timeline";
import { ProgramCard } from "@/components/program-card";
import {
  getCountry,
  getProgram,
  getRegion,
  listPrograms,
  relatedPrograms,
} from "@/lib/content";
import {
  difficultyLabels,
  immigrationTypeLabels,
  statusLabels,
} from "@/lib/schema";
import { moneyText, monthsText, toCardData } from "@/lib/display";
import { difficultyStyles, statusStyles, typeStyles } from "@/lib/ui";

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

  const related = relatedPrograms(program, 3);
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
          <Badge className={typeStyles[program.type]}>
            {immigrationTypeLabels[program.type]}
          </Badge>
          <Badge className={difficultyStyles[program.difficulty]}>
            {difficultyLabels[program.difficulty]}
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
        <p className="mt-3 max-w-3xl leading-relaxed text-slate-600">
          {program.summary}
        </p>
        <p className="mt-3 max-w-3xl rounded-xl bg-teal-50 p-3 text-sm leading-relaxed text-teal-900">
          <strong>适合谁：</strong>
          {program.suitableFor}
        </p>
      </header>

      {/* 关键数字 */}
      <section className="mt-8 grid grid-cols-2 gap-3 md:grid-cols-4">
        {[
          {
            label: "总费用估算",
            value: moneyText(cost.totalEstimate),
          },
          {
            label: "整体周期",
            value: monthsText(program.duration.totalMonths),
          },
          { label: "申请难度", value: difficultyLabels[program.difficulty] },
          { label: "信息核实日期", value: program.infoVerifiedAt },
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

      <div className="mt-12 grid gap-12 lg:grid-cols-[1fr_320px]">
        {/* 左主栏 */}
        <div className="min-w-0">
          {/* 流程时间线 */}
          <section>
            <SectionHeading
              title="分步流程"
              description={
                program.duration.note
                  ? undefined
                  : "各阶段时长为典型估算，以官方与个案为准。"
              }
            />
            {program.duration.note && (
              <p className="mb-4 text-sm text-slate-500">
                {program.duration.note}
              </p>
            )}
            <Timeline steps={program.duration.steps} />
          </section>

          {/* 申请条件 */}
          <section className="mt-12">
            <SectionHeading title="申请条件" />
            <ul className="space-y-3">
              {program.requirements.map((req) => (
                <li
                  key={req.title}
                  className="rounded-xl border border-slate-200 bg-white p-4"
                >
                  <p className="font-semibold text-slate-900">✓ {req.title}</p>
                  {req.detail && (
                    <p className="mt-1 text-sm leading-relaxed text-slate-600">
                      {req.detail}
                    </p>
                  )}
                </li>
              ))}
            </ul>
          </section>

          {/* 费用明细 */}
          <section className="mt-12">
            <SectionHeading
              title="费用明细"
              description={cost.note}
            />
            <div className="overflow-hidden rounded-2xl border border-slate-200">
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
          <section className="mt-12">
            <SectionHeading
              title="材料清单"
              description="按此逐项准备；带 * 项通常有有效期要求，临近递交再办理。"
            />
            <ul className="grid gap-2 md:grid-cols-2">
              {program.materials.map((m) => (
                <li
                  key={m}
                  className="flex items-start gap-2 rounded-xl border border-slate-200 bg-white p-3 text-sm text-slate-700"
                >
                  <span aria-hidden className="mt-0.5 text-teal-600">
                    ☐
                  </span>
                  {m}
                </li>
              ))}
            </ul>
          </section>

          {/* 风险与拒签 */}
          <section className="mt-12 grid gap-4 md:grid-cols-2">
            {program.commonRejections && (
              <div className="rounded-2xl border border-rose-100 bg-rose-50/50 p-5">
                <h3 className="font-bold text-rose-900">常见拒签原因</h3>
                <ul className="mt-2 space-y-1.5 text-sm leading-relaxed text-rose-800">
                  {program.commonRejections.map((r) => (
                    <li key={r}>· {r}</li>
                  ))}
                </ul>
              </div>
            )}
            {program.risks && (
              <div className="rounded-2xl border border-amber-100 bg-amber-50/50 p-5">
                <h3 className="font-bold text-amber-900">风险提示</h3>
                <ul className="mt-2 space-y-1.5 text-sm leading-relaxed text-amber-800">
                  {program.risks.map((r) => (
                    <li key={r}>· {r}</li>
                  ))}
                </ul>
              </div>
            )}
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
                  {h}
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

          <NoticeBox tone="warn">
            本文最后人工核实于 <strong>{program.infoVerifiedAt}</strong>
            。移民政策变动频繁，递交申请前请务必核对官方最新要求。
          </NoticeBox>
        </aside>
      </div>

      {/* 相关项目 */}
      {related.length > 0 && (
        <section className="mt-16 pb-16">
          <SectionHeading title="相关项目" />
          <div className="grid gap-4 md:grid-cols-3">
            {related.map((p) => (
              <ProgramCard key={p.slug} data={toCardData(p)} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
