import type { Metadata } from "next";
import { Breadcrumbs } from "@/components/breadcrumbs";
import { listCountries, listPrograms, listRegions } from "@/lib/content";
import { toCardData } from "@/lib/display";
import { immigrationTypeLabels } from "@/lib/schema";
import { ExploreClient } from "./explore-client";

export const metadata: Metadata = {
  title: "浏览移民项目",
  description: "按国家、省/州、移民方式与预算筛选对比全部移民项目：费用、周期、门槛一目了然。",
};

export default function ExplorePage() {
  const cards = listPrograms().map(toCardData);
  const countries = listCountries().map((c) => ({
    id: c.id,
    name: c.name,
    flag: c.flag,
  }));
  const regions = listRegions().map((r) => ({
    countryId: r.country,
    id: r.id,
    name: r.name,
  }));
  const types = Object.entries(immigrationTypeLabels).map(([id, label]) => ({
    id,
    label,
  }));

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <Breadcrumbs items={[{ label: "浏览项目" }]} />
      <h1 className="mt-6 text-3xl font-bold text-slate-900">浏览移民项目</h1>
      <p className="mt-2 text-slate-600">
        按国家、省/州、移民方式与预算筛选；总费用与周期为结构化估算，详情页有完整明细。
      </p>
      <div className="mt-6">
        <ExploreClient
          cards={cards}
          countries={countries}
          regions={regions}
          types={types}
        />
      </div>
    </div>
  );
}
