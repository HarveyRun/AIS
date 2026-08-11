import { useEffect, useState } from 'react';
import { X, ExternalLink } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { date, Empty, Status } from '../users/UsersPage.jsx';
import '../shared/Page.css';
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
  const [selected, setSelected] = useState(null);
  const [materials, setMaterials] = useState([]);
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const load = () =>
    adminApi
      .table(type, new URLSearchParams({ status }).toString())
      .then(setData)
      .catch((e) => setError(e.message));
  useEffect(() => {
    setSelected(null);
    load();
  }, [type, status]);
  const open = async (row) => {
    setSelected(row);
    setReason('');
    if (type === 'certifications') setMaterials(await adminApi.materials(row.id));
  };
  const review = async (approved) => {
    await adminApi.review(selected.id, { approved, reason });
    setSelected(null);
    load();
  };
  const process = async (statusValue) => {
    if (type === 'withdrawals') await adminApi.withdrawalStatus(selected.id, statusValue);
    else await adminApi.recordStatus(type, selected.id, statusValue);
    setSelected(null);
    load();
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
          <option value="PENDING">待处理</option>
          <option value="SUBMITTED">待处理</option>
          <option value="PROCESSING">处理中</option>
          <option value="APPROVED">已通过</option>
          <option value="COMPLETED">已完成</option>
          <option value="REJECTED">已驳回</option>
          <option value="RESOLVED">已解决</option>
          <option value="CLOSED">已关闭</option>
        </select>
      </div>
      {error && <div className="error-box">{error}</div>}
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
          <b>{r.title}</b>
          <small>{r.category}</small>
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
