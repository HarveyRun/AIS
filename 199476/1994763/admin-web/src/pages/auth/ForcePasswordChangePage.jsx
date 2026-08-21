import { useState } from 'react';
import { adminApi, token } from '../../api/adminApi.js';
import { message } from '../../components/feedback/message.js';
import './LoginPage.css';

export default function ForcePasswordChangePage({ onChanged, onLoggedOut }) {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmation: '' });
  const [saving, setSaving] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    if (form.newPassword.length < 10 || form.newPassword.length > 128) {
      return message.warning('新密码需为10至128位');
    }
    if (!/[A-Za-z]/.test(form.newPassword) || !/\d/.test(form.newPassword)) {
      return message.warning('新密码必须同时包含字母和数字');
    }
    if (form.newPassword !== form.confirmation) {
      return message.warning('两次输入的新密码不一致');
    }
    try {
      setSaving(true);
      await adminApi.changePassword({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      });
      message.success('密码已修改');
      await onChanged();
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const logout = async () => {
    try {
      await adminApi.logout();
    } catch {}
    token.set('');
    onLoggedOut();
  };

  return (
    <div className="admin-login">
      <section>
        <div className="login-brand">
          <img src="/brand/app-icon.png" alt="事先问" />
          <div><h1>事先问</h1><p>管理后台</p></div>
        </div>
        <h2>修改初始密码</h2>
        <p className="login-tip">当前密码仅用于初始化账号，修改后才能进入管理后台。</p>
        <form onSubmit={submit}>
          <label>当前密码<input type="password" maxLength={128} value={form.currentPassword} onChange={(event) => setForm({ ...form, currentPassword: event.target.value })} /></label>
          <label>新密码<input type="password" maxLength={128} value={form.newPassword} onChange={(event) => setForm({ ...form, newPassword: event.target.value })} placeholder="10至128位，同时包含字母和数字" /></label>
          <label>确认新密码<input type="password" maxLength={128} value={form.confirmation} onChange={(event) => setForm({ ...form, confirmation: event.target.value })} /></label>
          <button disabled={saving}>{saving ? '正在保存…' : '确认修改'}</button>
          <button className="admin-login-secondary" type="button" disabled={saving} onClick={logout}>退出登录</button>
        </form>
      </section>
    </div>
  );
}
