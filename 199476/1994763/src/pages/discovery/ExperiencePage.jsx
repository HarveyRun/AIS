import { Search } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import Page from '../../components/layout/Page.jsx';
import { api } from '../../api/http.js';
import './DiscoveryPages.css';

export default function ExperiencePage({
  go,
  setExperience,
  setProblem,
  setExperienceCategoryId,
  notify,
}) {
  const [keyword, setKeyword] = useState('');
  const [experiences, setExperiences] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    api
      .searchExperiences('')
      .then((items) => {
        if (active) setExperiences(items);
      })
      .catch((error) => {
        if (active) {
          setExperiences([]);
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
    if (!normalizedKeyword) return experiences;
    return experiences.filter((item) =>
      `${item.categoryName} ${item.title}`.toLocaleLowerCase('zh-CN').includes(normalizedKeyword),
    );
  }, [experiences, keyword]);

  const groupedResults = useMemo(() => {
    const groups = new Map();
    results.forEach((item) => {
      if (!groups.has(item.categoryId)) {
        groups.set(item.categoryId, {
          id: item.categoryId,
          name: item.categoryName,
          experiences: [],
        });
      }
      groups.get(item.categoryId).experiences.push(item);
    });
    return Array.from(groups.values());
  }, [results]);

  const selectExperience = (item) => {
    setProblem('');
    setExperience(item.title);
    setExperienceCategoryId(item.categoryId);
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
        {groupedResults.map((group) => (
          <div key={group.id}>
            <h2>{group.name}</h2>
            <div>
              {group.experiences.map((item) => {
                return (
                  <button
                    type="button"
                    onClick={() => selectExperience(item)}
                    key={`${item.categoryId}-${item.title}`}
                  >
                    <span>{item.title}</span>
                  </button>
                );
              })}
            </div>
          </div>
        ))}

        {!loading && groupedResults.length === 0 && <p className="discovery-empty">暂无数据</p>}
      </section>
    </Page>
  );
}
