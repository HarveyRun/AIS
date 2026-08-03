import { useState, type PropsWithChildren } from "react";
import { Link } from "@tanstack/react-router";
import { useAppStore } from "../application/store";
import { GlobalFeedback } from "../features/feedback/GlobalFeedback";
import { Icon } from "../shared/ui/Icon";

interface PublicLayoutProps extends PropsWithChildren {
  active?: "home" | "cases";
}

export function PublicLayout({ active, children }: PublicLayoutProps) {
  const { user } = useAppStore();
  const [menuOpen, setMenuOpen] = useState(false);
  const accountLink = user ? (
    <Link
      className="global-action ghost"
      to="/center/$tab"
      params={{ tab: "ideas" }}
    >
      <span className="live-dot" /> &nbsp;&nbsp;
      {user.name || user.email.split("@")[0]}
    </Link>
  ) : (
    <Link
      className="global-action ghost"
      to="/login"
      search={{ redirect: "/center/ideas" }}
    >
      登录
    </Link>
  );
  const centerLink = user ? (
    <Link to="/center/$tab" params={{ tab: "ideas" }}>
      个人中心
    </Link>
  ) : (
    <Link to="/login" search={{ redirect: "/center/ideas" }}>
      个人中心
    </Link>
  );
  return (
    <>
      <div className="global-nav-wrap" data-global-header>
        <nav
          className={`global-nav ${menuOpen ? "menu-open" : ""}`}
          aria-label="全站导航"
        >
          <Link className="global-brand" to="/" hash="top">
            <span className="global-logo">点</span>
            <span className="global-brand-copy">
              <strong>点成</strong>
              <small>助力小想法建站</small>
            </span>
          </Link>
          <div className="global-links">
            <Link
              className={active === "home" ? "active" : ""}
              to="/"
              onClick={() => setMenuOpen(false)}
            >
              首页
            </Link>
            <Link
              className={active === "cases" ? "active" : ""}
              to="/cases"
              onClick={() => setMenuOpen(false)}
            >
              案例
            </Link>
          </div>
          <button
            className="global-menu-toggle"
            type="button"
            aria-label={menuOpen ? "关闭导航" : "打开导航"}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((value) => !value)}
          >
            <span />
            <span />
          </button>
          <div className="global-actions">
            {accountLink}
            <Link className="global-action primary" to="/" hash="top">
              提出想法 <Icon name="arrow-up-right" />
            </Link>
          </div>
        </nav>
      </div>
      {children}
      <footer className="global-footer" data-global-footer>
        <div className="global-footer-inner">
          <div>
            <Link className="global-brand" to="/" hash="top">
              <span className="global-logo">点</span>
              <span>点成</span>
            </Link>
            <div className="global-footer-copy">
              一句话说出想要的，我们来实现。
            </div>
          </div>
          <div className="global-footer-links">
            <Link to="/rules">服务规则</Link>
            {centerLink}
            <button
              type="button"
              onClick={() =>
                document
                  .querySelector<HTMLButtonElement>(".feedback-launcher")
                  ?.click()
              }
            >
              留言与反馈
            </button>
          </div>
        </div>
      </footer>
      <GlobalFeedback />
    </>
  );
}
