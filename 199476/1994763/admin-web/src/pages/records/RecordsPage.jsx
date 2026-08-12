import { useEffect, useMemo, useState } from 'react';
import { X, ExternalLink } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { date, Empty, Status } from '../users/UsersPage.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import '../shared/Page.css';
import { message } from '../../components/feedback/message.js';
const meta = {
  certifications: ['认证审核', '核对用户提交的身份、岗位与经历材料'],
  inquiries: ['询问管理', '查看询问状态和资金流转'],
  withdrawals: ['提现处理', '核对并处理银行卡提现申请'],
  feedback: ['投诉反馈', '处理产品反馈与用户投诉'],
  cooperations: ['商务合作', '查看并跟进商务合作申请'],
};
export default function RecordsPage({ type }) {
  const [data, setData] = useState({ items: [], total: 0 });
  const [status, setStatus] = useState('');
  const [certificationCategory, setCertificationCategory] = useState('BASIC');
  const [selected, setSelected] = useState(null);
  const [materials, setMaterials] = useState([]);
  const [jobs, setJobs] = useState([]);
  const [reason, setReason] = useState('');
  const [selectedJobId, setSelectedJobId] = useState('');
  const [jobKeyword, setJobKeyword] = useState('');
  const [workYears, setWorkYears] = useState('');
  const [page, setPage] = useState(0);
  const size = 20;
  const visibleJobs = useMemo(() => {
    const keyword = jobKeyword.trim().toLowerCase();
    const matched = keyword
      ? jobs.filter((job) => (
          job.name.toLowerCase().includes(keyword)
          || (job.description || '').toLowerCase().includes(keyword)
        ))
      : jobs;

    return matched.slice(0, 20);
  }, [jobs, jobKeyword]);
  const load = (targetPage = page) => {
    return adminApi
      .table(type, new URLSearchParams({
        status,
        category: type === 'certifications' ? certificationCategory : '',
        page: targetPage,
        size,
      }).toString())
      .then(setData)
      .catch((e) => message.error(e.message));
  };
  useEffect(() => {
    setSelected(null);
    setPage(0);
    load(0);
  }, [type, status, certificationCategory]);
  const open = async (row) => {
    try {
      setSelected(row);
      setReason('');
      setSelectedJobId('');
      setJobKeyword('');
      setWorkYears(row.type === 'MAIN_JOB' && row.years != null ? String(row.years) : '');
      setMaterials([]);
      setJobs([]);
      if (type === 'certifications') {
        const requests = [adminApi.materials(row.id)];
        if (row.type === 'MAIN_JOB' && row.status === 'PENDING') {
          requests.push(adminApi.jobs());
        }
        const [materialResult, jobResult = []] = await Promise.all(requests);
        setMaterials(materialResult);
        setJobs(jobResult.filter((job) => job.active));
      }
    } catch (e) {
      setSelected(null);
      message.error(e.message);
    }
  };
  const review = async (approved) => {
    if (!approved && !reason.trim()) {
      message.warning('驳回时请填写原因');
      return;
    }
    try {
      if (approved && selected.type === 'MAIN_JOB' && !selectedJobId) {
        message.warning('请选择审核判定的岗位');
        return;
      }
      if (approved && selected.type === 'MAIN_JOB' && !/^\d+$/.test(workYears)) {
        message.warning('请填写工龄');
        return;
      }
      const numericYears = Number(workYears);
      if (approved && selected.type === 'MAIN_JOB' && (numericYears < 1 || numericYears > 80)) {
        message.warning('工龄必须是1至80之间的整数');
        return;
      }
      await adminApi.review(selected.id, {
        approved,
        reason: reason.trim(),
        jobId: selected.type === 'MAIN_JOB' ? Number(selectedJobId) : null,
        years: selected.type === 'MAIN_JOB' ? numericYears : null,
      });
      setSelected(null);
      await load();
      message.success(approved ? '认证已通过' : '认证已驳回');
    } catch (e) {
      message.error(e.message);
    }
  };
  const process = async (statusValue) => {
    try {
      if (type === 'withdrawals') await adminApi.withdrawalStatus(selected.id, statusValue);
      else await adminApi.recordStatus(type, selected.id, statusValue);
      setSelected(null);
      await load();
      message.success(type === 'withdrawals' ? '提现状态已更新' : '处理状态已更新');
    } catch (e) {
      message.error(e.message);
    }
  };
  return (
    <>
      <div className="page-title">
        <div>
          <h1>{meta[type][0]}</h1>
          <p>{meta[type][1]}</p>
        </div>
        <span>共 {data.total} 条</span>
      </div>
      <div className="toolbar">
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">全部状态</option>
          {statusOptions[type].map(([value, label]) => (
            <option value={value} key={value}>
              {label}
            </option>
          ))}
        </select>
      </div>
      {type === 'certifications' && (
        <div className="certification-tabs">
          <button className={certificationCategory === 'BASIC' ? 'active' : ''} onClick={() => setCertificationCategory('BASIC')}>基础信息</button>
          <button className={certificationCategory === 'EXPERIENCE' ? 'active' : ''} onClick={() => setCertificationCategory('EXPERIENCE')}>亲身经历</button>
        </div>
      )}
      <Pagination
        page={page}
        size={size}
        total={data.total}
        onChange={(next) => {
          setPage(next);
          load(next);
        }}
      />
      <div className="table-card">
        <table>
          <thead>
            <tr>
              {headers(type).map((x) => (
                <th key={x}>{x}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.items.map((row) => (
              <tr key={row.id}>
                {cells(type, row)}
                <td>
                  <button className="plain" onClick={() => open(row)}>
                    查看处理
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <Empty />}
      </div>
      {selected && (
        <>
          <button className="modal-mask" onClick={() => setSelected(null)} />
          <section className="detail-modal">
            <header>
              <div>
                <h2>{meta[type][0]}详情</h2>
                <p>编号 #{selected.id}</p>
              </div>
              <button onClick={() => setSelected(null)}>
                <X />
              </button>
            </header>
            <div className="detail-fields">
              {Object.entries(selected)
                .filter(([k]) => k !== 'id')
                .map(([k, v]) => (
                  <div key={k}>
                    <span>{labels[k] || k}</span>
                    <b>
                      {k.toLowerCase().includes('time') || k.endsWith('At')
                        ? date(v)
                        : String(v ?? '—')}
                    </b>
                  </div>
                ))}
            </div>
            {type === 'certifications' && (
              <>
                <h3>认证材料</h3>
                <div className="materials">
                  {materials.map((m) => (
                    <a href={m.url} target="_blank" rel="noreferrer" key={m.id}>
                      <span>{m.name}</span>
                      <ExternalLink />
                    </a>
                  ))}
                </div>
                {selected.status === 'PENDING' && (
                  <>
                    {selected.type === 'MAIN_JOB' && (
                      <>
                        <label className="review-job-field">
                          <span>审核判定岗位</span>
                          <input
                            value={jobKeyword}
                            onChange={(event) => {
                              setJobKeyword(event.target.value);
                              setSelectedJobId('');
                            }}
                            placeholder="输入岗位名称搜索"
                          />
                          <div className="review-job-options">
                            {visibleJobs.map((job) => (
                              <button
                                type="button"
                                className={selectedJobId === String(job.id) ? 'selected' : ''}
                                key={job.id}
                                onClick={() => {
                                  setSelectedJobId(String(job.id));
                                  setJobKeyword(job.name);
                                }}
                              >
                                <b>{job.name}</b>
                                {job.description && <small>{job.description}</small>}
                              </button>
                            ))}
                            {!visibleJobs.length && (
                              <p>没有找到相关岗位，请先到岗位管理中新增</p>
                            )}
                          </div>
                          <small>只可选择岗位库中已启用的岗位</small>
                        </label>
                        <label className="review-job-field">
                          <span>工龄</span>
                          <input
                            type="number"
                            min="1"
                            max="80"
                            step="1"
                            value={workYears}
                            onChange={(event) => setWorkYears(event.target.value)}
                            placeholder="请输入整数年数"
                          />
                          <small>岗位认证通过后，将展示在该用户档案中</small>
                        </label>
                      </>
                    )}
                    <textarea
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      placeholder="驳回时必须填写原因"
                    />
                    <footer>
                      <button className="danger" onClick={() => review(false)}>
                        驳回
                      </button>
                      <button className="primary" onClick={() => review(true)}>
                        通过认证
                      </button>
                    </footer>
                  </>
                )}
              </>
            )}
            {type === 'withdrawals' && selected.status === 'PROCESSING' && (
              <footer>
                <button className="danger" onClick={() => process('FAILED')}>
                  标记失败并退款
                </button>
                <button className="primary" onClick={() => process('COMPLETED')}>
                  确认已到账
                </button>
              </footer>
            )}
            {['feedback', 'cooperations'].includes(type) &&
              !['RESOLVED', 'CLOSED'].includes(selected.status) && (
                <footer>
                  <button className="plain" onClick={() => process('PROCESSING')}>
                    处理中
                  </button>
                  <button
                    className="primary"
                    onClick={() => process(type === 'feedback' ? 'RESOLVED' : 'CLOSED')}
                  >
                    完成处理
                  </button>
                </footer>
              )}
          </section>
        </>
      )}
    </>
  );
}
const statusOptions = {
  certifications: [
    ['PENDING', '待审核'],
    ['APPROVED', '已通过'],
    ['REJECTED', '已驳回'],
  ],
  inquiries: [
    ['PENDING', '待接受'],
    ['ACTIVE', '交流中'],
    ['AWAITING_CONFIRMATION', '待确认结束'],
    ['COMPLETED', '已完成'],
    ['REJECTED', '未接受'],
    ['CANCELLED', '已撤销'],
    ['EXPIRED', '已过期'],
    ['REFUNDED', '已退款'],
  ],
  withdrawals: [
    ['PROCESSING', '处理中'],
    ['COMPLETED', '已完成'],
    ['FAILED', '失败'],
  ],
  feedback: [
    ['SUBMITTED', '待处理'],
    ['PROCESSING', '处理中'],
    ['RESOLVED', '已解决'],
  ],
  cooperations: [
    ['SUBMITTED', '待处理'],
    ['PROCESSING', '处理中'],
    ['CLOSED', '已关闭'],
  ],
};
const labels = {
  uid: '用户UID',
  nickname: '昵称',
  category: '认证分类',
  type: '类型',
  title: '标题',
  description: '说明',
  years: '年限',
  status: '状态',
  rejectionReason: '驳回原因',
  submittedAt: '提交时间',
  topic: '主题',
  question: '询问内容',
  amount: '金额',
  fundsStatus: '资金状态',
  createdAt: '创建时间',
  questionerUid: '提问者UID',
  answererUid: '回答者UID',
  fee: '手续费',
  arrivalAmount: '到账金额',
  bankName: '银行',
  lastFour: '卡号后四位',
  contact: '联系方式',
  content: '内容',
  targetUid: '投诉对象UID',
};
function headers(type) {
  return {
    certifications: ['用户', '认证', '状态', '提交时间', '操作'],
    inquiries: ['双方UID', '询问内容', '金额', '状态', '操作'],
    withdrawals: ['用户', '到账银行卡', '金额 / 手续费', '状态', '操作'],
    feedback: ['用户', '类型', '内容', '状态', '操作'],
    cooperations: ['用户', '联系方式', '内容', '状态', '操作'],
  }[type];
}
function cells(type, r) {
  if (type === 'certifications')
    return (
      <>
        <td>
          <b>{r.nickname || `UID ${r.uid}`}</b>
          <small>UID {r.uid}</small>
        </td>
        <td>
          <b>{certificationTitle(r)}</b>
          <small>{certificationTypeName(r.type)}</small>
        </td>
        <td>
          <Status value={r.status} />
        </td>
        <td>{date(r.submittedAt)}</td>
      </>
    );
  if (type === 'inquiries')
    return (
      <>
        <td>
          {r.questionerUid} → {r.answererUid}
        </td>
        <td>
          <b>{r.topic || '未填写主题'}</b>
          <small>{r.question}</small>
        </td>
        <td>¥{r.amount}</td>
        <td>
          <Status value={r.status} />
        </td>
      </>
    );
  if (type === 'withdrawals')
    return (
      <>
        <td>
          {r.nickname || r.uid}
          <small>UID {r.uid}</small>
        </td>
        <td>
          {r.bankName}（{r.lastFour}）
        </td>
        <td>
          ¥{r.amount}
          <small>手续费 ¥{r.fee}</small>
        </td>
        <td>
          <Status value={r.status} />
        </td>
      </>
    );
  return (
    <>
      <td>
        {r.nickname || r.uid}
        <small>UID {r.uid}</small>
      </td>
      <td>{r.category || r.contact}</td>
      <td className="long-cell">{r.content}</td>
      <td>
        <Status value={r.status} />
      </td>
    </>
  );
}
function certificationTitle(record) {
  if (record.type === 'IDENTITY') return '身份信息';
  if (record.type === 'MAIN_JOB' && record.status === 'PENDING') return '岗位材料';
  return record.title;
}
function certificationTypeName(type) {
  return { IDENTITY: '基础信息 · 身份', MAIN_JOB: '基础信息 · 岗位', EXPERIENCE: '亲身经历' }[type] || type;
}
