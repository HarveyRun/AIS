import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import { date, Empty } from '../users/UsersPage.jsx';
import '../shared/Page.css';
import './InquiryDisputesPage.css';

const pageSize = 20;

export default function InquiryDisputesPage() {
  const { can } = useAdminAccess();
  const [data, setData] = useState({ items: [], total: 0 });
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('WATCHING');
  const [selected, setSelected] = useState(null);
  const [reason, setReason] = useState('');

  const load = useCallback(async () => {
    setData(await adminApi.inquiryRiskWatch({ keyword, status, page, size: pageSize }));
  }, [keyword, page, status]);

  useEffect(() => {
    load().catch((error) => message.error(error.message));
  }, [load]);

  const close = () => {
    setSelected(null);
    setReason('');
  };

  const submit = async () => {
    if (!reason.trim()) {
      message.warning('请填写查看结果与处理说明');
      return;
    }
    try {
      await adminApi.dismissInquiryRiskWatch(selected.userId, reason.trim());
      message.success('风险关注已处理');
      close();
      await load();
    } catch (error) {
      message.error(error.message);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>用户风险</h1>
          <p>集中查看综合风险关注用户；这里只提示人工关注，不会自动扣次数或封禁</p>
        </div>
        <span>共 {data.total || 0} 条</span>
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
          value={status}
          onChange={(event) => {
            setStatus(event.target.value);
            setPage(0);
          }}
        >
          <option value="WATCHING">关注中</option>
          <option value="DISMISSED">已处理</option>
        </select>
      </div>

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>用户</th>
              <th>综合风险值</th>
              <th>各级计数</th>
              <th>进入原因</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {(data.items || []).map((row) => (
              <tr key={row.userId}>
                <td>{row.uid}<small>{row.phone}</small></td>
                <td>{Number(row.riskScore || 0).toFixed(2)}</td>
                <td className="long-cell">{row.counters || '—'}</td>
                <td className="long-cell">{row.reason}</td>
                <td><Status value={row.status} /></td>
                <td>{date(row.updatedAt)}</td>
                <td>
                  {row.status === 'WATCHING' && can('RISK_WATCH_PROCESS') ? (
                    <button
                      className="plain"
                      onClick={() => {
                        setSelected(row);
                        setReason('');
                      }}
                    >
                      处理
                    </button>
                  ) : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items?.length && <Empty />}
      </div>

      <Pagination page={page} size={pageSize} total={data.total || 0} onChange={setPage} />

      {selected && (
        <div className="dispute-mask" onClick={close}>
          <aside onClick={(event) => event.stopPropagation()}>
            <header>
              <div>
                <h2>风险关注 · {selected.uid}</h2>
                <span>综合风险值 {Number(selected.riskScore || 0).toFixed(2)}</span>
              </div>
              <button onClick={close}>×</button>
            </header>
            <div className="dispute-drawer-body">
              <section>
                <h3>进入原因</h3>
                <p>{selected.reason}</p>
                <small>各级计数：{selected.counters || '—'}</small>
              </section>
              <section className="decision-block">
                <h3>人工关注处理</h3>
                <p>填写本次查看结果。完成处理只会关闭本条关注，不会自动处罚用户。</p>
                <textarea
                  rows="5"
                  maxLength="500"
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  placeholder="填写查看结果与处理说明"
                />
                <button type="button" onClick={submit}>完成处理</button>
              </section>
            </div>
          </aside>
        </div>
      )}
    </>
  );
}

function Status({ value }) {
  const active = value === 'WATCHING';
  const label = value === 'WATCHING' ? '关注中' : '已处理';
  return <span className={`status ${active ? 'pending' : 'active'}`}>{label}</span>;
}
