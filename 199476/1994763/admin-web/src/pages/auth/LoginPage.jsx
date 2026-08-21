import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi, token } from '../../api/adminApi.js';
import './LoginPage.css';
import { message } from '../../components/feedback/message.js';
export default function LoginPage({ onAuthenticated }) {
  const nav = useNavigate();
  const [setup, setSetup] = useState(false);
  const [form, setForm] = useState({ phone: '', password: '', displayName: '' });
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    adminApi
      .setupStatus()
      .then((r) => setSetup(r.needsSetup))
      .catch((e) => message.error(e.message));
  }, []);
  const submit = async (e) => {
    e.preventDefault();
    if (!/^1\d{10}$/.test(form.phone)) {
      message.warning('请输入正确的手机号');
      return;
    }
    if (form.password.length < 10) {
      message.warning('密码至少10位');
      return;
    }
    setLoading(true);
    try {
      const result = setup ? await adminApi.setup(form) : await adminApi.login(form);
      token.set(result.token);
      message.success(setup ? '管理员初始化成功' : '登录成功');
      onAuthenticated();
      nav('/dashboard', { replace: true });
    } catch (err) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  };
  return (
    <div className="admin-login">
      <section>
        <div className="login-brand">
          <img src="/brand/app-icon.png" alt="事先问" />
          <div>
            <h1>事先问</h1>
            <p>管理后台</p>
          </div>
        </div>
        <h2>{setup ? '创建首位管理员' : '管理员登录'}</h2>
        <p className="login-tip">
          {setup ? '系统尚未初始化，请设置管理员信息。' : '请输入管理员手机号和密码。'}
        </p>
        <form onSubmit={submit}>
          {setup && (
            <label>
              显示名称
              <input
                value={form.displayName}
                onChange={(e) => setForm({ ...form, displayName: e.target.value })}
                placeholder="例如：运营管理员"
              />
            </label>
          )}
          <label>
            手机号
            <input
              autoFocus
              value={form.phone}
              inputMode="numeric"
              maxLength={11}
              onChange={(e) => setForm({ ...form, phone: e.target.value.replace(/\D/g, '') })}
              placeholder="请输入管理员手机号"
            />
          </label>
          <label>
            密码
            <input
              type="password"
              maxLength={128}
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="至少10位，包含字母和数字"
            />
          </label>
          <button disabled={loading}>
            {loading ? '正在处理…' : setup ? '完成初始化' : '登录'}
          </button>
        </form>
      </section>
    </div>
  );
}
