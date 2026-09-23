import { segmentTerms, type TermIndex, type TermSegment } from "@/lib/term-link";

/**
 * 正文术语悬浮解释：命中的词显示虚线下划线，悬停/键盘聚焦弹出解释。
 * 服务端渲染的纯 CSS 实现（group-hover / group-focus），无 JS。
 */
export function TermsText({
  text,
  index,
}: {
  text: string;
  index: TermIndex;
}) {
  const segments: TermSegment[] = segmentTerms(text, index);
  return (
    <>
      {segments.map((seg, i) =>
        seg.term ? (
          <span
            key={i}
            tabIndex={0}
            className="group relative cursor-help border-b border-dashed border-teal-400/70 font-medium text-teal-700 outline-none"
          >
            {seg.text}
            <span className="pointer-events-none absolute bottom-full left-1/2 z-30 mb-1.5 hidden w-72 -translate-x-1/2 rounded-xl bg-slate-900/95 p-3 text-left text-xs font-normal leading-relaxed text-slate-100 shadow-xl group-hover:block group-focus:block">
              <span className="font-bold text-white">{seg.term.term}：</span>
              {seg.term.explanation}
            </span>
          </span>
        ) : (
          <span key={i}>{seg.text}</span>
        )
      )}
    </>
  );
}
