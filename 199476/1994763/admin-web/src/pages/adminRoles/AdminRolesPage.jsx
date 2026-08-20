import { useEffect, useMemo, useState } from 'react';
import { KeyRound, Pencil, Plus, Search, Trash2 } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import { RbacDrawer } from '../adminUsers/AdminUsersPage.jsx';
import '../shared/Page.css';
import '../adminUsers/AdminUsersPage.css';
import './AdminRolesPage.css';

const PAGE_SIZE = 20;
const EMPTY_FORM = { code: '', name: '', level: 100, description: '', active: true, permissionIds: [] };

export default function AdminRolesPage() {
  const { admin, can } = useAdminAccess();
  const [items, setItems] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [editor, setEditor] = useState(null);
  const [permissionTarget, setPermissionTarget] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [selectedIds, setSelectedIds] = useState([]);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [saving, setSaving] = useState(false);
  const isSuper = (admin?.roles || []).some((role) => role.code === 'SUPER_ADMIN');
  const ownLevel = Math.min(...(admin?.roles || []).map((role) => Number(role.level)), Number.MAX_SAFE_INTEGER);
  const manageable = (item) => isSuper || Number(item.level) > ownLevel;

  const load = async (targetPage = page, query = appliedKeyword) => {
    try {
      const result = await adminApi.adminRoles({ keyword: query, page: targetPage, size: PAGE_SIZE });
      setItems(result.items || []);
      setTotal(result.total || 0);
      setPage(result.page || 0);
    } catch (error) {
      message.error(error.message);
    }
  };

  const loadPermissions = async () => {
    if (!can('ROLE_CREATE') && !can('ROLE_ASSIGN_PERMISSION')) return;
    try {
      setPermissions(await adminApi.adminPermissionOptions());
    } catch (error) {
      message.error(error.message);
    }
  };

  useEffect(() => {
    load(0, '');
    loadPermissions();
  }, []);

  const groups = useMemo(() => permissions.reduce((result, permission) => {
    const key = permission.moduleName || '其它';
    if (!result[key]) result[key] = [];
    result[key].push(permission);
    return result;
  }, {}), [permissions]);

  const openCreate = () => {
    setForm(EMPTY_FORM);
    setEditor({ mode: 'create' });
  };

  const openEdit = (item) => {
    setForm({ code: item.code, name: item.name, level: item.level, description: item.description || '', active: item.active, permissionIds: [] });
    setEditor({ mode: 'edit', item });
  };

  const saveRole = async (event) => {
    event.preventDefault();
    if (!form.code.trim() || !form.name.trim()) return message.warning('请填写角色编码和名称');
    try {
      setSaving(true);
      if (editor.mode === 'create') {
        await adminApi.createAdminRole(form);
        message.success('角色已创建');
      } else {
        await adminApi.updateAdminRole(editor.item.id, form);
        message.success('角色资料已更新');
      }
      setEditor(null);
      await load(editor.mode === 'create' ? 0 : page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const openPermissions = (item) => {
    setPermissionTarget(item);
    setSelectedIds((item.permissionIds || []).map(Number));
  };

  const toggleModule = (modulePermissions) => {
    const moduleIds = modulePermissions.map((item) => Number(item.id));
    const allSelected = moduleIds.every((id) => selectedIds.includes(id));
    setSelectedIds(allSelected
      ? selectedIds.filter((id) => !moduleIds.includes(id))
      : [...new Set([...selectedIds, ...moduleIds])]);
  };

  const savePermissions = async () => {
    try {
      setSaving(true);
      await adminApi.assignRolePermissions(permissionTarget.id, selectedIds);
      message.success('角色权限已更新，关联账号需重新登录');
      setPermissionTarget(null);
      await load(page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    try {
      setSaving(true);
      await adminApi.deleteAdminRole(deleteTarget.id);
      setDeleteTarget(null);
      message.success('角色已删除');
      await load(items.length === 1 && page > 0 ? page - 1 : page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  return <>
    <div className="page-title"><div><h1>角色管理</h1><p>角色是一组权限模板，管理员选择角色后自动获得对应权限</p></div>{can('ROLE_CREATE') && <button className="primary rbac-title-button" onClick={openCreate}><Plus />新增角色</button>}</div>
    <form className="toolbar" onSubmit={(event) => { event.preventDefault(); const query = keyword.trim(); setAppliedKeyword(query); load(0, query); }}><label><Search /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索角色名称或编码" /></label><button>查询</button></form>
    <section className="table-card"><table><thead><tr><th>角色</th><th>级别</th><th>说明</th><th>账号数</th><th>权限数</th><th>状态</th><th>操作</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}>
      <td><b>{item.name}</b><small>{item.code}{item.systemRole ? ' · 系统预置' : ''}</small></td>
      <td>级别 {item.level}</td><td className="long-cell">{item.description || '-'}</td><td>{item.userCount}</td><td>{item.code === 'SUPER_ADMIN' ? '全部' : (item.permissionIds || []).length}</td>
      <td><span className={`status ${item.active ? 'active' : 'suspended'}`}>{item.active ? '启用' : '停用'}</span></td>
      <td><div className="row-actions">{can('ROLE_EDIT') && manageable(item) && <button className="plain" onClick={() => openEdit(item)}><Pencil />编辑</button>}{can('ROLE_ASSIGN_PERMISSION') && manageable(item) && item.code !== 'SUPER_ADMIN' && <button className="plain" onClick={() => openPermissions(item)}><KeyRound />配置权限</button>}{can('ROLE_DELETE') && manageable(item) && !item.systemRole && <button className="danger" onClick={() => setDeleteTarget(item)}><Trash2 />删除</button>}</div></td>
    </tr>)}</tbody></table>{!items.length && <div className="empty">暂无角色</div>}</section>
    <Pagination page={page} size={PAGE_SIZE} total={total} onChange={(next) => load(next)} />

    {editor && <RbacDrawer description="维护角色名称、级别和状态" title={editor.mode === 'create' ? '新增角色' : '编辑角色'} onClose={() => !saving && setEditor(null)}><form className="rbac-form" onSubmit={saveRole}>
      <label><span>角色编码</span><input value={form.code} disabled={Boolean(editor.item?.systemRole)} onChange={(event) => setForm({ ...form, code: event.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })} placeholder="例如 CONTENT_REVIEWER" /></label>
      <label><span>角色名称</span><input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
      <label><span>角色级别</span><input type="number" min="1" max="9999" value={form.level} onChange={(event) => setForm({ ...form, level: Number(event.target.value) })} /><small>数字越小级别越高，仅用于排序和识别，不自动扩大权限。</small></label>
      <label><span>角色说明</span><textarea maxLength={300} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
      <label><span>状态</span><select value={form.active ? 'true' : 'false'} disabled={editor.item?.code === 'SUPER_ADMIN'} onChange={(event) => setForm({ ...form, active: event.target.value === 'true' })}><option value="true">启用</option><option value="false">停用</option></select></label>
      {editor.mode === 'create' && <PermissionGroups groups={groups} value={form.permissionIds} setValue={(permissionIds) => setForm({ ...form, permissionIds })} toggleModule={(list) => { const ids = list.map((item) => Number(item.id)); const all = ids.every((id) => form.permissionIds.includes(id)); setForm({ ...form, permissionIds: all ? form.permissionIds.filter((id) => !ids.includes(id)) : [...new Set([...form.permissionIds, ...ids])] }); }} />}
      <div className="rbac-form-actions"><button className="plain" type="button" onClick={() => setEditor(null)}>取消</button><button className="primary" disabled={saving}>保存</button></div>
    </form></RbacDrawer>}

    {permissionTarget && <RbacDrawer description="勾选该角色拥有的页面与操作权限" title={`配置权限 · ${permissionTarget.name}`} onClose={() => !saving && setPermissionTarget(null)}><p className="rbac-drawer-intro">保存后，拥有该角色的管理员将自动获得所选权限。</p><PermissionGroups groups={groups} value={selectedIds} setValue={setSelectedIds} toggleModule={toggleModule} /><div className="rbac-form-actions"><button className="plain" onClick={() => setPermissionTarget(null)}>取消</button><button className="primary" disabled={saving} onClick={savePermissions}>保存权限</button></div></RbacDrawer>}
    <ConfirmDialog open={Boolean(deleteTarget)} title="删除角色" message={`确认删除“${deleteTarget?.name || ''}”吗？仍有关联账号时将无法删除。`} confirmText="确认删除" danger busy={saving} onCancel={() => !saving && setDeleteTarget(null)} onConfirm={remove} />
  </>;
}

function PermissionGroups({ groups, value, setValue, toggleModule }) {
  return <div className="permission-groups">{Object.entries(groups).map(([moduleName, items]) => {
    const allSelected = items.every((item) => value.includes(Number(item.id)));
    return <section key={moduleName}><header><b>{moduleName}</b><button type="button" onClick={() => toggleModule(items)}>{allSelected ? '取消全选' : '全选'}</button></header><div>{items.map((item) => {
      const id = Number(item.id); const checked = value.includes(id);
      return <label key={id}><input type="checkbox" checked={checked} onChange={() => setValue(checked ? value.filter((current) => current !== id) : [...value, id])} /><span>{item.actionName}</span><small>{item.name}</small></label>;
    })}</div></section>;
  })}</div>;
}
