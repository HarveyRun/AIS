import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { NoticeBox } from "@/components/notice";
import { getFaqs, getSiteMeta, listCountries } from "@/lib/content";
import { FaqClient, type FaqItemLite } from "./faq-client";

export const metadata: Metadata = {
  title: "常见问题",
  description: "按国家筛选的移民常见问题：枫叶卡/绿卡区别、英语要求、费用、雇主、排期等高频疑问的大白话解答。",
};

export default function FaqPage() {
  const faqs: FaqItemLite[] = getFaqs().map((f) => ({
    country: f.country,
    question: f.question,
    answer: f.answer,
  }));
  const countries = listCountries().map((c) => ({
    id: c.id,
    name: c.name,
    flag: c.flag,
  }));
  const site = getSiteMeta();

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
