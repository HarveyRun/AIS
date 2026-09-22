import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { NoticeBox } from "@/components/notice";
import { getGlossary } from "@/lib/content";

export const metadata: Metadata = {
  title: "移民术语表",
  description: "EOI、ITA、省提名、CLB、PERM、排期……移民申请中的高频术语中文解释。",
};

export default function GlossaryPage() {
  const terms = getGlossary();

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Breadcrumbs items={[{ label: "术语表" }]} />
      <h1 className="mt-6 text-3xl font-bold text-slate-900">移民术语表</h1>
      <p className="mt-2 text-slate-600">
        读项目页时遇到不懂的词，在这里查。共 {terms.length} 条。
      </p>

      <nav className="mt-6 flex flex-wrap gap-2" aria-label="术语索引">
        {terms.map((t) => (
          <a
            key={t.term}
            href={`#${encodeURIComponent(t.term)}`}
            className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600 hover:bg-teal-50 hover:text-teal-700"
          >
            {t.term}
          </a>
        ))}
      </nav>

      <div className="mt-8 space-y-3 pb-16">
        {terms.map((t) => (
          <article
            key={t.term}
            id={encodeURIComponent(t.term)}
            className="scroll-mt-20 rounded-2xl border border-slate-200 bg-white p-5"
          >
            <h2 className="font-bold text-slate-900">
              {t.term}
              {t.en && (
                <span className="ml-2 text-sm font-normal text-slate-400">{t.en}</span>
              )}
            </h2>
            <p className="mt-1.5 text-sm leading-relaxed text-slate-600">
              {t.explanation}
            </p>
          </article>
        ))}
      </div>

      <NoticeBox>
        缺了你需要的术语？该项目为静态内容，欢迎在项目仓库提 Issue 或直接补充 content/glossary.json。
      </NoticeBox>
    </div>
  );
}
