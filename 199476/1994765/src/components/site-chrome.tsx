import Link from "next/link";
import { getSiteMeta } from "@/lib/content";

const navItems = [
  { href: "/", label: "首页" },
  { href: "/faq/", label: "常见问题" },
  { href: "/glossary/", label: "术语表" },
];

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/85 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
        <Link href="/" className="flex items-center gap-2 font-bold text-slate-900">
          <span aria-hidden className="grid h-7 w-7 place-items-center rounded-lg bg-teal-600 text-sm text-white">
            移
          </span>
          <span>移民</span>
        </Link>

        <nav className="hidden items-center gap-1 md:flex" aria-label="主导航">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="rounded-lg px-3 py-1.5 text-sm text-slate-600 transition hover:bg-slate-100 hover:text-slate-900"
            >
              {item.label}
            </Link>
          ))}
        </nav>

        {/* 移动端菜单：无 JS 的 details 折叠 */}
        <details className="relative md:hidden">
          <summary className="flex cursor-pointer list-none items-center rounded-lg p-2 text-slate-600 hover:bg-slate-100">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <path d="M4 7h16M4 12h16M4 17h16" />
            </svg>
          </summary>
          <nav className="absolute right-0 top-12 w-48 rounded-xl border border-slate-200 bg-white p-2 shadow-lg" aria-label="移动端导航">
            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="block rounded-lg px-3 py-2 text-sm text-slate-700 hover:bg-slate-100"
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </details>
      </div>
    </header>
  );
}

export function SiteFooter() {
  const site = getSiteMeta();
  return (
    <footer className="border-t border-slate-200 bg-slate-50">
      <div className="mx-auto max-w-6xl px-4 py-10">
        <div className="flex flex-col gap-6 md:flex-row md:justify-between">
          <div className="max-w-2xl">
            <p className="mt-1 text-sm text-slate-600 font-bold">移民流程指南</p>
            <p className="mt-3 text-xs leading-relaxed text-slate-500">
              ⚠️ {site.disclaimer}
            </p>
          </div>
          <nav className="flex flex-col gap-2 text-sm" aria-label="页脚导航">
            {navItems.map((item) => (
              <Link key={item.href} href={item.href} className="text-slate-600 hover:text-slate-900">
                {item.label}
              </Link>
            ))}
          </nav>
        </div>
        <p className="mt-8 border-t border-slate-200 pt-4 text-xs text-slate-400">
          © {new Date().getFullYear()} {site.name} · {site.maintainer} · 内容仅供参考，以官方来源为准
        </p>
      </div>
    </footer>
  );
}
