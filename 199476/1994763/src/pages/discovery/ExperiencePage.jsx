import { ArrowRight, Check, Search } from 'lucide-react';
import { useMemo, useState } from 'react';
import Page from '../../components/layout/Page.jsx';
import './DiscoveryPages.css';

const experienceGroups = [
  {
    title: '工作中的经历',
    items: ['经历过劳动仲裁', '经历过裁员', '成功转过行', '开过实体店'],
  },
  {
    title: '生活中的经历',
    items: ['买过二手房', '装修过旧房', '处理过交通事故', '长期照顾过老人'],
  },
  {
    title: '人生选择',
    items: ['出国留过学', '回乡创过业', '独自带过孩子', '长期在异地生活'],
  },
];

export default function ExperiencePage({ go, experience, setExperience, setProblem }) {
  const [selected, setSelected] = useState(experience || '');
  const [keyword, setKeyword] = useState('');
  const visibleGroups = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return experienceGroups
      .map((group) => ({
        ...group,
        items: group.items.filter((item) => item.toLowerCase().includes(normalizedKeyword)),
      }))
      .filter((group) => group.items.length > 0);
  }, [keyword]);

  const continueToPeople = () => {
    setProblem('');
    setExperience(selected);
    go('filtered');
  };

  return (
    <Page title="按经历找人" back={() => go('home')}>
      <label className="discovery-search">
        <Search />
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="搜索关键词"
        />
      </label>

      <section className="experience-groups">
        {visibleGroups.map((group) => (
          <div key={group.title}>
            <h2>{group.title}</h2>
            <div>
              {group.items.map((item) => (
                <button
                  type="button"
                  className={selected === item ? 'active' : ''}
                  onClick={() => setSelected(item)}
                  key={item}
                >
                  <span>{item}</span>
                  {selected === item && <Check />}
                </button>
              ))}
            </div>
          </div>
        ))}
        {visibleGroups.length === 0 && <p className="discovery-empty">没有找到相关经历</p>}
      </section>

      <button
        type="button"
        disabled={!selected}
        className="sticky-primary"
        onClick={continueToPeople}
      >
        找人问问
        <ArrowRight size={18} />
      </button>
    </Page>
  );
}
