import { describe, expect, it } from "vitest";
import { legacyPageName, resolveAppTarget } from "./navigation";

describe("navigation compatibility", () => {
  it("把旧 HTML 地址转换成对应的 SPA 路由", () => {
    expect(resolveAppTarget("center.html#packages")).toEqual({
      destination: "/center/packages",
    });
    expect(resolveAppTarget("ideas.html")).toEqual({ destination: "/ideas" });
    expect(resolveAppTarget("index.html?mode=iteration&parent=idea-1")).toEqual(
      {
        destination: "/",
        homeSearch: { mode: "iteration", resume: undefined, parent: "idea-1" },
      },
    );
  });

  it("拒绝站外或未知回跳目标", () => {
    expect(resolveAppTarget("https://example.com/account")).toEqual({
      destination: "/center/ideas",
    });
    expect(resolveAppTarget("/unknown")).toEqual({
      destination: "/center/ideas",
    });
  });

  it("反馈页面来源继续使用旧数据标签", () => {
    expect(legacyPageName("/")).toBe("index.html");
    expect(legacyPageName("/center/packages")).toBe("center.html");
    expect(legacyPageName("/ideas")).toBe("ideas.html");
  });
});
