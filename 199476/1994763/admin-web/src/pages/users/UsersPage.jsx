import { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import '../shared/Page.css';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
export default function UsersPage() {
  const [data, setData] = useState({ items: [], total: 0 });
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const size = 20;
  const load = (targetPage = page) => {
    return adminApi
      .users(new URLSearchParams({ keyword, status, page: targetPage, size }).toString())
      .then(setData)
      .catch((e) => message.error(e.message));
  };
  useEffect(() => {
    setPage(0);
    load(0);
  }, [status]);
  const change = async (u) => {
    try {
      await adminApi.userStatus(u.id, u.accountStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE');
      await load();
      message.success(u.accountStatus === 'ACTIVE' ? '用户已停用' : '用户已恢复');
    } catch (e) {
      message.error(e.message);
    }
  };
  return (
    <>
      <div className="page-title">
        <div>
          <h1>用户管理</h1>
          <p>查看用户、答主状态与账户资金</p>
        </div>
        <span>共 {data.total} 人</span>
      </div>
      <div className="toolbar">
        <label>
          <Search />
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && load()}
            placeholder="搜索UID、手机号或昵称"
          />
        </label>
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">全部状态</option>
          <option value="ACTIVE">正常</option>
          <option value="SUSPENDED">已停用</option>
        </select>
          <button onClick={() => { setPage(0); load(0); }}>查询</button>
      </div>
      <Pagination page={page} size={size} total={data.total} onChange={(next) => { setPage(next); load(next); }} />
      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>用户</th>
              <th>手机号</th>
              <th>答主状态</th>
              <th>可用 / 冻结</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((u) => (
              <tr key={u.id}>
                <td>
                  <b>{u.nickname || `UID ${u.uid}`}</b>
                  <small>UID {u.uid}</small>
                </td>
                <td>{u.phone}</td>
                <td>
                  <Status value={u.answererStatus} />
                </td>
                <td>
                  ¥{u.availableBalance} / ¥{u.frozenBalance}
                </td>
                <td>{date(u.createdAt)}</td>
                <td>
                  <button
                    className={u.accountStatus === 'ACTIVE' ? 'danger' : 'plain'}
                    onClick={() => change(u)}
                  >
                    {u.accountStatus === 'ACTIVE' ? '停用' : '恢复'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <Empty />}
      </div>
    </>
  );
}
export const date = (v) => (v ? new Date(v).toLocaleString() : '—');
export const Status = ({ value }) => (
  <span className={`status ${String(value).toLowerCase()}`}>
    {{
      ACTIVE: '正常',
      SUSPENDED: '已停用',
      APPROVED: '已通过',
      NOT_APPLIED: '未申请',
      PENDING: '待处理',
      PROCESSING: '处理中',
      COMPLETED: '已完成',
      REJECTED: '已驳回',
      FAILED: '失败',
      SUBMITTED: '待处理',
      RESOLVED: '已解决',
      CLOSED: '已关闭',
      REFUNDED: '已退款',
      SETTLED: '已结算',
      CANCELLED: '已撤销',
      EXPIRED: '已过期',
      AWAITING_CONFIRMATION: '待确认结束',
    }[value] ||
      value ||
      '—'}
  </span>
);
export const Empty = () => <div className="empty">暂无数据</div>;
