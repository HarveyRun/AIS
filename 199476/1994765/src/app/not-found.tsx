import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto flex max-w-6xl flex-col items-center px-4 py-24 text-center">
      <p className="text-6xl" aria-hidden>
        🧭
      </p>
      <h1 className="mt-4 text-2xl font-bold text-slate-900">页面没有找到</h1>
      <p className="mt-2 text-slate-500">链接可能已失效，或内容还在整理中。</p>
      <div className="mt-6 flex gap-3">
        <Link
          href="/"
          className="rounded-xl bg-teal-600 px-5 py-2.5 font-medium text-white hover:bg-teal-700"
        >
          回首页
        </Link>
        <Link
          href="/basics/"
          className="rounded-xl border border-slate-300 px-5 py-2.5 font-medium text-slate-700 hover:bg-slate-50"
        >
          看移民科普
        </Link>
      </div>
    </div>
  );
}
