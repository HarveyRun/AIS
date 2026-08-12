import { useEffect, useMemo, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './DiscoveryManagementPage.css';

const MAIN_NAMES = { LIFE: '生活', WORK: '工作', ENTERTAINMENT: '娱乐' };
const emptyCategory = { mainCategory: 'LIFE', name: '', sortOrder: 1, active: true };
const newMatter = () => ({ categoryId: '', title: '', sortOrder: 1, active: true, jobs: [] });

export default function DiscoveryManagementPage() {
  const [data, setData] = useState({ categories: [], matters: [], matterJobs: [], jobs: [], experiences: [] });
  const [tab, setTab] = useState('categories');
  const [categoryDraft, setCategoryDraft] = useState(emptyCategory);
  const [editingCategoryId, setEditingCategoryId] = useState(null);
  const [matterDraft, setMatterDraft] = useState(newMatter);
  const [editingMatterId, setEditingMatterId] = useState(null);
  const [categoryMainFilter, setCategoryMainFilter] = useState('LIFE');
  const [matterMainFilter, setMatterMainFilter] = useState('LIFE');
  const [matterCategoryFilter, setMatterCategoryFilter] = useState('');
  const [confirmation, setConfirmation] = useState(null);
  const [confirming, setConfirming] = useState(false);

  const activeCategories = useMemo(() => data.categories.filter((item) => item.active), [data.categories]);
  const visibleCategories = useMemo(() => data.categories.filter((item) => item.mainCategory === categoryMainFilter), [data.categories, categoryMainFilter]);
  const matterFilterCategories = useMemo(() => activeCategories.filter((item) => item.mainCategory === matterMainFilter), [activeCategories, matterMainFilter]);
  const visibleMatters = useMemo(() => data.matters.filter((item) => item.mainCategory === matterMainFilter && (!matterCategoryFilter || String(item.categoryId) === matterCategoryFilter)), [data.matters, matterMainFilter, matterCategoryFilter]);
  const jobsForMatter = (matterId) => data.matterJobs.filter((item) => item.matterId === matterId);

  const load = async () => {
    try {
      setData(await adminApi.discovery());
    } catch (error) {
      message.error(error.message);
    }
  };
  useEffect(() => { load(); }, []);

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

  const submitCategory = async (event) => {
    event.preventDefault();
    const createPayload = { mainCategory: categoryDraft.mainCategory, name: categoryDraft.name, active: true };
    const success = editingCategoryId
      ? await perform(() => adminApi.updateDiscoveryCategory(editingCategoryId, categoryDraft), '分类已修改')
      : await perform(() => adminApi.createDiscoveryCategory(createPayload), '分类已新增');
    if (success) cancelCategoryEdit();
  };
  const editCategory = (item) => {
    setEditingCategoryId(item.id);
    setCategoryDraft({ mainCategory: item.mainCategory, name: item.name, sortOrder: item.sortOrder, active: item.active });
  };
  const cancelCategoryEdit = () => {
    setEditingCategoryId(null);
    setCategoryDraft(emptyCategory);
  };
  const deleteCategory = (item) => setConfirmation({
    title: '删除分类',
    message: `确定删除分类“${item.name}”吗？删除后无法恢复。`,
    action: async () => {
      const success = await perform(() => adminApi.deleteDiscoveryCategory(item.id), '分类已删除');
      if (success && editingCategoryId === item.id) cancelCategoryEdit();
      return success;
    },
  });

  const toggleJob = (jobId) => setMatterDraft((current) => ({
    ...current,
    jobs: current.jobs.some((item) => item.jobId === jobId)
      ? current.jobs.filter((item) => item.jobId !== jobId)
      : [...current.jobs, { jobId, type: 'PRIMARY' }],
  }));
  const changeJobType = (jobId, type) => setMatterDraft((current) => ({
    ...current,
    jobs: current.jobs.map((item) => item.jobId === jobId ? { ...item, type } : item),
  }));

  const submitMatter = async (event) => {
    event.preventDefault();
    const payload = {
      ...matterDraft,
      categoryId: Number(matterDraft.categoryId),
      jobs: matterDraft.jobs,
    };
    if (!payload.jobs.some((item) => item.type === 'PRIMARY')) {
      message.warning('至少把一个岗位设为重点问');
      return;
    }
    const createPayload = { categoryId: payload.categoryId, title: payload.title, active: true, jobs: payload.jobs };
    const success = editingMatterId
      ? await perform(() => adminApi.updateDiscoveryMatter(editingMatterId, payload), '事情和岗位方案已修改')
      : await perform(() => adminApi.createDiscoveryMatter(createPayload), '事情和岗位方案已新增');
    if (success) cancelMatterEdit();
  };
  const editMatter = (item) => {
    setEditingMatterId(item.id);
    setMatterDraft({ categoryId: String(item.categoryId), title: item.title, sortOrder: item.sortOrder, active: item.active, jobs: jobsForMatter(item.id).map(({ jobId, type }) => ({ jobId, type })) });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  const cancelMatterEdit = () => {
    setEditingMatterId(null);
    setMatterDraft(newMatter());
  };
  const deleteMatter = (item) => setConfirmation({
    title: '删除事情',
    message: `确定删除事情“${item.title}”吗？相关岗位方案也会一并删除。`,
    action: async () => {
      const success = await perform(() => adminApi.deleteDiscoveryMatter(item.id), '事情已删除');
      if (success && editingMatterId === item.id) cancelMatterEdit();
      return success;
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
      <div className="page-title"><div><h1>内容分类</h1><p>分类、事情、岗位方案和认证经历统一维护</p></div></div>
      <div className="discovery-tabs">
        <button className={tab === 'categories' ? 'active' : ''} onClick={() => setTab('categories')}>分类管理</button>
        <button className={tab === 'matters' ? 'active' : ''} onClick={() => setTab('matters')}>按事情找人</button>
        <button className={tab === 'experiences' ? 'active' : ''} onClick={() => setTab('experiences')}>经历分类</button>
      </div>

      {tab === 'categories' && (
        <section className="discovery-admin-section">
          <header><div><h2>分类管理</h2><p>管理生活、工作、娱乐三个业务大类下的子分类。</p></div></header>
          <div className="business-filter"><span>查看业务大类</span>{Object.entries(MAIN_NAMES).map(([value, label]) => <button className={categoryMainFilter === value ? 'active' : ''} key={value} onClick={() => setCategoryMainFilter(value)}>{label}</button>)}</div>
          <form className="management-form" onSubmit={submitCategory}>
            <label><span>所属大类</span><select value={categoryDraft.mainCategory} onChange={(event) => setCategoryDraft({ ...categoryDraft, mainCategory: event.target.value })}>{Object.entries(MAIN_NAMES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
            <label className="wide"><span>分类名称</span><input required maxLength="80" value={categoryDraft.name} onChange={(event) => setCategoryDraft({ ...categoryDraft, name: event.target.value })} placeholder="请输入分类名称" /></label>
            {editingCategoryId && <label><span>移动到第</span><input type="number" min="1" max={Math.max(1, visibleCategories.length)} value={categoryDraft.sortOrder} onChange={(event) => setCategoryDraft({ ...categoryDraft, sortOrder: Number(event.target.value) })} /></label>}
            {editingCategoryId && <label><span>状态</span><select value={String(categoryDraft.active)} onChange={(event) => setCategoryDraft({ ...categoryDraft, active: event.target.value === 'true' })}><option value="true">启用</option><option value="false">停用</option></select></label>}
            <div className="form-actions"><button className="primary" type="submit">{editingCategoryId ? '保存修改' : '新增分类'}</button>{editingCategoryId && <button className="plain" type="button" onClick={cancelCategoryEdit}>取消</button>}</div>
          </form>
          <div className="table-card"><table><thead><tr><th>所属大类</th><th>分类名称</th><th>排序</th><th>状态</th><th>操作</th></tr></thead><tbody>
            {visibleCategories.map((item) => <tr key={item.id}><td>{MAIN_NAMES[item.mainCategory]}</td><td><b>{item.name}</b></td><td>第 {item.sortOrder} 位</td><td><span className={`status ${item.active ? 'active' : 'suspended'}`}>{item.active ? '启用' : '停用'}</span></td><td className="row-actions"><button className="plain" onClick={() => editCategory(item)}>编辑</button><button className="danger" onClick={() => deleteCategory(item)}>删除</button></td></tr>)}
          </tbody></table>{!visibleCategories.length && <div className="empty">该大类暂无子分类</div>}</div>
        </section>
      )}

      {tab === 'matters' && (
        <section className="discovery-admin-section">
          <header><div><h2>按事情找人</h2><p>维护具体事情，并配置解决这件事情所需的岗位。</p></div></header>
          <div className="matter-filters"><label><span>业务大类</span><select value={matterMainFilter} onChange={(event) => { setMatterMainFilter(event.target.value); setMatterCategoryFilter(''); }}><option value="LIFE">生活</option><option value="WORK">工作</option><option value="ENTERTAINMENT">娱乐</option></select></label><label><span>只看子分类</span><select value={matterCategoryFilter} onChange={(event) => setMatterCategoryFilter(event.target.value)}><option value="">全部子分类</option>{matterFilterCategories.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label><span className="filter-count">共 {visibleMatters.length} 件事情</span></div>
          <form className="management-form matter-form" onSubmit={submitMatter}>
            <label><span>分类归属</span><select required value={matterDraft.categoryId} onChange={(event) => setMatterDraft({ ...matterDraft, categoryId: event.target.value })}><option value="">请选择</option>{activeCategories.map((item) => <option key={item.id} value={item.id}>{MAIN_NAMES[item.mainCategory]} / {item.name}</option>)}</select></label>
            <label className="wide"><span>事情名称</span><input required maxLength="160" value={matterDraft.title} onChange={(event) => setMatterDraft({ ...matterDraft, title: event.target.value })} placeholder="例如：我想装修" /></label>
            {editingMatterId && <label><span>移动到第</span><input type="number" min="1" value={matterDraft.sortOrder} onChange={(event) => setMatterDraft({ ...matterDraft, sortOrder: Number(event.target.value) })} /></label>}
            {editingMatterId && <label><span>状态</span><select value={String(matterDraft.active)} onChange={(event) => setMatterDraft({ ...matterDraft, active: event.target.value === 'true' })}><option value="true">启用</option><option value="false">停用</option></select></label>}

            <div className="participant-editor">
              <div className="participant-editor-title"><b>岗位方案</b><span>用户查找时，会匹配具备这些岗位的人</span></div>
              <div className="participant-picker">
                {data.jobs.filter((job) => job.active).map((job) => {
                  const selected = matterDraft.jobs.find((item) => item.jobId === job.id);
                  return (
                    <div className={`participant-picker-row ${selected ? 'selected' : ''}`} key={job.id}>
                      <label>
                        <input type="checkbox" checked={Boolean(selected)} onChange={() => toggleJob(job.id)} />
                        <span><b>{job.name}</b><small>{job.userCount} 位关联用户</small></span>
                      </label>
                      <p>{job.description || '暂无岗位介绍'}</p>
                      <select disabled={!selected} value={selected?.type || 'PRIMARY'} onChange={(event) => changeJobType(job.id, event.target.value)}>
                        <option value="PRIMARY">重点问</option>
                        <option value="SUPPORTING">顺便问</option>
                      </select>
                    </div>
                  );
                })}
                {!data.jobs.length && <div className="participant-picker-empty">暂无岗位，请先到岗位管理新增</div>}
              </div>
            </div>
            <div className="form-actions"><button className="primary" type="submit">{editingMatterId ? '保存修改' : '新增事情'}</button>{editingMatterId && <button className="plain" type="button" onClick={cancelMatterEdit}>取消</button>}</div>
          </form>
          <div className="matter-card-list">
            {visibleMatters.map((item) => {
              const matterJobs = jobsForMatter(item.id);
              return <article className="matter-admin-card" key={item.id}>
                <div className="matter-card-main"><small>{MAIN_NAMES[item.mainCategory]} / {item.categoryName} · 第 {item.sortOrder} 位</small><h3>{item.title}</h3><div className="matter-participant-summary">{matterJobs.map((job) => <span className={job.type === 'PRIMARY' ? 'primary-participant' : 'supporting-participant'} key={job.jobId}>{job.type === 'PRIMARY' ? '重点问' : '顺便问'} · {job.jobName}</span>)}{!matterJobs.length && <em>还没有配置岗位方案</em>}</div></div>
                <div><span className={`status ${item.active ? 'active' : 'suspended'}`}>{item.active ? '启用' : '停用'}</span><div className="row-actions"><button className="plain" onClick={() => editMatter(item)}>编辑</button><button className="danger" onClick={() => deleteMatter(item)}>删除</button></div></div>
              </article>;
            })}
            {!visibleMatters.length && <div className="empty">该筛选条件下暂无事情</div>}
          </div>
        </section>
      )}

      {tab === 'experiences' && (
        <section className="discovery-admin-section">
          <header><div><h2>经历分类</h2><p>只有审核通过并完成分类的经历才会出现在移动端。</p></div></header>
          <div className="table-card"><table><thead><tr><th>用户</th><th>认证经历</th><th>当前归属</th><th>调整分类</th></tr></thead><tbody>
            {data.experiences.map((item) => <tr key={item.id}><td><b>{item.nickname || `UID ${item.uid}`}</b><small>UID {item.uid}</small></td><td><b>{item.title}</b><small>{item.description || '无补充说明'}</small></td><td>{item.categoryId ? `${MAIN_NAMES[item.mainCategory]} / ${item.categoryName}` : <span className="unclassified">待分类</span>}</td><td><select className="table-select" value={item.categoryId || ''} onChange={(event) => perform(() => adminApi.classifyExperience(item.id, event.target.value ? Number(event.target.value) : null), '经历分类已更新')}><option value="">暂不展示</option>{activeCategories.map((category) => <option key={category.id} value={category.id}>{MAIN_NAMES[category.mainCategory]} / {category.name}</option>)}</select></td></tr>)}
          </tbody></table>{!data.experiences.length && <div className="empty">暂无已通过的亲身经历</div>}</div>
        </section>
      )}
      <ConfirmDialog open={Boolean(confirmation)} title={confirmation?.title} message={confirmation?.message} confirmText="确认删除" danger busy={confirming} onCancel={() => !confirming && setConfirmation(null)} onConfirm={confirmAction} />
    </>
  );
}
