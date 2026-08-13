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
  deleteAccount,
}) {
  const [name, setName] = useState(userProfile.name?.trim() || '');
  const [avatar, setAvatar] = useState(userProfile.avatar || '');
  const [avatarFile, setAvatarFile] = useState(null);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState('');

  const selectAvatar = (file) => {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      notify('请选择图片文件', 'warning');
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      notify('头像图片不能超过2MB', 'warning');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      setAvatar(reader.result);
      setAvatarFile(file);
    };
    reader.onerror = () => notify('头像读取失败，请重新选择', 'error');
    reader.readAsDataURL(file);
  };

  const saveProfile = async () => {
    const nextName = name.trim();
    try {
      await api.updateProfile({ nickname: nextName, avatarUrl: avatarFile ? userProfile.avatar : avatar });
      if (avatarFile) await api.updateAvatar(avatarFile);
      const latest = await api.me();
      setUserProfile({ id: latest.id, name: latest.nickname || '', uid: latest.uid, phone: latest.phone, avatar: latest.avatarUrl || '' });
      setAvatarFile(null);
      notify('个人信息已保存', 'success');
    } catch (requestError) {
      notify(requestError.message, 'error');
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
              }}
              placeholder={`UID ${userProfile.uid}`}
            />
            <small>{name.length}/12</small>
          </div>
        </label>

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

            <label>
              <span>输入“注销账号”确认</span>
              <input
                value={deleteConfirm}
                onChange={(event) => setDeleteConfirm(event.target.value)}
                placeholder="注销账号"
              />
            </label>

            <button
              className="account-delete-submit"
              type="button"
              disabled={deleteConfirm !== '注销账号'}
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
