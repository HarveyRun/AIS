import Link from "next/link";
import { CountryFlag } from "@/components/country-flag";
import {
  difficultyStyles,
  statusStyles,
  typeStyles,
  type ProgramCardData,
} from "@/lib/ui";

export function ProgramCard({ data }: { data: ProgramCardData }) {
  return (
    <Link
      href={`/programs/${data.slug}/`}
      className="group flex flex-col rounded-2xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-teal-300 hover:shadow-lg hover:shadow-teal-600/5"
    >
      <div className="flex flex-wrap items-center gap-1.5">
        <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">
          <CountryFlag
            countryId={data.countryId}
            flag={data.countryFlag}
            className="h-2.5 w-3.5"
          />
          {data.countryName}
          {data.regionName ? ` · ${data.regionName}` : ""}
        </span>
        <Badge className={typeStyles[data.type]}>{data.typeLabel}</Badge>
        <Badge className={difficultyStyles[data.difficulty]}>
          {data.difficultyLabel}
        </Badge>
      </div>

      <h3 className="mt-3 font-bold text-slate-900 group-hover:text-teal-700">
        {data.name}
      </h3>
      <p className="mt-0.5 text-xs text-slate-400">{data.nameEn}</p>
      <p className="mt-2 line-clamp-2 flex-1 text-sm leading-relaxed text-slate-600">
        {data.summary}
      </p>

      <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-1 border-t border-slate-100 pt-3 text-xs text-slate-500">
        <span>
          <span className="text-slate-400">总费用 </span>
          <span className="font-medium text-slate-700">{data.costText}</span>
        </span>
        <span>
          <span className="text-slate-400">周期 </span>
          <span className="font-medium text-slate-700">{data.durationText}</span>
        </span>
        <span className="ml-auto text-slate-400">
          核实于 {data.infoVerifiedAt}
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
