"use client";

import { useEffect, useState } from "react";
import { sites } from "@/lib/sites";

/* 联系邮箱：提交申请会打开邮件客户端并发送到此地址（TODO: 替换为实际邮箱） */
const CONTACT_EMAIL = "editor@inlightus.com";

type Modal = "none" | "contribute" | "join";

/* 弹窗打开时锁定主站滚动 */
function useLockScroll(active: boolean) {
  useEffect(() => {
    if (active) {
      const prev = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = prev;
      };
    }
  }, [active]);
}

function ModalShell({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="no-scrollbar max-h-[85vh] w-full max-w-md overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900">{title}</h2>
          <button onClick={onClose} className="rounded-full p-1 text-slate-400 hover:text-slate-700" aria-label="关闭">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" className="h-5 w-5">
              <path d="M18 6 6 18" />
              <path d="m6 6 12 12" />
            </svg>
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

const inputCls =
  "mt-1.5 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-800 focus:border-teal-500 focus:outline-none";
const labelCls = "block text-sm font-medium text-slate-700";

function SuccessNote() {
  return (
    <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm leading-6 text-emerald-800">
      <b>已为你打开邮件客户端</b>
      <br />
      发送邮件即完成申请。如未自动打开，请直接写信至
      <span className="mx-1 font-semibold">{CONTACT_EMAIL}</span>
      。
    </div>
  );
}

/* ---------- 参与编辑 ---------- */
function ContributeModal({ onClose }: { onClose: () => void }) {
  useLockScroll(true);
  const [site, setSite] = useState(sites[0]?.title ?? "");
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const subject = encodeURIComponent(`[参与编辑] ${site}`);
    const body = encodeURIComponent(`我想参与「${site}」的内容编辑。\n\n邮箱：${email}\n`);
    setSent(true);
    window.location.href = `mailto:${CONTACT_EMAIL}?subject=${subject}&body=${body}`;
  };

  return (
    <ModalShell title="参与编辑" onClose={onClose}>
      {sent ? (
        <SuccessNote />
      ) : (
        <form onSubmit={submit} className="mt-4 space-y-4">
          <div>
            <label className={labelCls}>参与站点</label>
            <select className={inputCls} value={site} onChange={(e) => setSite(e.target.value)}>
              {sites.map((s) => (
                <option key={s.title} value={s.title}>
                  {s.title}
                </option>
              ))}
              <option value="其它">其它（在邮件里说明）</option>
            </select>
          </div>
          <div>
            <label className={labelCls}>你的邮箱</label>
            <input
              type="email"
              required
              className={inputCls}
              placeholder="name@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <p className="mt-1 text-xs text-slate-400">仅用于编辑组回复你，不会公开。</p>
          </div>
          <button type="submit" className="w-full rounded-xl bg-teal-600 px-4 py-3 text-sm font-medium text-white transition hover:bg-teal-700">
            打开邮件客户端，发送申请
          </button>
        </form>
      )}
    </ModalShell>
  );
}

/* ---------- 加入光忆 ---------- */
function JoinModal({ onClose }: { onClose: () => void }) {
  useLockScroll(true);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("内容编辑");
  const [sent, setSent] = useState(false);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const subject = encodeURIComponent(`[加入光忆] ${role}`);
    const body = encodeURIComponent(`我想加入光忆，期望职位：${role}。\n\n邮箱：${email}\n`);
    setSent(true);
    window.location.href = `mailto:${CONTACT_EMAIL}?subject=${subject}&body=${body}`;
  };

  const roles = ["内容编辑", "开发者", "真实经历分享者", "其它"];

  return (
    <ModalShell title="加入光忆" onClose={onClose}>
      {sent ? (
        <SuccessNote />
      ) : (
        <form onSubmit={submit} className="mt-4 space-y-4">
          <div>
            <label className={labelCls}>你的邮箱</label>
            <input
              type="email"
              required
              className={inputCls}
              placeholder="name@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div>
            <label className={labelCls}>期望职位</label>
            <select className={inputCls} value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="内容编辑">内容编辑</option>
              <option value="开发者">开发者</option>
              <option value="真实经历分享者">真实经历分享者</option>
              <option value="其它">其它</option>
            </select>
          </div>
          <button type="submit" className="w-full rounded-xl bg-teal-600 px-4 py-3 text-sm font-medium text-white transition hover:bg-teal-700">
            打开邮件客户端，发送申请
          </button>
        </form>
      )}
    </ModalShell>
  );
}

/* ---------- 导航动作按钮（页头 / 页脚通用） ---------- */
export function NavActions({ className = "flex items-center gap-6" }: { className?: string }) {
  const [modal, setModal] = useState<Modal>("none");
  const btnCls = "text-sm font-medium text-slate-600 transition hover:text-teal-700";

  return (
    <>
      <div className={className}>
        <button onClick={() => setModal("contribute")} className={btnCls}>
          参与编辑
        </button>
        <button onClick={() => setModal("join")} className={btnCls}>
          加入光忆
        </button>
      </div>
      {modal === "contribute" && <ContributeModal onClose={() => setModal("none")} />}
      {modal === "join" && <JoinModal onClose={() => setModal("none")} />}
    </>
  );
}
