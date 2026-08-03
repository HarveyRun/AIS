import { useState } from "react";
import { appService } from "../../application/appService";
import { useAppNavigate } from "../../application/navigation";
import { useAppStore } from "../../application/store";
import { errorMessage, useAsyncAction } from "../../shared/lib/useAsyncAction";
import { useToast } from "../../shared/ui/Toast";

export function SettingsPanel() {
  const { user, sessionEmail } = useAppStore();
  const notify = useToast();
  const navigate = useAppNavigate();
  const [name, setName] = useState(user?.name || "");
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const { isPending: isSavingProfile, run: runProfile } = useAsyncAction();
  const { isPending: isChangingPassword, run: runPassword } = useAsyncAction();
  const { isPending: isDeleting, run: runDelete } = useAsyncAction();
  if (!user) return null;
  const logout = async () => {
    await appService.logout();
    await navigate("/", {});
  };
  return (
    <section className="panel active">
      <p className="panel-lead">管理当前账号的信息和密码。</p>
      <form
        className="settings-card"
        onSubmit={async (event) => {
          event.preventDefault();
          try {
            await runProfile(() => appService.updateProfile(sessionEmail, name));
            notify("显示名称已保存。");
          } catch (error) {
            notify(errorMessage(error, "名称保存失败，请稍后重试。"));
          }
        }}
      >
        <h2>基本信息</h2>
        <div className="field">
          <label htmlFor="display-name">显示名称</label>
          <input
            id="display-name"
            maxLength={30}
            placeholder="你的称呼"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </div>
        <div className="account-row">
          <span>登录邮箱</span>
          <b>{user.email}</b>
        </div>
        <div className="account-row">
          <span>我的邀请码</span>
          <span>
            <b>{user.inviteCode}</b>{" "}
            <button
              className="text-button"
              type="button"
              onClick={async () => {
                try {
                  await navigator.clipboard.writeText(user.inviteCode);
                  notify("邀请码已复制。");
                } catch {
                  notify("复制失败，请手动复制。");
                }
              }}
            >
              复制
            </button>
          </span>
        </div>
        <button className="button primary" type="submit" disabled={isSavingProfile}>
          {isSavingProfile ? "保存中…" : "保存名称"}
        </button>
      </form>
      <form
        className="settings-card"
        onSubmit={async (event) => {
          event.preventDefault();
          setPasswordError("");
          try {
            const result = await runPassword(() =>
              appService.changePassword(sessionEmail, oldPassword, newPassword),
            );
            if (!result.ok) {
              setPasswordError(result.error || "修改失败。");
              return;
            }
            setOldPassword("");
            setNewPassword("");
            notify("密码已修改，当前账号仍保持登录。其他设备需要重新登录。");
          } catch (error) {
            setPasswordError(errorMessage(error, "修改失败，请稍后重试。"));
          }
        }}
      >
        <h2>修改密码</h2>
        <div className="grid">
          <div className="field">
            <label htmlFor="old-password">当前密码</label>
            <input
              id="old-password"
              type="password"
              maxLength={72}
              required
              value={oldPassword}
              onChange={(event) => setOldPassword(event.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="new-password">新密码</label>
            <input
              id="new-password"
              type="password"
              minLength={8}
              maxLength={72}
              required
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
            />
          </div>
        </div>
        <div className="form-error">{passwordError}</div>
        <button className="button" type="submit" disabled={isChangingPassword}>
          {isChangingPassword ? "修改中…" : "修改密码"}
        </button>
      </form>
      <div className="settings-card">
        <h2>账号与数据</h2>
        <div className="account-row">
          <span>退出当前账号</span>
          <button className="text-button" type="button" onClick={logout}>
            退出登录
          </button>
        </div>
        <div className="account-row">
          <span>删除账号和全部业务数据</span>
          <button
            className="text-button danger"
            type="button"
            disabled={isDeleting}
            onClick={async () => {
              if (
                !window.confirm("确定删除账号及全部业务数据？此操作无法恢复。")
              )
                return;
              try {
                await runDelete(async () => {
                  await appService.deleteAccount(sessionEmail);
                  await navigate("/", { replace: true });
                });
              } catch (error) {
                notify(errorMessage(error, "账号删除失败，请稍后重试。"));
              }
            }}
          >
            {isDeleting ? "删除中…" : "删除账号"}
          </button>
        </div>
        <div className="notice">账号信息和使用记录由你管理。</div>
      </div>
    </section>
  );
}
