import { useEffect, useMemo, useState } from 'react';
import { FileAudio, FileVideo, Search, Upload, X } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import { date, Empty } from '../users/UsersPage.jsx';
import '../shared/Page.css';
import './OfflineCertificationsPage.css';

const PAGE_SIZE = 20;

export default function OfflineCertificationsPage() {
  const { can } = useAdminAccess();
  const [data, setData] = useState({ items: [], total: 0 });
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [status, setStatus] = useState('BOOKED');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [materials, setMaterials] = useState([]);
  const [jobs, setJobs] = useState([]);
  const [jobKeyword, setJobKeyword] = useState('');
  const [selectedJob, setSelectedJob] = useState(null);
  const [resultStatus, setResultStatus] = useState('APPROVED');
  const [years, setYears] = useState('');
  const [authenticityPercent, setAuthenticityPercent] = useState('');
  const [reason, setReason] = useState('');
  const [evidence, setEvidence] = useState(null);
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    load(0, appliedKeyword, status);
  }, [status]);

  const load = async (
    nextPage = page,
    nextKeyword = appliedKeyword,
    nextStatus = status,
  ) => {
    try {
      const result = await adminApi.offlineCertificationAppointments({
        keyword: nextKeyword,
        status: nextStatus,
        page: nextPage,
        size: PAGE_SIZE,
      });
      setData(result);
      setPage(nextPage);
    } catch (error) {
      message.error(error.message);
    }
  };

  const search = async (event) => {
    event.preventDefault();
    const nextKeyword = keyword.trim();
    setAppliedKeyword(nextKeyword);
    await load(0, nextKeyword, status);
  };

  const open = async (item) => {
    setSelected(item);
    setMaterials([]);
    setJobKeyword('');
    setSelectedJob(null);
    setResultStatus('APPROVED');
    setYears('');
    setAuthenticityPercent('');
    setReason('');
    setEvidence(null);
    try {
      const requests = [];
      if (item.certificationId) {
        requests.push(
          adminApi.offlineCertificationAppointmentMaterials(item.id).then(setMaterials),
        );
      }
      if (item.status === 'BOOKED' && can('OFFLINE_APPOINTMENT_PROCESS')) {
        requests.push(adminApi.jobOptions().then(setJobs));
      }
      await Promise.all(requests);
    } catch (error) {
      message.error(error.message);
    }
  };

  const matchingJobs = useMemo(() => {
    const query = jobKeyword.trim().toLowerCase();
    if (!query) return jobs.slice(0, 20);
    return jobs
      .filter((job) => String(job.name || '').toLowerCase().includes(query))
      .slice(0, 20);
  }, [jobKeyword, jobs]);

  const process = async () => {
    if (!selected || processing) return;
    if (resultStatus === 'APPROVED') {
      if (!selectedJob) {
        message.warning('请选择认证岗位');
        return;
      }
      const parsedYears = Number(years);
      if (!Number.isInteger(parsedYears) || parsedYears < 5 || parsedYears > 80) {
        message.warning('工龄需填写5至80之间的整数');
        return;
      }
      if (!evidence) {
        message.warning('认证通过前必须上传现场录音或录像');
        return;
      }
    }
    if (['APPROVED', 'REJECTED'].includes(resultStatus) && !authenticityPercent) {
      message.warning('请选择材料真实程度');
      return;
    }
    if (['REJECTED', 'CANCELLED'].includes(resultStatus) && !reason.trim()) {
      message.warning(resultStatus === 'REJECTED' ? '请填写不通过原因' : '请填写取消原因');
      return;
    }

    try {
      setProcessing(true);
      await adminApi.processOfflineCertificationAppointment(selected.id, {
        status: resultStatus,
        reason: reason.trim(),
        jobId: selectedJob?.id,
        years: years ? Number(years) : null,
        authenticityPercent: authenticityPercent
          ? Number(authenticityPercent)
          : null,
        evidence,
      });
      message.success(resultMessage(resultStatus));
      setSelected(null);
      await load(page);
    } catch (error) {
      message.error(error.message);
    } finally {
      setProcessing(false);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>线下认证</h1>
          <p>查看预约记录，并在现场完成后确认岗位认证结果</p>
        </div>
        <span>共 {data.total} 条</span>
      </div>

      <form className="toolbar" onSubmit={search}>
        <label>
          <Search aria-hidden="true" />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="用户UID、手机号或昵称"
            maxLength={30}
          />
        </label>
        <select value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="">全部状态</option>
          <option value="BOOKED">已预约</option>
          <option value="APPROVED">认证通过</option>
          <option value="REJECTED">认证不通过</option>
          <option value="NO_SHOW">未到场</option>
          <option value="CANCELLED">已取消</option>
        </select>
        <button type="submit">查询</button>
      </form>

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>用户</th>
              <th>预约时间</th>
              <th>地点</th>
              <th>状态</th>
              <th>处理时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((item) => (
              <tr key={item.id}>
                <td>
                  <b>{item.nickname || `UID ${item.uid}`}</b>
                  <small>UID {item.uid} · {item.phone}</small>
                </td>
                <td>{date(item.appointmentAt)}</td>
                <td>{item.city}</td>
                <td><AppointmentStatus value={item.status} /></td>
                <td>{date(item.processedAt)}</td>
                <td className="row-actions">
                  <button type="button" className="plain" onClick={() => open(item)}>
                    {item.status === 'BOOKED' && can('OFFLINE_APPOINTMENT_PROCESS')
                      ? '处理'
                      : '查看'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <Empty />}
      </div>

      <Pagination
        page={page}
        size={PAGE_SIZE}
        total={data.total}
        onChange={(nextPage) => load(nextPage)}
      />

      {selected && (
        <>
          <div className="modal-mask" onClick={() => setSelected(null)} />
          <section className="detail-modal offline-certification-drawer" role="dialog" aria-modal="true">
            <header>
              <div>
                <h2>线下认证记录</h2>
                <p>预约 #{selected.id}</p>
              </div>
              <button type="button" aria-label="关闭" onClick={() => setSelected(null)}>
                <X />
              </button>
            </header>

            <div className="detail-fields">
              <div><span>用户</span><b>{selected.nickname || `UID ${selected.uid}`}</b></div>
              <div><span>UID</span><b>{selected.uid}</b></div>
              <div><span>手机号</span><b>{selected.phone}</b></div>
              <div><span>预约地点</span><b>{selected.city}</b></div>
              <div><span>预约时间</span><b>{date(selected.appointmentAt)}</b></div>
              <div><span>当前状态</span><b>{statusLabel(selected.status)}</b></div>
              {selected.processedBy && <div><span>处理人</span><b>{selected.processedBy}</b></div>}
              {selected.processedAt && <div><span>处理时间</span><b>{date(selected.processedAt)}</b></div>}
              {selected.authenticityPercent != null && (
                <div><span>材料真实程度</span><b>{selected.authenticityPercent}%</b></div>
              )}
              {selected.jobReapplyAvailableAt && (
                <div><span>可再次申请</span><b>{date(selected.jobReapplyAvailableAt)}</b></div>
              )}
            </div>

            {selected.resultReason && (
              <div className="offline-result-reason">
                <span>处理说明</span>
                <p>{selected.resultReason}</p>
              </div>
            )}

            {materials.length > 0 && (
              <>
                <h3>现场认证凭证</h3>
                <div className="offline-materials">
                  {materials.map((material) => (
                    <EvidenceMaterial key={material.id} material={material} />
                  ))}
                </div>
              </>
            )}

            {selected.status === 'BOOKED' && can('OFFLINE_APPOINTMENT_PROCESS') && (
              <div className="offline-process-form">
                <label>
                  <span>处理结果</span>
                  <select
                    value={resultStatus}
                    onChange={(event) => setResultStatus(event.target.value)}
                  >
                    <option value="APPROVED">认证通过</option>
                    <option value="REJECTED">认证不通过</option>
                    <option value="NO_SHOW">未到场</option>
                    <option value="CANCELLED">取消预约</option>
                  </select>
                </label>

                {['APPROVED', 'REJECTED'].includes(resultStatus) && (
                  <OfflineAuthenticitySelector
                    value={authenticityPercent}
                    onChange={setAuthenticityPercent}
                  />
                )}

                {resultStatus === 'APPROVED' && (
                  <>
                    <label className="offline-job-picker">
                      <span>认证岗位</span>
                      <input
                        value={jobKeyword}
                        onChange={(event) => {
                          setJobKeyword(event.target.value);
                          setSelectedJob(null);
                        }}
                        placeholder="输入岗位名称搜索"
                        maxLength={40}
                      />
                      <div className="offline-job-options">
                        {matchingJobs.map((job) => (
                          <button
                            type="button"
                            className={selectedJob?.id === job.id ? 'selected' : ''}
                            key={job.id}
                            onClick={() => {
                              setSelectedJob(job);
                              setJobKeyword(job.name);
                            }}
                          >
                            {job.name}
                          </button>
                        ))}
                        {!matchingJobs.length && <small>没有匹配的岗位</small>}
                      </div>
                    </label>
                    <label>
                      <span>认证工龄</span>
                      <input
                        type="number"
                        min="5"
                        max="80"
                        step="1"
                        value={years}
                        onChange={(event) => setYears(event.target.value)}
                        placeholder="填写累计工龄，至少5年"
                      />
                    </label>
                    <label className="offline-evidence-upload">
                      <span>现场认证凭证</span>
                      <input
                        type="file"
                        accept="audio/*,video/*,.mp3,.wav,.m4a,.aac,.ogg,.flac,.mp4,.webm"
                        onChange={(event) => {
                          const file = event.target.files?.[0] || null;
                          if (file && file.size > 500 * 1024 * 1024) {
                            message.warning('录音或录像不能超过500MB');
                            event.target.value = '';
                            setEvidence(null);
                            return;
                          }
                          setEvidence(file);
                        }}
                      />
                      <div>
                        <Upload />
                        <b>{evidence ? evidence.name : '上传现场录音或录像'}</b>
                        <small>认证通过时必传，最大500MB</small>
                      </div>
                    </label>
                  </>
                )}

                {['REJECTED', 'CANCELLED', 'NO_SHOW'].includes(resultStatus) && (
                  <label>
                    <span>{resultStatus === 'REJECTED' ? '不通过原因' : '处理说明'}</span>
                    <textarea
                      value={reason}
                      onChange={(event) => setReason(event.target.value)}
                      placeholder={resultStatus === 'NO_SHOW' ? '选填' : '请填写具体原因'}
                      maxLength={300}
                    />
                  </label>
                )}

                <footer>
                  <button type="button" className="primary" disabled={processing} onClick={process}>
                    {processing ? '处理中' : '确认处理结果'}
                  </button>
                </footer>
              </div>
            )}
          </section>
        </>
      )}
    </>
  );
}

function EvidenceMaterial({ material }) {
  const kind = String(material.kind || '').toUpperCase();
  if (kind === 'AUDIO') {
    return (
      <div className="offline-material audio">
        <FileAudio />
        <span>{material.name}</span>
        <audio src={material.url} controls preload="metadata" />
      </div>
    );
  }
  return (
    <div className="offline-material video">
      <div><FileVideo /><span>{material.name}</span></div>
      <video src={material.url} controls preload="metadata" />
    </div>
  );
}

function statusLabel(value) {
  return {
    BOOKED: '已预约',
    APPROVED: '认证通过',
    REJECTED: '认证不通过',
    NO_SHOW: '未到场',
    CANCELLED: '已取消',
  }[value] || value;
}

function AppointmentStatus({ value }) {
  const style = {
    BOOKED: 'pending',
    APPROVED: 'approved',
    REJECTED: 'rejected',
    NO_SHOW: 'failed',
    CANCELLED: 'cancelled',
  }[value] || '';
  return <span className={`status ${style}`}>{statusLabel(value)}</span>;
}

function resultMessage(value) {
  return {
    APPROVED: '岗位认证已通过',
    REJECTED: '岗位认证已标记为不通过',
    NO_SHOW: '已记录用户未到场',
    CANCELLED: '预约已取消',
  }[value] || '处理完成';
}

const AUTHENTICITY_LEVELS = [
  [100, 0],
  [90, 6],
  [80, 12],
  [60, 18],
  [51, 24],
  [40, 30],
  [20, 36],
  [0, 42],
];

function OfflineAuthenticitySelector({ value, onChange }) {
  return (
    <div className="offline-authenticity-selector">
      <span>材料真实程度</span>
      <div>
        {AUTHENTICITY_LEVELS.map(([percent, months]) => (
          <button
            type="button"
            className={value === String(percent) ? 'selected' : ''}
            key={percent}
            onClick={() => onChange(String(percent))}
          >
            <b>{percent}%</b>
            <small>{months === 0 ? '不限制' : `${months}个月`}</small>
          </button>
        ))}
      </div>
      <small>下方时间为再次申请线上或线下岗位认证的间隔</small>
    </div>
  );
}
