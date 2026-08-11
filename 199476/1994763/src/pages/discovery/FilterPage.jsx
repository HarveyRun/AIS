import { useState } from 'react';
import { Search } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import { people } from '../../data/mockData.js';
import TalentCard from '../../components/talent/TalentCard.jsx';
import './DiscoveryPages.css';

const semanticGroups = [
  ['劳动仲裁', '辞退', '离职', '裁员', '工资', '劳动关系'],
  ['二手房', '买房', '租房', '押金', '房东', '产权'],
  ['装修', '旧房', '漏水', '水电', '收纳', '保洁', '家居'],
  ['老人', '孩子', '新生儿', '幼儿园', '家庭', '邻居'],
  ['开店', '门店', '商铺', '经营', '客流', '创业', '实体店'],
  ['程序', '软件', '小程序', '电脑', '网络', '应用', '产品'],
  ['交通事故', '车辆', '剐蹭'],
  ['留学', '异地生活', '出国'],
];

function normalizeContext(value) {
  return String(value || '')
    .replace(/经历过|处理过|亲自|如何|怎么|应该|需要|哪些|什么|第一次|长期/g, '')
    .replace(/成功转过行/g, '转行成功')
    .replace(/装修过旧房/g, '装修过房子')
    .replace(/出国留过学/g, '留过学')
    .replace(/独自带过孩子/g, '独自带孩子')
    .replace(/在异地生活/g, '异地生活');
}

export default function FilterPage({
  go,
  setTalent,
  problem = '',
  experience = '',
  title = '找人',
  backScreen = 'home',
}) {
  const [keyword, setKeyword] = useState('');
  const filtered = people.filter((person) => {
    const personContext = normalizeContext(
      [
        person.main,
        person.venture,
        person.industry,
        ...(person.tags || []),
        ...(person.experiences || []),
      ].join(''),
    );
    const normalizedProblem = normalizeContext(problem);
    const relatedProblemGroup = semanticGroups.find((group) =>
      group.some((word) => problem.includes(word)),
    );
    const matchesProblem =
      !problem ||
      personContext.includes(normalizedProblem) ||
      relatedProblemGroup?.some((word) => personContext.includes(normalizeContext(word)));
    const normalizedExperience = normalizeContext(experience);
    const matchesExperience =
      !experience ||
      personContext.includes(normalizedExperience) ||
      semanticGroups
        .find((group) => group.some((word) => experience.includes(word)))
        ?.some((word) => personContext.includes(normalizeContext(word)));
    const matchesKeyword =
      !keyword || [person.main, person.venture, person.name].join(' ').includes(keyword);
    return matchesProblem && matchesExperience && matchesKeyword;
  });
  return (
    <Page title={title} back={() => go(backScreen)}>
      {(problem || experience) && (
        <section className="result-title compact">
          <span>{experience ? '你想问问' : '你想了解'}</span>
          <h2>{experience || problem}</h2>
          <p>{experience ? '下面的人提交过相关经历证明' : '下面的人长期从事相关工作'}</p>
        </section>
      )}
      <section className="filter-panel">
        <div className="searchbox">
          <Search size={18} />
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="搜索岗位或昵称"
          />
        </div>
      </section>
      <section className="talent-section results">
        <div className="section-head simple">
          <h2>找到这些人</h2>
          <small>{filtered.length}人</small>
        </div>
        <div className="talent-list">
          {filtered.map((p) => (
            <TalentCard
              key={p.uid}
              p={p}
              onClick={() => {
                setTalent(p);
                go('talent');
              }}
            />
          ))}
        </div>
      </section>
    </Page>
  );
}
