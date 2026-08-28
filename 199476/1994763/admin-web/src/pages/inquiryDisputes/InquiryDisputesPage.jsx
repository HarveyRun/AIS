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
  const [mode, setMode] = useState('ends');
  const [data, setData] = useState({ items: [], total: 0 });
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('PENDING');
  const [drawer, setDrawer] = useState(null);

  const load = useCallback(async () => {
    const query = { keyword, status, page, size: pageSize };
    const result = mode === 'ends'
      ? await adminApi.inquiryEndDisputes(query)
      : mode === 'reports'
        ? await adminApi.inquiryMessageReportCases(query)
        : await adminApi.inquiryRiskWatch({ ...query, status: status || 'WATCHING' });
    setData(result);
  }, [keyword, mode, page, status]);

  useEffect(() => {
    load().catch((error) => message.error(error.message));
  }, [load]);

  const switchMode = (next) => {
    setMode(next);
    setPage(0);
    setStatus(next === 'risk' ? 'WATCHING' : 'PENDING');
    setDrawer(null);
  };

  const openReport = async (id) => {
    try {
      const detail = await adminApi.inquiryMessageReportCase(id);
      setDrawer({
        type: 'report',
        item: detail,
        decisions: detail.reports.map((row) => ({
          reportId: row.id,
          status: row.status === 'PENDING' ? '' : row.status,
          reason: row.decisionReason || '',
        })),
      });
    } catch (error) {
      message.error(error.message);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>询问纠纷</h1>
          <p>结束申请、消息举报和综合风险关注分别处理，所有结论保留记录</p>
        </div>
        <span>共 {data.total || 0} 条</span>
      </div>

      <div className="dispute-tabs">
        <Tab active={mode === 'ends'} onClick={() => switchMode('ends')}>结束纠纷</Tab>
        <Tab active={mode === 'reports'} onClick={() => switchMode('reports')}>消息举报</Tab>
        <Tab active={mode === 'risk'} onClick={() => switchMode('risk')}>风险关注</Tab>
      </div>

      <div className="toolbar">
        <input
          value={keyword}
          onChange={(event) => { setKeyword(event.target.value); setPage(0); }}
          placeholder="用户UID或手机号"
        />
        <select value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}>
          {mode === 'risk' ? <>
            <option value="WATCHING">关注中</option>
            <option value="DISMISSED">已处理</option>
          </> : <>
            <option value="PENDING">待处理</option>
            <option value="RESOLVED">已处理</option>
            <option value="">全部</option>
          </>}
        </select>
      </div>

      <div className="table-card">
        {mode === 'ends' && <EndTable
          rows={data.items || []}
          canProcess={can('INQUIRY_DISPUTE_PROCESS')}
          onOpen={(item) => setDrawer({ type: 'end', item, decision: '', reason: '' })}
        />}
        {mode === 'reports' && <ReportTable rows={data.items || []} onOpen={openReport} />}
        {mode === 'risk' && <RiskTable
          rows={data.items || []}
          canProcess={can('RISK_WATCH_PROCESS')}
          onOpen={(item) => setDrawer({ type: 'risk', item, reason: '' })}
        />}
        {!data.items?.length && <Empty />}
      </div>
      <Pagination page={page} size={pageSize} total={data.total || 0} onChange={setPage} />

      {drawer && <DisputeDrawer
        drawer={drawer}
        setDrawer={setDrawer}
        canProcessReport={can('MESSAGE_REPORT_PROCESS')}
        canViewOriginal={can('SENSITIVE_ORIGINAL_VIEW')}
        onDone={async () => { setDrawer(null); await load(); }}
      />}
    </>
  );
}

function Tab({ active, onClick, children }) {
  return <button type="button" className={active ? 'active' : ''} onClick={onClick}>{children}</button>;
}

function EndTable({ rows, canProcess, onOpen }) {
  return <table>
    <thead><tr><th>询问</th><th>提问者</th><th>回答者</th><th>剩余金额</th><th>触发方式</th><th>状态</th><th>时间</th><th>操作</th></tr></thead>
    <tbody>{rows.map((row) => <tr key={row.id}>
      <td>#{row.inquiryId}<small>{row.question}</small></td>
      <td>{row.questionerUid}<small>{row.questionerPhone}</small></td>
      <td>{row.answererUid}<small>{row.answererPhone}</small></td>
      <td>¥{row.settleableAmount}</td>
      <td>{row.triggerType === 'QUESTIONER_DISAGREED' ? '提问者不同意结束' : '24小时未处理'}</td>
      <td><Status value={row.status} /></td>
      <td>{date(row.createdAt)}</td>
      <td>{canProcess || row.status !== 'PENDING' ? <button className="plain" onClick={() => onOpen(row)}>{row.status === 'PENDING' ? '处理' : '查看'}</button> : '待处理'}</td>
    </tr>)}</tbody>
  </table>;
}

function ReportTable({ rows, onOpen }) {
  return <table>
    <thead><tr><th>询问</th><th>提问者</th><th>回答者</th><th>举报数量</th><th>订单状态</th><th>处理状态</th><th>时间</th><th>操作</th></tr></thead>
    <tbody>{rows.map((row) => <tr key={row.id}>
      <td>#{row.inquiryId}<small>{row.question}</small></td>
      <td>{row.questionerUid}<small>{row.questionerPhone}</small></td>
      <td>{row.answererUid}<small>{row.answererPhone}</small></td>
      <td>{row.reportCount} 条</td>
      <td>{inquiryStatus(row.inquiryStatus)}</td>
      <td><Status value={row.status} /></td>
      <td>{date(row.createdAt)}</td>
      <td><button className="plain" onClick={() => onOpen(row.id)}>查看处理</button></td>
    </tr>)}</tbody>
  </table>;
}

function RiskTable({ rows, canProcess, onOpen }) {
  return <table>
    <thead><tr><th>用户</th><th>综合风险值</th><th>各级计数</th><th>进入原因</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
    <tbody>{rows.map((row) => <tr key={row.userId}>
      <td>{row.uid}<small>{row.phone}</small></td>
      <td>{Number(row.riskScore || 0).toFixed(2)}</td>
      <td className="long-cell">{row.counters || '—'}</td>
      <td className="long-cell">{row.reason}</td>
      <td><Status value={row.status} /></td>
      <td>{date(row.updatedAt)}</td>
      <td>{row.status === 'WATCHING' && canProcess ? <button className="plain" onClick={() => onOpen(row)}>处理</button> : '—'}</td>
    </tr>)}</tbody>
  </table>;
}

function DisputeDrawer({ drawer, setDrawer, canProcessReport, canViewOriginal, onDone }) {
  const update = (patch) => setDrawer((current) => ({ ...current, ...patch }));

  const submitEnd = async () => {
    if (!drawer.decision || !drawer.reason.trim()) return message.warning('请选择处理结果并填写处理依据');
    try {
      await adminApi.resolveInquiryEndDispute(drawer.item.id, { decision: drawer.decision, reason: drawer.reason.trim() });
      message.success('结束纠纷已处理');
      await onDone();
    } catch (error) { message.error(error.message); }
  };

  const submitReports = async () => {
    if (drawer.decisions.some((item) => !item.status || !item.reason.trim())) return message.warning('请逐条选择结论并填写依据');
    try {
      await adminApi.resolveInquiryMessageReportCase(drawer.item.id, { decisions: drawer.decisions });
      message.success('消息举报已处理');
      await onDone();
    } catch (error) { message.error(error.message); }
  };

  const submitRisk = async () => {
    if (!drawer.reason.trim()) return message.warning('请填写人工关注处理说明');
    try {
      await adminApi.dismissInquiryRiskWatch(drawer.item.userId, drawer.reason.trim());
      message.success('风险关注已处理');
      await onDone();
    } catch (error) { message.error(error.message); }
  };

  const showOriginal = async (id) => {
    try {
      const original = await adminApi.inquiryMessageReportOriginal(id);
      update({ originalEvidence: original });
    } catch (error) { message.error(error.message); }
  };

  const setReportDecision = (index, patch) => update({
    decisions: drawer.decisions.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item),
  });
  const reportProcessable = drawer.type === 'report'
    && !['PENDING', 'ACTIVE', 'AWAITING_CONFIRMATION'].includes(drawer.item.inquiryStatus);

  return <div className="dispute-mask" onClick={() => setDrawer(null)}>
    <aside onClick={(event) => event.stopPropagation()}>
      <header><div><h2>{drawerTitle(drawer)}</h2><span>{drawerSubtitle(drawer)}</span></div><button onClick={() => setDrawer(null)}>×</button></header>
      <div className="dispute-drawer-body">
        {drawer.type === 'end' && <>
          <section><h3>本次询问</h3><p>{drawer.item.question}</p><small>当前剩余可结算 ¥{drawer.item.settleableAmount}</small></section>
          {drawer.item.status === 'PENDING' && <section className="decision-block">
            <h3>处理结果</h3>
            <select value={drawer.decision} onChange={(event) => update({ decision: event.target.value })}>
              <option value="">请选择</option>
              <option value="QUESTIONER_VIOLATION">提问者违规，结算剩余金额</option>
              <option value="ANSWERER_VIOLATION">回答者违规，退回剩余金额</option>
            </select>
            <textarea rows="5" maxLength="500" value={drawer.reason} onChange={(event) => update({ reason: event.target.value })} placeholder="填写完整处理依据" />
            <button type="button" onClick={submitEnd}>确认处理</button>
          </section>}
        </>}
        {drawer.type === 'report' && <>
          <section><h3>原始提问</h3><p>{drawer.item.question}</p><small>订单状态：{inquiryStatus(drawer.item.inquiryStatus)}</small></section>
          {drawer.item.reports.map((row, index) => <section className="report-detail" key={row.id}>
            <div><h3>举报 #{row.id}</h3><Status value={row.status} /></div>
            <p>{reportType(row.reportType)} · 对应 {row.violationLevel} 级</p>
            <blockquote>{row.content || '—'}</blockquote>
            <small>举报人 {row.reporterUid} · 被举报人 {row.reportedUid}</small>
            {canViewOriginal && <button className="plain" type="button" onClick={() => showOriginal(row.id)}>查看隔离保存的原始证据</button>}
            {drawer.originalEvidence?.reportId === row.id && <div className="original-evidence">
              <b>原始提问</b>
              <p>{drawer.originalEvidence.questionOriginal || '—'}</p>
              <b>原始消息 / 文件标识</b>
              <p>{drawer.originalEvidence.messageOriginal || '—'}</p>
            </div>}
            {drawer.item.status === 'PENDING' && canProcessReport && reportProcessable && <div className="report-decision">
              <select value={drawer.decisions[index].status} onChange={(event) => setReportDecision(index, { status: event.target.value })}>
                <option value="">选择处理结果</option>
                <option value="PASSED">举报通过</option>
                <option value="REJECTED">举报驳回</option>
                <option value="IGNORED">忽略</option>
              </select>
              <input maxLength="500" value={drawer.decisions[index].reason} onChange={(event) => setReportDecision(index, { reason: event.target.value })} placeholder="本条处理依据" />
            </div>}
          </section>)}
          {drawer.item.status === 'PENDING' && !reportProcessable && <div className="process-waiting">订单结束后可处理举报结论</div>}
          {drawer.item.status === 'PENDING' && canProcessReport && reportProcessable && <footer><button type="button" onClick={submitReports}>提交全部处理结果</button></footer>}
        </>}
        {drawer.type === 'risk' && <section className="decision-block">
          <h3>人工关注处理</h3>
          <p>该名单只用于人工关注，不会自动扣次数或封禁。</p>
          <textarea rows="5" maxLength="500" value={drawer.reason} onChange={(event) => update({ reason: event.target.value })} placeholder="填写查看结果与处理说明" />
          <button type="button" onClick={submitRisk}>完成处理</button>
        </section>}
      </div>
    </aside>
  </div>;
}

function Status({ value }) {
  const active = value === 'PENDING' || value === 'WATCHING';
  return <span className={`status ${active ? 'pending' : 'active'}`}>{statusText(value)}</span>;
}

function statusText(value) {
  return { PENDING: '待处理', RESOLVED: '已处理', WATCHING: '关注中', DISMISSED: '已处理', PASSED: '通过', REJECTED: '驳回', IGNORED: '忽略' }[value] || value;
}

function inquiryStatus(value) {
  return { ACTIVE: '交流中', AWAITING_CONFIRMATION: '等待结束确认', DISPUTED: '纠纷处理中', COMPLETED: '已结算', END_DISPUTE_REFUNDED: '已退款', TIMEOUT_REFUNDED: '超时全额退款' }[value] || value;
}

function reportType(value) {
  return { LOW_RELEVANCE: '和原始提问相关性极低', PORN_GAMBLING_DRUGS: '黄赌毒内容', NATIONAL_SECURITY: '危害国家安全、破坏社会稳定的言论' }[value] || value;
}

function drawerTitle(drawer) {
  if (drawer.type === 'end') return `结束纠纷 #${drawer.item.id}`;
  if (drawer.type === 'report') return `消息举报 #${drawer.item.id}`;
  return `风险关注 · ${drawer.item.uid}`;
}

function drawerSubtitle(drawer) {
  if (drawer.type === 'risk') return `综合风险值 ${Number(drawer.item.riskScore || 0).toFixed(2)}`;
  return `询问 #${drawer.item.inquiryId}`;
}
