import { useEffect, useMemo, useState } from 'react';
import { ChevronRight, Plus, Search, UsersRound, X } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import '../discovery/DiscoveryManagementPage.css';
import '../shared/LibraryManagement.css';
import './ExperiencesPage.css';

const MAIN_NAMES = {
  GENERAL: '通用',
  LIFE: '生活',
  WORK: '工作',
  ENTERTAINMENT: '娱乐',
};
const PAGE_SIZE = 20;
const emptyExperience = () => ({ categoryId: '', name: '', active: true });

export default function ExperiencesPage() {
  const [data, setData] = useState({ categories: [], experienceCatalogs: [] });
  const [catalogKeyword, setCatalogKeyword] = useState('');
  const [catalogPage, setCatalogPage] = useState(0);
  const [draft, setDraft] = useState(emptyExperience);
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [confirmation, setConfirmation] = useState(null);
  const [confirming, setConfirming] = useState(false);
  const [usersModal, setUsersModal] = useState(null);
  const [usersModalItems, setUsersModalItems] = useState([]);
  const [usersModalPage, setUsersModalPage] = useState(0);
  const [usersModalTotal, setUsersModalTotal] = useState(0);
  const [usersModalLoading, setUsersModalLoading] = useState(false);

  const load = async () => {
    try {
      setData(await adminApi.discovery());
    } catch (error) {
      message.error(error.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const experienceCategories = useMemo(
    () => data.categories.filter(
      (item) => item.active && item.contentScope !== 'MATTERS',
    ),
    [data.categories],
  );
  const visibleCatalogs = useMemo(() => {
    const keyword = catalogKeyword.trim().toLocaleLowerCase('zh-CN');
    return data.experienceCatalogs.filter((item) => (
      !keyword
      || `${item.name} ${item.categoryName}`.toLocaleLowerCase('zh-CN').includes(keyword)
    ));
  }, [data.experienceCatalogs, catalogKeyword]);
  const catalogItems = visibleCatalogs.slice(
    catalogPage * PAGE_SIZE,
    (catalogPage + 1) * PAGE_SIZE,
  );

  useEffect(() => {
    const lastPage = Math.max(0, Math.ceil(visibleCatalogs.length / PAGE_SIZE) - 1);
    if (catalogPage > lastPage) setCatalogPage(lastPage);
  }, [visibleCatalogs.length, catalogPage]);

  const perform = async (action, successText) => {
    try {
      await action();
      await load();
      message.success(successText);
      return true;
    } catch (error) {
      message.error(error.message);
      return false;
    }
  };

  const openCreate = () => {
    setEditingId(null);
    setDraft(emptyExperience());
    setEditorOpen(true);
  };
  const openEdit = (item) => {
    setEditingId(item.id);
    setDraft({ categoryId: String(item.categoryId), name: item.name, active: item.active });
    setEditorOpen(true);
  };
  const closeEditor = () => {
    setEditingId(null);
    setDraft(emptyExperience());
    setEditorOpen(false);
  };
  const submit = async (event) => {
    event.preventDefault();
    const payload = { ...draft, categoryId: Number(draft.categoryId) };
    const success = editingId
      ? await perform(() => adminApi.updateDiscoveryExperience(editingId, payload), '经历已修改')
      : await perform(() => adminApi.createDiscoveryExperience(payload), '经历已新增');
    if (success) closeEditor();
  };
  const toggleCatalog = (item) => perform(
    () => adminApi.updateDiscoveryExperience(item.id, {
      categoryId: item.categoryId,
      name: item.name,
      active: !item.active,
    }),
    item.active ? '经历已停用' : '经历已启用',
  );
  const removeCatalog = (item) => setConfirmation({
    title: '删除经历',
    message: `确定删除经历“${item.name}”吗？`,
    action: () => perform(() => adminApi.deleteDiscoveryExperience(item.id), '经历已删除'),
  });
  const loadExperienceUsers = async (experience, page = 0) => {
    try {
      setUsersModalLoading(true);
      const result = await adminApi.experienceUsers(experience.id, page, PAGE_SIZE);
      setUsersModalItems(result.items);
      setUsersModalTotal(result.total);
      setUsersModalPage(result.page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setUsersModalLoading(false);
    }
  };
  const openExperienceUsers = async (experience) => {
    setUsersModal(experience);
    setUsersModalItems([]);
    setUsersModalPage(0);
    setUsersModalTotal(experience.userCount);
    await loadExperienceUsers(experience, 0);
  };
  const closeExperienceUsers = () => {
    if (usersModalLoading) return;
    setUsersModal(null);
    setUsersModalItems([]);
  };
  const updateUserExperience = async (action, successText) => {
    try {
      await action();
      await Promise.all([load(), loadExperienceUsers(usersModal, usersModalPage)]);
      message.success(successText);
    } catch (error) {
      message.error(error.message);
    }
  };
  const toggleUserExperience = (item) => updateUserExperience(
    () => adminApi.setCertificationEnabled(item.id, !item.enabled),
    item.enabled ? '用户经历已停用' : '用户经历已启用',
  );
  const removeUserExperience = (item) => setConfirmation({
    title: '删除用户经历',
    message: `确定删除“${item.title}”这条用户经历吗？`,
    action: async () => {
      try {
        await adminApi.deleteCertification(item.id);
        await Promise.all([load(), loadExperienceUsers(usersModal, usersModalPage)]);
        message.success('用户经历已删除');
        return true;
      } catch (error) {
        message.error(error.message);
        return false;
      }
    },
  });
  const confirmAction = async () => {
    if (!confirmation) return;
    setConfirming(true);
    const success = await confirmation.action();
    setConfirming(false);
    if (success) setConfirmation(null);
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>经历管理</h1>
          <p>维护平台经历，点击关联人数可查看认证用户</p>
        </div>
        <div className="library-title-actions">
          <span>共 {visibleCatalogs.length} 条经历</span>
          <button className="primary" type="button" onClick={openCreate}><Plus />新增经历</button>
        </div>
      </div>

      <section className="discovery-admin-section library-section">
          <div className="library-toolbar">
            <label className="library-search-field">
              <Search /><input value={catalogKeyword} onChange={(event) => { setCatalogKeyword(event.target.value); setCatalogPage(0); }} placeholder="搜索经历名称或所属分类" />
            </label>
          </div>
          <div className="table-card library-table-card">
            <table>
              <thead><tr><th>经历名称</th><th>所属分类</th><th>关联用户</th><th>状态</th><th>操作</th></tr></thead>
              <tbody>
                {catalogItems.map((item) => (
                  <tr key={item.id}>
                    <td><b>{item.name}</b></td>
                    <td>{MAIN_NAMES[item.mainCategory]} / {item.categoryName}</td>
                    <td>
                      <button className="library-count-link" type="button" onClick={() => openExperienceUsers(item)}>
                        <UsersRound />
                        <span>{item.userCount} 人</span>
                        <em>查看</em>
                        <ChevronRight />
                      </button>
                    </td>
                    <td><span className={`status ${item.active ? 'active' : 'suspended'}`}>{item.active ? '启用' : '停用'}</span></td>
                    <td className="row-actions">
                      <button className="plain" onClick={() => openEdit(item)}>编辑</button>
                      <button className="plain" onClick={() => toggleCatalog(item)}>{item.active ? '停用' : '启用'}</button>
                      <button className="danger" onClick={() => removeCatalog(item)}>删除</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!catalogItems.length && <div className="empty">暂无经历</div>}
          </div>
          <Pagination page={catalogPage} size={PAGE_SIZE} total={visibleCatalogs.length} onChange={setCatalogPage} />
      </section>

      {editorOpen && (
        <>
          <button className="modal-mask" type="button" aria-label="关闭" onClick={closeEditor} />
          <section className="detail-modal discovery-editor-modal">
            <header><div><h2>{editingId ? '编辑经历' : '新增经历'}</h2><p>维护平台统一经历库</p></div><button type="button" onClick={closeEditor}><X /></button></header>
            <form className="editor-form" onSubmit={submit}>
              <label><span>所属分类</span><select required value={draft.categoryId} onChange={(event) => setDraft({ ...draft, categoryId: event.target.value })}><option value="">请选择</option>{experienceCategories.map((item) => <option key={item.id} value={item.id}>{MAIN_NAMES[item.mainCategory]} / {item.name}</option>)}</select></label>
              <label><span>经历名称</span><input required maxLength="100" value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} placeholder="例如：经历过国外留学" /></label>
              <footer><button className="plain" type="button" onClick={closeEditor}>取消</button><button className="primary" type="submit">{editingId ? '保存修改' : '确认新增'}</button></footer>
            </form>
          </section>
        </>
      )}

      {usersModal && (
        <>
          <button className="modal-mask" type="button" aria-label="关闭" onClick={closeExperienceUsers} />
          <section className="detail-modal experience-users-modal">
            <header>
              <div><h2>{usersModal.name} · 关联用户</h2><p>共 {usersModalTotal} 人</p></div>
              <button type="button" onClick={closeExperienceUsers}><X /></button>
            </header>
            <div className="experience-users-modal-body">
              <table>
                <thead><tr><th>用户</th><th>用户填写内容</th><th>调整关联</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                  {usersModalItems.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <div className="experience-user-cell">
                          <div className="experience-user-avatar">
                            {item.avatarUrl ? <img src={item.avatarUrl} alt="" /> : <span>{(item.nickname || item.uid).slice(0, 1)}</span>}
                          </div>
                          <div><b>{item.nickname || `UID ${item.uid}`}</b><small>UID {item.uid}</small></div>
                        </div>
                      </td>
                      <td><b>{item.title}</b><small>{item.description || '无补充说明'}</small></td>
                      <td>
                        <select className="table-select" value={usersModal.id} onChange={(event) => updateUserExperience(() => adminApi.classifyExperience(item.id, Number(event.target.value)), '经历关联已更新')}>
                          {data.experienceCatalogs.filter((entry) => entry.active).map((entry) => <option key={entry.id} value={entry.id}>{entry.name}（{entry.categoryName}）</option>)}
                        </select>
                      </td>
                      <td><span className={`status ${item.enabled ? 'active' : 'suspended'}`}>{item.enabled ? '启用' : '停用'}</span></td>
                      <td className="row-actions">
                        <button className="plain" type="button" onClick={() => toggleUserExperience(item)}>{item.enabled ? '停用' : '启用'}</button>
                        <button className="danger" type="button" onClick={() => removeUserExperience(item)}>删除</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!usersModalLoading && !usersModalItems.length && <div className="experience-users-empty">该经历下暂无关联用户</div>}
            </div>
            <Pagination page={usersModalPage} size={PAGE_SIZE} total={usersModalTotal} onChange={(page) => loadExperienceUsers(usersModal, page)} />
          </section>
        </>
      )}

      <ConfirmDialog open={Boolean(confirmation)} title={confirmation?.title} message={confirmation?.message} confirmText="确认删除" danger busy={confirming} onCancel={() => !confirming && setConfirmation(null)} onConfirm={confirmAction} />
    </>
  );
}
