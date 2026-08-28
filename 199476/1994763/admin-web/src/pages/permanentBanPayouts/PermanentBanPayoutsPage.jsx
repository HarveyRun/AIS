import { useCallback, useEffect, useRef, useState } from 'react';
import { Search } from 'lucide-react';

import { adminApi } from '../../api/adminApi.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import Pagination from '../../components/data/Pagination.jsx';
import { message } from '../../components/feedback/message.js';
import '../shared/Page.css';
import './PermanentBanPayoutsPage.css';

const pageSize = 20;

const statuses = [
  ['', '全部状态'],
  ['WAITING_ALIPAY', '缺少支付宝账户'],
  ['WAITING_EXPORT', '待导出'],
  ['EXPORTED', '待回填结果'],
  ['FAILED', '出款失败'],
  ['COMPLETED', '已到账'],
];

export default function PermanentBanPayoutsPage() {
  const { can } = useAdminAccess();
  const resultFile = useRef(null);
  const [data, setData] = useState({ items: [], total: 0 });
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [exporting, setExporting] = useState(false);

  const load = useCallback(
    async (targetPage = page) => {
      const result = await adminApi.permanentBanPayouts({
        keyword: appliedKeyword,
        status,
        page: targetPage,
        size: pageSize,
      });
      setData(result);
    },
    [appliedKeyword, page, status],
  );

  useEffect(() => {
    load().catch((error) => message.error(error.message));
  }, [load]);

  const search = (event) => {
    event.preventDefault();
    setPage(0);
    setAppliedKeyword(keyword.trim());
  };

  const exportRows = async () => {
    try {
      setExporting(true);
      const file = await adminApi.exportPermanentBanPayouts();
      saveFile(file);
      message.success('永久封禁余额批次已导出');
      await load();
    } catch (error) {
      message.error(error.message);
    } finally {
      setExporting(false);
    }
  };

  const importResults = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    try {
      const result = await adminApi.importPermanentBanPayoutResults(file);
      message.success(
        `已处理：成功 ${result.successCount} 条，失败 ${result.failedCount} 条，处理中 ${result.pendingCount} 条`,
      );
      await load();
    } catch (error) {
      message.error(error.message);
    }
  };

  const downloadBatch = async (batchNo) => {
    try {
      saveFile(await adminApi.downloadPermanentBanPayoutBatch(batchNo));
    } catch (error) {
      message.error(error.message);
    }
  };

  const retry = async (id) => {
    try {
      await adminApi.retryPermanentBanPayout(id);
      message.success('已重新进入待导出');
      await load();
    } catch (error) {
      message.error(error.message);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>永久封禁余额处理</h1>
          <p>仅处理永久封禁账号的可提现收入，与用户主动提现完全分开</p>
        </div>
        <div className="page-title-actions">
          <span>共 {data.total} 条</span>
          {can('PERMANENT_BAN_PAYOUT_PROCESS') && (
            <>
              <input
                ref={resultFile}
                type="file"
                accept=".xlsx"
                hidden
                onChange={importResults}
              />
              <button className="plain" type="button" onClick={() => resultFile.current?.click()}>
                导入处理结果
              </button>
            </>
          )}
          {can('PERMANENT_BAN_PAYOUT_EXPORT') && (
            <button
              className="primary"
              type="button"
              disabled={exporting}
              onClick={exportRows}
            >
              {exporting ? '导出中' : '导出待处理余额'}
            </button>
          )}
        </div>
      </div>

      <div className="ban-payout-note">
        每条记录只包含永久封禁时尚未提现的可提现收入；充值余额和普通提现中的金额不会进入此处。
      </div>

      <form className="toolbar" onSubmit={search}>
        <label>
          <Search aria-hidden="true" />
          <input
            value={keyword}
            maxLength={20}
            placeholder="输入用户UID或手机号"
            onChange={(event) => setKeyword(event.target.value)}
          />
        </label>
        <select
          value={status}
          onChange={(event) => {
            setStatus(event.target.value);
            setPage(0);
          }}
        >
          {statuses.map(([value, label]) => (
            <option value={value} key={value}>{label}</option>
          ))}
        </select>
        <button type="submit">查询</button>
      </form>

      <section className="table-card">
        <table>
          <thead>
            <tr>
              <th>用户</th>
              <th>处理金额</th>
              <th>支付宝账户</th>
              <th>状态</th>
              <th>处理期限</th>
              <th>批次</th>
              <th>生成时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((row) => (
              <tr key={row.id}>
                <td>
                  <b>UID {row.uid}</b>
                  <small>{row.phone || '-'}</small>
                </td>
                <td><b>¥{money(row.amount)}</b></td>
                <td>{row.alipayAccount || '尚未完成授权'}</td>
                <td>
                  <span className={`status ${String(row.status).toLowerCase()}`}>
                    {statusLabel(row.status)}
                  </span>
                  {row.resultReason && <small>{row.resultReason}</small>}
                </td>
                <td className={isOverdue(row) ? 'ban-payout-overdue' : ''}>
                  {date(row.dueAt)}
                </td>
                <td>{row.batchNo || '-'}</td>
                <td>{date(row.createdAt)}</td>
                <td className="row-actions">
                  {row.batchNo && can('PERMANENT_BAN_PAYOUT_EXPORT') && (
                    <button className="plain" type="button" onClick={() => downloadBatch(row.batchNo)}>
                      下载批次
                    </button>
                  )}
                  {row.status === 'FAILED' && can('PERMANENT_BAN_PAYOUT_PROCESS') && (
                    <button className="plain" type="button" onClick={() => retry(row.id)}>
                      重新导出
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.items.length && <div className="empty">暂无数据</div>}
      </section>

      <Pagination
        page={page}
        size={pageSize}
        total={data.total}
        onChange={(nextPage) => setPage(nextPage)}
      />
    </>
  );
}

function statusLabel(value) {
  return statuses.find(([status]) => status === value)?.[1] || value;
}

function money(value) {
  const amount = Number(value || 0);
  return Number.isInteger(amount) ? String(amount) : amount.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
}

function date(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-';
}

function isOverdue(row) {
  return !['COMPLETED'].includes(row.status) && row.dueAt && new Date(row.dueAt).getTime() < Date.now();
}

function saveFile(file) {
  const url = URL.createObjectURL(file.blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = file.filename;
  link.click();
  URL.revokeObjectURL(url);
}
