import { Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { appService } from "../application/appService";
import { resolveAppTarget, useAppNavigate } from "../application/navigation";
import { GlobalFeedback } from "../features/feedback/GlobalFeedback";
import { errorMessage, useAsyncAction } from "../shared/lib/useAsyncAction";
import { Icon } from "../shared/ui/Icon";
import "../styles/pages/login.css";

export function LoginPage({ redirectTo }: { redirectTo?: string }) {
  const navigate = useAppNavigate();
  const target = resolveAppTarget(redirectTo);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const { isPending, run } = useAsyncAction();
  useEffect(() => {
    document.title = "登录｜点成";
  }, []);
  return (
    <div className="login-page">
      <main className="login-main">
        <section className="login-card">
          <div className="login-card-head">
            <Link className="login-mark" to="/" aria-label="点成首页">
              点
            </Link>
            <Link className="login-back-home" to="/">
              <Icon name="arrow-left" /> 返回首页
            </Link>
          </div>
          <h1>登录</h1>
          <p className="intro">欢迎回来，请登录</p>
          <form
            onSubmit={async (event) => {
              event.preventDefault();
              setError("");
              try {
                const result = await run(() =>
                  appService.login(email, password),
                );
                if (!result.ok) {
                  setError(result.error || "登录失败。");
                  return;
                }
                await navigate(target.destination, {
                  replace: true,
                  homeSearch: target.homeSearch,
                });
              } catch (reason) {
                setError(errorMessage(reason, "登录失败，请稍后重试。"));
              }
            }}
          >
            <div className="field">
              <label htmlFor="email">邮箱</label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                maxLength={190}
                required
                placeholder="name@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="password">密码</label>
              <div className="password-wrap">
                <input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  minLength={8}
                  maxLength={72}
                  autoComplete="current-password"
                  required
                  placeholder="至少 8 位"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
                <button
                  className="password-toggle"
                  type="button"
                  onClick={() => setShowPassword((value) => !value)}
                >
                  {showPassword ? "隐藏" : "显示"}
                </button>
              </div>
            </div>
            <div className="message" role="alert">
              {error}
            </div>
            <button className="submit" type="submit" disabled={isPending}>
              {isPending ? "登录中…" : "登录"}
            </button>
          </form>
          <p className="auth-switch">
            还没有账号？
            <Link
              to="/register"
              search={{ redirect: redirectTo || "/center/ideas" }}
            >
              立即注册
            </Link>
          </p>
        </section>
      </main>
      <GlobalFeedback />
    </div>
  );
}
