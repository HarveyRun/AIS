import { useEffect, useState } from 'react';
import { LockKeyhole, Smartphone } from 'lucide-react';
import './AuthPages.css';
import CanvasLogo from '../../components/brand/CanvasLogo.jsx';
import { api } from '../../api/http.js';

const digitsOnly = (value) => value.replace(/\D/g, '');

export default function LoginPage({ go, onLogin, notify }) {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [sent, setSent] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [agreed, setAgreed] = useState(false);

  useEffect(() => {
    if (countdown <= 0) return undefined;
    const timer = window.setInterval(() => setCountdown((current) => current - 1), 1000);
    return () => window.clearInterval(timer);
  }, [countdown]);

  const sendCode = async () => {
    try {
      await api.sendCode(phone);
      setSent(true);
      setCountdown(60);
      setCode('');
      notify('验证码已发送', 'success');
    } catch (requestError) {
      notify(requestError.message, 'error');
    }
  };

  const submit = async () => {
    if (!agreed) {
      notify('请先阅读并同意用户协议和隐私政策', 'warning');
      return;
    }
    if (!sent) {
      notify('请先获取验证码', 'warning');
      return;
    }
    try {
      const result = await api.login(phone, code);
      onLogin(result);
    } catch (requestError) {
      notify(requestError.message, 'error');
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-brand">
        <div>
          <CanvasLogo size={62} />
        </div>
        <h1>事先问</h1>
        <p>有事先问问过来人</p>
      </div>
      <section className="auth-card">
        <h2>手机号登录</h2>
        <p>首次登录将自动创建账号</p>
        <label>手机号</label>
        <div className="auth-input">
          <Smartphone />
          <input
            maxLength="11"
            inputMode="numeric"
            value={phone}
            onChange={(event) => {
              setPhone(digitsOnly(event.target.value));
              setSent(false);
              setCountdown(0);
            }}
            placeholder="请输入手机号"
          />
        </div>
        <label>验证码</label>
        <div className="auth-input">
          <LockKeyhole />
          <input
            maxLength="6"
            inputMode="numeric"
            value={code}
            onChange={(event) => {
              setCode(digitsOnly(event.target.value));
            }}
            placeholder="请输入验证码"
          />
          <button type="button" onClick={sendCode} disabled={phone.length !== 11 || countdown > 0}>
            {countdown > 0 ? `${countdown}秒` : sent ? '重新获取' : '获取验证码'}
          </button>
        </div>
        <button
          type="button"
          className="auth-submit"
          disabled={phone.length !== 11 || code.length !== 6}
          onClick={submit}
        >
          登录
        </button>
        <label className="agree auth-login-agree">
          <input
            type="checkbox"
            checked={agreed}
            onChange={(event) => setAgreed(event.target.checked)}
          />
          <span>
            我已阅读并同意
            <button type="button" onClick={(event) => { event.preventDefault(); go('terms'); }}>
              《用户协议》
            </button>
            和
            <button type="button" onClick={(event) => { event.preventDefault(); go('privacy'); }}>
              《隐私政策》
            </button>
          </span>
        </label>
      </section>
    </div>
  );
}
