import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import './FinanceReconciliationPage.css';

const pageSize = 20;

export default function FinanceReconciliationPage() {
  const { can } = useAdminAccess();
  const [tab, setTab] = useState('differences');
  const [summary, setSummary] = useState({});
  const [data, setData] = useState({ items: [], total: 0 });
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('OPEN');
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState(null);
  const [resolution, setResolution] = useState('');
  const [billDate, setBillDate] = useState(() => new Date().toISOString().slice(0, 10));

  const load = useCallback(async () => {
    let rows;
    if (tab === 'tasks') {
      rows = await adminApi.reconciliationTasks(page, pageSize);
    } else if (tab === 'vouchers') {
      rows = await adminApi.fundVouchers(keyword, page, pageSize);
    } else {
      rows = await adminApi.reconciliationDifferences(status, page, pageSize);
    }
    const head = await adminApi.reconciliationSummary();
    setSummary(head);
    setData(rows);
  }, [tab, page, status, keyword]);

  useEffect(() => {
    load().catch((error) => message.error(error.message));
  }, [load]);

  const run = async () => {
    try {
      const result = await adminApi.runReconciliation();
      message.success(`检查完成，发现 ${result.differenceCount} 条差异`);
      await load();
    } catch (error) {
      message.error(error.message);
    }
  };

  const upload = async (event, type) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    try {
      if (type === 'alipay') {
        await adminApi.importAlipayBill(file, billDate);
      } else {
        await adminApi.importWithdrawalResults(file);
      }
      message.success('文件已经处理');
      await load();
    } catch (error) {
      message.error(error.message);
    }
  };

  const resolve = async () => {
    if (!resolution.trim()) {
      message.warning('请填写核查结论');
      return;
    }
    try {
      await adminApi.resolveReconciliationDifference(selected.id, resolution.trim());
      message.success('差异已经处理');
      setSelected(null);
      setResolution('');
      await load();
    } catch (error) {
      message.error(error.message);
    }
  };

  const changeTab = (nextTab) => {
    setTab(nextTab);
    setPage(0);
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>资金对账</h1>
          <p>核对平台账、用户余额、询问资金与支付宝账单，差异必须留痕处理</p>
        </div>
      </div>

      <div className="finance-summary">
        <Summary label="待处理差异" value={summary.openDifferences || 0} />
        <Summary label="资金凭证" value={summary.voucherCount || 0} />
        <Summary label="平台已得服务费" value={`¥${summary.earnedServiceFee || 0}`} />
        <Summary label="支付宝通道费" value={`¥${summary.channelFee || 0}`} />
      </div>

      <div className="finance-actions">
        {can('FINANCE_RECONCILIATION_RUN') && (
          <button className="primary" onClick={run}>立即内部对账</button>
        )}
        {can('FINANCE_RECONCILIATION_IMPORT') && (
          <>
            <input
              className="finance-bill-date"
              type="date"
              value={billDate}
              onChange={(event) => setBillDate(event.target.value)}
            />
            <label>
              导入支付宝CSV
              <input type="file" accept=".csv" onChange={(event) => upload(event, 'alipay')} />
            </label>
            <label>
              导入普通提现结果
              <input type="file" accept=".xlsx" onChange={(event) => upload(event, 'withdrawal')} />
            </label>
          </>
        )}
      </div>

      <div className="finance-tabs">
        {[
          ['differences', '差异'],
          ['tasks', '对账批次'],
          ['vouchers', '资金凭证'],
        ].map(([id, label]) => (
          <button
            key={id}
            className={tab === id ? 'active' : ''}
            onClick={() => changeTab(id)}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="toolbar">
        {tab === 'differences' && (
          <select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
          >
            <option value="OPEN">待处理</option>
            <option value="RESOLVED">已处理</option>
            <option value="">全部</option>
          </select>
        )}
        {tab === 'vouchers' && (
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="凭证号、业务编号或说明"
          />
        )}
      </div>

      <FinanceTable
        tab={tab}
        items={data.items}
        canResolve={can('FINANCE_RECONCILIATION_RESOLVE')}
        onResolve={(row) => {
          setSelected(row);
          setResolution('');
        }}
      />
      <Pagination page={page} size={pageSize} total={data.total || 0} onChange={setPage} />

      {selected && (
        <div className="finance-mask" onClick={() => setSelected(null)}>
          <aside onClick={(event) => event.stopPropagation()}>
            <header>
              <div>
                <h2>处理资金差异</h2>
                <span>{selected.differenceType} · {selected.businessType} {selected.businessId}</span>
              </div>
              <button onClick={() => setSelected(null)}>×</button>
            </header>
            <div className="finance-difference-detail">
              <b>{selected.detail}</b>
              <span>应有金额：{selected.expectedAmount ?? '-'}</span>
              <span>实际金额：{selected.actualAmount ?? '-'}</span>
            </div>
            <label className="finance-resolution">
              <span>核查结论</span>
              <textarea
                value={resolution}
                onChange={(event) => setResolution(event.target.value)}
                maxLength={500}
                rows={6}
                placeholder="说明差异原因和实际处理结果"
              />
              <small>这里只记录处理结论，不会自动修改用户余额。</small>
            </label>
            <footer>
              <button className="plain" onClick={() => setSelected(null)}>取消</button>
              <button className="primary" onClick={resolve}>确认处理</button>
            </footer>
          </aside>
        </div>
      )}
    </>
  );
}

function Summary({ label, value }) {
  return <section><span>{label}</span><b>{value}</b></section>;
}

function FinanceTable({ tab, items, canResolve, onResolve }) {
  return (
    <div className="table-card">
      <table>
        <thead><tr>{headers(tab).map((item) => <th key={item}>{item}</th>)}</tr></thead>
        <tbody>
          {items.map((row) => (
            <tr key={row.id}>{cells(tab, row, canResolve, () => onResolve(row))}</tr>
          ))}
        </tbody>
      </table>
      {!items.length && <div className="empty">暂无数据</div>}
    </div>
  );
}

function headers(tab) {
  if (tab === 'tasks') return ['批次', '类型', '日期', '结果', '检查/匹配/差异', '完成时间'];
  if (tab === 'vouchers') return ['凭证号', '业务', '动作', '金额', '平衡值', '发生时间'];
  return ['差异', '业务', '应有金额', '实际金额', '状态', '说明', '操作'];
}

function cells(tab, row, canResolve, onResolve) {
  if (tab === 'tasks') {
    return <>
      <td>#{row.id}</td><td>{row.provider} · {row.billType}</td><td>{String(row.billDate || '')}</td>
      <td><span className={`status ${String(row.status).toLowerCase()}`}>{row.status}</span></td>
      <td>{row.recordCount}/{row.matchedCount}/{row.differenceCount}</td><td>{date(row.completedAt)}</td>
    </>;
  }
  if (tab === 'vouchers') {
    return <>
      <td>{row.voucherNo}</td><td>{row.businessType} #{row.businessId}</td><td>{row.actionCode}</td>
      <td>¥{row.amount}</td><td>{row.balance}</td><td>{date(row.occurredAt)}</td>
    </>;
  }
  return <>
    <td>{row.differenceType}</td><td>{row.businessType} {row.businessId}</td>
    <td>{row.expectedAmount ?? '-'}</td><td>{row.actualAmount ?? '-'}</td>
    <td><span className={`status ${String(row.status).toLowerCase()}`}>{row.status === 'OPEN' ? '待处理' : '已处理'}</span></td>
    <td><b>{row.detail}</b>{row.resolution && <small>{row.resolution}</small>}</td>
    <td>{row.status === 'OPEN' && canResolve ? <button className="plain" onClick={onResolve}>处理</button> : '-'}</td>
  </>;
}

function date(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-';
}
