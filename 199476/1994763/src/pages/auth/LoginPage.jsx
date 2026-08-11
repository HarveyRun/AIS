import { useEffect, useState } from 'react';
import { LockKeyhole, Smartphone } from 'lucide-react';
import './AuthPages.css';
import CanvasLogo from '../../components/brand/CanvasLogo.jsx';

const digitsOnly = (value) => value.replace(/\D/g, '');

export default function LoginPage({ go, onLogin, notify }) {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [sent, setSent] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [error, setError] = useState('');

  useEffect(() => {
    if (countdown <= 0) return undefined;
    const timer = window.setInterval(() => setCountdown((current) => current - 1), 1000);
    return () => window.clearInterval(timer);
  }, [countdown]);

  const sendCode = () => {
    setSent(true);
    setCountdown(60);
    setCode('');
    setError('');
    notify('验证码已发送，演示验证码为 123456');
  };

  const submit = () => {
    if (!sent) {
      setError('请先获取验证码');
      return;
    }
    if (code !== '123456') {
      setError('验证码不正确');
      return;
    }
    onLogin({ phone });
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
        <h2>欢迎回来</h2>
        <p>使用手机号登录</p>
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
              setError('');
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
              setError('');
            }}
            placeholder="请输入验证码"
          />
          <button type="button" onClick={sendCode} disabled={phone.length !== 11 || countdown > 0}>
            {countdown > 0 ? `${countdown}秒` : sent ? '重新获取' : '获取验证码'}
          </button>
        </div>
        {sent && <small className="auth-code-hint">演示验证码：123456</small>}
        {error && <small className="auth-error">{error}</small>}
        <button
          type="button"
          className="auth-submit"
          disabled={phone.length !== 11 || code.length !== 6}
          onClick={submit}
        >
          登录
        </button>
        <div className="auth-switch">
          还没有账号？
          <button type="button" onClick={() => go('register')}>
            立即注册
          </button>
        </div>
        <small className="auth-legal-links">
          登录即代表你同意
          <button type="button" onClick={() => go('terms')}>
            《用户协议》
          </button>
          和
          <button type="button" onClick={() => go('privacy')}>
            《隐私政策》
          </button>
        </small>
      </section>
    </div>
  );
}
