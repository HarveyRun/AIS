import {
  Outlet,
  RouterProvider,
  Link,
  createRootRoute,
  createRoute,
  createRouter,
  redirect,
} from "@tanstack/react-router";
import type { CenterTabPath } from "./application/navigation";
import { appApi } from "./api/client";
import { sessionRepository } from "./infrastructure/sessionRepository";
import { CasesPage } from "./pages/CasesPage";
import { CenterPage } from "./pages/CenterPage";
import { HomePage } from "./pages/HomePage";
import { IdeasPage } from "./pages/IdeasPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { RulesPage } from "./pages/RulesPage";

interface AuthSearch {
  redirect?: string;
}
interface HomeSearch {
  mode?: "iteration";
  resume?: "idea";
  parent?: string;
}

const parseAuthSearch = (search: Record<string, unknown>): AuthSearch => ({
  redirect: typeof search.redirect === "string" ? search.redirect : undefined,
});

const rootRoute = createRootRoute({
  component: () => <Outlet />,
  notFoundComponent: () => (
    <main style={{ padding: 40, fontFamily: "sans-serif" }}>
      <h1>页面不存在</h1>
      <Link to="/">返回首页</Link>
    </main>
  ),
});

const homeRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/",
  validateSearch: (search: Record<string, unknown>): HomeSearch => ({
    mode: search.mode === "iteration" ? "iteration" : undefined,
    resume: search.resume === "idea" ? "idea" : undefined,
    parent: typeof search.parent === "string" ? search.parent : undefined,
  }),
  component: () => <HomePage search={homeRoute.useSearch()} />,
});

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/login",
  validateSearch: parseAuthSearch,
  component: () => <LoginPage redirectTo={loginRoute.useSearch().redirect} />,
});

const registerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/register",
  validateSearch: parseAuthSearch,
  component: () => (
    <RegisterPage redirectTo={registerRoute.useSearch().redirect} />
  ),
});

const ideasRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/ideas",
  component: IdeasPage,
});
const casesRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/cases",
  component: CasesPage,
});
const rulesRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/rules",
  component: RulesPage,
});

function requireAccount(locationHref: string): void {
  const email = sessionRepository.getSnapshot();
  if (!email || !appApi.getSnapshot().users[email]) {
    sessionRepository.clear();
    throw redirect({ to: "/login", search: { redirect: locationHref } });
  }
}

const centerIndexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/center",
  beforeLoad: ({ location }) => requireAccount(location.href),
  loader: () => {
    throw redirect({ to: "/center/$tab", params: { tab: "ideas" } });
  },
});

const centerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/center/$tab",
  beforeLoad: ({ location }) => requireAccount(location.href),
  component: () => (
    <CenterPage requestedTab={centerRoute.useParams().tab as CenterTabPath} />
  ),
});

const routeTree = rootRoute.addChildren([
  homeRoute,
  loginRoute,
  registerRoute,
  ideasRoute,
  casesRoute,
  rulesRoute,
  centerIndexRoute,
  centerRoute,
]);

export const router = createRouter({
  routeTree,
  defaultPreload: "intent",
  scrollRestoration: true,
});

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}

export function AppRouter() {
  return <RouterProvider router={router} />;
}
