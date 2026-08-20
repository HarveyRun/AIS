import { useEffect, useState } from 'react';
import { KeyRound, Pencil, Plus, Search, ShieldCheck, Trash2, X } from 'lucide-react';
import { adminApi, token } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './AdminUsersPage.css';

const PAGE_SIZE = 20;
const DEFAULT_PASSWORD = '123456abcAbc';
const EMPTY_FORM = { phone: '', displayName: '', password: '', status: 'ACTIVE', roleIds: [] };

export default function AdminUsersPage() {
  const { admin, can } = useAdminAccess();
  const [items, setItems] = useState([]);
  const [roles, setRoles] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [editor, setEditor] = useState(null);
  const [roleTarget, setRoleTarget] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [selectedRoleIds, setSelectedRoleIds] = useState([]);
  const [confirm, setConfirm] = useState(null);
  const [saving, setSaving] = useState(false);
  const isSuper = (admin?.roles || []).some((role) => role.code === 'SUPER_ADMIN');
  const ownLevel = Math.min(...(admin?.roles || []).map((role) => Number(role.level)), Number.MAX_SAFE_INTEGER);
  const targetLevel = (item) => Math.min(...(item.roles || []).map((role) => Number(role.level)), Number.MAX_SAFE_INTEGER);
  const canManage = (item) => isSuper || targetLevel(item) > ownLevel;

  const load = async (targetPage = page, query = appliedKeyword) => {
    try {
      const result = await adminApi.adminUsers({ keyword: query, page: targetPage, size: PAGE_SIZE });
      setItems(result.items || []);
      setTotal(result.total || 0);
      setPage(result.page || 0);
    } catch (error) {
      message.error(error.message);
    }
  };

  const loadRoles = async () => {
    if (!can('ADMIN_USER_CREATE') && !can('ADMIN_USER_ASSIGN_ROLE')) return;
    try {
      setRoles(await adminApi.adminRoleOptions());
    } catch (error) {
      message.error(error.message);
    }
  };

  useEffect(() => {
    load(0, '');
    loadRoles();
  }, []);

  const openCreate = () => {
    setForm({ ...EMPTY_FORM, password: DEFAULT_PASSWORD });
    setEditor({ mode: 'create' });
  };

  const openEdit = (item) => {
    setForm({
      phone: item.phone,
      displayName: item.displayName,
      password: '',
      status: item.status,
      roleIds: [],
    });
    setEditor({ mode: 'edit', item });
  };

  const saveUser = async (event) => {
    event.preventDefault();
    if (!form.phone.trim() || !form.displayName.trim()) {
      message.warning('请填写手机号和管理员名称');
      return;
    }
    if (editor.mode === 'create' && form.roleIds.length === 0) {
      message.warning('请至少选择一个角色');
      return;
    }
    try {
      setSaving(true);
      if (editor.mode === 'create') {
        await adminApi.createAdminUser(form);
        message.success('后台账号已创建');
      } else {
        await adminApi.updateAdminUser(editor.item.id, {
          phone: form.phone,
          displayName: form.displayName,
          status: form.status,
        });
        message.success('后台账号已更新');
      }
      setEditor(null);
      await load(editor.mode === 'create' ? 0 : page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const openRoles = (item) => {
    setRoleTarget(item);
    setSelectedRoleIds((item.roles || []).map((role) => Number(role.id)));
  };

  const saveRoles = async () => {
    if (!selectedRoleIds.length) return message.warning('请至少选择一个角色');
    try {
      setSaving(true);
      await adminApi.assignAdminUserRoles(roleTarget.id, selectedRoleIds);
      message.success('账号角色已更新，下次登录生效');
      setRoleTarget(null);
      await load(page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const runConfirmedAction = async () => {
    if (!confirm) return;
    try {
      setSaving(true);
      if (confirm.type === 'reset') {
        await adminApi.resetAdminPassword(confirm.item.id);
        if (confirm.item.id === admin?.id) {
          token.set('');
          window.dispatchEvent(new Event('shixianwen-admin-unauthorized'));
          return;
        }
        message.success(`密码已重置为 ${DEFAULT_PASSWORD}`);
      } else {
        await adminApi.deleteAdminUser(confirm.item.id);
        message.success('后台账号已删除');
      }
      setConfirm(null);
      await load(items.length === 1 && page > 0 ? page - 1 : page);
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
          <h1>后台账号</h1>
          <p>管理员资料与角色分开维护，角色决定账号能够查看和操作的功能</p>
        </div>
        {can('ADMIN_USER_CREATE') && (
          <button className="primary rbac-title-button" type="button" onClick={openCreate}>
            <Plus />新增管理员
          </button>
        )}
      </div>

      <form className="toolbar" onSubmit={(event) => {
        event.preventDefault();
        const query = keyword.trim();
        setAppliedKeyword(query);
        load(0, query);
      }}>
        <label><Search /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索手机号或管理员名称" /></label>
        <button type="submit">查询</button>
      </form>

      <section className="table-card">
        <table>
          <thead><tr><th>管理员</th><th>手机号</th><th>角色</th><th>状态</th><th>最近登录</th><th>操作</th></tr></thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td><b>{item.displayName}</b><small>#{item.id}{item.id === admin?.id ? ' · 当前账号' : ''}</small></td>
                <td>{item.phone}</td>
                <td><div className="rbac-tags">{(item.roles || []).map((role) => <span key={role.id}>{role.name}</span>)}</div></td>
                <td><span className={`status ${item.status === 'ACTIVE' ? 'active' : 'suspended'}`}>{item.status === 'ACTIVE' ? '启用' : '停用'}</span></td>
                <td>{formatDate(item.lastLoginAt)}</td>
                <td><div className="row-actions">
                  {can('ADMIN_USER_EDIT') && (item.id === admin?.id || canManage(item)) && <button className="plain" type="button" onClick={() => openEdit(item)}><Pencil />编辑</button>}
                  {can('ADMIN_USER_ASSIGN_ROLE') && item.id !== admin?.id && canManage(item) && <button className="plain" type="button" onClick={() => openRoles(item)}><ShieldCheck />角色</button>}
                  {can('ADMIN_USER_RESET_PASSWORD') && (item.id === admin?.id || canManage(item)) && <button className="plain" type="button" onClick={() => setConfirm({ type: 'reset', item })}><KeyRound />重置密码</button>}
                  {can('ADMIN_USER_DELETE') && item.id !== admin?.id && canManage(item) && <button className="danger" type="button" onClick={() => setConfirm({ type: 'delete', item })}><Trash2 />删除</button>}
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
        {!items.length && <div className="empty">暂无后台账号</div>}
      </section>
      <Pagination page={page} size={PAGE_SIZE} total={total} onChange={(next) => load(next)} />

      {editor && (
        <RbacDrawer description="维护后台登录账号" title={editor.mode === 'create' ? '新增管理员' : '编辑管理员'} onClose={() => !saving && setEditor(null)}>
          <form className="rbac-form" onSubmit={saveUser}>
            <label><span>手机号</span><input maxLength={20} value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} /></label>
            <label><span>管理员名称</span><input maxLength={60} value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /></label>
            {editor.mode === 'create' && <label><span>初始密码</span><input value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /><small>未修改时使用平台默认密码</small></label>}
            <label><span>状态</span><select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}><option value="ACTIVE">启用</option><option value="DISABLED">停用</option></select></label>
            {editor.mode === 'create' && <RoleChecks roles={roles} value={form.roleIds} onChange={(roleIds) => setForm({ ...form, roleIds })} />}
            <div className="rbac-form-actions"><button className="plain" type="button" onClick={() => setEditor(null)}>取消</button><button className="primary" disabled={saving}>保存</button></div>
          </form>
        </RbacDrawer>
      )}

      {roleTarget && (
        <RbacDrawer description="为该账号选择角色" title={`配置角色 · ${roleTarget.displayName}`} onClose={() => !saving && setRoleTarget(null)}>
          <p className="rbac-drawer-intro">保存后，该账号将自动获得所选角色包含的全部权限。</p>
          <RoleChecks roles={roles} value={selectedRoleIds} onChange={setSelectedRoleIds} />
          <div className="rbac-form-actions"><button className="plain" type="button" onClick={() => setRoleTarget(null)}>取消</button><button className="primary" type="button" disabled={saving} onClick={saveRoles}>保存角色</button></div>
        </RbacDrawer>
      )}

      <ConfirmDialog
        open={Boolean(confirm)}
        title={confirm?.type === 'delete' ? '删除后台账号' : '重置登录密码'}
        message={confirm?.type === 'delete' ? `确认删除“${confirm?.item?.displayName || ''}”吗？该操作为逻辑删除。` : `确认将“${confirm?.item?.displayName || ''}”的密码重置为 ${DEFAULT_PASSWORD} 吗？`}
        confirmText={confirm?.type === 'delete' ? '确认删除' : '确认重置'}
        danger
        busy={saving}
        onCancel={() => !saving && setConfirm(null)}
        onConfirm={runConfirmedAction}
      />
    </>
  );
}

function RoleChecks({ roles, value, onChange }) {
  return <fieldset className="rbac-checks"><legend>角色</legend>{roles.map((role) => {
    const checked = value.includes(Number(role.id));
    return <label key={role.id}><input type="checkbox" checked={checked} onChange={() => onChange(checked ? value.filter((id) => id !== Number(role.id)) : [...value, Number(role.id)])} /><span><b>{role.name}</b><small>{role.code}</small></span></label>;
  })}</fieldset>;
}

export function RbacDrawer({ title, description, onClose, children }) {
  return (
    <>
      <div className="modal-mask" onClick={onClose} />
      <section className="detail-modal rbac-drawer" role="dialog" aria-modal="true">
        <header>
          <div>
            <h2>{title}</h2>
            <p>{description}</p>
          </div>
          <button type="button" aria-label="关闭" onClick={onClose}>
            <X />
          </button>
        </header>
        <div className="rbac-drawer-content">{children}</div>
      </section>
    </>
  );
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '尚未登录';
}
