import { useEffect, useState } from 'react';
import { Pencil, Plus, Search, Trash2 } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import { RbacDrawer } from '../adminUsers/AdminUsersPage.jsx';
import '../shared/Page.css';
import '../adminUsers/AdminUsersPage.css';

const PAGE_SIZE = 20;
const EMPTY_FORM = { code: '', name: '', moduleName: '', actionName: '', sortOrder: 0, active: true };

export default function AdminPermissionsPage() {
  const { can } = useAdminAccess();
  const [items, setItems] = useState([]);
  const [modules, setModules] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [moduleName, setModuleName] = useState('');
  const [filters, setFilters] = useState({ keyword: '', module: '' });
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [editor, setEditor] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [saving, setSaving] = useState(false);

  const load = async (targetPage = page, query = filters) => {
    try {
      const result = await adminApi.adminPermissions({ ...query, page: targetPage, size: PAGE_SIZE });
      setItems(result.items || []);
      setTotal(result.total || 0);
      setPage(result.page || 0);
    } catch (error) {
      message.error(error.message);
    }
  };

  useEffect(() => {
    load(0, { keyword: '', module: '' });
    adminApi.adminPermissionModules().then(setModules).catch((error) => message.error(error.message));
  }, []);

  const search = (event) => {
    event.preventDefault();
    const next = { keyword: keyword.trim(), module: moduleName };
    setFilters(next);
    load(0, next);
  };

  const openCreate = () => {
    setForm(EMPTY_FORM);
    setEditor({ mode: 'create' });
  };

  const openEdit = (item) => {
    setForm({ code: item.code, name: item.name, moduleName: item.moduleName, actionName: item.actionName, sortOrder: item.sortOrder, active: item.active });
    setEditor({ mode: 'edit', item });
  };

  const save = async (event) => {
    event.preventDefault();
    if (!form.code.trim() || !form.name.trim() || !form.moduleName.trim() || !form.actionName.trim()) {
      return message.warning('请填写完整的权限信息');
    }
    try {
      setSaving(true);
      if (editor.mode === 'create') {
        await adminApi.createAdminPermission(form);
        message.success('权限已创建');
      } else {
        await adminApi.updateAdminPermission(editor.item.id, form);
        message.success('权限已更新');
      }
      setEditor(null);
      await load(editor.mode === 'create' ? 0 : page);
      setModules(await adminApi.adminPermissionModules());
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    try {
      setSaving(true);
      await adminApi.deleteAdminPermission(deleteTarget.id);
      setDeleteTarget(null);
      message.success('权限已删除');
      await load(items.length === 1 && page > 0 ? page - 1 : page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  return <>
    <div className="page-title"><div><h1>权限管理</h1><p>权限精确到页面与操作按钮，系统权限编码受保护</p></div>{can('PERMISSION_CREATE') && <button className="primary rbac-title-button" onClick={openCreate}><Plus />新增权限</button>}</div>
    <form className="toolbar" onSubmit={search}>
      <label><Search /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索权限名称或编码" /></label>
      <select value={moduleName} onChange={(event) => setModuleName(event.target.value)}><option value="">全部模块</option>{modules.map((item) => <option key={item} value={item}>{item}</option>)}</select>
      <button>查询</button>
    </form>
    <section className="table-card"><table><thead><tr><th>权限</th><th>模块</th><th>操作级别</th><th>排序</th><th>类型</th><th>状态</th><th>操作</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}>
      <td><b>{item.name}</b><small>{item.code}</small></td><td>{item.moduleName}</td><td>{item.actionName}</td><td>{item.sortOrder}</td><td>{item.systemPermission ? '系统权限' : '自定义权限'}</td>
      <td><span className={`status ${item.active ? 'active' : 'suspended'}`}>{item.active ? '启用' : '停用'}</span></td>
      <td><div className="row-actions">{can('PERMISSION_EDIT') && <button className="plain" onClick={() => openEdit(item)}><Pencil />编辑</button>}{can('PERMISSION_DELETE') && !item.systemPermission && <button className="danger" onClick={() => setDeleteTarget(item)}><Trash2 />删除</button>}</div></td>
    </tr>)}</tbody></table>{!items.length && <div className="empty">暂无权限</div>}</section>
    <Pagination page={page} size={PAGE_SIZE} total={total} onChange={(next) => load(next)} />

    {editor && <RbacDrawer description="维护按钮级权限信息" title={editor.mode === 'create' ? '新增权限' : '编辑权限'} onClose={() => !saving && setEditor(null)}><form className="rbac-form" onSubmit={save}>
      <label><span>权限编码</span><input value={form.code} disabled={Boolean(editor.item?.systemPermission)} onChange={(event) => setForm({ ...form, code: event.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })} placeholder="例如 ARTICLE_APPROVE" /></label>
      <label><span>权限名称</span><input maxLength={100} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
      <label><span>所属模块</span><input maxLength={60} list="permission-modules" value={form.moduleName} onChange={(event) => setForm({ ...form, moduleName: event.target.value })} /><datalist id="permission-modules">{modules.map((item) => <option key={item} value={item} />)}</datalist></label>
      <label><span>按钮或操作</span><input maxLength={60} value={form.actionName} onChange={(event) => setForm({ ...form, actionName: event.target.value })} placeholder="例如查看、新增、审核、删除" /></label>
      <label><span>排序</span><input type="number" value={form.sortOrder} onChange={(event) => setForm({ ...form, sortOrder: Number(event.target.value) })} /></label>
      <label><span>状态</span><select value={form.active ? 'true' : 'false'} onChange={(event) => setForm({ ...form, active: event.target.value === 'true' })}><option value="true">启用</option><option value="false">停用</option></select></label>
      <div className="rbac-form-actions"><button className="plain" type="button" onClick={() => setEditor(null)}>取消</button><button className="primary" disabled={saving}>保存</button></div>
    </form></RbacDrawer>}
    <ConfirmDialog open={Boolean(deleteTarget)} title="删除权限" message={`确认删除“${deleteTarget?.name || ''}”吗？相关角色将同时失去该权限。`} confirmText="确认删除" danger busy={saving} onCancel={() => !saving && setDeleteTarget(null)} onConfirm={remove} />
  </>;
}
