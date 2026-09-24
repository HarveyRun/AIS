import Link from "next/link";
import { CountryFlag } from "@/components/country-flag";
import {
  statusStyles,
  typeStyles,
  type ProgramCardData,
} from "@/lib/ui";

export function ProgramCard({
  data,
  showLocation = true,
  showScope = "all",
  showType = true,
  scopeText,
}: {
  data: ProgramCardData;
  /** “国家 · 省/州/区”位置标签；在省/州/区详情页内冗余，可关 */
  showLocation?: boolean;
  /** 范围标签：all 全部显示 / federalOnly 仅联邦卡片显示 / none 不显示 */
  showScope?: "all" | "federalOnly" | "none";
  /** 移民方式标签；已按方式分组的列表中冗余，可关 */
  showType?: boolean;
  /** 范围标签文案覆盖，如省页内把“安大略省项目”显示为“本省项目” */
  scopeText?: string;
}) {
  const showScopeBadge =
    showScope === "all" ||
    (showScope === "federalOnly" && data.scope === "federal");
  return (
    <Link
      href={`/programs/${data.slug}/`}
      className="group flex flex-col rounded-2xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-teal-300 hover:shadow-lg hover:shadow-teal-600/5"
    >
      {(showLocation || showScopeBadge || showType) && (
        <div className="flex flex-wrap items-center gap-1.5">
          {showLocation && (
            <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">
              <CountryFlag
                countryId={data.countryId}
                flag={data.countryFlag}
                className="h-2.5 w-3.5"
              />
              {data.countryName}
              {data.regionName ? ` · ${data.regionName}` : ""}
            </span>
          )}
          {showScopeBadge && (
            <Badge
              className={
                data.scope === "federal"
                  ? "bg-blue-50 text-blue-700 ring-blue-600/20"
                  : "bg-orange-50 text-orange-700 ring-orange-600/20"
              }
            >
              {scopeText ?? data.scopeLabel}
            </Badge>
          )}
          {showType && <Badge className={typeStyles[data.type]}>{data.typeLabel}</Badge>}
        </div>
      )}

      <h3 className="mt-3 font-bold text-slate-900 group-hover:text-teal-700">
        {data.name}
      </h3>
      <p className="mt-0.5 text-xs text-slate-400">{data.nameEn}</p>
      <p className="mt-2 line-clamp-2 flex-1 text-sm leading-relaxed text-slate-600">
        {data.summary}
      </p>

      <div className="mt-4 flex items-center border-t border-slate-100 pt-3 text-xs text-slate-400">
        <span>更新时间 {data.infoVerifiedAt}</span>
        <span className="ml-auto text-teal-700 opacity-0 transition group-hover:opacity-100">
          查看详情 →
        </span>
      </div>
    </Link>
  );
}

export function Badge({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${className}`}
    >
      {children}
    </span>
  );
}
