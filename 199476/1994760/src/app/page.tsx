import { sites } from "@/lib/sites";

const featurePills = ["移民经历", "路线复盘", "避坑故事"];

export default function HomePage() {
  return (
    <div className="mx-auto max-w-6xl px-4">
      {/* Hero */}
      <section className="py-10">
        <p className=" max-w-2xl text-base leading-7 text-slate-600">
          从这里开始，每一个站点，解决一件事
        </p>
      </section>

      {/* 站点集合 */}
      <section className="pb-20">
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {sites.map((s) => (
            <a
              key={s.title}
              href={s.url}
              target="_blank"
              rel="noopener noreferrer"
              className="group overflow-hidden rounded-2xl border border-slate-200 bg-white transition hover:-translate-y-0.5 hover:border-teal-300 hover:shadow-xl hover:shadow-teal-600/10"
            >
              <div className="relative aspect-[16/10] overflow-hidden bg-slate-100">
                <img
                  src={s.preview}
                  alt={`${s.title} 预览`}
                  className="h-full w-full object-cover object-top transition duration-500 group-hover:scale-[1.03]"
                />
                <span className="absolute left-3 top-3 flex items-center gap-1.5 rounded-full bg-emerald-500/95 px-2.5 py-0.5 text-xs font-medium text-white">
                  <span className="h-1.5 w-1.5 rounded-full bg-white" />
                  已上线
                </span>
              </div>
              <div className="flex items-center justify-between gap-3 border-t border-slate-100 p-4">
                <div>
                  <h2 className="font-bold text-slate-900 group-hover:text-teal-700">
                    {s.title}
                  </h2>
                  <p className="mt-0.5 line-clamp-1 text-xs text-slate-500">{s.desc}</p>
                </div>
                <span className="shrink-0 text-sm font-medium text-teal-700 transition group-hover:translate-x-0.5">
                  立即查看 →
                </span>
              </div>
            </a>
          ))}

          {/* 敬请期待占位卡片 */}
          <div className="flex min-h-[260px] flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-200 p-6 text-center">
            <span className="text-3xl">🚧</span>
            <p className="mt-3 font-semibold text-slate-500">更多站点筹备中</p>
            <p className="mt-1 text-xs text-slate-400">
              下一件「具体的事」，正在路上
            </p>
          </div>
        </div>
      </section>

      {/* 底部引导：去光忆 */}
      <section className="pb-20">
        <div className="overflow-hidden rounded-2xl bg-slate-900">
          <div className="flex flex-col items-center gap-6 px-8 py-10 md:flex-row md:gap-10 md:px-10">
            <img
              src="/icons/guangyi.png"
              alt="光忆 App"
              className="h-16 w-16 shrink-0 rounded-2xl object-cover shadow-lg"
            />
            <div className="flex-1 text-center md:text-left">
              <h2 className="text-xl font-bold text-white md:text-2xl">
                真实移民经历，在「光忆」App
              </h2>
              <p className="mt-2 text-sm leading-6 text-slate-400">
                亲历者的第一手路线复盘与避坑故事，按国家与路线分类，持续更新。
              </p>
            </div>
            <div className="shrink-0 rounded-xl bg-white px-5 py-3 text-center">
              <p className="text-xs text-slate-400">App Store / 安卓应用商店</p>
              <p className="mt-0.5 text-sm font-bold text-slate-900">搜索「光忆」</p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
