import type { Metadata } from "next";
import Link from "next/link";
import { NavActions } from "@/components/nav-actions";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "a.ilu — 集合站",
    template: "%s | Inlightus",
  },
  description: "每一个站点，解决一件事。",
};

function Header() {
  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Link href="/" className="flex items-center gap-2.5">
          <img src="/icons/guangyi.png" alt="光忆" className="h-9 w-9 rounded-lg object-cover" />
          <span className="hidden text-xs font-medium tracking-widest text-slate-400 sm:block">
            INLIGHTUS
          </span>
        </Link>
        <nav className="flex items-center gap-6 text-sm font-medium text-slate-600">
          <NavActions />
        </nav>
      </div>
    </header>
  );
}

function Footer() {
  return (
    <footer className="border-t border-slate-200 bg-slate-50">
      <div className="mx-auto flex max-w-6xl flex-col gap-3 px-4 py-8 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
        <p>© {new Date().getFullYear()} Inlightus · 光忆出品 · 内容仅供参考</p>
        <div className="flex gap-5">
          <NavActions className="flex gap-5" />
        </div>
      </div>
    </footer>
  );
}

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body className="flex min-h-screen flex-col bg-white text-slate-800">
        <Header />
        <main className="flex-1">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
