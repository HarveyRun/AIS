import { useEffect, useMemo, useState } from 'react';
import { Plus, Search, X } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './DiscoveryManagementPage.css';

const MAIN_NAMES = { GENERAL: '通用', LIFE: '生活', WORK: '工作', ENTERTAINMENT: '娱乐' };
const emptyCategory = { mainCategory: 'LIFE', name: '', sortOrder: 1, active: true };
const newMatter = () => ({ categoryId: '', title: '', sortOrder: 1, active: true, jobs: [] });
const tablePageSize = 20;

export default function DiscoveryManagementPage() {
  const [data, setData] = useState({
    categories: [],
    matters: [],
    matterJobs: [],
    jobs: [],
  });
  const [tab, setTab] = useState('categories');
  const [categoryDraft, setCategoryDraft] = useState(emptyCategory);
  const [editingCategoryId, setEditingCategoryId] = useState(null);
  const [matterDraft, setMatterDraft] = useState(newMatter);
  const [editingMatterId, setEditingMatterId] = useState(null);
  const [categoryMainFilter, setCategoryMainFilter] = useState('GENERAL');
  const [matterMainFilter, setMatterMainFilter] = useState('GENERAL');
  const [matterCategoryFilter, setMatterCategoryFilter] = useState('');
  const [categoryKeyword, setCategoryKeyword] = useState('');
  const [appliedCategoryKeyword, setAppliedCategoryKeyword] = useState('');
  const [matterKeyword, setMatterKeyword] = useState('');
  const [appliedMatterKeyword, setAppliedMatterKeyword] = useState('');
  const [jobKeyword, setJobKeyword] = useState('');
  const [confirmation, setConfirmation] = useState(null);
  const [confirming, setConfirming] = useState(false);
  const [categoryPage, setCategoryPage] = useState(0);
  const [matterPage, setMatterPage] = useState(0);
  const [categoryEditorOpen, setCategoryEditorOpen] = useState(false);
  const [matterEditorOpen, setMatterEditorOpen] = useState(false);

  const activeCategories = useMemo(
    () => data.categories.filter((item) => item.active),
    [data.categories],
  );
  const matterCategories = useMemo(
    () => activeCategories.filter((item) => item.contentScope !== 'EXPERIENCES'),
    [activeCategories],
  );
  const visibleCategories = useMemo(
    () =>
      data.categories.filter(
        (item) =>
          item.mainCategory === categoryMainFilter &&
          (!appliedCategoryKeyword ||
            item.name.toLowerCase().includes(appliedCategoryKeyword.toLowerCase())),
      ),
    [data.categories, categoryMainFilter, appliedCategoryKeyword],
  );
  const pagedCategories = useMemo(
    () => visibleCategories.slice(categoryPage * tablePageSize, (categoryPage + 1) * tablePageSize),
    [visibleCategories, categoryPage],
  );
  const matterFilterCategories = useMemo(
    () => matterCategories.filter((item) => item.mainCategory === matterMainFilter),
    [matterCategories, matterMainFilter],
  );
  const visibleMatters = useMemo(
    () =>
      data.matters.filter(
        (item) =>
          item.mainCategory === matterMainFilter &&
          (!matterCategoryFilter || String(item.categoryId) === matterCategoryFilter) &&
          (!appliedMatterKeyword ||
            item.title.toLowerCase().includes(appliedMatterKeyword.toLowerCase())),
      ),
    [data.matters, matterMainFilter, matterCategoryFilter, appliedMatterKeyword],
  );
  const pagedMatters = useMemo(
    () => visibleMatters.slice(matterPage * tablePageSize, (matterPage + 1) * tablePageSize),
    [visibleMatters, matterPage],
  );
  const visibleJobs = useMemo(() => {
    const keyword = jobKeyword.trim().toLowerCase();
    return data.jobs.filter(
      (job) => job.active && (!keyword || job.name.toLowerCase().includes(keyword)),
    );
  }, [data.jobs, jobKeyword]);
  const jobsForMatter = (matterId) => data.matterJobs.filter((item) => item.matterId === matterId);
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
  useEffect(() => {
    const lastPage = Math.max(0, Math.ceil(visibleCategories.length / tablePageSize) - 1);
    if (categoryPage > lastPage) setCategoryPage(lastPage);
  }, [visibleCategories.length, categoryPage]);
  useEffect(() => {
    const lastPage = Math.max(0, Math.ceil(visibleMatters.length / tablePageSize) - 1);
    if (matterPage > lastPage) setMatterPage(lastPage);
  }, [visibleMatters.length, matterPage]);

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
    const createPayload = {
      mainCategory: categoryDraft.mainCategory,
      name: categoryDraft.name,
      active: true,
    };
    const success = editingCategoryId
      ? await perform(
          () => adminApi.updateDiscoveryCategory(editingCategoryId, categoryDraft),
          '分类已修改',
        )
      : await perform(() => adminApi.createDiscoveryCategory(createPayload), '分类已新增');
    if (success) cancelCategoryEdit();
  };
  const editCategory = (item) => {
    setEditingCategoryId(item.id);
    setCategoryDraft({
      mainCategory: item.mainCategory,
      name: item.name,
      sortOrder: item.sortOrder,
      active: item.active,
    });
    setCategoryEditorOpen(true);
  };
  const cancelCategoryEdit = () => {
    setEditingCategoryId(null);
    setCategoryDraft(emptyCategory);
    setCategoryEditorOpen(false);
  };
  const deleteCategory = (item) =>
    setConfirmation({
      title: '删除分类',
      message: `确定删除分类“${item.name}”吗？删除后无法恢复。`,
      action: async () => {
        const success = await perform(
          () => adminApi.deleteDiscoveryCategory(item.id),
          '分类已删除',
        );
        if (success && editingCategoryId === item.id) cancelCategoryEdit();
        return success;
      },
    });
  const toggleCategory = (item) =>
    perform(
      () =>
        adminApi.updateDiscoveryCategory(item.id, {
          mainCategory: item.mainCategory,
          name: item.name,
          sortOrder: item.sortOrder,
          active: !item.active,
        }),
      item.active ? '分类已停用' : '分类已启用',
    );

  const toggleJob = (jobId) =>
    setMatterDraft((current) => ({
      ...current,
      jobs: current.jobs.some((item) => item.jobId === jobId)
        ? current.jobs.filter((item) => item.jobId !== jobId)
        : [...current.jobs, { jobId }],
    }));

  const submitMatter = async (event) => {
    event.preventDefault();
    const payload = {
      ...matterDraft,
      categoryId: Number(matterDraft.categoryId),
      jobs: matterDraft.jobs,
    };
    if (!payload.jobs.length) {
      message.warning('请至少选择一个岗位');
      return;
    }
    const createPayload = {
      categoryId: payload.categoryId,
      title: payload.title,
      active: true,
      jobs: payload.jobs,
    };
    const success = editingMatterId
      ? await perform(
          () => adminApi.updateDiscoveryMatter(editingMatterId, payload),
          '事情和岗位方案已修改',
        )
      : await perform(() => adminApi.createDiscoveryMatter(createPayload), '事情和岗位方案已新增');
    if (success) cancelMatterEdit();
  };
  const editMatter = (item) => {
    setEditingMatterId(item.id);
    setMatterDraft({
      categoryId: String(item.categoryId),
      title: item.title,
      sortOrder: item.sortOrder,
      active: item.active,
      jobs: jobsForMatter(item.id).map(({ jobId }) => ({ jobId })),
    });
    setMatterEditorOpen(true);
  };
  const cancelMatterEdit = () => {
    setEditingMatterId(null);
    setMatterDraft(newMatter());
    setMatterEditorOpen(false);
    setJobKeyword('');
  };
  const deleteMatter = (item) =>
    setConfirmation({
      title: '删除事情',
      message: `确定删除事情“${item.title}”吗？相关岗位方案也会一并删除。`,
      action: async () => {
        const success = await perform(() => adminApi.deleteDiscoveryMatter(item.id), '事情已删除');
        if (success && editingMatterId === item.id) cancelMatterEdit();
        return success;
      },
    });
  const toggleMatter = (item) =>
    perform(
      () =>
        adminApi.updateDiscoveryMatter(item.id, {
          categoryId: item.categoryId,
          title: item.title,
          sortOrder: item.sortOrder,
          active: !item.active,
          jobs: jobsForMatter(item.id).map(({ jobId }) => ({ jobId })),
        }),
      item.active ? '事情已停用' : '事情已启用',
    );
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
          <h1>分类管理</h1>
          <p>维护事情与经历共用的分类，以及事情对应的岗位方案</p>
        </div>
      </div>
      <div className="discovery-tabs">
        <button
          className={tab === 'categories' ? 'active' : ''}
          onClick={() => setTab('categories')}
        >
          通用分类
        </button>
        <button className={tab === 'matters' ? 'active' : ''} onClick={() => setTab('matters')}>
          按事情找人
        </button>
      </div>

      {tab === 'categories' && (
        <section className="discovery-admin-section">
          <header className="section-heading-actions">
            <div>
              <h2>通用分类</h2>
              <p>这里的分类同时用于按事情找人和按经历找人。</p>
            </div>
            <button
              className="primary"
              type="button"
              onClick={() => {
                setEditingCategoryId(null);
                setCategoryDraft(emptyCategory);
                setCategoryEditorOpen(true);
              }}
            >
              <Plus />
              新增分类
            </button>
          </header>
          <form
            className="query-toolbar"
            onSubmit={(event) => {
              event.preventDefault();
              setAppliedCategoryKeyword(categoryKeyword.trim());
              setCategoryPage(0);
            }}
          >
            <label>
              <span>业务大类</span>
              <select
                value={categoryMainFilter}
                onChange={(event) => {
                  setCategoryMainFilter(event.target.value);
                  setCategoryPage(0);
                }}
              >
                <option value="GENERAL">通用</option>
                <option value="LIFE">生活</option>
                <option value="WORK">工作</option>
                <option value="ENTERTAINMENT">娱乐</option>
              </select>
            </label>
            <label className="query-keyword">
              <span>分类名称</span>
              <div>
                <Search />
                <input
                  value={categoryKeyword}
                  onChange={(event) => setCategoryKeyword(event.target.value)}
                  placeholder="输入分类名称"
                />
              </div>
            </label>
            <button className="primary" type="submit">
              查询
            </button>
            <button
              className="plain"
              type="button"
              onClick={() => {
                setCategoryKeyword('');
                setAppliedCategoryKeyword('');
                setCategoryPage(0);
              }}
            >
              重置
            </button>
            <em>共 {visibleCategories.length} 条</em>
          </form>
          <div className="table-card">
            <table>
              <thead>
                <tr>
                  <th>所属大类</th>
                  <th>分类名称</th>
                  <th>排序</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {pagedCategories.map((item) => (
                  <tr key={item.id}>
                    <td>{MAIN_NAMES[item.mainCategory]}</td>
                    <td>
                      <b>{item.name}</b>
                    </td>
                    <td>第 {item.sortOrder} 位</td>
                    <td>
                      <span className={`status ${item.active ? 'active' : 'suspended'}`}>
                        {item.active ? '启用' : '停用'}
                      </span>
                    </td>
                    <td className="row-actions">
                      {item.mainCategory === 'GENERAL' ? (
                        <span className="fixed-category">固定分类</span>
                      ) : (
                        <>
                          <button className="plain" onClick={() => editCategory(item)}>
                            编辑
                          </button>
                          <button className="plain" onClick={() => toggleCategory(item)}>
                            {item.active ? '停用' : '启用'}
                          </button>
                          <button className="danger" onClick={() => deleteCategory(item)}>
                            删除
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!visibleCategories.length && <div className="empty">该大类暂无子分类</div>}
          </div>
          <Pagination
            page={categoryPage}
            size={tablePageSize}
            total={visibleCategories.length}
            onChange={setCategoryPage}
          />
        </section>
      )}

      {tab === 'matters' && (
        <section className="discovery-admin-section">
          <header className="section-heading-actions">
            <div>
              <h2>按事情找人</h2>
              <p>查询事情，或单独新增并配置岗位方案。</p>
            </div>
            <button
              className="primary"
              type="button"
              onClick={() => {
                setEditingMatterId(null);
                setMatterDraft(newMatter());
                setJobKeyword('');
                setMatterEditorOpen(true);
              }}
            >
              <Plus />
              新增事情
            </button>
          </header>
          <form
            className="query-toolbar matter-query"
            onSubmit={(event) => {
              event.preventDefault();
              setAppliedMatterKeyword(matterKeyword.trim());
              setMatterPage(0);
            }}
          >
            <label>
              <span>业务大类</span>
              <select
                value={matterMainFilter}
                onChange={(event) => {
                  setMatterMainFilter(event.target.value);
                  setMatterCategoryFilter('');
                  setMatterPage(0);
                }}
              >
                <option value="GENERAL">通用</option>
                <option value="LIFE">生活</option>
                <option value="WORK">工作</option>
                <option value="ENTERTAINMENT">娱乐</option>
              </select>
            </label>
            <label>
              <span>子分类</span>
              <select
                value={matterCategoryFilter}
                onChange={(event) => {
                  setMatterCategoryFilter(event.target.value);
                  setMatterPage(0);
                }}
              >
                <option value="">全部子分类</option>
                {matterFilterCategories.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="query-keyword">
              <span>事情名称</span>
              <div>
                <Search />
                <input
                  value={matterKeyword}
                  onChange={(event) => setMatterKeyword(event.target.value)}
                  placeholder="输入事情名称"
                />
              </div>
            </label>
            <button className="primary" type="submit">
              查询
            </button>
            <button
              className="plain"
              type="button"
              onClick={() => {
                setMatterCategoryFilter('');
                setMatterKeyword('');
                setAppliedMatterKeyword('');
                setMatterPage(0);
              }}
            >
              重置
            </button>
            <em>共 {visibleMatters.length} 条</em>
          </form>
          <div className="table-card matter-table">
            <table>
              <thead>
                <tr>
                  <th>事情名称</th>
                  <th>分类归属</th>
                  <th>岗位方案</th>
                  <th>排序</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {pagedMatters.map((item) => {
                  const matterJobs = jobsForMatter(item.id);
                  return (
                    <tr key={item.id}>
                      <td>
                        <b>{item.title}</b>
                      </td>
                      <td>
                        {MAIN_NAMES[item.mainCategory]} / {item.categoryName}
                      </td>
                      <td>
                        <div className="table-job-tags">
                          {matterJobs.map((job) => (
                            <span key={job.jobId}>{job.jobName}</span>
                          ))}
                          {!matterJobs.length && <em>未配置</em>}
                        </div>
                      </td>
                      <td>第 {item.sortOrder} 位</td>
                      <td>
                        <span className={`status ${item.active ? 'active' : 'suspended'}`}>
                          {item.active ? '启用' : '停用'}
                        </span>
                      </td>
                      <td className="row-actions">
                        <button className="plain" onClick={() => editMatter(item)}>
                          编辑
                        </button>
                        <button className="plain" onClick={() => toggleMatter(item)}>
                          {item.active ? '停用' : '启用'}
                        </button>
                        <button className="danger" onClick={() => deleteMatter(item)}>
                          删除
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {!visibleMatters.length && <div className="empty">暂无符合条件的事情</div>}
          </div>
          <Pagination
            page={matterPage}
            size={tablePageSize}
            total={visibleMatters.length}
            onChange={setMatterPage}
          />
        </section>
      )}

      {categoryEditorOpen && (
        <>
          <div className="modal-mask" onClick={cancelCategoryEdit} />
          <section className="detail-modal discovery-editor-modal" role="dialog" aria-modal="true">
            <header>
              <div>
                <h2>{editingCategoryId ? '编辑分类' : '新增分类'}</h2>
                <p>维护通用分类</p>
              </div>
              <button type="button" aria-label="关闭" onClick={cancelCategoryEdit}>
                <X />
              </button>
            </header>
            <form className="editor-form" onSubmit={submitCategory}>
              <label>
                <span>所属大类</span>
                <select
                  value={categoryDraft.mainCategory}
                  onChange={(event) =>
                    setCategoryDraft({ ...categoryDraft, mainCategory: event.target.value })
                  }
                >
                  {Object.entries(MAIN_NAMES)
                    .filter(([value]) => value !== 'GENERAL')
                    .map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                </select>
              </label>
              <label>
                <span>分类名称</span>
                <input
                  required
                  maxLength="80"
                  value={categoryDraft.name}
                  onChange={(event) =>
                    setCategoryDraft({ ...categoryDraft, name: event.target.value })
                  }
                  placeholder="请输入分类名称"
                />
              </label>
              {editingCategoryId && (
                <label>
                  <span>移动到第</span>
                  <input
                    type="number"
                    min="1"
                    value={categoryDraft.sortOrder}
                    onChange={(event) =>
                      setCategoryDraft({ ...categoryDraft, sortOrder: Number(event.target.value) })
                    }
                  />
                </label>
              )}
              <footer>
                <button className="plain" type="button" onClick={cancelCategoryEdit}>
                  取消
                </button>
                <button className="primary" type="submit">
                  {editingCategoryId ? '保存修改' : '确认新增'}
                </button>
              </footer>
            </form>
          </section>
        </>
      )}

      {matterEditorOpen && (
        <>
          <div className="modal-mask" onClick={cancelMatterEdit} />
          <section
            className="detail-modal discovery-editor-modal matter-editor-modal"
            role="dialog"
            aria-modal="true"
          >
            <header>
              <div>
                <h2>{editingMatterId ? '编辑事情' : '新增事情'}</h2>
                <p>填写事情并配置对应岗位</p>
              </div>
              <button type="button" aria-label="关闭" onClick={cancelMatterEdit}>
                <X />
              </button>
            </header>
            <form className="editor-form" onSubmit={submitMatter}>
              <label>
                <span>分类归属</span>
                <select
                  required
                  value={matterDraft.categoryId}
                  onChange={(event) =>
                    setMatterDraft({ ...matterDraft, categoryId: event.target.value })
                  }
                >
                  <option value="">请选择</option>
                  {matterCategories.map((item) => (
                    <option key={item.id} value={item.id}>
                      {MAIN_NAMES[item.mainCategory]} / {item.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>事情名称</span>
                <input
                  required
                  maxLength="160"
                  value={matterDraft.title}
                  onChange={(event) =>
                    setMatterDraft({ ...matterDraft, title: event.target.value })
                  }
                  placeholder="请输入具体事情"
                />
              </label>
              {editingMatterId && (
                <label>
                  <span>移动到第</span>
                  <input
                    type="number"
                    min="1"
                    value={matterDraft.sortOrder}
                    onChange={(event) =>
                      setMatterDraft({ ...matterDraft, sortOrder: Number(event.target.value) })
                    }
                  />
                </label>
              )}
              <div className="participant-editor">
                <div className="participant-editor-title">
                  <b>岗位方案</b>
                  <span>至少选择一个岗位</span>
                </div>
                <div className="participant-search">
                  <input
                    value={jobKeyword}
                    onChange={(event) => setJobKeyword(event.target.value)}
                    placeholder="输入岗位名称搜索"
                  />
                  {jobKeyword && (
                    <button type="button" onClick={() => setJobKeyword('')}>
                      清空
                    </button>
                  )}
                </div>
                <div className="participant-picker">
                  {visibleJobs.map((job) => {
                    const selected = matterDraft.jobs.find((item) => item.jobId === job.id);
                    return (
                      <div
                        className={`participant-picker-row ${selected ? 'selected' : ''}`}
                        key={job.id}
                      >
                        <label>
                          <input
                            type="checkbox"
                            checked={Boolean(selected)}
                            onChange={() => toggleJob(job.id)}
                          />
                          <span>
                            <b>{job.name}</b>
                            <small>{job.userCount} 位关联用户</small>
                          </span>
                        </label>
                        <p>{job.description || '暂无岗位介绍'}</p>
                      </div>
                    );
                  })}
                  {!visibleJobs.length && (
                    <div className="participant-picker-empty">
                      {jobKeyword ? '没有匹配的岗位' : '暂无岗位，请先到岗位管理新增'}
                    </div>
                  )}
                </div>
              </div>
              <footer>
                <button className="plain" type="button" onClick={cancelMatterEdit}>
                  取消
                </button>
                <button className="primary" type="submit">
                  {editingMatterId ? '保存修改' : '确认新增'}
                </button>
              </footer>
            </form>
          </section>
        </>
      )}
      <ConfirmDialog
        open={Boolean(confirmation)}
        title={confirmation?.title}
        message={confirmation?.message}
        confirmText="确认删除"
        danger
        busy={confirming}
        onCancel={() => !confirming && setConfirmation(null)}
        onConfirm={confirmAction}
      />
    </>
  );
}
