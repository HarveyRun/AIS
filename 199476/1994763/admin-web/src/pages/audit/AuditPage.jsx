import { useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import { date, Empty } from '../users/UsersPage.jsx';
import '../shared/Page.css';
export default function AuditPage() {
  const [data, setData] = useState({ items: [], total: 0 });
  useEffect(() => {
    adminApi.logs().then(setData);
  }, []);
  return (
    <>
      <div className="page-title">
        <div>
          <h1>操作记录</h1>
          <p>重要管理操作均在此留痕</p>
        </div>
        <span>共 {data.total} 条</span>
      </div>
      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>管理员</th>
              <th>操作</th>
              <th>对象</th>
              <th>内容</th>
              <th>IP</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((x) => (
              <tr key={x.id}>
                <td>{x.adminName}</td>
                <td>{x.action}</td>
                <td>
                  {x.targetType} #{x.targetId}
                </td>
                <td>{x.detail}</td>
                <td>{x.ipAddress}</td>
                <td>{date(x.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <Empty />}
      </div>
    </>
  );
}
