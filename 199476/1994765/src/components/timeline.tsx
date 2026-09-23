import type { TermIndex } from "@/lib/term-link";
import { TermsText } from "@/components/terms-text";

export interface TimelineStep {
  title: string;
  detail?: string;
  durationHint?: string;
}

export function Timeline({
  steps,
  termIndex,
}: {
  steps: TimelineStep[];
  /** 传入后，步骤标题与说明里的术语会显示悬浮解释 */
  termIndex?: TermIndex;
}) {
  return (
    <ol className="relative space-y-6 border-l-2 border-teal-100 pl-6">
      {steps.map((step, i) => (
        <li key={step.title} className="relative">
          <span
            aria-hidden
            className="absolute -left-[33px] grid h-6 w-6 place-items-center rounded-full bg-teal-600 text-xs font-bold text-white ring-4 ring-teal-50"
          >
            {i + 1}
          </span>
          <div className="flex flex-wrap items-center gap-2">
            <h4 className="font-semibold text-slate-900">
              {termIndex ? (
                <TermsText text={step.title} index={termIndex} />
              ) : (
                step.title
              )}
            </h4>
            {step.durationHint && step.durationHint !== "-" && (
              <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">
                ⏱ {step.durationHint}
              </span>
            )}
          </div>
          {step.detail && (
            <p className="mt-1 text-sm leading-relaxed text-slate-600">
              {termIndex ? (
                <TermsText text={step.detail} index={termIndex} />
              ) : (
                step.detail
              )}
            </p>
          )}
        </li>
      ))}
    </ol>
  );
}
