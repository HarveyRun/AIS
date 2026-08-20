import { useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import { date, Empty } from '../users/UsersPage.jsx';
import '../shared/Page.css';

export default function SecurityEventsPage() {
  const { can } = useAdminAccess();
  const [data, setData] = useState({ items: [], total: 0 });
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({ severity: '', status: 'OPEN', type: '' });
  const size = 20;
  const load = (targetPage = page) => {
    adminApi.securityEvents({ ...filters, page: targetPage, size })
      .then(setData)
      .catch((error) => message.error(error.message));
  };
  useEffect(() => { load(0); }, []);
  const review = async (id) => {
    try {
      await adminApi.reviewSecurityEvent(id);
      message.success('已标记为已处理');
      load(page);
    } catch (error) {
      message.error(error.message);
    }
  };
  return (
    <>
      <div className="page-title">
        <div><h1>安全事件</h1><p>登录、资金、接口、上传和聊天的异常行为集中在这里</p></div>
        <span>共 {data.total} 条</span>
      </div>
      <div className="toolbar">
        <input value={filters.type} placeholder="搜索事件类型" onChange={(event) => setFilters({ ...filters, type: event.target.value })} />
        <select value={filters.severity} onChange={(event) => setFilters({ ...filters, severity: event.target.value })}>
          <option value="">全部级别</option><option value="CRITICAL">严重</option><option value="HIGH">高风险</option>
          <option value="MEDIUM">中风险</option><option value="LOW">低风险</option><option value="INFO">信息</option>
        </select>
        <select value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
          <option value="OPEN">待处理</option><option value="REVIEWED">已处理</option><option value="">全部状态</option>
        </select>
        <button type="button" onClick={() => { setPage(0); load(0); }}>查询</button>
      </div>
      <div className="table-card">
        <table>
          <thead><tr><th>风险</th><th>事件</th><th>用户 / 管理员</th><th>网络与设备</th><th>内容</th><th>时间</th><th>操作</th></tr></thead>
          <tbody>
            {data.items.map((item) => (
              <tr key={item.id}>
                <td><span className={`status ${severityClass(item.severity)}`}>{severityLabel(item.severity)}</span></td>
                <td>{item.eventType}</td>
                <td><b>{item.userId ? `用户 #${item.userId}` : '—'}</b><small>{item.adminUserId ? `管理员 #${item.adminUserId}` : ''}</small></td>
                <td><b>{item.requestIp || '—'}</b><small title={item.deviceId || ''}>{item.deviceId || ''}</small></td>
                <td className="long-cell" title={item.detail || ''}>{item.detail || '—'}</td>
                <td>{date(item.createdAt)}</td>
                <td>{item.reviewStatus === 'OPEN' && can('SECURITY_EVENT_REVIEW') ? <button className="plain" type="button" onClick={() => review(item.id)}>标记处理</button> : item.reviewStatus === 'OPEN' ? '待处理' : '已处理'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <Empty />}
      </div>
      <Pagination page={page} size={size} total={data.total} onChange={(next) => { setPage(next); load(next); }} />
    </>
  );
}

function severityLabel(value) {
  return { CRITICAL: '严重', HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险', INFO: '信息' }[value] || value;
}
function severityClass(value) {
  if (value === 'CRITICAL' || value === 'HIGH') return 'failed';
  if (value === 'MEDIUM') return 'pending';
  return 'active';
}
