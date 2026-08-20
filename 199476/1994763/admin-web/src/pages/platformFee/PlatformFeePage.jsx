import { useEffect, useState } from 'react';
import { Apple, Percent, Smartphone } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import { message } from '../../components/feedback/message.js';
import './PlatformFeePage.css';

const previewAmount = 100;

export default function PlatformFeePage() {
  const { can } = useAdminAccess();
  const [setting, setSetting] = useState(null);
  const [rates, setRates] = useState({ android: '', ios: '' });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    adminApi
      .platformFee()
      .then(applySetting)
      .catch((error) => message.error(error.message));
  }, []);

  const applySetting = (result) => {
    setSetting(result);
    setRates({
      android: formatRate(result.androidRatePercent),
      ios: formatRate(result.iosRatePercent),
    });
  };

  const save = async (event) => {
    event.preventDefault();
    const androidRate = validateRate(rates.android, 'Android');
    const iosRate = validateRate(rates.ios, 'iOS');
    if (androidRate == null || iosRate == null) return;

    try {
      setSaving(true);
      applySetting(await adminApi.updatePlatformFee(androidRate, iosRate));
      message.success('Android和iOS服务费率已更新');
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>平台服务费</h1>
          <p>按发起询问的客户端平台计算，新费率只影响保存后新发起的询问</p>
        </div>
      </div>

      <form className="fee-platform-form" onSubmit={save}>
        <FeeCard
          icon={Smartphone}
          platform="Android"
          value={rates.android}
          updatedAt={setting?.androidUpdatedAt}
          onChange={(value) => setRates((current) => ({ ...current, android: value }))}
          disabled={!can('PLATFORM_FEE_EDIT')}
        />
        <FeeCard
          icon={Apple}
          platform="iOS"
          value={rates.ios}
          updatedAt={setting?.iosUpdatedAt}
          onChange={(value) => setRates((current) => ({ ...current, ios: value }))}
          disabled={!can('PLATFORM_FEE_EDIT')}
        />
        {can('PLATFORM_FEE_EDIT') && <button className="primary fee-save" type="submit" disabled={saving}>
          {saving ? '保存中' : '保存两端费率'}
        </button>}
      </form>
    </>
  );
}

function FeeCard({ icon: Icon, platform, value, updatedAt, onChange, disabled }) {
  const rate = Number(value) || 0;
  const fee = previewAmount * rate / 100;
  return (
    <section className="fee-setting-card">
      <div className="fee-setting-heading">
        <i><Icon /></i>
        <div>
          <b>{platform} 服务费率</b>
          <span>从该平台发起的询问，按此费率结算</span>
        </div>
      </div>
      <label>
        <span>服务费率</span>
        <div className="fee-rate-input">
          <input
            value={value}
            onChange={(event) => onChange(event.target.value)}
            inputMode="decimal"
            placeholder="5"
            maxLength={6}
            disabled={disabled}
          />
          <em>%</em>
        </div>
      </label>
      <div className="fee-preview">
        <span>询问金额 ¥{previewAmount}</span>
        <span>服务费 ¥{fee.toFixed(2)}</span>
        <strong>回答方收入 ¥{(previewAmount - fee).toFixed(2)}</strong>
      </div>
      {updatedAt && (
        <small className="fee-updated-at">
          最近更新：{new Date(updatedAt).toLocaleString('zh-CN')}
        </small>
      )}
    </section>
  );
}

function validateRate(value, platform) {
  const rate = Number(value);
  if (!Number.isFinite(rate) || rate < 0 || rate > 99) {
    message.warning(`${platform}费率必须在0%至99%之间`);
    return null;
  }
  return rate;
}

function formatRate(value) {
  const number = Number(value || 0);
  return Number.isInteger(number) ? String(number) : number.toFixed(2).replace(/0+$/, '');
}
