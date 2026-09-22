"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  assessmentQuestionsMeta,
  rankPrograms,
  type AssessmentAnswers,
} from "@/lib/assessment";
import { immigrationTypeLabels } from "@/lib/schema";
import { CountryFlag } from "@/components/country-flag";

export interface AssessmentProgramInfo {
  slug: string;
  name: string;
  /** 打分用 id */
  country: string;
  type: string;
  /** 展示用 */
  countryName: string;
  countryFlag: string;
  regionName?: string;
  typeLabel: string;
  difficulty: "low" | "mid" | "high";
  eligibility?: import("@/lib/schema").Eligibility;
}

interface AssessmentClientProps {
  countries: { id: string; name: string; flag: string }[];
  programs: AssessmentProgramInfo[];
}

const STEPS = ["意向国家", "学历语言", "年龄经验", "预算资源"];

function Choice<T extends string | number | boolean>({
  options,
  value,
  onChange,
  columns = 2,
}: {
  options: { value: T; label: string; hint?: string }[];
  value: T | null;
  onChange: (v: T) => void;
  columns?: number;
}) {
  return (
    <div className={`grid gap-2`} style={{ gridTemplateColumns: `repeat(${columns}, minmax(0,1fr))` }}>
      {options.map((opt) => (
        <button
          key={String(opt.value)}
          type="button"
          onClick={() => onChange(opt.value)}
          className={`rounded-xl border p-3 text-left transition ${
            value === opt.value
              ? "border-teal-500 bg-teal-50 ring-1 ring-teal-500"
              : "border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50"
          }`}
        >
          <span className={`block text-sm font-medium ${value === opt.value ? "text-teal-800" : "text-slate-800"}`}>
            {opt.label}
          </span>
          {opt.hint && <span className="mt-0.5 block text-xs text-slate-400">{opt.hint}</span>}
        </button>
      ))}
    </div>
  );
}

export function AssessmentClient({ countries, programs }: AssessmentClientProps) {
  const [step, setStep] = useState(0);
  const [done, setDone] = useState(false);

  const [targets, setTargets] = useState<string[]>([]);
  const [education, setEducation] = useState<AssessmentAnswers["education"] | null>(null);
  const [language, setLanguage] = useState<AssessmentAnswers["language"] | null>(null);
  const [age, setAge] = useState<number | null>(null);
  const [workYears, setWorkYears] = useState<AssessmentAnswers["workYears"] | null>(null);
  const [budgetTier, setBudgetTier] = useState<AssessmentAnswers["budgetTier"] | null>(null);
  const [hasJobOffer, setHasJobOffer] = useState<boolean | null>(null);
  const [hasFamilyAbroad, setHasFamilyAbroad] = useState<boolean | null>(null);

  const stepReady = [
    targets.length > 0,
    education != null && language != null,
    age != null && workYears != null,
    budgetTier != null && hasJobOffer != null && hasFamilyAbroad != null,
  ][step];

  const answers: AssessmentAnswers = {
    targetCountries: targets,
    education: education ?? 3,
    language: language ?? 0,
    budgetTier: budgetTier ?? "low",
    workYears: workYears ?? 0,
    age: age,
    hasJobOffer: hasJobOffer ?? false,
    hasFamilyAbroad: hasFamilyAbroad ?? false,
  };

  const results = useMemo(() => {
    if (!done) return [];
    return rankPrograms(programs, answers, 5);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [done, programs]);

  const toggleTarget = (id: string) =>
    setTargets((prev) =>
      prev.includes(id)
        ? prev.filter((x) => x !== id)
        : id === "any"
          ? ["any"]
          : [...prev.filter((x) => x !== "any"), id]
    );

  const reset = () => {
    setDone(false);
    setStep(0);
    setTargets([]);
    setEducation(null);
    setLanguage(null);
    setAge(null);
    setWorkYears(null);
    setBudgetTier(null);
    setHasJobOffer(null);
    setHasFamilyAbroad(null);
  };

  /* ---------- 结果页 ---------- */
  if (done) {
    return (
      <div>
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold text-slate-900">为你匹配到 {results.length} 条路径</h2>
          <button
            onClick={reset}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-50"
          >
            重新评估
          </button>
        </div>

        {results.length === 0 && (
          <div className="mt-6 rounded-2xl border border-dashed border-slate-300 p-10 text-center text-slate-500">
            没有匹配到路径，请调整答案后重试。
          </div>
        )}

        <div className="mt-6 space-y-4">
          {results.map((match, idx) => {
            const p = programs.find((x) => x.slug === match.slug);
            if (!p) return null;
            return (
              <div key={match.slug} className="rounded-2xl border border-slate-200 bg-white p-5">
                <div className="flex flex-wrap items-center gap-3">
                  <span
                    aria-hidden
                    className={`grid h-10 w-10 shrink-0 place-items-center rounded-full text-sm font-bold text-white ${
                      idx === 0 ? "bg-teal-600" : idx === 1 ? "bg-teal-500" : "bg-slate-400"
                    }`}
                  >
                    {match.score}分
                  </span>
                  <div className="min-w-0 flex-1">
                    <h3 className="flex items-center gap-1.5 font-bold text-slate-900">
                      <CountryFlag
                        countryId={p.country}
                        flag={p.countryFlag}
                        className="h-3 w-4"
                      />
                      {p.name}
                    </h3>
                    <p className="text-xs text-slate-400">
                      {p.countryName}
                      {p.regionName ? ` · ${p.regionName}` : ""} · {p.typeLabel}
                    </p>
                  </div>
                  <Link
                    href={`/programs/${p.slug}/`}
                    className="rounded-lg bg-teal-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-teal-700"
                  >
                    查看流程
                  </Link>
                </div>

                <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-slate-100">
                  <div
                    className={`h-full rounded-full ${match.score >= 70 ? "bg-teal-500" : match.score >= 50 ? "bg-amber-400" : "bg-rose-400"}`}
                    style={{ width: `${match.score}%` }}
                  />
                </div>

                {match.matched.length > 0 && (
                  <ul className="mt-3 flex flex-wrap gap-1.5">
                    {match.matched.map((m) => (
                      <li
                        key={m}
                        className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs text-emerald-700 ring-1 ring-inset ring-emerald-600/20"
                      >
                        ✓ {m}
                      </li>
                    ))}
                  </ul>
                )}

                {match.gaps.length > 0 && (
                  <div className="mt-3 space-y-1.5">
                    {match.gaps.map((g) => (
                      <p key={g.dimension} className="text-xs leading-relaxed text-amber-800">
                        <span className="mr-1 rounded bg-amber-100 px-1.5 py-0.5 font-medium">
                          待补强：{g.dimension}
                        </span>
                        {g.detail}
                      </p>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <p className="mt-6 text-xs leading-relaxed text-slate-400">
          匹配结果基于项目页的结构化条件与常规经验规则，仅供参考，不构成申请建议；正式申请前请核对官方要求或咨询持牌顾问。
        </p>
      </div>
    );
  }

  /* ---------- 问卷 ---------- */
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-6 md:p-8">
      {/* 进度 */}
      <div className="flex items-center gap-2">
        {STEPS.map((label, i) => (
          <div key={label} className="flex flex-1 items-center gap-2">
            <span
              className={`grid h-7 w-7 shrink-0 place-items-center rounded-full text-xs font-bold ${
                i < step
                  ? "bg-teal-100 text-teal-700"
                  : i === step
                    ? "bg-teal-600 text-white"
                    : "bg-slate-100 text-slate-400"
              }`}
            >
              {i + 1}
            </span>
            <span className={`hidden text-xs sm:block ${i === step ? "font-medium text-slate-900" : "text-slate-400"}`}>
              {label}
            </span>
            {i < STEPS.length - 1 && <span className="h-px flex-1 bg-slate-200" />}
          </div>
        ))}
      </div>

      <div className="mt-8">
        {step === 0 && (
          <>
            <h3 className="font-bold text-slate-900">你考虑哪些国家/地区？（可多选）</h3>
            <div className="mt-3 flex flex-wrap gap-2">
              {[{ id: "any", name: "还没想好", flag: "🌐" }, ...countries].map((c) => (
                <button
                  key={c.id}
                  type="button"
                  onClick={() => toggleTarget(c.id)}
                  className={`rounded-full px-4 py-2 text-sm font-medium transition ${
                    targets.includes(c.id)
                      ? "bg-teal-600 text-white"
                      : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                  }`}
                >
                  {c.flag} {c.name}
                </button>
              ))}
            </div>
          </>
        )}

        {step === 1 && (
          <>
            <h3 className="font-bold text-slate-900">最高学历</h3>
            <div className="mt-3">
              <Choice
                columns={5}
                options={assessmentQuestionsMeta.educationLevels.map((label, i) => ({
                  value: (i + 1) as AssessmentAnswers["education"],
                  label,
                }))}
                value={education}
                onChange={setEducation}
              />
            </div>
            <h3 className="mt-8 font-bold text-slate-900">英语水平（大致）</h3>
            <div className="mt-3">
              <Choice
                columns={4}
                options={assessmentQuestionsMeta.languageLevels.map((label, i) => ({
                  value: i as AssessmentAnswers["language"],
                  label,
                }))}
                value={language}
                onChange={setLanguage}
              />
            </div>
          </>
        )}

        {step === 2 && (
          <>
            <h3 className="font-bold text-slate-900">你的年龄</h3>
            <input
              type="number"
              min={16}
              max={70}
              value={age ?? ""}
              onChange={(e) => setAge(e.target.value ? Number(e.target.value) : null)}
              placeholder="如 28"
              className="mt-3 w-40 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-teal-500 focus:outline-none"
            />
            <h3 className="mt-8 font-bold text-slate-900">全职工作经验</h3>
            <div className="mt-3">
              <Choice
                columns={4}
                options={assessmentQuestionsMeta.workYearLevels.map((label, i) => ({
                  value: i as AssessmentAnswers["workYears"],
                  label,
                }))}
                value={workYears}
                onChange={setWorkYears}
              />
            </div>
          </>
        )}

        {step === 3 && (
          <>
            <h3 className="font-bold text-slate-900">可用于移民的总预算（人民币）</h3>
            <div className="mt-3">
              <Choice
                columns={3}
                options={assessmentQuestionsMeta.budgetLevels.map((label, i) => ({
                  value: ["low", "mid", "high"][i] as AssessmentAnswers["budgetTier"],
                  label,
                  hint: ["10-20 万", "含留学/雇主项目", "投资类为主"][i],
                }))}
                value={budgetTier}
                onChange={setBudgetTier}
              />
            </div>
            <h3 className="mt-8 font-bold text-slate-900">是否已拿到海外雇主的 offer？</h3>
            <div className="mt-3">
              <Choice
                columns={2}
                options={[
                  { value: true, label: "已拿到" },
                  { value: false, label: "还没有" },
                ]}
                value={hasJobOffer}
                onChange={setHasJobOffer}
              />
            </div>
            <h3 className="mt-8 font-bold text-slate-900">
              是否有直系亲属已是国外的公民或永居？
            </h3>
            <div className="mt-3">
              <Choice
                columns={2}
                options={[
                  { value: true, label: "有" },
                  { value: false, label: "没有" },
                ]}
                value={hasFamilyAbroad}
                onChange={setHasFamilyAbroad}
              />
            </div>
          </>
        )}
      </div>

      {/* 步骤控制 */}
      <div className="mt-10 flex items-center justify-between">
        <button
          onClick={() => (step === 0 ? reset() : setStep(step - 1))}
          className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50"
        >
          {step === 0 ? "重置" : "上一步"}
        </button>
        {step < STEPS.length - 1 ? (
          <button
            disabled={!stepReady}
            onClick={() => setStep(step + 1)}
            className="rounded-lg bg-teal-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-teal-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            下一步
          </button>
        ) : (
          <button
            disabled={!stepReady}
            onClick={() => setDone(true)}
            className="rounded-lg bg-teal-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-teal-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            查看匹配结果
          </button>
        )}
      </div>
    </div>
  );
}
