"use client";

import { useEffect, useState } from "react";

const DISMISS_KEY = "pc-notice-dismissed";

/* 手机端打开时提示电脑端浏览更顺畅；每个会话只提示一次（选「继续浏览」后不再打扰）。
   预览入口：任意页面地址后加 ?mobile=1 可在电脑端强制查看此提示。 */
export function MobileNotice() {
  const [show, setShow] = useState(false);
  const [copied, setCopied] = useState(false);
  const [url, setUrl] = useState("");

  useEffect(() => {
    const force = new URLSearchParams(window.location.search).has("mobile");
    const isMobile =
      /Android|iPhone|iPad|iPod|Mobile|HarmonyOS/i.test(navigator.userAgent) ||
      (window.matchMedia("(max-width: 768px)").matches && "ontouchstart" in window);
    if ((isMobile || force) && sessionStorage.getItem(DISMISS_KEY) !== "1") {
      setUrl(window.location.href);
      setShow(true);
    }
  }, []);

  if (!show) return null;

  const dismiss = () => {
    sessionStorage.setItem(DISMISS_KEY, "1");
    setShow(false);
  };

  const copyUrl = async () => {
    try {
      await navigator.clipboard.writeText(url);
    } catch {
      const ta = document.createElement("textarea");
      ta.value = url;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand("copy");
      ta.remove();
    }
    setCopied(true);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 text-center shadow-2xl">
        {/* 电脑 + 手机 图形 */}
        <div className="mx-auto flex items-end justify-center gap-1">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            className="h-12 w-12 text-teal-600"
          >
            <rect x="2" y="3" width="20" height="14" rx="2" />
            <line x1="8" x2="16" y1="21" y2="21" />
            <line x1="12" x2="12" y1="17" y2="21" />
          </svg>
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.8}
            strokeLinecap="round"
            strokeLinejoin="round"
            className="h-7 w-7 text-slate-300"
          >
            <rect width="14" height="20" x="5" y="2" rx="2" />
            <path d="M12 18h.01" />
          </svg>
        </div>

        <h2 className="mt-4 text-xl font-bold text-slate-900">推荐电脑端浏览</h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          小屏幕排版可能受限，电脑端体验更佳。
        </p>

        <div className="mt-4 rounded-xl bg-slate-50 px-4 py-3">
          <p className="text-xs text-slate-400">把下面的网址发送到电脑上打开</p>
          <p className="mt-1 select-all break-all font-mono text-sm text-slate-800">{url}</p>
        </div>

        <div className="mt-4 space-y-2">
          <button
            onClick={copyUrl}
            className="w-full rounded-xl bg-teal-600 px-4 py-3 text-sm font-medium text-white transition hover:bg-teal-700"
          >
            {copied ? "已复制 ✓ 去电脑上粘贴打开" : "复制网址"}
          </button>
          <button
            onClick={dismiss}
            className="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
          >
            继续用手机浏览
          </button>
        </div>
      </div>
    </div>
  );
}
