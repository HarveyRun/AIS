import { useEffect, useRef, useState } from 'react';
import { ArrowLeft, ArrowRight, Check } from 'lucide-react';
import './AuthPages.css';
import { api } from '../../api/http.js';

const CODE_LENGTH = 4;

function digitsOnly(value) {
  return value.replace(/\D/g, '');
}

function formatPhone(value) {
  const first = value.slice(0, 3);
  const middle = value.slice(3, 7);
  const last = value.slice(7, 11);
  return [first, middle, last].filter(Boolean).join(' ');
}

export default function LoginPage({ go, onLogin, notify }) {
  const [step, setStep] = useState('phone');
  const [phone, setPhone] = useState('');
  const [sentPhone, setSentPhone] = useState('');
  const [code, setCode] = useState('');
  const [countdown, setCountdown] = useState(0);
  const [agreed, setAgreed] = useState(false);
  const [sending, setSending] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const codeInputRef = useRef(null);
  const verifyingRef = useRef(false);

  useEffect(() => {
    if (countdown <= 0) return undefined;
    const timer = window.setTimeout(() => {
      setCountdown((current) => Math.max(0, current - 1));
    }, 1000);
    return () => window.clearTimeout(timer);
  }, [countdown]);

  useEffect(() => {
    if (step !== 'code') return undefined;
    const frame = window.requestAnimationFrame(() => codeInputRef.current?.focus());
    return () => window.cancelAnimationFrame(frame);
  }, [step]);

  const sendCode = async () => {
    if (phone.length !== 11 || sending) return;
    if (!agreed) {
      notify('请先阅读并同意用户协议和隐私政策', 'warning');
      return;
    }
    if (sentPhone === phone && countdown > 0) {
      setCode('');
      setStep('code');
      return;
    }

    setSending(true);
    try {
      await api.sendCode(phone);
      setSentPhone(phone);
      setCode('');
      setCountdown(60);
      setStep('code');
    } catch (requestError) {
      notify(requestError.message, 'error');
    } finally {
      setSending(false);
    }
  };

  const verifyCode = async (nextCode) => {
    if (nextCode.length !== CODE_LENGTH || verifyingRef.current) return;
    verifyingRef.current = true;
    setVerifying(true);
    try {
      const result = await api.login(sentPhone, nextCode);
      onLogin(result);
    } catch (requestError) {
      setCode('');
      notify(requestError.message, 'error');
      window.requestAnimationFrame(() => codeInputRef.current?.focus());
    } finally {
      verifyingRef.current = false;
      setVerifying(false);
    }
  };

  const changeCode = (value) => {
    const nextCode = digitsOnly(value).slice(0, CODE_LENGTH);
    setCode(nextCode);
    if (nextCode.length === CODE_LENGTH) verifyCode(nextCode);
  };

  const resendCode = async () => {
    if (countdown > 0 || sending) return;
    setSending(true);
    try {
      await api.sendCode(phone);
      setSentPhone(phone);
      setCode('');
      setCountdown(60);
      codeInputRef.current?.focus();
    } catch (requestError) {
      notify(requestError.message, 'error');
    } finally {
      setSending(false);
    }
  };

  const resendLabel = verifying
    ? '正在验证…'
    : countdown > 0
      ? `重新获取（${countdown}s）`
      : sending
        ? '正在发送…'
        : '重新获取';

  if (step === 'code') {
    return (
      <div className="login-page login-code-page">
        <button
          type="button"
          className="login-back"
          aria-label="返回填写手机号"
          disabled={verifying}
          onClick={() => {
            setCode('');
            setStep('phone');
          }}
        >
          <ArrowLeft />
        </button>

        <section className="login-code-content">
          <h1>输入验证码</h1>
          <p>验证码已发送至 +86 {formatPhone(sentPhone)}</p>

          <div className="login-code-input" onClick={() => codeInputRef.current?.focus()}>
            <input
              ref={codeInputRef}
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={CODE_LENGTH}
              aria-label="验证码"
              value={code}
              disabled={verifying}
              onChange={(event) => changeCode(event.target.value)}
            />
            {Array.from({ length: CODE_LENGTH }, (_, index) => (
              <span
                key={index}
                className={
                  !verifying && index === Math.min(code.length, CODE_LENGTH - 1) ? 'active' : ''
                }
              >
                {code[index] || ''}
              </span>
            ))}
          </div>

          <button
            type="button"
            className="login-resend"
            disabled={countdown > 0 || sending || verifying}
            onClick={resendCode}
          >
            {resendLabel}
          </button>
        </section>
      </div>
    );
  }

  return (
    <div className="login-page login-phone-page">
      <section className="login-phone-content">
        <h1>事先问</h1>

        <div className="login-phone-input">
          <span>+86</span>
          <input
            type="tel"
            inputMode="numeric"
            autoComplete="tel"
            maxLength={13}
            aria-label="手机号"
            value={formatPhone(phone)}
            onChange={(event) => {
              const nextPhone = digitsOnly(event.target.value).slice(0, 11);
              setPhone(nextPhone);
              if (nextPhone !== sentPhone) setCountdown(0);
            }}
            placeholder="请输入手机号"
          />
        </div>

        <p className="login-account-tip">未注册的手机号验证后自动创建账户</p>

        <button
          type="button"
          className="login-next"
          aria-label="获取验证码"
          disabled={phone.length !== 11 || sending}
          onClick={sendCode}
        >
          <ArrowRight />
        </button>
      </section>

      <div className="login-agreement">
        <label aria-label="同意用户协议和隐私政策">
          <input
            type="checkbox"
            checked={agreed}
            onChange={(event) => setAgreed(event.target.checked)}
          />
          <i className={agreed ? 'checked' : ''}>
            <Check />
          </i>
        </label>
        <p>
          我已阅读并同意
          <button type="button" onClick={() => go('terms')}>
            《用户协议》
          </button>
          和
          <button type="button" onClick={() => go('privacy')}>
            《隐私政策》
          </button>
        </p>
      </div>
    </div>
  );
}
