import { useEffect, useState } from 'react';
import {
  Check,
  Copy,
  KeyRound,
  MessageSquareOff,
  Pencil,
  Plus,
  Smartphone,
  Trash2,
  X,
} from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './AppTestAccountPage.css';

const EMPTY_FORM = {
  phone: '',
  verificationCode: '',
  enabled: true,
};
const PAGE_SIZE = 20;

export default function AppTestAccountPage() {
  const [accounts, setAccounts] = useState([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const loadAccounts = async (targetPage = page) => {
    try {
      const data = await adminApi.appTestAccounts(targetPage, PAGE_SIZE);
      setAccounts(data.items || []);
      setTotal(data.total || 0);
      setPage(data.page || 0);
    } catch (error) {
      message.error(error.message);
    }
  };

  useEffect(() => {
    loadAccounts(0);
  }, []);

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setEditorOpen(true);
  };

  const openEdit = (account) => {
    setEditingId(account.id);
    setForm({
      phone: account.phone,
      verificationCode: account.verificationCode,
      enabled: account.enabled,
    });
    setEditorOpen(true);
  };

  const closeEditor = () => {
    if (saving) return;
    setEditorOpen(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const validate = () => {
    if (!/^1\d{10}$/.test(form.phone)) {
      message.warning('请输入正确的11位手机号');
      return false;
    }
    if (!/^\d{4}$/.test(form.verificationCode)) {
      message.warning('验证码必须是4位数字');
      return false;
    }
    return true;
  };

  const save = async (event) => {
    event.preventDefault();
    if (!validate() || saving) return;
    setSaving(true);
    try {
      if (editingId) {
        await adminApi.updateAppTestAccount(editingId, form);
        message.success('App超级账号已更新');
      } else {
        await adminApi.createAppTestAccount(form);
        message.success('App超级账号已新增');
      }
      setEditorOpen(false);
      setEditingId(null);
      setForm(EMPTY_FORM);
      await loadAccounts(editingId ? page : 0);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const toggleEnabled = async (account) => {
    try {
      await adminApi.updateAppTestAccount(account.id, {
        phone: account.phone,
        verificationCode: account.verificationCode,
        enabled: !account.enabled,
      });
      await loadAccounts(page);
    } catch (error) {
      message.error(error.message);
    }
  };

  const remove = async () => {
    if (!deleteTarget || deleting) return;
    setDeleting(true);
    try {
      await adminApi.deleteAppTestAccount(deleteTarget.id);
      setDeleteTarget(null);
      message.success('App超级账号已删除');
      const nextPage = accounts.length === 1 && page > 0 ? page - 1 : page;
      await loadAccounts(nextPage);
    } catch (error) {
      message.error(error.message);
    } finally {
      setDeleting(false);
    }
  };

  const copy = async (value, label) => {
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
      message.success(`${label}已复制`);
    } catch {
      message.error('复制失败，请手动复制');
    }
  };

  return (
    <>
      <div className="page-title app-test-account-title">
        <div>
          <h1>App超级账号</h1>
          <p>维护 App 测试与商店审核使用的固定登录账号</p>
        </div>
        <button className="primary" type="button" onClick={openCreate}>
          <Plus />
          新增账号
        </button>
      </div>

      <div className="app-test-account-layout">
        <section className="app-test-account-card app-test-account-list-card">
          <header>
            <span className="app-test-account-icon">
              <Smartphone />
            </span>
            <div>
              <h2>账号列表</h2>
              <p>共 {accounts.length} 个，启停和验证码均可单独维护。</p>
            </div>
          </header>

          <div className="app-test-account-table-wrap">
            <table>
              <thead>
                <tr>
                  <th>手机号</th>
                  <th>固定验证码</th>
                  <th>状态</th>
                  <th>最近更新</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {accounts.map((account) => (
                  <tr key={account.id}>
                    <td>
                      <b>{account.phone}</b>
                    </td>
                    <td>
                      <button
                        className="app-test-account-copy"
                        type="button"
                        onClick={() => copy(account.verificationCode, '验证码')}
                      >
                        {account.verificationCode}
                        <Copy />
                      </button>
                    </td>
                    <td>
                      <button
                        type="button"
                        className={`app-test-account-status ${account.enabled ? 'enabled' : ''}`}
                        onClick={() => toggleEnabled(account)}
                      >
                        {account.enabled ? '已启用' : '已停用'}
                      </button>
                    </td>
                    <td>
                      <span className="app-test-account-updated">
                        {account.updatedBy || '管理员'}
                        <small>{new Date(account.updatedAt).toLocaleString()}</small>
                      </span>
                    </td>
                    <td>
                      <div className="table-actions">
                        <button type="button" onClick={() => openEdit(account)}>
                          <Pencil />
                          编辑
                        </button>
                        <button
                          className="danger"
                          type="button"
                          onClick={() => setDeleteTarget(account)}
                        >
                          <Trash2 />
                          删除
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!accounts.length && <div className="empty">暂无App超级账号</div>}
          </div>
          <Pagination page={page} size={PAGE_SIZE} total={total} onChange={loadAccounts} />
        </section>

        <aside className="app-test-account-notes">
          <h2>使用范围</h2>
          <ul>
            <li>
              <Check />
              <div>
                <b>平台功能验证</b>
                <span>稳定登录指定 App 账号，检查完整业务流程。</span>
              </div>
            </li>
            <li>
              <MessageSquareOff />
              <div>
                <b>减少短信消耗</b>
                <span>启用的账号不会调用短信渠道。</span>
              </div>
            </li>
            <li>
              <Smartphone />
              <div>
                <b>应用商店审核</b>
                <span>可分别准备多个审核账号和验证码。</span>
              </div>
            </li>
          </ul>
          <p>这些账号在 App 内仍是普通用户，不具备后台管理权限。</p>
        </aside>
      </div>

      {editorOpen && (
        <>
          <button className="modal-mask" type="button" aria-label="关闭" onClick={closeEditor} />
          <section className="detail-modal app-test-account-editor" role="dialog" aria-modal="true">
            <header>
              <div>
                <h2>{editingId ? '编辑App超级账号' : '新增App超级账号'}</h2>
                <p>每个手机号对应一套独立的固定验证码。</p>
              </div>
              <button type="button" aria-label="关闭" onClick={closeEditor}>
                <X />
              </button>
            </header>
            <form onSubmit={save}>
              <label>
                <span>固定手机号</span>
                <div className="app-test-account-input">
                  <Smartphone />
                  <input
                    autoFocus
                    inputMode="numeric"
                    maxLength={11}
                    value={form.phone}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        phone: event.target.value.replace(/\D/g, '').slice(0, 11),
                      }))
                    }
                    placeholder="请输入11位手机号"
                  />
                </div>
              </label>
              <label>
                <span>固定验证码</span>
                <div className="app-test-account-input">
                  <KeyRound />
                  <input
                    inputMode="numeric"
                    maxLength={4}
                    value={form.verificationCode}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        verificationCode: event.target.value.replace(/\D/g, '').slice(0, 4),
                      }))
                    }
                    placeholder="请输入4位数字"
                  />
                </div>
              </label>
              <div className="app-test-account-switch-row">
                <div>
                  <b>允许登录 App</b>
                  <span>关闭后，该手机号恢复普通验证码登录。</span>
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={form.enabled}
                  className={`app-test-account-switch ${form.enabled ? 'enabled' : ''}`}
                  onClick={() => setForm((current) => ({ ...current, enabled: !current.enabled }))}
                >
                  <i />
                </button>
              </div>
              <footer>
                <button className="plain" type="button" disabled={saving} onClick={closeEditor}>
                  取消
                </button>
                <button className="primary" type="submit" disabled={saving}>
                  {saving ? '保存中…' : '保存'}
                </button>
              </footer>
            </form>
          </section>
        </>
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="删除App超级账号"
        message={`确定删除手机号“${deleteTarget?.phone || ''}”吗？删除后将不能再使用固定验证码登录。`}
        confirmText="删除"
        danger
        busy={deleting}
        onConfirm={remove}
        onCancel={() => setDeleteTarget(null)}
      />
    </>
  );
}
