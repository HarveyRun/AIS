"use client";

import { useState } from "react";

/* 各国打分计算器（简化估算版）：完整规则以各国官方计算器为准 */

const selectCls =
  "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-700 focus:border-teal-500 focus:outline-none";
const labelCls = "w-40 shrink-0 text-sm text-slate-600";
const scoreCls = "w-12 shrink-0 text-right text-sm font-semibold text-slate-900";

function Row({
  label,
  score,
  children,
}: {
  label: string;
  score: number | string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex items-center gap-3 py-2">
      <span className={labelCls}>{label}</span>
      <div className="flex-1">{children}</div>
      <span className={scoreCls}>{score}</span>
    </div>
  );
}

function Sel({
  value,
  onChange,
  options,
}: {
  value: string;
  onChange: (v: string) => void;
  options: [string, number][];
}) {
  return (
    <select className={selectCls} value={value} onChange={(e) => onChange(e.target.value)}>
      {options.map(([v, s]) => (
        <option key={v} value={v}>
          {v}
        </option>
      ))}
    </select>
  );
}

function Chk({
  checked,
  onChange,
  label,
}: {
  checked: boolean;
  onChange: (b: boolean) => void;
  label: string;
}) {
  return (
    <label className="flex items-center gap-2 text-sm text-slate-600">
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} className="h-4 w-4 accent-teal-600" />
      {label}
    </label>
  );
}

/* ---------- 加拿大 EE（CRS 单身简化版） ---------- */
function CanadaCalc() {
  const [age, setAge] = useState("20-29");
  const [edu, setEdu] = useState("本科");
  const [lang, setLang] = useState("CLB 7（约雅思四个6）");
  const [caExp, setCaExp] = useState("无");
  const [pnp, setPnp] = useState(false);

  const ageS: Record<string, number> = { "18-19": 99, "20-29": 110, "30-34": 99, "35-39": 88, "40-44": 66, "45-46": 22, "47+": 0 };
  const eduS: Record<string, number> = { 博士: 140, 硕士: 126, 本科: 120, 大专: 98, 高中: 30 };
  const langS: Record<string, number> = { "CLB 9+（约四个7以上）": 124, "CLB 8（约四个7）": 88, "CLB 7（约四个6）": 68, "CLB 5-6": 36, "CLB 4 及以下": 0 };
  const caS: Record<string, number> = { 无: 0, "1 年": 40, "2 年": 50, "3 年": 60, "4 年": 70, "5 年及以上": 80 };

  const total =
    (ageS[age] ?? 0) + (eduS[edu] ?? 0) + (langS[lang] ?? 0) + (caS[caExp] ?? 0) + (pnp ? 600 : 0);

  return (
    <div>
      <Row label="年龄" score={ageS[age]}>
        <Sel value={age} onChange={setAge} options={Object.keys(ageS).map((k) => [k, ageS[k]])} />
      </Row>
      <Row label="学历" score={eduS[edu]}>
        <Sel value={edu} onChange={setEdu} options={Object.keys(eduS).map((k) => [k, eduS[k]])} />
      </Row>
      <Row label="英语（CLB 档）" score={langS[lang]}>
        <Sel value={lang} onChange={setLang} options={Object.keys(langS).map((k) => [k, langS[k]])} />
      </Row>
      <Row label="加拿大工作经验" score={caS[caExp]}>
        <Sel value={caExp} onChange={setCaExp} options={Object.keys(caS).map((k) => [k, caS[k]])} />
      </Row>
      <Row label="获省提名" score={pnp ? 600 : 0}>
        <Chk checked={pnp} onChange={setPnp} label="已获省提名（+600）" />
      </Row>
      <div className="mt-4 rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-600">
        <p className="font-semibold text-slate-900">
          估算总分：<span className="text-teal-700">{total}</span>
          {pnp && <span className="ml-2 text-xs font-normal text-slate-500">（含省提名 600 分）</span>}
        </p>
        <p className="mt-1 text-xs">
          本表为单身申请人简化估算（未含海外经验转移分、法语加分等）。普通轮次近年获邀分多在 500 上下；获省提名加 600 分后基本锁定。
        </p>
      </div>
    </div>
  );
}

/* ---------- 澳洲 EOI（65 分入池制） ---------- */
function AustraliaCalc() {
  const [age, setAge] = useState("25-32");
  const [eng, setEng] = useState("四个 6");
  const [osExp, setOsExp] = useState("无");
  const [edu, setEdu] = useState("本科/硕士");
  const [state, setState] = useState("无担保");

  const ageS: Record<string, number> = { "18-24": 25, "25-32": 30, "33-39": 25, "40-44": 15, "45 岁及以上": 0 };
  const engS: Record<string, number> = { "四个 6": 0, "四个 7": 10, "四个 8": 20 };
  const expS: Record<string, number> = { 无: 0, "3-4 年": 5, "5-7 年": 10, "8 年及以上": 15 };
  const eduS: Record<string, number> = { 博士: 20, "本科/硕士": 15, "大专/职教": 10 };
  const stS: Record<string, number> = { 无担保: 0, "190 州担保（+5）": 5, "491 偏远担保（+15）": 15 };

  const total =
    (ageS[age] ?? 0) + (engS[eng] ?? 0) + (expS[osExp] ?? 0) + (eduS[edu] ?? 0) + (stS[state] ?? 0) + 10;

  return (
    <div>
      <Row label="年龄" score={ageS[age]}>
        <Sel value={age} onChange={setAge} options={Object.keys(ageS).map((k) => [k, ageS[k]])} />
      </Row>
      <Row label="英语" score={engS[eng]}>
        <Sel value={eng} onChange={setEng} options={Object.keys(engS).map((k) => [k, engS[k]])} />
      </Row>
      <Row label="海外工作经验" score={expS[osExp]}>
        <Sel value={osExp} onChange={setOsExp} options={Object.keys(expS).map((k) => [k, expS[k]])} />
      </Row>
      <Row label="学历" score={eduS[edu]}>
        <Sel value={edu} onChange={setEdu} options={Object.keys(eduS).map((k) => [k, eduS[k]])} />
      </Row>
      <Row label="州担保" score={stS[state]}>
        <Sel value={state} onChange={setState} options={Object.keys(stS).map((k) => [k, stS[k]])} />
      </Row>
      <Row label="单身申请人" score={10}>
        <span className="text-xs text-slate-400">固定加分</span>
      </Row>
      <div className="mt-4 rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-600">
        <p className="font-semibold text-slate-900">
          估算总分：<span className="text-teal-700">{total}</span>
          <span className="ml-2 text-xs font-normal text-slate-500">（入池线 65 分）</span>
        </p>
        <p className="mt-1 text-xs">
          简化估算：已计入单身 10 分，未含澳洲本地学历/经验、配偶加分等。65 分仅是入池线，热门职业实际获邀普遍 85-95 分。
        </p>
      </div>
    </div>
  );
}

/* ---------- 新西兰六分制 ---------- */
function NZCalc() {
  const [edu, setEdu] = useState("无");
  const [reg, setReg] = useState("无");
  const [inc, setInc] = useState("无");
  const [exp, setExp] = useState("无");

  const eduS: Record<string, number> = { 无: 0, 本科: 3, 硕士: 5, 博士: 6 };
  const regS: Record<string, number> = { 无: 0, "注册 2-3 年": 2, "注册 4-5 年": 4, "注册 6 年以上": 6 };
  const incS: Record<string, number> = { 无: 0, "中位数 1.5 倍": 2, "2 倍": 3, "2.5 倍": 4, "3 倍": 5, "4 倍": 6 };
  const expS: Record<string, number> = { 无: 0, "1 年": 1, "2 年": 2, "3 年及以上": 3 };

  const base = Math.max(eduS[edu] ?? 0, regS[reg] ?? 0, incS[inc] ?? 0);
  const total = Math.min(6, base + (expS[exp] ?? 0));

  return (
    <div>
      <p className="mb-2 rounded-lg bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-800">
        规则：学历、职业注册、收入三条路线「取最高的一条」为基本分，新西兰本地技术工作经验每年加 1 分，凑满 6 分即达标。
      </p>
      <Row label="学历" score={eduS[edu]}>
        <Sel value={edu} onChange={setEdu} options={Object.keys(eduS).map((k) => [k, eduS[k]])} />
      </Row>
      <Row label="职业注册" score={regS[reg]}>
        <Sel value={reg} onChange={setReg} options={Object.keys(regS).map((k) => [k, regS[k]])} />
      </Row>
      <Row label="收入（对中位数倍数）" score={incS[inc]}>
        <Sel value={inc} onChange={setInc} options={Object.keys(incS).map((k) => [k, incS[k]])} />
      </Row>
      <Row label="本地技术工作经验" score={expS[exp]}>
        <Sel value={exp} onChange={setExp} options={Object.keys(expS).map((k) => [k, expS[k]])} />
      </Row>
      <div className="mt-4 rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-600">
        <p className="font-semibold text-slate-900">
          估算总分：<span className="text-teal-700">{total}</span> / 6
          {total >= 6 ? (
            <span className="ml-2 text-xs text-emerald-600">达标 ✓</span>
          ) : (
            <span className="ml-2 text-xs text-slate-500">还差 {6 - total} 分</span>
          )}
        </p>
        <p className="mt-1 text-xs">前提：持有认证雇主的全职 offer。语言要求雅思总分 6.5 档。</p>
      </div>
    </div>
  );
}

/* ---------- 德国机会卡 ---------- */
function GermanyCalc() {
  const [qual, setQual] = useState(false);
  const [de, setDe] = useState("无");
  const [en, setEn] = useState(false);
  const [exp, setExp] = useState(false);
  const [age, setAge] = useState("35 岁以下");
  const [lived, setLived] = useState(false);
  const [spouse, setSpouse] = useState(false);

  const deS: Record<string, number> = { 无: 0, A2: 1, "B1 及以上": 2 };
  const ageS: Record<string, number> = { "35 岁以下": 2, "35-39 岁": 1, "40 岁及以上": 0 };

  const total =
    (qual ? 4 : 0) + (deS[de] ?? 0) + (en ? 1 : 0) + (exp ? 2 : 0) + (ageS[age] ?? 0) + (lived ? 1 : 0) + (spouse ? 1 : 0);

  return (
    <div>
      <p className="mb-2 rounded-lg bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-800">
        规则：积分满 6 分 + 学历或职业资格被德国认可 + 生活资金证明（约 1.3 万欧元/年），即可获得一年求职签。
      </p>
      <Row label="资格被德国认可" score={qual ? 4 : 0}>
        <Chk checked={qual} onChange={setQual} label="学位或职业资格已获认可（4 分）" />
      </Row>
      <Row label="德语水平" score={deS[de]}>
        <Sel value={de} onChange={setDe} options={Object.keys(deS).map((k) => [k, deS[k]])} />
      </Row>
      <Row label="英语 C1" score={en ? 1 : 0}>
        <Chk checked={en} onChange={setEn} label="英语达到 C1（+1 分）" />
      </Row>
      <Row label="相关经验" score={exp ? 2 : 0}>
        <Chk checked={exp} onChange={setExp} label="近 5 年内 2 年相关经验（+2 分）" />
      </Row>
      <Row label="年龄" score={ageS[age]}>
        <Sel value={age} onChange={setAge} options={Object.keys(ageS).map((k) => [k, ageS[k]])} />
      </Row>
      <Row label="德国经历" score={lived ? 1 : 0}>
        <Chk checked={lived} onChange={setLived} label="曾在德国居住 6 个月以上（+1 分）" />
      </Row>
      <Row label="配偶同申" score={spouse ? 1 : 0}>
        <Chk checked={spouse} onChange={setSpouse} label="配偶也符合机会卡条件（+1 分）" />
      </Row>
      <div className="mt-4 rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-600">
        <p className="font-semibold text-slate-900">
          估算总分：<span className="text-teal-700">{total}</span> / 6
          {total >= 6 ? (
            <span className="ml-2 text-xs text-emerald-600">达标 ✓</span>
          ) : (
            <span className="ml-2 text-xs text-slate-500">还差 {6 - total} 分</span>
          )}
        </p>
        <p className="mt-1 text-xs">资格未被认可时无法达标——先走职业资格对比认证（Anerkennung）。</p>
      </div>
    </div>
  );
}

/* ---------- 弹窗主体 ---------- */
const tabs = [
  { id: "ca", name: "🇨🇦 加拿大 EE" },
  { id: "au", name: "🇦🇺 澳洲 EOI" },
  { id: "nz", name: "🇳🇿 新西兰六分制" },
  { id: "de", name: "🇩🇪 德国机会卡" },
] as const;

export function ScoreCalculatorModal({ onClose }: { onClose: () => void }) {
  const [tab, setTab] = useState<(typeof tabs)[number]["id"]>("ca");
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="flex max-h-[85vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 标题区固定 */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <h2 className="text-xl font-bold text-slate-900">各国打分计算器</h2>
          <button onClick={onClose} className="rounded-full p-1 text-slate-400 hover:text-slate-700" aria-label="关闭">
            ×
          </button>
        </div>

        {/* 内容区滚动 */}
        <div className="no-scrollbar flex-1 overflow-y-auto px-6 py-4">
          <div className="flex flex-wrap gap-2">
            {tabs.map((t) => (
              <button
                key={t.id}
                onClick={() => setTab(t.id)}
                className={`rounded-full px-4 py-2 text-sm font-medium transition ${
                  tab === t.id ? "bg-teal-600 text-white" : "bg-white text-slate-600 ring-1 ring-inset ring-slate-200 hover:bg-slate-100"
                }`}
              >
                {t.name}
              </button>
            ))}
          </div>
          <div className="mt-5">
            {tab === "ca" && <CanadaCalc />}
            {tab === "au" && <AustraliaCalc />}
            {tab === "nz" && <NZCalc />}
            {tab === "de" && <GermanyCalc />}
          </div>
          <p className="mt-5 border-t border-slate-100 pt-3 text-xs leading-5 text-slate-400">
            以上均为简化估算模型，仅用于快速自查方向；正式分数请以各国官方计算器与当期政策为准。
          </p>
        </div>
      </div>
    </div>
  );
}
