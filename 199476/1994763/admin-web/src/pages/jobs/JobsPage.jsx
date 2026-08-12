import { useEffect, useMemo, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './JobsPage.css';

const emptyJob = {
  name: '',
  description: '',
  active: true,
};

export default function JobsPage() {
  const [jobs, setJobs] = useState([]);
  const [people, setPeople] = useState([]);
  const [tab, setTab] = useState('jobs');
  const [draft, setDraft] = useState(emptyJob);
  const [editingId, setEditingId] = useState(null);
  const [selectedJobId, setSelectedJobId] = useState('');
  const [confirmation, setConfirmation] = useState(null);

  const selectedJob = useMemo(
    () => jobs.find((job) => job.id === Number(selectedJobId)),
    [jobs, selectedJobId],
  );

  const loadJobs = async () => {
    try {
      const result = await adminApi.jobs();
      setJobs(result);
      return result;
    } catch (error) {
      message.error(error.message);
      return null;
    }
  };

  useEffect(() => {
    loadJobs();
  }, []);

  const chooseJob = async (jobId) => {
    setSelectedJobId(String(jobId));
    setPeople([]);

    try {
      setPeople(await adminApi.jobUsers(jobId));
    } catch (error) {
      message.error(error.message);
    }
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
      await loadJobs();
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
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>岗位管理</h1>
          <p>维护岗位库，并查看每个岗位下的人员</p>
        </div>
      </div>

      <div className="discovery-tabs">
        <button
          className={tab === 'jobs' ? 'active' : ''}
          onClick={() => setTab('jobs')}
        >
          岗位库
        </button>
        <button
          className={tab === 'people' ? 'active' : ''}
          onClick={() => setTab('people')}
        >
          岗位人员
        </button>
      </div>

      {tab === 'jobs' && (
        <section className="jobs-section">
          <form className="management-form" onSubmit={submitJob}>
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

            <label className="wide">
              <span>一句话介绍</span>
              <input
                maxLength="240"
                value={draft.description}
                onChange={(event) => setDraft({ ...draft, description: event.target.value })}
                placeholder="说明这个岗位主要能提供什么帮助"
              />
            </label>

            {editingId && (
              <label>
                <span>状态</span>
                <select
                  value={String(draft.active)}
                  onChange={(event) => setDraft({
                    ...draft,
                    active: event.target.value === 'true',
                  })}
                >
                  <option value="true">启用</option>
                  <option value="false">停用</option>
                </select>
              </label>
            )}

            <div className="form-actions">
              <button className="primary" type="submit">
                {editingId ? '保存修改' : '新增岗位'}
              </button>
              {editingId && (
                <button
                  className="plain"
                  type="button"
                  onClick={() => {
                    setEditingId(null);
                    setDraft(emptyJob);
                  }}
                >
                  取消
                </button>
              )}
            </div>
          </form>

          <div className="table-card">
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
                    <td>{job.userCount} 人</td>
                    <td>{job.matterCount} 件</td>
                    <td>
                      <span className={`status ${job.active ? 'active' : 'suspended'}`}>
                        {job.active ? '启用' : '停用'}
                      </span>
                    </td>
                    <td className="row-actions">
                      <button className="plain" onClick={() => editJob(job)}>编辑</button>
                      <button
                        className="danger"
                        onClick={() => setConfirmation({
                          title: '删除岗位',
                          message: `确定删除岗位“${job.name}”吗？`,
                          action: () => adminApi.deleteJob(job.id),
                        })}
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
        </section>
      )}

      {tab === 'people' && (
        <section className="job-people-layout">
          <aside>
            {jobs.map((job) => (
              <button
                className={selectedJobId === String(job.id) ? 'active' : ''}
                key={job.id}
                onClick={() => chooseJob(job.id)}
              >
                <b>{job.name}</b>
                <small>{job.userCount} 人</small>
              </button>
            ))}
          </aside>

          <main>
            {selectedJob ? (
              <>
                <header>
                  <div>
                    <h2>{selectedJob.name}</h2>
                    <p>共 {people.length} 人</p>
                  </div>
                </header>

                <div className="job-people-list">
                  {people.map((person) => (
                    <article key={person.userId}>
                      <div className="job-person-avatar">
                        {person.avatarUrl ? (
                          <img src={person.avatarUrl} alt="" />
                        ) : (
                          <span>{(person.nickname || person.uid).slice(0, 1)}</span>
                        )}
                      </div>
                      <div className="job-person-name">
                        <b>{person.nickname || `UID ${person.uid}`}</b>
                        <small>UID {person.uid}</small>
                      </div>
                      <div className="job-person-details">
                        <span>{person.years ? `${person.years}年工龄` : '工龄未记录'}</span>
                        <span>{person.verified ? '已认证' : '未认证'}</span>
                      </div>
                    </article>
                  ))}
                  {!people.length && (
                    <div className="job-people-empty">该岗位下暂无人员</div>
                  )}
                </div>
              </>
            ) : (
              <div className="jobs-placeholder">请选择一个岗位查看人员</div>
            )}
          </main>
        </section>
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
            await loadJobs();
            message.success('岗位已删除');
          } catch (error) {
            message.error(error.message);
          }
        }}
      />
    </>
  );
}
