import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import './AnswerQualityPage.css';

const pageSize = 20;

export default function AnswerQualityPage() {
  const [data, setData] = useState({ items: [], total: 0 });
  const [summary, setSummary] = useState({});
  const [page, setPage] = useState(0);
  const [risk, setRisk] = useState('');
  const [keyword, setKeyword] = useState('');

  const load = useCallback(async () => {
    const [head, rows] = await Promise.all([
      adminApi.answerQualitySummary(),
      adminApi.answerQualityEvaluations({ keyword, risk, page, size: pageSize }),
    ]);
    setSummary(head);
    setData(rows);
  }, [keyword, risk, page]);

  useEffect(() => {
    load().catch((error) => message.error(error.message));
  }, [load]);

  return (
    <>
      <div className="page-title">
        <div>
          <h1>交流评价</h1>
          <p>查看提问者在询问结束后提交的交流评价</p>
        </div>
        <span>共 {data.total || 0} 条</span>
      </div>

      <div className="quality-summary">
        <Summary label="全部评价" value={summary.evaluationCount || 0} />
        <Summary label="存在不符合项" value={summary.riskyEvaluationCount || 0} />
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
      </div>

      <EvaluationTable items={data.items || []} />
      <Pagination page={page} size={pageSize} total={data.total || 0} onChange={setPage} />
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
