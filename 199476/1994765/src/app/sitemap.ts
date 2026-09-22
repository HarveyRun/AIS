import type { MetadataRoute } from "next";
import { listCountries, listPrograms, listRegions } from "@/lib/content";

export const dynamic = "force-static";

export const baseUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "https://example.com";

export default function sitemap(): MetadataRoute.Sitemap {
  const staticPages = ["", "/explore/", "/assessment/", "/glossary/", "/about/"].map(
    (p) => ({
      url: `${baseUrl}${p}`,
      lastModified: new Date(),
      changeFrequency: "weekly" as const,
      priority: p === "" ? 1 : 0.8,
    })
  );

  const countryPages = listCountries().map((c) => ({
    url: `${baseUrl}/countries/${c.id}/`,
    lastModified: new Date(),
    changeFrequency: "weekly" as const,
    priority: 0.9,
  }));

  const regionPages = listRegions().map((r) => ({
    url: `${baseUrl}/countries/${r.country}/${r.id}/`,
    lastModified: new Date(),
    changeFrequency: "weekly" as const,
    priority: 0.7,
  }));

  const programPages = listPrograms().map((p) => ({
    url: `${baseUrl}/programs/${p.slug}/`,
    lastModified: new Date(p.infoVerifiedAt),
    changeFrequency: "monthly" as const,
    priority: 0.8,
  }));

  return [...staticPages, ...countryPages, ...regionPages, ...programPages];
}
