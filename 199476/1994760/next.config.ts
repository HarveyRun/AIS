import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 纯内容型站点：构建为纯静态站点，可直接部署到任意静态托管
  output: "export",
  trailingSlash: true,
  images: { unoptimized: true },
};

export default nextConfig;
