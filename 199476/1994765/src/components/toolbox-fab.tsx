"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ScoreCalculatorModal } from "./score-calculators";

type Modal = "none" | "consultant" | "calculator" | "app";

/* 光忆 App 下载链接（配置后自动显示下载按钮） */
const GUANGYI_URL = "";

function Icon({ name, className = "h-5 w-5" }: { name: string; className?: string }) {
  const paths: Record<string, React.ReactNode> = {
    pickaxe: (
      <>
        <path d="M4 20.5 15 7" />
        <path d="M7 4q8.5-1.5 13 11.5" />
      </>
    ),
    close: (
      <>
        <path d="M18 6 6 18" />
        <path d="m6 6 12 12" />
      </>
    ),
  };
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      {paths[name]}
    </svg>
  );
}

const menuItems = [
  { key: "basics", icon: "📚", title: "移民科普", desc: "从 0 看懂移民常识", href: "/basics/" },
  { key: "consultant", icon: "🛡️", title: "持牌顾问名录", desc: "各国官方持牌查询入口", modal: "consultant" as Modal },
  { key: "calculator", icon: "🧮", title: "打分计算器", desc: "加 / 澳 / 新 / 德 简化估算", modal: "calculator" as Modal },
  { key: "app", icon: "📖", title: "真实案例库", desc: "光忆 App · 移民经历", modal: "app" as Modal },
];

/* ---------- 持牌顾问名录 + 移民官方网站 弹窗 ---------- */
function ConsultantModal({ onClose }: { onClose: () => void }) {
  const [tab, setTab] = useState<"agents" | "official">("agents");

  const agents = [
    {
      flag: "🇨🇦",
      country: "加拿大",
      who: "移民顾问须持 CICC（加拿大移民与公民顾问学院）注册的 RCIC 牌照；律师由各省律师协会监管。",
      url: "https://register.college-ic.ca/",
      link: "CICC 注册查询",
    },
    {
      flag: "🇦🇺",
      country: "澳大利亚",
      who: "移民代理必须在 OMARA（移民代理注册管理局）注册，拥有可查的 MARA 注册号。",
      url: "https://www.mara.gov.au/",
      link: "MARA 注册查询",
    },
    {
      flag: "🇺🇸",
      country: "美国",
      who: "移民业务由执业律师或司法部认证代表处理，可通过美国移民律师协会（AILA）查找律师。",
      url: "https://www.ailalawyer.com/",
      link: "AILA 律师查找",
    },
    {
      flag: "🇳🇿",
      country: "新西兰",
      who: "移民顾问须在 IAA（移民顾问管理局）注册持牌（持牌移民顾问 LICENSED IMMIGRATION ADVISER）。",
      url: "https://www.iaa.govt.nz/",
      link: "IAA 注册查询",
    },
    {
      flag: "🇩🇪",
      country: "德国",
      who: "无强制持牌移民顾问体系，建议委托执业律师（Rechtsanwalt），可通过联邦律师协会查询执业资格。",
      url: "https://www.bundesrechtsanwaltskammer.de/",
      link: "联邦律师协会查询",
    },
    {
      flag: "🇪🇸",
      country: "西班牙",
      who: "建议委托执业律师或正规 gestor（事务代理），通过各地律师协会（Colegio de Abogados）查询执业资格。",
      url: "https://www.abogacia.es/",
      link: "律师协会查询",
    },
  ];

  const official = [
    { flag: "🇨🇦", country: "加拿大", who: "移民、难民与公民部（IRCC）：所有申请、进度查询与政策发布。", url: "https://www.canada.ca/en/immigration-refugees-citizenship.html", link: "canada.ca" },
    { flag: "🇦🇺", country: "澳大利亚", who: "内政部（Department of Home Affairs）：签证申请与移民政策官方入口。", url: "https://immi.homeaffairs.gov.au/", link: "immi.homeaffairs.gov.au" },
    { flag: "🇺🇸", country: "美国", who: "美国公民及移民服务局（USCIS）：绿卡、职业移民与表格官方渠道。", url: "https://www.uscis.gov/", link: "uscis.gov" },
    { flag: "🇳🇿", country: "新西兰", who: "新西兰移民局（Immigration New Zealand）：居留、工签与绿名单政策。", url: "https://www.immigration.govt.nz/", link: "immigration.govt.nz" },
    { flag: "🇩🇪", country: "德国", who: "联邦政府官方移民门户 Make it in Germany：技术移民法全流程指引。", url: "https://www.make-it-in-germany.com/", link: "make-it-in-germany.com" },
    { flag: "🇪🇸", country: "西班牙", who: "外来人口服务门户：居留申请、续签与各类别政策细则。", url: "https://extranjeros.inclusion.gob.es/", link: "extranjeros.inclusion.gob.es" },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="flex max-h-[85vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 标题区固定 */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <h2 className="text-xl font-bold text-slate-900">持牌顾问与官方网站</h2>
          <button onClick={onClose} className="rounded-full p-1 text-slate-400 hover:text-slate-700" aria-label="关闭">
            <Icon name="close" className="h-5 w-5" />
          </button>
        </div>

        {/* 分类 tab 固定 */}
        <div className="flex gap-2 border-b border-slate-100 px-6 py-3">
          <button
            onClick={() => setTab("agents")}
            className={`rounded-full px-4 py-2 text-sm font-medium transition ${
              tab === "agents" ? "bg-teal-600 text-white" : "bg-white text-slate-600 ring-1 ring-inset ring-slate-200 hover:bg-slate-100"
            }`}
          >
            🛡️ 持牌顾问名录
          </button>
          <button
            onClick={() => setTab("official")}
            className={`rounded-full px-4 py-2 text-sm font-medium transition ${
              tab === "official" ? "bg-teal-600 text-white" : "bg-white text-slate-600 ring-1 ring-inset ring-slate-200 hover:bg-slate-100"
            }`}
          >
            🏛️ 移民官方网站
          </button>
        </div>

        {/* 内容区滚动 */}
        <div className="no-scrollbar flex-1 overflow-y-auto px-6 py-4">
          {tab === "agents" && (
            <>
              <p className="text-sm leading-6 text-slate-600">
                移民顾问在多数国家是持牌监管行业。付费咨询前，务必在官方入口核实对方的注册号——本站不推荐、不背书任何顾问或机构。
              </p>
              <div className="mt-4 space-y-3">
                {agents.map((r) => (
                  <div key={r.country} className="rounded-xl border border-slate-200 p-4">
                    <div className="flex items-center gap-2 font-bold text-slate-900">
                      <span>{r.flag}</span>
                      {r.country}
                    </div>
                    <p className="mt-1.5 text-sm leading-6 text-slate-600">{r.who}</p>
                    <a href={r.url} target="_blank" rel="noopener noreferrer" className="mt-2 inline-block text-sm font-medium text-teal-700 hover:text-teal-800">
                      {r.link} ↗
                    </a>
                  </div>
                ))}
              </div>
              <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-900">
                <strong>防骗提示：</strong>任何「保过」「内部渠道」「不成功不收费却先收大额押金」的话术都是违规信号。付费前核实注册号、签订书面合同、尽量分期付款。
              </div>
            </>
          )}

          {tab === "official" && (
            <>
              <p className="text-sm leading-6 text-slate-600">
                一切申请条件、费用与流程，都以各国官方网站的当期信息为准。遇到与官网不一致的说法，一律以官网为准。
              </p>
              <div className="mt-4 space-y-3">
                {official.map((r) => (
                  <div key={r.country} className="rounded-xl border border-slate-200 p-4">
                    <div className="flex items-center gap-2 font-bold text-slate-900">
                      <span>{r.flag}</span>
                      {r.country}
                    </div>
                    <p className="mt-1.5 text-sm leading-6 text-slate-600">{r.who}</p>
                    <a href={r.url} target="_blank" rel="noopener noreferrer" className="mt-2 inline-block text-sm font-medium text-teal-700 hover:text-teal-800">
                      {r.link} ↗
                    </a>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

/* ---------- 真实案例库（光忆 App）弹窗 ---------- */
function AppModal({ onClose }: { onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="no-scrollbar max-h-[85vh] w-full max-w-md overflow-y-auto rounded-2xl bg-white p-6 text-center shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <img
          src="/icons/guangyi.png"
          alt="光忆 App"
          className="mx-auto h-20 w-20 rounded-2xl object-cover shadow-lg ring-1 ring-black/5"
        />
        <p className="mt-5 text-sm font-medium text-teal-700">案例库 App</p>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          移民经历、路线复盘与避坑故事
        </p>
        <div className="mt-5 rounded-xl bg-slate-50 px-4 py-3.5 text-sm leading-6 text-slate-600">
          请在 <b>App Store</b> 或各<b>安卓应用商店</b>搜索
          <span className="mx-1 font-semibold text-teal-700">「光忆」</span>
          下载体验
        </div>
        {GUANGYI_URL && (
          <a
            href={GUANGYI_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-2 block rounded-xl bg-teal-600 px-4 py-3 text-sm font-medium text-white transition hover:bg-teal-700"
          >
            前往下载「光忆」↗
          </a>
        )}
      </div>
    </div>
  );
}

/* ---------- 悬浮工具箱 ---------- */
export function ToolboxFab() {
  const [open, setOpen] = useState(false);
  const [modal, setModal] = useState<Modal>("none");

  /* 弹窗打开时锁定主站滚动 */
  useEffect(() => {
    if (modal !== "none") {
      const prev = document.body.style.overflow;
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = prev;
      };
    }
  }, [modal]);

  return (
    <>
      {/* 悬浮按钮 */}
      <div className="fixed bottom-6 right-5 z-40 flex flex-col items-end gap-3">
        {open && (
          <div className="w-64 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
            <div className="border-b border-slate-100 px-4 py-2.5 text-xs font-semibold text-slate-400">
              移民工具箱
            </div>
            {menuItems.map((item) =>
              item.href ? (
                <Link
                  key={item.key}
                  href={item.href}
                  onClick={() => setOpen(false)}
                  className="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-slate-50"
                >
                  <span className="text-xl">{item.icon}</span>
                  <span className="flex-1">
                    <span className="block text-sm font-semibold text-slate-900">{item.title}</span>
                    <span className="block text-xs text-slate-400">{item.desc}</span>
                  </span>
                </Link>
              ) : (
                <button
                  key={item.key}
                  onClick={() => {
                    setModal(item.modal ?? "none");
                    setOpen(false);
                  }}
                  className="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-slate-50"
                >
                  <span className="text-xl">{item.icon}</span>
                  <span className="flex-1">
                    <span className="block text-sm font-semibold text-slate-900">{item.title}</span>
                    <span className="block text-xs text-slate-400">{item.desc}</span>
                  </span>
                </button>
              )
            )}
          </div>
        )}
        <button
          onClick={() => setOpen(!open)}
          aria-label="移民工具箱"
          className={`flex h-14 w-14 items-center justify-center rounded-full text-white shadow-lg transition ${
            open
              ? "bg-gradient-to-br from-teal-700 to-teal-900"
              : "bg-gradient-to-br from-teal-500 to-teal-700 hover:shadow-xl hover:shadow-teal-600/40"
          }`}
        >
          <Icon name={open ? "close" : "pickaxe"} className="h-6 w-6" />
        </button>
      </div>

      {/* 弹窗 */}
      {modal === "consultant" && <ConsultantModal onClose={() => setModal("none")} />}
      {modal === "calculator" && <ScoreCalculatorModal onClose={() => setModal("none")} />}
      {modal === "app" && <AppModal onClose={() => setModal("none")} />}
    </>
  );
}
