import { Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { appService } from "../application/appService";
import { resolveAppTarget, useAppNavigate } from "../application/navigation";
import { GlobalFeedback } from "../features/feedback/GlobalFeedback";
import { errorMessage, useAsyncAction } from "../shared/lib/useAsyncAction";
import { Icon } from "../shared/ui/Icon";
import "../styles/pages/login.css";

export function RegisterPage({ redirectTo }: { redirectTo?: string }) {
  const navigate = useAppNavigate();
  const target = resolveAppTarget(redirectTo);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [inviteDigits, setInviteDigits] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const { isPending, run } = useAsyncAction();
  useEffect(() => {
    document.title = "注册｜点成";
  }, []);
  return (
    <div className="login-page">
      <main className="login-main">
        <section className="login-card">
          <div className="login-card-head">
            <Link className="login-mark" to="/" aria-label="返回首页">
              点
            </Link>
            <Link className="login-back-home" to="/">
              <Icon name="arrow-left" /> 返回首页
            </Link>
          </div>
          <h1>注册</h1>
          <p className="intro">请使用有效邀请码注册账号</p>
          <form
            onSubmit={async (event) => {
              event.preventDefault();
              setError("");
              try {
                const result = await run(() =>
                  appService.register(
                    email,
                    password,
                    confirmation,
                    inviteDigits,
                  ),
                );
                if (!result.ok) {
                  setError(result.error || "注册失败。");
                  return;
                }
                await navigate(target.destination, {
                  replace: true,
                  homeSearch: target.homeSearch,
                });
              } catch (reason) {
                setError(errorMessage(reason, "注册失败，请稍后重试。"));
              }
            }}
          >
            <div className="field">
              <label htmlFor="register-email">邮箱</label>
              <input
                id="register-email"
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
              <label htmlFor="register-password">密码</label>
              <div className="password-wrap">
                <input
                  id="register-password"
                  type={showPassword ? "text" : "password"}
                  minLength={8}
                  maxLength={72}
                  autoComplete="new-password"
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
            <div className="field">
              <label htmlFor="confirm-password">确认密码</label>
              <input
                id="confirm-password"
                type={showPassword ? "text" : "password"}
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                required
                placeholder="再次输入密码"
                value={confirmation}
                onChange={(event) => setConfirmation(event.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="inviteDigits">邀请码</label>
              <div className="invite-wrap">
                <span className="invite-prefix">DC-</span>
                <input
                  id="inviteDigits"
                  inputMode="numeric"
                  autoComplete="off"
                  maxLength={6}
                  pattern="[0-9]{6}"
                  required
                  placeholder="6 位数字"
                  aria-label="邀请码后六位"
                  value={inviteDigits}
                  onChange={(event) =>
                    setInviteDigits(
                      event.target.value.replace(/\D/g, "").slice(0, 6),
                    )
                  }
                />
              </div>
            </div>
            <div className="message" role="alert">
              {error}
            </div>
            <button className="submit" type="submit" disabled={isPending}>
              {isPending ? "注册中…" : "注册并登录"}
            </button>
          </form>
          <p className="auth-note">
            注册即表示你同意遵守<Link to="/rules">服务规则</Link>。
          </p>
          <p className="auth-switch">
            已经有账号？
            <Link
              to="/login"
              search={{ redirect: redirectTo || "/center/ideas" }}
            >
              直接登录
            </Link>
          </p>
        </section>
      </main>
      <GlobalFeedback />
    </div>
  );
}
