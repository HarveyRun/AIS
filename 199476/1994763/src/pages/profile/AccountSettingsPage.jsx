import { useState } from 'react';
import { Camera, Trash2, X } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import UserAvatar from '../../components/profile/UserAvatar.jsx';
import './AccountSettingsPage.css';
import { api } from '../../api/http.js';

export default function AccountSettingsPage({
  go,
  notify,
  userProfile,
  setUserProfile,
  conversations,
  balance,
  frozenAmount,
  deleteAccount,
}) {
  const [name, setName] = useState(userProfile.name?.trim() || '');
  const [avatar, setAvatar] = useState(userProfile.avatar || '');
  const [profileError, setProfileError] = useState('');
  const [avatarFile, setAvatarFile] = useState(null);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState('');
  const activeInquiryCount = conversations.filter((conversation) =>
    ['pending', 'active', 'awaiting_confirmation'].includes(conversation.inquiryStatus),
  ).length;
  const canDeleteAccount = balance <= 0 && frozenAmount <= 0 && activeInquiryCount === 0;

  const selectAvatar = (file) => {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      setProfileError('请选择图片文件');
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setProfileError('头像图片不能超过2MB');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      setAvatar(reader.result);
      setProfileError('');
      setAvatarFile(file);
    };
    reader.onerror = () => setProfileError('头像读取失败，请重新选择');
    reader.readAsDataURL(file);
  };

  const saveProfile = async () => {
    const nextName = name.trim();
    try {
      const updated = await api.updateProfile({ nickname: nextName, avatarUrl: avatarFile ? userProfile.avatar : avatar });
      const avatarResult = avatarFile ? await api.updateAvatar(avatarFile) : updated;
      setUserProfile((current) => ({ ...current, name: avatarResult.nickname || '', avatar: avatarResult.avatarUrl || '' }));
      setAvatarFile(null);
      notify('个人信息已保存');
    } catch (requestError) {
      setProfileError(requestError.message);
    }
  };

  return (
    <Page title="账号设置" back={() => go('profile')} className="account-settings-page">
      <section className="account-profile-card">
        <div className="account-avatar-setting">
          <UserAvatar src={avatar} uid={userProfile.uid} name={name} />
          <div>
            <label>
              <Camera />
              更换头像
              <input
                type="file"
                hidden
                accept="image/*"
                onChange={(event) => selectAvatar(event.target.files?.[0])}
              />
            </label>
            {avatar && (
              <button type="button" onClick={() => setAvatar('')}>
                使用默认头像
              </button>
            )}
          </div>
        </div>

        <label className="account-name-setting">
          <span>昵称</span>
          <div>
            <input
              value={name}
              maxLength={12}
              onChange={(event) => {
                setName(event.target.value);
                setProfileError('');
              }}
              placeholder={`UID ${userProfile.uid}`}
            />
            <small>{name.length}/12</small>
          </div>
        </label>

        {profileError && <p className="account-profile-error">{profileError}</p>}
        <button className="account-profile-save" type="button" onClick={saveProfile}>
          保存
        </button>
      </section>

      <section className="account-danger-zone">
        <div>
          <i>
            <Trash2 />
          </i>
          <span>
            <b>注销账号</b>
            <small>清除账号资料和相关记录</small>
          </span>
        </div>
        <button
          type="button"
          onClick={() => {
            setDeleteConfirm('');
            setDeleteOpen(true);
          }}
        >
          申请注销
        </button>
      </section>

      {deleteOpen && (
        <>
          <button className="sheet-mask" type="button" onClick={() => setDeleteOpen(false)} />
          <section className="account-delete-sheet">
            <header>
              <div>
                <h2>注销账号</h2>
                <p>注销后，个人资料、认证和交流记录将被清除。</p>
              </div>
              <button type="button" onClick={() => setDeleteOpen(false)} aria-label="关闭">
                <X />
              </button>
            </header>

            {!canDeleteAccount && (
              <div className="account-delete-blocked">
                <b>当前暂时不能注销</b>
                {balance > 0 && <span>账户仍有可用余额 ¥{balance.toFixed(2)}</span>}
                {frozenAmount > 0 && <span>仍有 ¥{frozenAmount.toFixed(2)} 正在冻结</span>}
                {activeInquiryCount > 0 && <span>仍有 {activeInquiryCount} 个询问尚未结束</span>}
              </div>
            )}

            {canDeleteAccount && (
              <label>
                <span>输入“注销账号”确认</span>
                <input
                  value={deleteConfirm}
                  onChange={(event) => setDeleteConfirm(event.target.value)}
                  placeholder="注销账号"
                />
              </label>
            )}

            <button
              className="account-delete-submit"
              type="button"
              disabled={!canDeleteAccount || deleteConfirm !== '注销账号'}
              onClick={deleteAccount}
            >
              确认注销
            </button>
          </section>
        </>
      )}
    </Page>
  );
}
