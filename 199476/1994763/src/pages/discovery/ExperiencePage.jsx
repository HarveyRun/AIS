import { ArrowRight, Check, Search } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import Page from '../../components/layout/Page.jsx';
import './DiscoveryPages.css';

export default function ExperiencePage({
  go,
  experience,
  experienceCategoryId,
  setExperience,
  setProblem,
  category,
  setCategory,
  setExperienceCategoryId,
  catalog = { categories: [] },
  refreshCatalog,
}) {
  const [selected, setSelected] = useState(experience || '');
  const [selectedCategoryId, setSelectedCategoryId] = useState(experienceCategoryId || null);
  const [keyword, setKeyword] = useState('');
  const categories = catalog.categories || [];
  const selectedMain = categories.find((item) => item.name === category) || categories[0];
  useEffect(() => {
    refreshCatalog?.();
  }, [refreshCatalog]);
  const visibleGroups = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return (selectedMain?.subcategories || [])
      .map((group) => ({
        ...group,
        items: group.experiences.filter((item) =>
          item.title.toLowerCase().includes(normalizedKeyword),
        ),
      }))
      .filter((group) => group.items.length > 0);
  }, [keyword, selectedMain]);

  const continueToPeople = () => {
    setProblem('');
    setExperience(selected);
    setExperienceCategoryId(selectedCategoryId);
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

      <div className="category-tabs">
        {categories.map((item) => (
          <button
            className={selectedMain?.code === item.code ? 'active' : ''}
            onClick={() => {
              setCategory(item.name);
              setSelected('');
              setSelectedCategoryId(null);
            }}
            key={item.code}
          >
            {item.name}
          </button>
        ))}
      </div>

      <section className="experience-groups">
        {visibleGroups.map((group) => (
          <div key={group.id}>
            <h2>{group.name}</h2>
            <div>
              {group.items.map((item) => (
                <button
                  type="button"
                  className={selected === item.title && selectedCategoryId === group.id ? 'active' : ''}
                  onClick={() => {
                    setSelected(item.title);
                    setSelectedCategoryId(group.id);
                  }}
                  key={`${group.id}-${item.title}`}
                >
                  <span>{item.title}</span>
                  {selected === item.title && selectedCategoryId === group.id && <Check />}
                </button>
              ))}
            </div>
          </div>
        ))}
        {visibleGroups.length === 0 && <p className="discovery-empty">没有找到相关经历</p>}
      </section>

      <button
        type="button"
        disabled={!selected || !selectedCategoryId}
        className="sticky-primary"
        onClick={continueToPeople}
      >
        找人问问
        <ArrowRight size={18} />
      </button>
    </Page>
  );
}
