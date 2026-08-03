import { useNavigate } from "@tanstack/react-router";
import { useCallback } from "react";

export type CenterTabPath =
  | "ideas"
  | "balance"
  | "packages"
  | "notifications"
  | "team"
  | "settings"
  | "admin";
export type AppDestination =
  | "/"
  | "/login"
  | "/register"
  | "/ideas"
  | "/cases"
  | "/rules"
  | `/center/${CenterTabPath}`;

const legacyTargets: Record<string, AppDestination> = {
  "index.html": "/",
  "ideas.html": "/ideas",
  "cases.html": "/cases",
  "rules.html": "/rules",
  "login.html": "/login",
  "center.html": "/center/ideas",
};

export interface ResolvedAppTarget {
  destination: AppDestination;
  homeSearch?: { mode?: "iteration"; resume?: "idea"; parent?: string };
}

export function resolveAppTarget(
  target: string | null | undefined,
): ResolvedAppTarget {
  if (!target) return { destination: "/center/ideas" };
  const url = new URL(target, window.location.origin);
  const legacyName = url.pathname.split("/").pop() || "";
  if (legacyTargets[legacyName]) {
    if (legacyName === "center.html") {
      const tab = url.hash.slice(1) as CenterTabPath;
      return {
        destination: `/center/${["ideas", "balance", "packages", "notifications", "team", "settings", "admin"].includes(tab) ? tab : "ideas"}`,
      };
    }
    if (legacyName === "index.html")
      return {
        destination: "/",
        homeSearch: {
          mode:
            url.searchParams.get("mode") === "iteration"
              ? "iteration"
              : undefined,
          resume:
            url.searchParams.get("resume") === "idea" ? "idea" : undefined,
          parent: url.searchParams.get("parent") || undefined,
        },
      };
    return { destination: legacyTargets[legacyName] };
  }
  if (
    /^\/center\/(ideas|balance|packages|notifications|team|settings|admin)$/.test(
      url.pathname,
    )
  )
    return { destination: url.pathname as AppDestination };
  if (url.pathname === "/")
    return {
      destination: "/",
      homeSearch: {
        mode:
          url.searchParams.get("mode") === "iteration"
            ? "iteration"
            : undefined,
        resume: url.searchParams.get("resume") === "idea" ? "idea" : undefined,
        parent: url.searchParams.get("parent") || undefined,
      },
    };
  if (
    ["/login", "/register", "/ideas", "/cases", "/rules"].includes(url.pathname)
  )
    return { destination: url.pathname as AppDestination };
  return { destination: "/center/ideas" };
}

export function normalizeAppDestination(
  target: string | null | undefined,
): AppDestination {
  return resolveAppTarget(target).destination;
}

export function legacyPageName(pathname = window.location.pathname): string {
  if (pathname === "/") return "index.html";
  if (pathname.startsWith("/center")) return "center.html";
  const route = pathname.slice(1);
  return ["login", "register", "ideas", "cases", "rules"].includes(route)
    ? `${route}.html`
    : "index.html";
}

export function useAppNavigate() {
  const navigate = useNavigate();
  return useCallback(
    async (
      destination: AppDestination,
      options?: {
        replace?: boolean;
        redirect?: string;
        homeSearch?: { mode?: "iteration"; resume?: "idea"; parent?: string };
      },
    ) => {
      const replace = options?.replace;
      if (destination.startsWith("/center/")) {
        const tab = destination.split("/").at(-1) as CenterTabPath;
        await navigate({ to: "/center/$tab", params: { tab }, replace });
        return;
      }
      if (destination === "/login") {
        await navigate({
          to: "/login",
          search: { redirect: options?.redirect },
          replace,
        });
        return;
      }
      if (destination === "/register") {
        await navigate({
          to: "/register",
          search: { redirect: options?.redirect },
          replace,
        });
        return;
      }
      if (destination === "/") {
        await navigate({ to: "/", search: options?.homeSearch || {}, replace });
        return;
      }
      if (destination === "/ideas") await navigate({ to: "/ideas", replace });
      if (destination === "/cases") await navigate({ to: "/cases", replace });
      if (destination === "/rules") await navigate({ to: "/rules", replace });
    },
    [navigate],
  );
}
