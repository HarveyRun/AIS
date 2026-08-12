import { Search } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import Page from '../../components/layout/Page.jsx';
import { api } from '../../api/http.js';
import './DiscoveryPages.css';

export default function KnowledgePage({
  go,
  setProblem,
  setMatterId,
  setExperience,
  setExperienceCategoryId,
  notify,
}) {
  const [keyword, setKeyword] = useState('');
  const [matters, setMatters] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    api
      .searchMatters('')
      .then((items) => {
        if (active) setMatters(items);
      })
      .catch((error) => {
        if (active) {
          setMatters([]);
          notify(error.message, 'error');
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [notify]);

  const results = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLocaleLowerCase('zh-CN');
    if (!normalizedKeyword) return matters;
    return matters.filter((matter) =>
      `${matter.categoryName} ${matter.title}`
        .toLocaleLowerCase('zh-CN')
        .includes(normalizedKeyword),
    );
  }, [keyword, matters]);

  const groupedResults = useMemo(() => {
    const groups = new Map();
    results.forEach((matter) => {
      if (!groups.has(matter.categoryId)) {
        groups.set(matter.categoryId, {
          id: matter.categoryId,
          name: matter.categoryName,
          matters: [],
        });
      }
      groups.get(matter.categoryId).matters.push(matter);
    });
    return Array.from(groups.values());
  }, [results]);

  const selectMatter = (matter) => {
    setExperience('');
    setExperienceCategoryId(null);
    setProblem(matter.title);
    setMatterId(matter.id);
    go('filtered');
  };

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

      <section className="tree-list">
        {groupedResults.map((group) => (
          <div className="tree-group" key={group.id}>
            <h3>
              {group.name}
              <span>{group.matters.length}件事</span>
            </h3>
            <div>
              {group.matters.map((matter) => (
                <button type="button" key={matter.id} onClick={() => selectMatter(matter)}>
                  {matter.title}
                  <span>•</span>
                </button>
              ))}
            </div>
          </div>
        ))}

        {!loading && groupedResults.length === 0 && (
          <p className="discovery-empty">没有找到相关事情</p>
        )}
      </section>
    </Page>
  );
}
