import { useState } from 'react';
import { useNavigation } from '../../navigation/NavigationContext';
import './algorithm.css';

interface ModelWeights {
  logistic: number;
  gradientBoosting: number;
  extraTrees: number;
}

interface CombinationFeatures {
  sum: number;
  odd: number;
  even: number;
  zones: number[];
  repeat: number;
}

export interface AlgorithmPageData {
  meta: {
    drawCount: number;
    dataStartIssue: string;
    dataEndIssue: string;
    redBaseline: number;
  };
  prediction: {
    modelWeights: {
      red: ModelWeights;
      blue: ModelWeights;
    };
    selected: {
      reds: number[];
      blue: number;
      marginal: number;
      structureScore: number;
      pairPmi: number;
      score: number;
      features: CombinationFeatures;
    };
    selectedDetails: Record<string, {
      number: number;
      modelProb: number;
      rank: number;
      recent20: number;
      recent50: number;
      gap: number;
    }>;
    blueDetail: {
      number: number;
      modelProb: number;
      rank: number;
      recent20: number;
      gap: number;
    };
    strategies: Array<{
      id: string;
      reds: number[];
      note: string;
      features: CombinationFeatures;
    }>;
    redPool: number[];
  };
  backtest: {
    testDraws: number;
    startIssue: string;
    endIssue: string;
    redTop6: number;
    redRandomTop6: number;
    blueTop1: number;
    blueRandomTop1: number;
  };
}

const CODE_EXAMPLES = [
  {
    id: 'history',
    label: '01 历史记录',
    title: '检查号码的历史表现',
    description: '先把每一期开奖整理成统一格式，再为每个号码生成一份可以直接比较的历史记录。',
    data: [
      '每期6个红球、1个蓝球和开奖日期。',
      '最近20、50、100、200期，以及全部历史。',
      '号码上次出现的时间和最近5期记录。',
    ],
    logic: [
      '把每个号码在每一期标记为“出现”或“未出现”。',
      '同时观察短期和长期，避免只看某一小段数据。',
      '计算时只读取预测日期之前已经发生的开奖。',
    ],
    methods: [
      '分段统计：分别计算不同时间范围内的出现次数。',
      '近期加权：越近的开奖保留稍高影响。',
      '间隔计算：记录距离上次出现已经过去多少期。',
    ],
    code: `def build_features(draws, end_index, number, color):
    history = draws[:end_index]       # 只读取预测期之前的数据
    appeared = occurrence_series(history, number, color)

    return {
        "frequency_all": appeared.mean(),
        "frequency_20": appeared[-20:].mean(),
        "frequency_50": appeared[-50:].mean(),
        "frequency_100": appeared[-100:].mean(),
        "frequency_200": appeared[-200:].mean(),
        "heat_10": ewma(appeared, span=10),
        "heat_30": ewma(appeared, span=30),
        "heat_90": ewma(appeared, span=90),
        "current_gap": current_gap(appeared),
        "lag_1_to_5": appeared[-5:].tolist(),
        "weekday_rate": weekday_rate(history, number),
        "last_draw_transition": transition_rate(history, number),
    }`,
  },
  {
    id: 'ranking',
    label: '02 统一比较',
    title: '按统一标准排列号码',
    description: '三种方法分别判断每个号码，再按历史检验表现合并结果，得到最终顺序。',
    data: [
      '第一步为每个号码生成的全部历史记录。',
      '33个红球和16个蓝球分开计算。',
      '三种方法在历史检验中的实际表现。',
    ],
    logic: [
      '三种方法独立判断，避免结果只依赖一种思路。',
      '历史检验表现更稳定的方法，占比更高。',
      '合并后从高到低排序，红球保留前18个。',
    ],
    methods: [
      '逻辑回归：判断各项历史记录与结果的稳定关系。',
      '梯度提升：发现多项记录组合后出现的变化。',
      '极端随机树：用多组不同判断共同投票，降低单次偏差。',
    ],
    code: `def rank_numbers(models, features, weights, expected_count):
    lr_prob = models["logistic"].predict_proba(features)[:, 1]
    hgb_prob = models["gradient_boosting"].predict_proba(features)[:, 1]
    et_prob = models["extra_trees"].predict_proba(features)[:, 1]

    merged = (
        weights["logistic"] * lr_prob
        + weights["gradient_boosting"] * hgb_prob
        + weights["extra_trees"] * et_prob
    )

    probability = isotonic_calibrator.transform(merged)
    for _ in range(8):
        probability *= expected_count / probability.sum()
        probability = np.clip(probability, 1e-6, 1 - 1e-6)

    return np.argsort(probability)[::-1]`,
  },
  {
    id: 'combination',
    label: '03 组合选择',
    title: '比较全部可选组合',
    description: '前18个红球可以组成18,564种选择。系统逐一计算，不直接照搬排名前6。',
    data: [
      '前18个红球各自的最终得分。',
      '历史开奖中整组号码的常见分布。',
      '任意两个号码过去同时出现的次数。',
    ],
    logic: [
      '列出前18个红球能够组成的全部18,564种选择。',
      '先看6个号码各自的得分，再看整组是否过度集中。',
      '最后参考号码之间过去同时出现的情况，选出总分最高的一组。',
    ],
    methods: [
      '单号得分：汇总6个号码在第二步得到的结果。',
      '整组检查：比较总和、前后跨度、奇偶数量和位置分布。',
      '共同出现检查：判断号码搭配是否明显偏离历史记录。',
    ],
    code: `def score_combination(reds, red_probability, history):
    marginal = sum(np.log(red_probability[n]) for n in reds)
    structure = structure_log_density(
        sum_value=sum(reds),
        span=max(reds) - min(reds),
        odd_count=sum(n % 2 for n in reds),
        zone_counts=count_zones(reds),
        repeat_count=count_repeats(reds, history[-1]),
    )
    pair_pmi = mean_pair_pmi(reds, history)

    return marginal + 0.55 * structure + 0.35 * pair_pmi

candidates = combinations(red_pool[:18], 6)
best_reds = max(candidates, key=lambda reds: score_combination(
    reds, red_probability, history
))`,
  },
  {
    id: 'validation',
    label: '04 结果检验',
    title: '用历史开奖检验方法',
    description: '沿着时间顺序重复预测，确保每次都只使用当时已经发生的数据，再与随机选择比较。',
    data: [
      '连续360期真实历史开奖。',
      '每一期之前能够看到的全部历史记录。',
      '同样数量的随机选择结果，作为比较标准。',
    ],
    logic: [
      '从较早的一期开始，假装下一期结果还未知。',
      '重新执行前三步，记录红球命中数和蓝球是否命中。',
      '向后移动一期并重复，最后汇总360次结果。',
    ],
    methods: [
      '按时间向前检验：训练数据永远早于被检验的开奖。',
      '红球记录：排名前6平均命中多少个。',
      '蓝球记录：排名第1的号码命中了多少次。',
    ],
    code: `def walk_forward_backtest(draws, test_draws=360):
    results = []

    for target_index in range(len(draws) - test_draws, len(draws)):
        history = draws[:target_index]       # 不读取目标期开奖
        actual = draws[target_index]

        red_rank, blue_rank = predict(history)
        results.append({
            "red_hits": len(set(red_rank[:6]) & set(actual.reds)),
            "blue_hit": int(blue_rank[0] == actual.blue),
        })

    return compare_with_random_baseline(results)`,
  },
] as const;

function BallRow({ numbers, blue }: { numbers: number[]; blue?: number }) {
  return (
    <div className="logic-ball-row">
      {numbers.map((number) => <span className="logic-ball" key={number}>{String(number).padStart(2, '0')}</span>)}
      {blue != null && <><i>+</i><span className="logic-ball blue">{String(blue).padStart(2, '0')}</span></>}
    </div>
  );
}

function formatWeights(weights: ModelWeights): string {
  return `逻辑回归 ${Math.round(weights.logistic * 100)}% · 梯度提升 ${Math.round(weights.gradientBoosting * 100)}% · 极端随机树 ${Math.round(weights.extraTrees * 100)}%`;
}

export function AlgorithmPage({ data }: { data: AlgorithmPageData }) {
  const { activePage } = useNavigation();
  const [activeCode, setActiveCode] = useState<(typeof CODE_EXAMPLES)[number]['id']>('history');
  const codeExample = CODE_EXAMPLES.find((example) => example.id === activeCode) ?? CODE_EXAMPLES[0];
  const selected = data.prediction.selected;
  const exampleNumber = Object.values(data.prediction.selectedDetails)
    .filter((detail) => selected.reds.includes(detail.number))
    .sort((left, right) => left.rank - right.rank)[0];
  const marginalStrategy = data.prediction.strategies.find((strategy) => strategy.id === 'marginal');
  const marginalReds = marginalStrategy?.reds ?? selected.reds;
  const marginalFeatures = marginalStrategy?.features ?? selected.features;
  const removedNumbers = marginalReds.filter((number) => !selected.reds.includes(number));
  const addedNumbers = selected.reds.filter((number) => !marginalReds.includes(number));
  const currentImplementation = {
    history: `当前使用：第 ${data.meta.dataStartIssue} 期至第 ${data.meta.dataEndIssue} 期，共 ${data.meta.drawCount.toLocaleString('zh-CN')} 期历史开奖。`,
    ranking: `当前权重：红球 ${formatWeights(data.prediction.modelWeights.red)}；蓝球 ${formatWeights(data.prediction.modelWeights.blue)}。`,
    combination: `当前计算：从排名前 ${data.prediction.redPool.length} 的红球中，完整比较 18,564 种6球组合。`,
    validation: `当前检验：第 ${data.backtest.startIssue} 期至第 ${data.backtest.endIssue} 期，共 ${data.backtest.testDraws} 期。`,
  }[codeExample.id];

  return (
    <section id="algorithm" className={`page algorithm-page${activePage === 'algorithm' ? ' active' : ''}`}>
      <div className="simple-intro">
        <span className="intro-kicker">预测方法</span>
        <h2>预测号码如何生成</h2>
        <p className="main-answer">系统分析 {data.meta.drawCount.toLocaleString('zh-CN')} 期历史开奖，用同一套标准比较全部号码，并从 18,564 种红球组合中选择最终结果。</p>
        <div className="intro-result">
          <span>本期参考号码</span>
          <BallRow numbers={selected.reds} blue={selected.blue} />
        </div>
      </div>

      <div className="simple-story">
        <section className="story-step">
          <div className="story-side"><span>01</span><b>历史记录</b></div>
          <div className="story-content">
            <h2>检查号码的历史表现</h2>
            <p>查看每个号码近期出现了多少次，以及距离上次出现过去了多久。</p>
            <div className="history-line">
              <span className="logic-ball">{String(exampleNumber.number).padStart(2, '0')}</span>
              <span>最近20期 <b>{exampleNumber.recent20}次</b></span>
              <span>最近50期 <b>{exampleNumber.recent50}次</b></span>
              <span>上次出现 <b>{exampleNumber.gap === 0 ? '就是上一期' : `${exampleNumber.gap}期前`}</b></span>
            </div>
            <p className="story-note">33个红球都经过相同检查，标准不会因号码不同而改变。</p>
          </div>
        </section>

        <section className="story-step">
          <div className="story-side"><span>02</span><b>统一比较</b></div>
          <div className="story-content">
            <h2>按统一标准排列号码</h2>
            <p>第一步得到的记录不能直接决定顺序。系统先把每个号码的近期次数、长期次数和间隔情况合成一个比较分，再按分数从高到低排列。</p>
            <div className="story-process">
              <div><span>放入</span><b>33份号码记录</b><small>每个号码使用相同内容</small></div>
              <i>→</i>
              <div><span>计算</span><b>得到一个比较分</b><small>同一套规则处理33个号码</small></div>
              <i>→</i>
              <div><span>得到</span><b>从高到低的排名</b><small>前18个进入组合比较</small></div>
            </div>
            <div className="story-example-result"><span className="logic-ball">{String(exampleNumber.number).padStart(2, '0')}</span><p>本期{exampleNumber.number}号的比较分排在<b>第{exampleNumber.rank}名</b>，因此进入前18个候选号码。</p></div>
            <p className="story-note">这个分数只用于比较号码先后，不等于中奖概率。</p>
          </div>
        </section>

        <section className="story-step">
          <div className="story-side"><span>03</span><b>组合选择</b></div>
          <div className="story-content">
            <h2>比较全部可选组合</h2>
            <p>前18个号码可以组成 18,564 组。每组都比较三件事：6个号码各自的分数、整组号码的分布、号码之间过去的搭配记录，三项合起来最高的一组成为最终组合。</p>
            <div className="story-compare">
              <div>
                <span className="combination-label"><b>只取排名前6</b><small>奇偶 {marginalFeatures.odd}:{marginalFeatures.even} · 上期重号 {marginalFeatures.repeat} 个</small></span>
                <BallRow numbers={marginalReds} />
              </div>
              <div className="choice-change"><b>拿掉 {removedNumbers.map((number) => String(number).padStart(2, '0')).join('、')}</b><i>换成</i><b>{addedNumbers.map((number) => String(number).padStart(2, '0')).join('、')}</b></div>
              <div className="final-choice">
                <span className="combination-label"><b>综合得分最高</b><small>奇偶 {selected.features.odd}:{selected.features.even} · 上期重号 {selected.features.repeat} 个</small></span>
                <BallRow numbers={selected.reds} />
              </div>
            </div>
            <p className="story-note">这次换号不是因为{removedNumbers.map((number) => String(number).padStart(2, '0')).join('、')}单独表现差，而是换成{addedNumbers.map((number) => String(number).padStart(2, '0')).join('、')}后，6个号码作为一组的总分更高。</p>
            <div className="blue-story"><span className="logic-ball blue">{String(selected.blue).padStart(2, '0')}</span><p>蓝球单独处理：比较16个号码后，选择排名最高的{selected.blue}号。</p></div>
          </div>
        </section>

        <section className="story-step">
          <div className="story-side"><span>04</span><b>结果检验</b></div>
          <div className="story-content">
            <h2>用历史开奖检验方法</h2>
            <p>检验时先遮住某一期的开奖结果，只使用它之前的数据重新预测；记录命中情况后，再移动到下一期，连续重复 {data.backtest.testDraws} 次。</p>
            <div className="backtest-process">
              <span>遮住当期开奖结果</span><i>→</i><span>只用更早数据预测</span><i>→</i><span>揭晓并记录命中</span><i>→</i><span>向后重复360期</span>
            </div>
            <div className="check-story">
              <p><span>红球前6平均每期命中</span><b>{data.backtest.redTop6.toFixed(3)} 个</b><small>随机选择 {data.backtest.redRandomTop6.toFixed(3)} 个 · 未高于随机</small></p>
              <p><span>蓝球第一名命中率</span><b>{(data.backtest.blueTop1 * 100).toFixed(1)}%</b><small>随机选择 {(data.backtest.blueRandomTop1 * 100).toFixed(1)}% · 略高于随机</small></p>
            </div>
            <p className="story-note">当前检验中，红球方法没有显示出高于随机选择的优势；蓝球略高，但不足以保证未来结果。</p>
          </div>
        </section>
      </div>

      <details className="algorithm-technical">
        <summary>
          <em>进阶内容</em>
          <span><b>我想了解 更底层的实现</b><small>对应上面的四个步骤，查看每一步使用的数据、处理逻辑、算法和核心代码。</small></span>
          <strong>展开</strong>
        </summary>
        <div className="algorithm-code-tool">
          <div className="algorithm-code-tabs" role="tablist" aria-label="四步实现说明">
            {CODE_EXAMPLES.map((example) => (
              <button key={example.id} className={activeCode === example.id ? 'active' : ''} role="tab" aria-selected={activeCode === example.id} onClick={() => setActiveCode(example.id)}>
                {example.label}
              </button>
            ))}
          </div>
          <div className="algorithm-code-panel" role="tabpanel">
            <div className="algorithm-code-title">
              <div><h3>{codeExample.title}</h3><p>{codeExample.description}</p></div>
              <span>Python</span>
            </div>
            <p className="implementation-current">{currentImplementation}</p>
            <div className="implementation-guide">
              <section>
                <h4>使用的数据</h4>
                <ul>{codeExample.data.map((item) => <li key={item}>{item}</li>)}</ul>
              </section>
              <section>
                <h4>处理逻辑</h4>
                <ul>{codeExample.logic.map((item) => <li key={item}>{item}</li>)}</ul>
              </section>
              <section>
                <h4>使用的方法</h4>
                <ul>{codeExample.methods.map((item) => <li key={item}>{item}</li>)}</ul>
              </section>
            </div>
            <div className="algorithm-code-caption"><b>核心代码</b><span>代码与本步骤的处理顺序一致</span></div>
            <pre><code>{codeExample.code}</code></pre>
          </div>
        </div>
      </details>
    </section>
  );
}
