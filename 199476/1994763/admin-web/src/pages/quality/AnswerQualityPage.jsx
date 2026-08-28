import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import './AnswerQualityPage.css';

const pageSize = 20;

export default function AnswerQualityPage() {
  const { can } = useAdminAccess();
  const [data, setData] = useState({ items: [], total: 0 });
  const [summary, setSummary] = useState({});
  const [page, setPage] = useState(0);
  const [mode, setMode] = useState('reviews');
  const [status, setStatus] = useState('PENDING');
  const [risk, setRisk] = useState('');
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState(null);
  const [evidence, setEvidence] = useState([]);
  const [decisionReason, setDecisionReason] = useState('');
  const [penaltyDuration, setPenaltyDuration] = useState('NONE');

  const load = useCallback(async () => {
    const rowsRequest = mode === 'reviews'
      ? adminApi.answerQualityReviews({ keyword, status, page, size: pageSize })
      : adminApi.answerQualityEvaluations({ keyword, risk, page, size: pageSize });
    const [head, rows] = await Promise.all([
      adminApi.answerQualitySummary(),
      rowsRequest,
    ]);
    setSummary(head);
    setData(rows);
  }, [keyword, status, risk, mode, page]);

  useEffect(() => {
    load().catch((error) => message.error(error.message));
  }, [load]);

  const open = async (id) => {
    try {
      const [detail, messages] = await Promise.all([
        adminApi.answerQualityDetail(id),
        adminApi.answerQualityEvidence(id),
      ]);
      setSelected(detail);
      setEvidence(messages);
      setDecisionReason('');
      setPenaltyDuration('NONE');
    } catch (error) {
      message.error(error.message);
    }
  };

  const resolve = async (decision) => {
    if (!decisionReason.trim()) {
      message.warning('请填写复核结论');
      return;
    }
    try {
      await adminApi.resolveAnswerQuality(selected.id, {
        decision,
        reason: decisionReason.trim(),
        penaltyDuration: decision === 'REFUND' ? penaltyDuration : 'NONE',
      });
      message.success('复核已经处理');
      setSelected(null);
      await load();
    } catch (error) {
      message.error(error.message);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>回答质量</h1>
          <p>处理用户提出的质量复核，只支持全额退款或维持正常结算</p>
        </div>
      </div>

      <div className="quality-summary">
        <Summary label="待复核" value={summary.pending || 0} />
        <Summary label="已退款" value={summary.refunded || 0} />
        <Summary label="正常结算" value={summary.settled || 0} />
        <Summary label="已收到评价" value={summary.evaluationCount || 0} />
        <Summary label="存在明显问题" value={summary.riskyEvaluationCount || 0} />
      </div>

      <div className="quality-tabs">
        <button
          className={mode === 'reviews' ? 'active' : ''}
          onClick={() => { setMode('reviews'); setPage(0); }}
        >
          质量复核
        </button>
        <button
          className={mode === 'evaluations' ? 'active' : ''}
          onClick={() => { setMode('evaluations'); setPage(0); }}
        >
          用户评价
        </button>
      </div>

      <div className="toolbar">
        <input
          value={keyword}
          onChange={(event) => {
            setKeyword(event.target.value);
            setPage(0);
          }}
          placeholder="用户UID或手机号"
        />
        {mode === 'reviews' ? (
          <select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
          >
            <option value="PENDING">待复核</option>
            <option value="REFUNDED">已退款</option>
            <option value="SETTLED">正常结算</option>
            <option value="">全部</option>
          </select>
        ) : (
          <select
            value={risk}
            onChange={(event) => {
              setRisk(event.target.value);
              setPage(0);
            }}
          >
            <option value="">全部评价</option>
            <option value="RISKY">存在不符合项</option>
          </select>
        )}
      </div>

      {mode === 'reviews' ? <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>询问</th>
              <th>提问者</th>
              <th>回答者</th>
              <th>金额</th>
              <th>复核原因</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((row) => (
              <tr key={row.id}>
                <td>
                  #{row.inquiryId}
                  <small>{row.question}</small>
                </td>
                <td>
                  {row.questionerUid}
                  <small>{row.questionerPhone}</small>
                </td>
                <td>
                  {row.answererUid}
                  <small>{row.answererPhone}</small>
                </td>
                <td>¥{row.amount}</td>
                <td>
                  {reviewReason(row.reasonCode)}
                  <small>{row.description}</small>
                </td>
                <td>
                  <span className={`status ${String(row.status).toLowerCase()}`}>
                    {statusText(row.status)}
                  </span>
                </td>
                <td>
                  <button className="plain" onClick={() => open(row.id)}>
                    查看处理
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <div className="empty">暂无记录</div>}
      </div> : <EvaluationTable items={data.items} />}
      <Pagination page={page} size={pageSize} total={data.total || 0} onChange={setPage} />

      {selected && (
        <div className="quality-mask" onClick={() => setSelected(null)}>
          <aside onClick={(event) => event.stopPropagation()}>
            <header>
              <div>
                <h2>质量复核 #{selected.id}</h2>
                <span>
                  询问 #{selected.inquiryId} · ¥{selected.amount}
                </span>
              </div>
              <button onClick={() => setSelected(null)}>×</button>
            </header>

            <section>
              <h3>用户说明</h3>
              <b>{reviewReason(selected.reasonCode)}</b>
              <p>{selected.description}</p>
            </section>

            <section>
              <h3>交流记录</h3>
              <div className="quality-messages">
                {evidence.map((item) => (
                  <div key={item.id}>
                    <b>{item.senderUid}</b>
                    {item.messageType === 'IMAGE' ? (
                      <a href={item.attachmentUrl} target="_blank" rel="noreferrer">
                        查看图片
                      </a>
                    ) : (
                      <p>{item.content}</p>
                    )}
                    <small>{new Date(item.createdAt).toLocaleString('zh-CN')}</small>
                  </div>
                ))}
              </div>
            </section>

            {selected.status === 'PENDING' && can('ANSWER_QUALITY_REVIEW') && (
              <section className="quality-decision">
                <h3>复核结论</h3>
                <textarea
                  value={decisionReason}
                  onChange={(event) => setDecisionReason(event.target.value)}
                  maxLength={500}
                  rows={5}
                  placeholder="说明判断依据和处理结论"
                />
                {can('ANSWER_QUALITY_PENALIZE') && (
                  <label>
                    <span>退款时对回答者的处罚</span>
                    <select
                      value={penaltyDuration}
                      onChange={(event) => setPenaltyDuration(event.target.value)}
                    >
                      <option value="NONE">不处罚</option>
                      <option value="DAYS_3">封禁3天</option>
                      <option value="DAYS_7">封禁7天</option>
                      <option value="DAYS_15">封禁15天</option>
                      <option value="PERMANENT">永久封禁</option>
                    </select>
                  </label>
                )}
              </section>
            )}

            {selected.status === 'PENDING' && can('ANSWER_QUALITY_REVIEW') && (
              <footer>
                <button className="plain" onClick={() => resolve('SETTLE')}>
                  维持正常结算
                </button>
                <button className="danger" onClick={() => resolve('REFUND')}>
                  全额退款
                </button>
              </footer>
            )}
          </aside>
        </div>
      )}
    </>
  );
}

function Summary({ label, value }) {
  return (
    <span>
      {label}
      <b>{value}</b>
    </span>
  );
}

function EvaluationTable({ items }) {
  const dimensions = [
    ['answeredLevel', '回答问题'],
    ['specificLevel', '内容具体'],
    ['matchedLevel', '经历相符'],
    ['usefulLevel', '实际帮助'],
    ['communicationLevel', '交流态度'],
    ['askAgainLevel', '再次询问'],
  ];
  return (
    <div className="table-card">
      <table>
        <thead>
          <tr>
            <th>询问</th>
            <th>提问者</th>
            <th>回答者</th>
            <th>六项评价</th>
            <th>问题标签与说明</th>
            <th>评价时间</th>
          </tr>
        </thead>
        <tbody>
          {items.map((row) => (
            <tr key={row.id}>
              <td>#{row.inquiryId}<small>{row.question}</small></td>
              <td>{row.questionerUid}<small>{row.questionerPhone}</small></td>
              <td>{row.answererUid}<small>{row.answererPhone}</small></td>
              <td>
                <div className="quality-levels">
                  {dimensions.map(([key, label]) => (
                    <span className={`level-${row[key]}`} key={key}>
                      {label}：{levelText(row[key])}
                    </span>
                  ))}
                </div>
              </td>
              <td>
                {negativeTags(row.negativeTags)}
                <small>{row.comment || '未填写补充说明'}</small>
              </td>
              <td>{new Date(row.createdAt).toLocaleString('zh-CN')}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!items.length && <div className="empty">暂无评价</div>}
    </div>
  );
}

function levelText(value) {
  return { 0: '不符合', 1: '一般', 2: '符合' }[Number(value)] || '-';
}

function negativeTags(value) {
  if (!value) return '-';
  try {
    const values = typeof value === 'string' ? JSON.parse(value) : value;
    return Array.isArray(values) && values.length ? values.join('、') : '-';
  } catch {
    return '-';
  }
}

function reviewReason(value) {
  return (
    {
      NO_EFFECTIVE_ANSWER: '没有提供有效回答',
      CLEARLY_OFF_TOPIC: '明显答非所问',
      SUSPECTED_FABRICATION: '疑似虚构经历或身份',
      JOB_MISMATCH: '实际岗位与认证信息不符',
      HARASSMENT: '交流中存在骚扰',
      OFF_PLATFORM_PAYMENT: '引导平台外付款',
      OTHER: '其他问题',
    }[value] || value
  );
}

function statusText(value) {
  return { PENDING: '待复核', REFUNDED: '已全额退款', SETTLED: '正常结算' }[value] || value;
}
