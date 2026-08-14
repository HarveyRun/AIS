import { useEffect, useState } from 'react';
import { ChevronRight, Plus, Search, UsersRound, X } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './JobsPage.css';
import '../shared/LibraryManagement.css';

const emptyJob = {
  name: '',
  description: '',
  active: true,
};

export default function JobsPage() {
  const [jobs, setJobs] = useState([]);
  const [jobsPage, setJobsPage] = useState(0);
  const [jobsTotal, setJobsTotal] = useState(0);
  const [jobsKeyword, setJobsKeyword] = useState('');
  const [appliedJobsKeyword, setAppliedJobsKeyword] = useState('');
  const pageSize = 20;
  const [draft, setDraft] = useState(emptyJob);
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [confirmation, setConfirmation] = useState(null);
  const [peopleModal, setPeopleModal] = useState(null);
  const [peopleModalItems, setPeopleModalItems] = useState([]);
  const [peopleModalPage, setPeopleModalPage] = useState(0);
  const [peopleModalTotal, setPeopleModalTotal] = useState(0);
  const [peopleModalLoading, setPeopleModalLoading] = useState(false);

  const loadJobs = async (page = 0, keyword = appliedJobsKeyword) => {
    setJobs([]);
    try {
      const result = await adminApi.jobs(keyword, page, pageSize);
      setJobs(result.items);
      setJobsTotal(result.total);
      setJobsPage(result.page);
      return result;
    } catch (error) {
      message.error(error.message);
      return null;
    }
  };

  useEffect(() => {
    loadJobs(0, '');
  }, []);

  const searchJobs = async (event) => {
    event.preventDefault();
    const keyword = jobsKeyword.trim();
    setAppliedJobsKeyword(keyword);
    await loadJobs(0, keyword);
  };

  const submitJob = async (event) => {
    event.preventDefault();

    try {
      if (editingId) {
        await adminApi.updateJob(editingId, draft);
      } else {
        await adminApi.createJob(draft);
      }

      message.success(editingId ? '岗位已修改' : '岗位已新增');
      setDraft(emptyJob);
      setEditingId(null);
      setEditorOpen(false);
      await loadJobs(editingId ? jobsPage : 0, appliedJobsKeyword);
    } catch (error) {
      message.error(error.message);
    }
  };

  const editJob = (job) => {
    setEditingId(job.id);
    setDraft({
      name: job.name,
      description: job.description || '',
      active: job.active,
    });
    setEditorOpen(true);
  };

  const createJob = () => {
    setEditingId(null);
    setDraft(emptyJob);
    setEditorOpen(true);
  };

  const closeEditor = () => {
    setEditingId(null);
    setDraft(emptyJob);
    setEditorOpen(false);
  };

  const loadJobPeopleModal = async (job, page = 0) => {
    try {
      setPeopleModalLoading(true);
      const result = await adminApi.allJobUsers('', page, pageSize, job.id);
      setPeopleModalItems(result.items);
      setPeopleModalTotal(result.total);
      setPeopleModalPage(result.page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setPeopleModalLoading(false);
    }
  };

  const openJobPeople = async (job) => {
    setPeopleModal(job);
    setPeopleModalItems([]);
    setPeopleModalPage(0);
    setPeopleModalTotal(job.userCount);
    await loadJobPeopleModal(job, 0);
  };

  const closePeopleModal = () => {
    if (peopleModalLoading) return;
    setPeopleModal(null);
    setPeopleModalItems([]);
  };

  const toggleJobStatus = async (job) => {
    try {
      await adminApi.updateJob(job.id, {
        name: job.name,
        description: job.description || '',
        active: !job.active,
      });
      message.success(job.active ? '岗位已停用' : '岗位已启用');
      await loadJobs(jobsPage, appliedJobsKeyword);
    } catch (error) {
      message.error(error.message);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>岗位管理</h1>
          <p>维护平台岗位，点击关联人数可查看岗位人员</p>
        </div>
        <div className="library-title-actions">
          <span>共 {jobsTotal} 个岗位</span>
          <button className="primary" type="button" onClick={createJob}><Plus />新增岗位</button>
        </div>
      </div>

      <section className="jobs-section library-section">
        <form className="library-toolbar" onSubmit={searchJobs}>
          <label className="library-search-field">
            <Search />
            <input value={jobsKeyword} onChange={(event) => setJobsKeyword(event.target.value)} placeholder="搜索岗位名称" />
          </label>
          <button className="primary" type="submit">
            搜索
          </button>
          {appliedJobsKeyword && (
            <button
              className="plain"
              type="button"
              onClick={() => {
                setJobsKeyword('');
                setAppliedJobsKeyword('');
                loadJobs(0, '');
              }}
            >
              清空
            </button>
          )}
        </form>

        <div className="table-card library-table-card">
          <table>
            <thead>
              <tr>
                <th>岗位</th>
                <th>人员</th>
                <th>关联事情</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {jobs.map((job) => (
                <tr key={job.id}>
                  <td>
                    <b>{job.name}</b>
                    <small>{job.description || '暂无介绍'}</small>
                  </td>
                  <td>
                    <button
                      className="library-count-link"
                      type="button"
                      onClick={() => openJobPeople(job)}
                    >
                      <UsersRound />
                      <span>{job.userCount} 人</span>
                      <em>查看</em>
                      <ChevronRight />
                    </button>
                  </td>
                  <td>{job.matterCount} 件</td>
                  <td>
                    <span className={`status ${job.active ? 'active' : 'suspended'}`}>
                      {job.active ? '启用' : '停用'}
                    </span>
                  </td>
                  <td className="row-actions">
                    <button className="plain" type="button" onClick={() => editJob(job)}>
                      编辑
                    </button>
                    <button className="plain" type="button" onClick={() => toggleJobStatus(job)}>
                      {job.active ? '停用' : '启用'}
                    </button>
                    <button
                      className="danger"
                      type="button"
                      onClick={() =>
                        setConfirmation({
                          title: '删除岗位',
                          message: `确定删除岗位“${job.name}”吗？`,
                          action: () => adminApi.deleteJob(job.id),
                        })
                      }
                    >
                      删除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!jobs.length && <div className="empty">暂无岗位</div>}
        </div>
        <Pagination
          page={jobsPage}
          size={pageSize}
          total={jobsTotal}
          onChange={(page) => loadJobs(page, appliedJobsKeyword)}
        />
      </section>

      {editorOpen && (
        <>
          <div className="modal-mask" onClick={closeEditor} />
          <section className="detail-modal job-editor-modal" role="dialog" aria-modal="true">
            <header>
              <div>
                <h2>{editingId ? '编辑岗位' : '新增岗位'}</h2>
                <p>维护岗位名称和介绍</p>
              </div>
              <button type="button" aria-label="关闭" onClick={closeEditor}>
                <X />
              </button>
            </header>
            <form className="job-editor-form" onSubmit={submitJob}>
              <label>
                <span>岗位名称</span>
                <input
                  required
                  maxLength="80"
                  value={draft.name}
                  onChange={(event) => setDraft({ ...draft, name: event.target.value })}
                  placeholder="例如：UI设计师"
                />
              </label>
              <label>
                <span>一句话介绍</span>
                <input
                  maxLength="240"
                  value={draft.description}
                  onChange={(event) => setDraft({ ...draft, description: event.target.value })}
                  placeholder="说明这个岗位主要能提供什么帮助"
                />
              </label>
              <footer>
                <button className="plain" type="button" onClick={closeEditor}>
                  取消
                </button>
                <button className="primary" type="submit">
                  {editingId ? '保存修改' : '确认新增'}
                </button>
              </footer>
            </form>
          </section>
        </>
      )}

      {peopleModal && (
        <>
          <div
            className="modal-mask"
            onClick={closePeopleModal}
          />
          <section className="detail-modal job-people-modal" role="dialog" aria-modal="true">
            <header>
              <div>
                <h2>{peopleModal.name} · 岗位人员</h2>
                <p>共 {peopleModalTotal} 人</p>
              </div>
              <button type="button" aria-label="关闭" onClick={closePeopleModal}>
                <X />
              </button>
            </header>
            <div className="job-people-modal-body">
              <table>
                <thead>
                  <tr>
                    <th>用户</th>
                    <th>UID</th>
                    <th>工龄</th>
                    <th>认证状态</th>
                    <th>账号状态</th>
                  </tr>
                </thead>
                <tbody>
                  {peopleModalItems.map((person) => (
                    <tr key={person.relationId}>
                      <td>
                        <div className="job-person-cell">
                          <div className="job-person-avatar">
                            {person.avatarUrl ? (
                              <img src={person.avatarUrl} alt="" />
                            ) : (
                              <span>{(person.nickname || person.uid).slice(0, 1)}</span>
                            )}
                          </div>
                          <b>{person.nickname || `UID ${person.uid}`}</b>
                        </div>
                      </td>
                      <td>{person.uid}</td>
                      <td>{person.years ? `${person.years} 年` : '未记录'}</td>
                      <td>
                        <span className={`status ${person.verified ? 'active' : 'suspended'}`}>
                          {person.verified ? '已认证' : '未认证'}
                        </span>
                      </td>
                      <td>{person.accountStatus === 'ACTIVE' ? '正常' : '已停用'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!peopleModalLoading && !peopleModalItems.length && (
                <div className="job-people-modal-empty">该岗位下暂无人员</div>
              )}
            </div>
            <Pagination
              page={peopleModalPage}
              size={pageSize}
              total={peopleModalTotal}
              onChange={(page) => loadJobPeopleModal(peopleModal, page)}
            />
          </section>
        </>
      )}

      <ConfirmDialog
        open={Boolean(confirmation)}
        title={confirmation?.title}
        message={confirmation?.message}
        confirmText="确认删除"
        danger
        onCancel={() => setConfirmation(null)}
        onConfirm={async () => {
          try {
            await confirmation.action();
            setConfirmation(null);
            await loadJobs(jobsPage, appliedJobsKeyword);
            message.success('岗位已删除');
          } catch (error) {
            message.error(error.message);
          }
        }}
      />
    </>
  );
}
