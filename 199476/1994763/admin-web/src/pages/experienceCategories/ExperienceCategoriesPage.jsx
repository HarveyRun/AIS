import { useEffect, useMemo, useState } from 'react';
import { Pencil, Plus, Power, PowerOff, Trash2, X } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './ExperienceCategoriesPage.css';

const emptyForm = { name: '', sortOrder: 0, enabled: true, parentId: null, targetCategoryId: null };

export default function ExperienceCategoriesPage() {
  const { can } = useAdminAccess();
  const [items, setItems] = useState([]);
  const [selectedParentId, setSelectedParentId] = useState(null);
  const [editor, setEditor] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = async () => {
    try { setItems(await adminApi.experienceCategories()); }
    catch (error) { message.error(error.message); }
  };
  useEffect(() => { load(); }, []);

  const parents = useMemo(() => items.filter((item) => item.parentId == null), [items]);
  const activeParentId = parents.some((item) => item.id === selectedParentId)
    ? selectedParentId : parents[0]?.id ?? null;
  const selectedParent = parents.find((item) => item.id === activeParentId);
  const children = items.filter((item) => item.parentId === activeParentId);
  const editorParent = parents.find((item) => item.id === form.parentId);
  const recommendationEditor = Boolean(editorParent?.recommendationGroup);
  const realLeaves = items.filter((item) => item.parentId != null && item.enabled &&
    parents.some((parent) => parent.id === item.parentId && parent.enabled && !parent.recommendationGroup));

  const openEditor = (item = null, parentId = null) => {
    setEditor(item || 'new');
    setForm(item ? {
      name: item.name, sortOrder: item.sortOrder,
      enabled: item.enabled, parentId: item.parentId, targetCategoryId: item.targetCategoryId,
    } : { ...emptyForm, parentId, sortOrder: (parentId == null ? parents : children).at(-1)?.sortOrder + 10 || 10 });
  };

  const save = async (event) => {
    event.preventDefault();
    if (recommendationEditor && !form.targetCategoryId) { message.warning('请选择真实二级分类'); return; }
    if (!recommendationEditor && !form.name.trim()) { message.warning('请输入分类名称'); return; }
    setBusy(true);
    try {
      const body = { ...form, name: recommendationEditor
        ? realLeaves.find((item) => item.id === Number(form.targetCategoryId))?.name || ''
        : form.name.trim(), targetCategoryId: recommendationEditor ? Number(form.targetCategoryId) : null,
        sortOrder: Number(form.sortOrder) };
      if (editor?.id) await adminApi.updateExperienceCategory(editor.id, body);
      else await adminApi.createExperienceCategory(body);
      message.success('已保存');
      setEditor(null);
      await load();
    } catch (error) { message.error(error.message); }
    finally { setBusy(false); }
  };

  const toggle = async (item) => {
    try {
      await adminApi.setExperienceCategoryEnabled(item.id, !item.enabled);
      await load();
    } catch (error) { message.error(error.message); }
  };

  const remove = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      await adminApi.deleteExperienceCategory(deleteTarget.id);
      setDeleteTarget(null);
      message.success('已删除');
      await load();
    } catch (error) { message.error(error.message); }
    finally { setBusy(false); }
  };

  const actions = (item) => <div className="experience-category-actions">
    {can('EXPERIENCE_CATEGORY_EDIT') && <button type="button" onClick={() => openEditor(item)}><Pencil />编辑</button>}
    {can('EXPERIENCE_CATEGORY_EDIT') && <button type="button" onClick={() => toggle(item)}>
      {item.enabled ? <PowerOff /> : <Power />}{item.enabled ? '停用' : '启用'}
    </button>}
    {can('EXPERIENCE_CATEGORY_DELETE') && <button className="danger" type="button" onClick={() => setDeleteTarget(item)}><Trash2 />删除</button>}
  </div>;

  return <div className="experience-category-page">
    <div className="page-title"><div><h1>经历分类</h1><p>经历归属真实二级分类；“常见推荐”仅引用已有分类，不会产生另一套经历分类。</p></div></div>
    <div className="experience-category-grid">
      <section className="experience-category-parents">
        <header><h2>一级分类</h2>{can('EXPERIENCE_CATEGORY_CREATE') && <button type="button" onClick={() => openEditor()}><Plus />新增</button>}</header>
        <div className="experience-category-parent-list">
          {parents.map((item) => <div key={item.id} className={item.id === activeParentId ? 'active' : ''}>
            <button className="experience-category-parent-name" type="button" onClick={() => setSelectedParentId(item.id)}>
              <span>{item.name}</span><small>{item.enabled ? '启用' : '停用'}</small>
            </button>
            {actions(item)}
          </div>)}
        </div>
      </section>
      <section className="experience-category-children">
        <header><div><h2>{selectedParent?.recommendationGroup ? '推荐入口' : '二级分类'}</h2><p>{selectedParent?.recommendationGroup ? '引用真实分类；点击后筛选同一批经历' : selectedParent ? `所属：${selectedParent.name}` : '请先新增一级分类'}</p></div>
          {activeParentId != null && can('EXPERIENCE_CATEGORY_CREATE') && <button type="button" onClick={() => openEditor(null, activeParentId)}><Plus />{selectedParent?.recommendationGroup ? '添加推荐' : '新增二级分类'}</button>}
        </header>
        <div className="experience-category-table-wrap"><table><thead><tr><th>排序</th><th>名称</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>{children.map((item) => <tr key={item.id}><td>{item.sortOrder}</td><td><strong>{item.name}</strong></td>
            <td>{item.enabled && selectedParent?.enabled ? '启用' : '停用'}</td><td>{actions(item)}</td></tr>)}</tbody>
        </table>{children.length === 0 && <div className="empty">暂无二级分类</div>}</div>
      </section>
    </div>
    {editor && <div className="experience-category-dialog-layer" role="presentation">
      <button className="experience-category-dialog-mask" type="button" aria-label="关闭" onClick={() => !busy && setEditor(null)} />
      <section className="experience-category-editor" role="dialog" aria-modal="true">
        <header><h2>{editor === 'new' ? '新增分类' : '编辑分类'}</h2><button type="button" aria-label="关闭" onClick={() => setEditor(null)}><X /></button></header>
        <form onSubmit={save}>
          <label>所属一级分类<select value={form.parentId ?? ''} disabled={Boolean(editor?.id && editorParent?.recommendationGroup)} onChange={(event) => setForm({ ...form, parentId: event.target.value ? Number(event.target.value) : null, targetCategoryId: null })}>
            <option value="">无（一级分类）</option>{parents.filter((item) => item.id !== editor?.id).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select></label>
          {recommendationEditor ? <label>引用真实二级分类<select value={form.targetCategoryId ?? ''} onChange={(event) => setForm({ ...form, targetCategoryId: event.target.value ? Number(event.target.value) : null })}>
            <option value="">请选择</option>{realLeaves.map((item) => <option key={item.id} value={item.id}>{parents.find((parent) => parent.id === item.parentId)?.name} / {item.name}</option>)}
          </select></label> : <label>分类名称<input maxLength={40} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>}
          <label>排序值<input type="number" value={form.sortOrder} onChange={(event) => setForm({ ...form, sortOrder: event.target.value })} /></label>
          <label className="experience-category-checkbox"><input type="checkbox" checked={form.enabled} onChange={(event) => setForm({ ...form, enabled: event.target.checked })} />启用</label>
          <footer><button type="button" onClick={() => setEditor(null)}>取消</button><button type="submit" disabled={busy}>{busy ? '保存中…' : '保存'}</button></footer>
        </form>
      </section>
    </div>}
    <ConfirmDialog open={Boolean(deleteTarget)} title="删除分类" message={deleteTarget?.parentId == null
      ? '删除一级分类会同时下架其全部二级分类；已有经历仍保留原分类展示。确定删除吗？'
      : '已有经历仍保留原分类展示。确定删除吗？'} danger busy={busy} confirmText="删除"
      onCancel={() => setDeleteTarget(null)} onConfirm={remove} />
  </div>;
}
