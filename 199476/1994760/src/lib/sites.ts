export interface SiteEntry {
  title: string;
  desc: string;
  url: string;
  preview: string;
  status: "online" | "wip";
}

/* TODO: 移民指南正式部署后，替换为线上地址 */
const IMMIGRATION_URL = "https://a.inlightus.com/immigration/";

export const sites: SiteEntry[] = [
  {
    title: "移民指南",
    desc: "写给普通人的移民方式、流程与费用科普指南",
    url: IMMIGRATION_URL,
    preview: "/previews/immigration.png",
    status: "online",
  },
];
