import { useEffect, useState } from 'react';
import { CircleDollarSign, Gift, UsersRound } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import { message } from '../../components/feedback/message.js';
import './InvitationCampaignPage.css';

export default function InvitationCampaignPage() {
  const { can } = useAdminAccess();
  const [setting, setSetting] = useState(null);
  const [enabled, setEnabled] = useState(false);
  const [rewardAmount, setRewardAmount] = useState('3');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    adminApi.invitationCampaign()
      .then(applySetting)
      .catch((error) => message.error(error.message));
  }, []);

  const applySetting = (result) => {
    setSetting(result);
    setEnabled(result.enabled === true);
    setRewardAmount(formatAmount(result.rewardAmount));
  };

  const save = async (event) => {
    event.preventDefault();
    const amount = validateAmount(rewardAmount);
    if (amount == null) return;

    try {
      setSaving(true);
      applySetting(await adminApi.updateInvitationCampaign(enabled, amount));
      message.success(enabled ? '邀请答主活动已上架' : '邀请答主活动已下架');
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
          <h1>邀请答主活动</h1>
          <p>控制 App 邀请码入口和每位成功邀请的红包金额</p>
        </div>
      </div>

      <form className="invitation-campaign-layout" onSubmit={save}>
        <section className="invitation-campaign-card invitation-campaign-control">
          <div className="invitation-campaign-heading">
            <i><Gift /></i>
            <div>
              <b>活动状态</b>
              <span>下架后，App 不再显示“填写邀请码”入口</span>
            </div>
            <em className={enabled ? 'is-online' : 'is-offline'}>
              {enabled ? '已上架' : '已下架'}
            </em>
          </div>

          <label className="invitation-campaign-switch">
            <span>在 App 中展示活动入口</span>
            <input
              type="checkbox"
              checked={enabled}
              onChange={(event) => setEnabled(event.target.checked)}
              disabled={!can('INVITATION_CAMPAIGN_EDIT')}
            />
            <i aria-hidden="true" />
          </label>

          <label className="invitation-reward-field">
            <span>每位成功邀请红包</span>
            <div>
              <input
                value={rewardAmount}
                onChange={(event) => setRewardAmount(event.target.value)}
                inputMode="decimal"
                maxLength={6}
                disabled={!can('INVITATION_CAMPAIGN_EDIT') || setting?.enabled === true}
              />
              <em>元</em>
            </div>
            <small>
              {setting?.enabled
                ? '活动进行中不可调整红包金额，请先下架活动'
                : '可设置 0.01 至 999 元，修改后只影响新填写的邀请码'}
            </small>
          </label>

          {can('INVITATION_CAMPAIGN_EDIT') && (
            <button className="primary" type="submit" disabled={saving}>
              {saving ? '保存中' : '保存设置'}
            </button>
          )}
        </section>

        <section className="invitation-campaign-stats">
          <StatCard
            icon={UsersRound}
            label="成功邀请"
            value={`${Number(setting?.successfulInvitations || 0)} 人`}
          />
          <StatCard
            icon={CircleDollarSign}
            label="累计发放红包"
            value={`¥${formatAmount(setting?.totalRewards || 0)}`}
          />
        </section>

        {setting?.updatedAt && (
          <p className="invitation-campaign-updated">
            最近更新：{new Date(setting.updatedAt).toLocaleString('zh-CN')}
            {setting.updatedBy ? ` · ${setting.updatedBy}` : ''}
          </p>
        )}
      </form>
    </>
  );
}

function StatCard({ icon: Icon, label, value }) {
  return (
    <div className="invitation-stat-card">
      <i><Icon /></i>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function validateAmount(value) {
  const text = String(value || '').trim();
  if (!/^\d{1,3}(\.\d{1,2})?$/.test(text)) {
    message.warning('红包金额必须在0.01至999元之间，最多两位小数');
    return null;
  }
  const amount = Number(text);
  if (amount < 0.01 || amount > 999) {
    message.warning('红包金额必须在0.01至999元之间');
    return null;
  }
  return amount;
}

function formatAmount(value) {
  const amount = Number(value || 0);
  return Number.isInteger(amount)
    ? String(amount)
    : amount.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
}
