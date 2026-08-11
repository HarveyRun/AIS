import { useEffect, useState } from 'react';
import { ArrowLeft, CircleUserRound, LockKeyhole, Smartphone } from 'lucide-react';
import './AuthPages.css';
import CanvasLogo from '../../components/brand/CanvasLogo.jsx';

const digitsOnly = (value) => value.replace(/\D/g, '');

export default function RegisterPage({ go, onRegister, notify }) {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [agreed, setAgreed] = useState(true);
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
    if (!sent || code !== '123456') {
      setError(sent ? '验证码不正确' : '请先获取验证码');
      return;
    }
    onRegister({ name: name.trim(), phone });
  };

  return (
    <div className="auth-page register">
      <button type="button" className="auth-back" onClick={() => go('login')}>
        <ArrowLeft />
      </button>
      <div className="auth-brand compact">
        <div>
          <CanvasLogo size={50} />
        </div>
        <h1>创建账号</h1>
        <p>一切从这里开始</p>
      </div>
      <section className="auth-card">
        <label>昵称（选填）</label>
        <div className="auth-input">
          <CircleUserRound />
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength="12"
            placeholder="请输入昵称"
          />
        </div>
        <label>手机号</label>
        <div className="auth-input">
          <Smartphone />
          <input
            value={phone}
            onChange={(event) => {
              setPhone(digitsOnly(event.target.value));
              setSent(false);
              setCountdown(0);
              setError('');
            }}
            maxLength="11"
            inputMode="numeric"
            placeholder="请输入手机号"
          />
        </div>
        <label>验证码</label>
        <div className="auth-input">
          <LockKeyhole />
          <input
            value={code}
            onChange={(event) => {
              setCode(digitsOnly(event.target.value));
              setError('');
            }}
            maxLength="6"
            inputMode="numeric"
            placeholder="请输入验证码"
          />
          <button type="button" disabled={phone.length !== 11 || countdown > 0} onClick={sendCode}>
            {countdown > 0 ? `${countdown}秒` : sent ? '重新获取' : '获取验证码'}
          </button>
        </div>
        {sent && <small className="auth-code-hint">演示验证码：123456</small>}
        {error && <small className="auth-error">{error}</small>}
        <label className="agree">
          <input
            type="checkbox"
            checked={agreed}
            onChange={(event) => setAgreed(event.target.checked)}
          />
          <span>
            我已阅读并同意
            <button
              type="button"
              onClick={(event) => {
                event.preventDefault();
                go('terms');
              }}
            >
              用户协议
            </button>
            与
            <button
              type="button"
              onClick={(event) => {
                event.preventDefault();
                go('privacy');
              }}
            >
              隐私政策
            </button>
          </span>
        </label>
        <button
          type="button"
          className="auth-submit"
          disabled={phone.length !== 11 || code.length !== 6 || !agreed}
          onClick={submit}
        >
          注册并进入
        </button>
      </section>
    </div>
  );
}
