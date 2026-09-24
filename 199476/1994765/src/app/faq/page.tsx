import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { getFaqs, listCountries } from "@/lib/content";
import { FaqClient, type FaqItemLite } from "./faq-client";

export const metadata: Metadata = {
  title: "常见问题",
  description:
    "移民常见问题大白话解答：常识扫盲、路径选择、项目细节三级分类，覆盖不会英语能移民吗、花多少钱、年龄限制、签证区别、排期等高频疑问。",
};

export default function FaqPage() {
  const faqs: FaqItemLite[] = getFaqs().map((f) => ({
    country: f.country,
    category: f.category,
    question: f.question,
    answer: f.answer,
  }));
  const countries = listCountries()
    // FAQ 筛选标签只显示已收录问题的国家（新国家暂无 FAQ 时不出现空标签）
    .filter((c) => faqs.some((f) => f.country === c.id))
    .map((c) => ({
      id: c.id,
      name: c.name,
      flag: c.flag,
    }));

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <Breadcrumbs items={[{ label: "常见问题" }]} />
      <h1 className="mt-6 text-3xl font-bold text-slate-900">常见问题</h1>
      <div className="mt-6">
        <FaqClient faqs={faqs} countries={countries} />
      </div>
    </div>
  );
}
