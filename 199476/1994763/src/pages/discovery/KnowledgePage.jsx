import { ArrowRight, Check } from 'lucide-react';
import { useEffect } from 'react';
import Page from '../../components/layout/Page.jsx';
import './DiscoveryPages.css';

export default function KnowledgePage({
  go,
  category,
  setCategory,
  matterId,
  setMatterId,
  setProblem,
  setExperience,
  setExperienceCategoryId,
  catalog = { categories: [] },
  refreshCatalog,
}) {
  const categories = catalog.categories || [];
  const selectedCategory = categories.find((item) => item.name === category) || categories[0];
  useEffect(() => { refreshCatalog?.(); }, [refreshCatalog]);
  const visibleGroups = (selectedCategory?.subcategories || []).filter((group) => group.matters.length > 0);

  return (
    <Page title="按事情找人" back={() => go('home')}>
      <div className="category-tabs">
        {categories.map((item) => <button className={selectedCategory?.code === item.code ? 'active' : ''} onClick={() => { setCategory(item.name); setProblem(''); setMatterId(null); }} key={item.code}>{item.name}</button>)}
      </div>
      <section className="tree-list">
        {visibleGroups.map((group) => (
          <div className="tree-group" key={group.id}>
            <h3>{group.name}<span>{group.matters.length}件事</span></h3>
            <div>
              {group.matters.map((matter) => (
                <button className={matterId === matter.id ? 'selected' : ''} key={matter.id} onClick={() => { setExperience(''); setExperienceCategoryId(null); setProblem(matter.title); setMatterId(matter.id); }}>
                  {matter.title}{matterId === matter.id ? <Check size={15} /> : <span>•</span>}
                </button>
              ))}
            </div>
          </div>
        ))}
        {!visibleGroups.length && <p className="discovery-empty">没有找到相关事情</p>}
      </section>
      <button disabled={!matterId} className="sticky-primary" onClick={() => go('filtered')}>找人问问 <ArrowRight size={18} /></button>
    </Page>
  );
}
