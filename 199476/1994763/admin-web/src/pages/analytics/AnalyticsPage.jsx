import { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  BadgeCheck,
  ChartNoAxesCombined,
  Download,
  Filter,
  RefreshCw,
  Search,
  TrendingUp,
  Users,
} from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { message } from '../../components/feedback/message.js';
import { useAdminAccess } from '../../app/AdminAccessContext.jsx';
import '../shared/Page.css';
import './AnalyticsPage.css';

const tabs = [
  ['overview', '数据概览', ChartNoAxesCombined],
  ['funnel', '转化漏斗', TrendingUp],
  ['content', '经历内容', Search],
  ['supply', '经历供需', Users],
  ['answerers', '经历发布者', BadgeCheck],
  ['retention', '用户留存', Activity],
  ['quality', '数据质量', Filter],
];

const labels = {
  activeUsers: '活跃用户',
  homeViews: '首页访问',
  profileViews: '个人信息查看',
  inquiriesCreated: '发起询问',
  inquiriesAccepted: '接受询问',
  inquiriesSettled: '完成结算',
  gmv: '完成交易金额',
  serviceFee: '平台服务费',
  answererIncome: '回答收入',
  approvedAnswerers: '经历发布者',
  acceptingAnswerers: '正在接受询问',
  pausedAnswerers: '暂停接受询问',
  experienceSubmitted: '经历申请',
  approved: '审核通过',
  rejected: '审核未通过',
  inquiryUsers: '发起过询问的用户',
  repeatUsers: '再次发起询问的用户',
  repeatRate: '再次询问率',
  storedEvents: '已保存事件',
  clientEvents: 'App行为事件',
  serverEvents: '后端事实事件',
  missingSession: '缺少会话编号',
  delayedEvents: '延迟超过10分钟',
  attempted: '上报事件',
  accepted: '成功写入',
  duplicate: '重复拦截',
};

const moneyKeys = new Set(['gmv', 'serviceFee', 'answererIncome']);
const percentKeys = new Set(['repeatRate']);

function dateValue(date) {
  return date.toISOString().slice(0, 10);
}

function initialFilters() {
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - 6);
  return {
    startDate: dateValue(start),
    endDate: dateValue(end),
    platform: '',
    environment: 'prod',
    includeTest: false,
  };
}

export default function AnalyticsPage() {
  const { can } = useAdminAccess();
  const [active, setActive] = useState('overview');
  const [filters, setFilters] = useState(initialFilters);
  const [appliedFilters, setAppliedFilters] = useState(initialFilters);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);

  const load = async (section = active, query = appliedFilters) => {
    setLoading(true);
    try {
      setData(await adminApi.analytics(section, query));
    } catch (error) {
      message.error(error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(active, appliedFilters);
  }, [active, appliedFilters]);

  const apply = () => {
    if (!filters.startDate || !filters.endDate) {
      message.warning('请选择统计日期');
      return;
    }
    if (filters.startDate > filters.endDate) {
      message.warning('结束日期不能早于开始日期');
      return;
    }
    setAppliedFilters({ ...filters });
  };

  const exportCurrent = () => {
    const content = `\uFEFF${JSON.stringify({
      section: active,
      filters: appliedFilters,
      data,
    }, null, 2)}`;
    const url = URL.createObjectURL(new Blob([content], { type: 'application/json;charset=utf-8' }));
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `事先问运营分析-${active}-${appliedFilters.startDate}-${appliedFilters.endDate}.json`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  return (
    <>
      <div className="page-title analytics-title">
        <div>
          <h1>运营分析</h1>
          <p>查看用户从找人到完成交流的真实业务过程</p>
        </div>
        <div className="analytics-title-actions">
          {can('ANALYTICS_EXPORT') && (
            <button type="button" className="ghost-button" onClick={exportCurrent} disabled={!data}>
              <Download />导出当前数据
            </button>
          )}
          <button type="button" className="ghost-button" onClick={() => load()} disabled={loading}>
            <RefreshCw className={loading ? 'spinning' : ''} />刷新
          </button>
        </div>
      </div>

      <section className="analytics-filter-card">
        <label>
          <span>开始日期</span>
          <input type="date" value={filters.startDate} onChange={(event) => setFilters({ ...filters, startDate: event.target.value })} />
        </label>
        <label>
          <span>结束日期</span>
          <input type="date" value={filters.endDate} onChange={(event) => setFilters({ ...filters, endDate: event.target.value })} />
        </label>
        <label>
          <span>客户端</span>
          <select value={filters.platform} onChange={(event) => setFilters({ ...filters, platform: event.target.value })}>
            <option value="">全部</option>
            <option value="android">Android</option>
            <option value="ios">iOS</option>
          </select>
        </label>
        <label>
          <span>数据环境</span>
          <select value={filters.environment} onChange={(event) => setFilters({ ...filters, environment: event.target.value })}>
            <option value="prod">生产</option>
            <option value="test">测试</option>
            <option value="dev">开发</option>
          </select>
        </label>
        <label className="analytics-test-filter">
          <input type="checkbox" checked={filters.includeTest} onChange={(event) => setFilters({ ...filters, includeTest: event.target.checked })} />
          <span>包含测试账号</span>
        </label>
        <button type="button" className="primary-button" onClick={apply}>查询</button>
      </section>

      <div className="analytics-tabs" role="tablist">
        {tabs.map(([key, label, Icon]) => (
          <button
            type="button"
            role="tab"
            aria-selected={active === key}
            className={active === key ? 'active' : ''}
            key={key}
            onClick={() => setActive(key)}
          >
            <Icon />{label}
          </button>
        ))}
      </div>

      <section className={`analytics-content ${loading ? 'loading' : ''}`}>
        <AnalyticsContent section={active} data={data} />
      </section>
    </>
  );
}

function AnalyticsContent({ section, data }) {
  if (!data) return <div className="analytics-empty">正在准备数据</div>;
  if (section === 'overview') {
    return <><MetricCards data={data.cards} /><TrendChart rows={data.trend || []} /></>;
  }
  if (section === 'funnel') return <Funnel stages={data.stages || []} />;
  if (section === 'content') {
    return (
      <div className="analytics-list-grid">
        <Ranking title="热门经历" rows={data.experiences} />
        <Ranking title="轮播点击" rows={data.banners} />
      </div>
    );
  }
  if (section === 'supply') return <SupplyTable rows={data.rows || []} />;
  return <MetricCards data={data.cards || {}} />;
}

function MetricCards({ data = {} }) {
  const entries = Object.entries(data).filter(([key]) => labels[key]);
  if (!entries.length) return <div className="analytics-empty">当前时间范围内暂无数据</div>;
  return (
    <div className="analytics-metric-grid">
      {entries.map(([key, value]) => (
        <article key={key}>
          <span>{labels[key]}</span>
          <strong>{formatValue(key, value)}</strong>
        </article>
      ))}
    </div>
  );
}

function TrendChart({ rows }) {
  const max = useMemo(() => Math.max(1, ...rows.map((row) => Number(row.homeViews || 0))), [rows]);
  return (
    <div className="analytics-panel">
      <div className="analytics-panel-title"><h2>每日趋势</h2><span>首页访问 / 个人信息 / 询问</span></div>
      {rows.length === 0 ? <div className="analytics-empty">暂无趋势数据</div> : (
        <div className="trend-bars">
          {rows.map((row) => (
            <div className="trend-column" key={String(row.day)} title={`${row.day} 首页${row.homeViews || 0}次`}>
              <div className="trend-value">{row.homeViews || 0}</div>
              <div className="trend-bar-track"><i style={{ height: `${Math.max(5, Number(row.homeViews || 0) / max * 100)}%` }} /></div>
              <small>{String(row.day).slice(5)}</small>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function Funnel({ stages }) {
  const first = Math.max(1, Number(stages[0]?.count || 0));
  return (
    <div className="analytics-panel funnel-list">
      {stages.map((stage, index) => {
        const count = Number(stage.count || 0);
        const rate = index === 0 ? 100 : count / first * 100;
        return (
          <article key={stage.label}>
            <div><span>{index + 1}</span><b>{stage.label}</b><strong>{count}</strong><em>{rate.toFixed(1)}%</em></div>
            <i style={{ width: `${Math.max(2, Math.min(100, rate))}%` }} />
          </article>
        );
      })}
    </div>
  );
}

function Ranking({ title, rows = [] }) {
  return (
    <div className="analytics-panel ranking-panel">
      <div className="analytics-panel-title"><h2>{title}</h2><span>前30项</span></div>
      {rows.length === 0 ? <div className="analytics-empty">暂无数据</div> : rows.map((row, index) => (
        <div className="ranking-row" key={`${row.contentId}-${index}`}>
          <span>{index + 1}</span><b>{row.contentName}</b><em>{row.userCount || 0}人</em><strong>{row.eventCount || 0}次</strong>
        </div>
      ))}
    </div>
  );
}

function SupplyTable({ rows }) {
  return (
    <div className="analytics-panel analytics-table-wrap">
      <div className="analytics-panel-title"><h2>经历供需情况</h2><span>需求高、可接受询问人数少的经历应优先补充</span></div>
      <table><thead><tr><th>经历</th><th>需求次数</th><th>可接受询问人数</th><th>状态</th></tr></thead>
        <tbody>{rows.map((row) => {
          const shortage = Number(row.demandCount || 0) > Number(row.availablePeople || 0);
          return <tr key={row.experienceId}><td>{row.experienceName}</td><td>{row.demandCount || 0}</td><td>{row.availablePeople || 0}</td><td><span className={shortage ? 'supply-shortage' : 'supply-normal'}>{shortage ? '需要补充' : '供应正常'}</span></td></tr>;
        })}</tbody>
      </table>
      {rows.length === 0 && <div className="analytics-empty">暂无经历数据</div>}
    </div>
  );
}

function formatValue(key, value) {
  if (moneyKeys.has(key)) return `¥${Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}`;
  if (percentKeys.has(key)) return `${Number(value || 0).toFixed(2)}%`;
  return Number(value || 0).toLocaleString('zh-CN');
}
