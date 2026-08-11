import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi, token } from '../../api/adminApi.js';
import './LoginPage.css';
export default function LoginPage() {
  const nav = useNavigate();
  const [setup, setSetup] = useState(false);
  const [form, setForm] = useState({ phone: '', password: '', displayName: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    adminApi
      .setupStatus()
      .then((r) => setSetup(r.needsSetup))
      .catch((e) => setError(e.message));
  }, []);
  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const result = setup ? await adminApi.setup(form) : await adminApi.login(form);
      token.set(result.token);
      nav('/dashboard');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };
  return (
    <div className="admin-login">
      <section>
        <div className="login-brand">
          <i>问</i>
          <div>
            <h1>事先问</h1>
            <p>管理后台</p>
          </div>
        </div>
        <h2>{setup ? '创建首位管理员' : '管理员登录'}</h2>
        <p className="login-tip">
          {setup ? '系统尚未初始化，请设置管理账号。' : '请输入管理账号继续。'}
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
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="至少10位，包含字母和数字"
            />
          </label>
          {error && <div className="login-error">{error}</div>}
          <button disabled={loading}>
            {loading ? '正在处理…' : setup ? '完成初始化' : '登录'}
          </button>
        </form>
      </section>
    </div>
  );
}
