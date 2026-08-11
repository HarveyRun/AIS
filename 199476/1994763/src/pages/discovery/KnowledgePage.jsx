import { ArrowRight, Check, Search } from 'lucide-react';
import { useMemo, useState } from 'react';
import Page from '../../components/layout/Page.jsx';
import { knowledge } from '../../data/knowledgeData.js';
import './DiscoveryPages.css';

export default function KnowledgePage({
  go,
  category,
  setCategory,
  problem,
  setProblem,
  setExperience,
}) {
  const [keyword, setKeyword] = useState('');
  const visibleGroups = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return Object.entries(knowledge[category])
      .map(([group, matters]) => [
        group,
        matters.filter((matter) => matter.toLowerCase().includes(normalizedKeyword)),
      ])
      .filter(([, matters]) => matters.length > 0);
  }, [category, keyword]);

  return (
    <Page title="按事情找人" back={() => go('home')}>
      <label className="discovery-search">
        <Search />
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="搜索关键词"
        />
      </label>

      <div className="category-tabs">
        {Object.keys(knowledge).map((item) => (
          <button
            className={category === item ? 'active' : ''}
            onClick={() => {
              setCategory(item);
              setProblem('');
            }}
            key={item}
          >
            {item}
          </button>
        ))}
      </div>

      <section className="tree-list">
        {visibleGroups.map(([group, matters]) => (
          <div className="tree-group" key={group}>
            <h3>
              {group}
              <span>{matters.length}件事</span>
            </h3>
            <div>
              {matters.map((matter) => (
                <button
                  className={problem === matter ? 'selected' : ''}
                  key={matter}
                  onClick={() => {
                    setExperience('');
                    setProblem(matter);
                  }}
                >
                  {matter}
                  {problem === matter ? <Check size={15} /> : <span>•</span>}
                </button>
              ))}
            </div>
          </div>
        ))}
        {visibleGroups.length === 0 && <p className="discovery-empty">没有找到相关事情</p>}
      </section>

      <button disabled={!problem} className="sticky-primary" onClick={() => go('filtered')}>
        找人问问 <ArrowRight size={18} />
      </button>
    </Page>
  );
}
