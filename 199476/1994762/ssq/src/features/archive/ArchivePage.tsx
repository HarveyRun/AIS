import { useEffect, useMemo, useState } from 'react';
import { useNavigation } from '../../navigation/NavigationContext';
import {
  createForecastSnapshot,
  mergeForecastArchive,
  parseForecastArchive,
  type ArchiveModelWeights,
  type ArchiveSourceData,
  type ForecastSnapshot,
} from './archiveModel';
import './archive.css';

type ArchiveView = 'records' | 'backtest' | 'version';

export type ArchivePageData = ArchiveSourceData & {
  backtest: {
    testDraws: number;
    startIssue: string;
    endIssue: string;
    redTop6: number;
    redRandomTop6: number;
    blueTop1: number;
    blueRandomTop1: number;
    redIssues: string[];
    redHits: number[];
    redRolling: number[];
    blueIssues: string[];
    blueHits: number[];
    blueRolling: number[];
    redHitDist: Array<{ hits: number; count: number }>;
  };
};

const STORAGE_KEY = 'ssq-forecast-archive-v1';

function pad(number: number): string {
  return String(number).padStart(2, '0');
}

function percent(value: number, digits = 1): string {
  return `${(value * 100).toFixed(digits)}%`;
}

function getInitialArchive(data: ArchivePageData): ForecastSnapshot[] {
  const current = createForecastSnapshot(data);
  let saved: ForecastSnapshot[] = [];
  try {
    saved = parseForecastArchive(window.localStorage.getItem(STORAGE_KEY));
  } catch {
    saved = [];
  }
  return mergeForecastArchive(saved, current, data.stats.draws);
}

function ForecastBalls({ snapshot, actual = false }: { snapshot: ForecastSnapshot; actual?: boolean }) {
  const reds = actual ? snapshot.actual?.reds ?? [] : snapshot.reds;
  const blue = actual ? snapshot.actual?.blue : snapshot.blue;
  const redHits = new Set(snapshot.review?.redHits ?? []);
  return (
    <div className="archive-balls">
      {reds.map((number) => (
        <span className={`archive-ball${actual && redHits.has(number) ? ' hit' : ''}`} key={number}>{pad(number)}</span>
      ))}
      <i>+</i>
      {blue != null && <span className={`archive-ball blue${actual && snapshot.review?.blueHit ? ' hit' : ''}`}>{pad(blue)}</span>}
    </div>
  );
}

function NumberReason({ reason }: { reason: ForecastSnapshot['reasons'][number] }) {
  return (
    <article className="number-reason">
      <div className="number-reason-head">
        <span className={`archive-ball${reason.color === 'blue' ? ' blue' : ''}`}>{pad(reason.number)}</span>
        <div><b>排名第 {reason.rank}</b><small>下期概率 {percent(reason.probability)}</small></div>
      </div>
      <dl>
        <div><dt>近20期</dt><dd>{reason.recent20} 次</dd></div>
        <div><dt>当前间隔</dt><dd>{reason.gap === 0 ? '上期出现' : `${reason.gap} 期`}</dd></div>
      </dl>
      <div className="reason-lines">
        <p><span>提高排名</span>{reason.positive.length ? reason.positive.slice(0, 2).join('、') : '没有单独突出项'}</p>
        <p><span>降低排名</span>{reason.negative.length ? reason.negative.slice(0, 2).join('、') : '没有单独突出项'}</p>
      </div>
    </article>
  );
}

function pathFor(values: number[], width: number, height: number, min: number, max: number): string {
  const span = Math.max(max - min, 0.0001);
  return values.map((value, index) => {
    const x = values.length === 1 ? width / 2 : (index / (values.length - 1)) * width;
    const y = height - ((value - min) / span) * height;
    return `${index === 0 ? 'M' : 'L'}${x.toFixed(2)},${y.toFixed(2)}`;
  }).join(' ');
}

function BacktestLineChart({
  title,
  description,
  issues,
  values,
  baseline,
  color,
  valueFormatter,
}: {
  title: string;
  description: string;
  issues: string[];
  values: number[];
  baseline: number;
  color: string;
  valueFormatter: (value: number) => string;
}) {
  const width = 720;
  const height = 190;
  const allValues = [...values, baseline];
  const rawMin = Math.min(...allValues);
  const rawMax = Math.max(...allValues);
  const padding = Math.max((rawMax - rawMin) * 0.16, rawMax * 0.05, 0.02);
  const min = Math.max(0, rawMin - padding);
  const max = rawMax + padding;
  const baselineY = height - ((baseline - min) / (max - min)) * height;
  const last = values.at(-1) ?? 0;

  return (
    <section className="backtest-chart-block">
      <div className="backtest-chart-head">
        <div><h3>{title}</h3><p>{description}</p></div>
        <strong style={{ color }}>{valueFormatter(last)}</strong>
      </div>
      <svg className="backtest-chart" viewBox={`0 0 ${width} ${height + 34}`} role="img" aria-label={`${title}，最新值${valueFormatter(last)}`}>
        {[0, 1, 2, 3].map((line) => <line key={line} x1="0" x2={width} y1={(height / 3) * line} y2={(height / 3) * line} className="chart-grid-line" />)}
        <line x1="0" x2={width} y1={baselineY} y2={baselineY} className="chart-baseline" />
        <path d={pathFor(values, width, height, min, max)} fill="none" stroke={color} strokeWidth="3" vectorEffect="non-scaling-stroke" />
        <circle cx={width} cy={height - ((last - min) / (max - min)) * height} r="5" fill={color} />
        <text x="0" y={height + 26}>{issues[0]}</text>
        <text x={width} y={height + 26} textAnchor="end">{issues.at(-1)}</text>
        <text x={width - 5} y={Math.max(12, baselineY - 7)} textAnchor="end">随机基准 {valueFormatter(baseline)}</text>
      </svg>
    </section>
  );
}

function WeightRows({ weights }: { weights: ArchiveModelWeights }) {
  const rows = [
    ['逻辑回归', weights.logistic],
    ['梯度提升', weights.gradientBoosting],
    ['极端随机树', weights.extraTrees],
  ] as const;
  return (
    <div className="weight-rows">
      {rows.map(([name, value]) => (
        <div className="weight-row" key={name}>
          <span>{name}</span><div><i style={{ width: `${value * 100}%` }} /></div><b>{percent(value, 0)}</b>
        </div>
      ))}
    </div>
  );
}

export function ArchivePage({ data }: { data: ArchivePageData }) {
  const { activePage } = useNavigation();
  const [view, setView] = useState<ArchiveView>('records');
  const [archive, setArchive] = useState<ForecastSnapshot[]>(() => getInitialArchive(data));
  const [selectedIssue, setSelectedIssue] = useState(data.meta.forecastIssue);

  useEffect(() => {
    const next = mergeForecastArchive(archive, createForecastSnapshot(data), data.stats.draws);
    setArchive(next);
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    } catch {
      // The archive remains usable for this session when browser storage is unavailable.
    }
    // The current data issue is the only input that should create a new immutable snapshot.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data.meta.forecastIssue, data.meta.dataEndIssue]);

  const selected = archive.find((snapshot) => snapshot.issue === selectedIssue) ?? archive[0];
  const reviewedCount = archive.filter((snapshot) => snapshot.review).length;
  const recentRows = useMemo(() => data.backtest.redIssues.slice(-12).map((issue, index) => {
    const absoluteIndex = data.backtest.redIssues.length - 12 + index;
    return {
      issue,
      redHits: data.backtest.redHits[absoluteIndex],
      blueHit: data.backtest.blueHits[absoluteIndex] === 1,
    };
  }).reverse(), [data.backtest]);
  const maxDistribution = Math.max(...data.backtest.redHitDist.map((item) => item.count));

  return (
    <section id="archive" className={`page archive-page${activePage === 'archive' ? ' active' : ''}`}>
      <div className="archive-heading">
        <span>预测记录</span>
        <h2>预测档案与结果复盘</h2>
        <p>保存每一期发布时的号码、数据范围和算法版本。开奖同步后，系统自动补充实际结果，不改写原始预测。</p>
        <div className="archive-summary">
          <div><span>真实预测档案</span><b>{archive.length} 期</b></div>
          <div><span>已完成复盘</span><b>{reviewedCount} 期</b></div>
          <div><span>当前预测</span><b>第 {data.meta.forecastIssue} 期</b></div>
        </div>
      </div>

      <div className="archive-tabs" role="tablist" aria-label="预测档案视图">
        <button className={view === 'records' ? 'active' : ''} role="tab" aria-selected={view === 'records'} onClick={() => setView('records')}>档案与复盘</button>
        <button className={view === 'backtest' ? 'active' : ''} role="tab" aria-selected={view === 'backtest'} onClick={() => setView('backtest')}>历史检验</button>
        <button className={view === 'version' ? 'active' : ''} role="tab" aria-selected={view === 'version'} onClick={() => setView('version')}>版本与依据</button>
      </div>

      {view === 'records' && selected && (
        <div className="archive-workspace">
          <aside className="archive-list" aria-label="预测期次">
            <h3>预测期次</h3>
            {archive.map((snapshot) => (
              <button className={snapshot.issue === selected.issue ? 'active' : ''} key={snapshot.issue} onClick={() => setSelectedIssue(snapshot.issue)}>
                <span>第 {snapshot.issue} 期</span><small>{snapshot.review ? '已复盘' : '等待开奖'} · {snapshot.forecastDate}</small>
              </button>
            ))}
          </aside>

          <div className="archive-detail">
            <header className="archive-record-head">
              <div><span>{selected.review ? '已完成复盘' : '等待开奖'}</span><h3>第 {selected.issue} 期预测</h3></div>
              <time>{selected.forecastDate}</time>
            </header>
            <ForecastBalls snapshot={selected} />
            <div className="archive-metadata">
              <div><span>数据截止</span><b>{selected.dataEndIssue}</b></div>
              <div><span>使用开奖</span><b>{selected.drawCount.toLocaleString('zh-CN')} 期</b></div>
              <div><span>算法版本</span><b>V{selected.algorithm.version}</b></div>
              <div><span>版本编号</span><b>{selected.algorithm.fingerprint}</b></div>
            </div>

            <section className="archive-review">
              <div className="archive-section-title"><span>开奖结果</span><h3>{selected.review ? '预测与实际结果' : '开奖后自动复盘'}</h3></div>
              {selected.review ? (
                <>
                  <ForecastBalls snapshot={selected} actual />
                  <p>红球命中 <b>{selected.review.redHits.length} 个</b>：{selected.review.redHits.length ? selected.review.redHits.map(pad).join('、') : '无'}；蓝球<b>{selected.review.blueHit ? '命中' : '未命中'}</b>。</p>
                </>
              ) : (
                <p>当前数据中还没有第 {selected.issue} 期开奖。开奖数据同步后，这里会自动显示实际号码和命中情况。</p>
              )}
            </section>

            <section className="archive-reasons">
              <div className="archive-section-title"><span>入选依据</span><h3>每个号码为什么进入本期结果</h3><p>依据保存在本期快照中，后续算法更新不会改变这里的记录。</p></div>
              <div className="number-reason-grid">{selected.reasons.map((reason) => <NumberReason key={`${reason.color}-${reason.number}`} reason={reason} />)}</div>
            </section>
          </div>
        </div>
      )}

      {view === 'backtest' && (
        <div className="backtest-view">
          <div className="backtest-intro">
            <div><span>检验范围</span><b>{data.backtest.startIssue} - {data.backtest.endIssue}</b><small>{data.backtest.testDraws} 期逐期检验</small></div>
            <div><span>红球前6</span><b>{data.backtest.redTop6.toFixed(3)} 个</b><small>随机选择 {data.backtest.redRandomTop6.toFixed(3)} 个</small></div>
            <div><span>蓝球第一名</span><b>{percent(data.backtest.blueTop1)}</b><small>随机选择 {percent(data.backtest.blueRandomTop1)}</small></div>
          </div>
          <p className="backtest-boundary">历史检验会重新执行过去每一期的预测流程，但不等于当时真实发布并保存过的预测档案。</p>

          <div className="backtest-chart-grid">
            <BacktestLineChart title="红球前6滚动命中" description="最近50次检验的平均命中数如何变化" issues={data.backtest.redIssues} values={data.backtest.redRolling} baseline={data.backtest.redRandomTop6} color="#ff4f70" valueFormatter={(value) => `${value.toFixed(3)} 个`} />
            <BacktestLineChart title="蓝球第一名滚动命中率" description="最近50次检验的蓝球命中率如何变化" issues={data.backtest.blueIssues} values={data.backtest.blueRolling} baseline={data.backtest.blueRandomTop1} color="#4c91ff" valueFormatter={(value) => percent(value)} />
          </div>

          <div className="backtest-lower-grid">
            <section className="hit-distribution">
              <div className="archive-section-title"><span>命中分布</span><h3>360期红球命中情况</h3></div>
              {data.backtest.redHitDist.map((item) => (
                <div className="distribution-row" key={item.hits}>
                  <span>命中 {item.hits} 个</span><div><i style={{ width: `${(item.count / maxDistribution) * 100}%` }} /></div><b>{item.count} 期</b>
                </div>
              ))}
            </section>
            <section className="backtest-table-wrap">
              <div className="archive-section-title"><span>最近记录</span><h3>最近12次历史检验</h3></div>
              <table className="backtest-table"><thead><tr><th>期号</th><th>红球命中</th><th>蓝球</th></tr></thead><tbody>{recentRows.map((row) => <tr key={row.issue}><td>{row.issue}</td><td>{row.redHits} 个</td><td>{row.blueHit ? '命中' : '未中'}</td></tr>)}</tbody></table>
            </section>
          </div>
        </div>
      )}

      {view === 'version' && selected && (
        <div className="version-view">
          <section className="version-overview">
            <div className="archive-section-title"><span>当前版本</span><h3>算法 V{selected.algorithm.version}</h3><p>版本信息与预测快照一起保存，用于还原每一期当时的计算条件。</p></div>
            <dl>
              <div><dt>版本编号</dt><dd>{selected.algorithm.fingerprint}</dd></div>
              <div><dt>生成时间</dt><dd>{selected.createdAt}</dd></div>
              <div><dt>数据截止</dt><dd>第 {selected.dataEndIssue} 期</dd></div>
              <div><dt>组合公式</dt><dd>单号分数 + 0.55 × 结构分 + 0.35 × 搭配分</dd></div>
            </dl>
          </section>
          <div className="version-weight-grid">
            <section><h3>红球模型权重</h3><WeightRows weights={selected.algorithm.redWeights} /></section>
            <section><h3>蓝球模型权重</h3><WeightRows weights={selected.algorithm.blueWeights} /></section>
          </div>
          <section className="version-history">
            <div className="archive-section-title"><span>版本记录</span><h3>档案中的算法版本</h3></div>
            {archive.map((snapshot) => (
              <div className="version-history-row" key={snapshot.issue}><span>第 {snapshot.issue} 期</span><b>V{snapshot.algorithm.version}</b><code>{snapshot.algorithm.fingerprint}</code></div>
            ))}
          </section>
        </div>
      )}
    </section>
  );
}
