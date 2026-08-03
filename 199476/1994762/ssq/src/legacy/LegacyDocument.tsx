import parse, { domToReact, Element, type DOMNode, type HTMLReactParserOptions } from 'html-react-parser';
import { Children, type ReactNode, useMemo } from 'react';
import { AlgorithmPage, type AlgorithmPageData } from '../features/algorithm/AlgorithmPage';
import { ArchivePage, type ArchivePageData } from '../features/archive/ArchivePage';
import { type PageId, type WorkspacePageId, useNavigation } from '../navigation/NavigationContext';
import './navigation.css';
import bodyTemplate from './body.html?raw';

function hasClass(node: Element, name: string): boolean {
  return (node.attribs.class ?? '').split(/\s+/).includes(name);
}

function toggleClass(className: string, name: string, enabled: boolean): string {
  const classes = new Set(className.split(/\s+/).filter(Boolean));
  if (enabled) classes.add(name);
  else classes.delete(name);
  return [...classes].join(' ');
}

function Shell({ children }: { children: ReactNode }) {
  return <div className="shell">{children}</div>;
}

function Header({ children }: { children: ReactNode }) {
  return <header className="header">{children}</header>;
}

function MainNavigation({ children: _children }: { children: ReactNode }) {
  const { activePage, lastWorkspacePage, setActivePage } = useNavigation();
  return (
    <nav className="nav" aria-label="主导航">
      <PageNavigationButton page="forecast" className="">首页</PageNavigationButton>
      <button
        className={toggleClass('', 'active', activePage !== 'forecast')}
        onClick={() => {
          setActivePage(lastWorkspacePage);
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }}
      >
        数据与说明
      </button>
    </nav>
  );
}

const workspacePages: Array<{ id: WorkspacePageId; label: string }> = [
  { id: 'dimensions', label: '趋势分析' },
  { id: 'history', label: '历史统计' },
  { id: 'archive', label: '预测档案' },
  { id: 'algorithm', label: '预测原理' },
  { id: 'professional', label: '术语说明' },
];

function WorkspaceNavigation() {
  const { activePage, setActivePage } = useNavigation();
  if (activePage === 'forecast') return null;

  return (
    <div className="workspace-navigation">
      <div className="workspace-navigation-title">数据与说明</div>
      <nav className="workspace-navigation-tabs" aria-label="数据与说明导航">
        {workspacePages.map((page) => (
          <button
            key={page.id}
            className={activePage === page.id ? 'active' : ''}
            onClick={() => {
              setActivePage(page.id);
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
          >
            {page.label}
          </button>
        ))}
      </nav>
    </div>
  );
}

function PageSection({ id, className, children }: { id: string; className: string; children: ReactNode }) {
  const { activePage } = useNavigation();
  return <section id={id} className={toggleClass(className, 'active', activePage === id)}>{children}</section>;
}

function PageNavigationButton({ page, className, children }: { page: PageId; className: string; children: ReactNode }) {
  const { activePage, setActivePage } = useNavigation();
  return (
    <button
      className={toggleClass(className, 'active', activePage === page)}
      data-page={page}
      onClick={() => {
        setActivePage(page);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }}
    >
      {children}
    </button>
  );
}

function HistoryNavigationButton({ section, className, children }: { section: string; className: string; children: ReactNode }) {
  const { activeHistorySection, setActiveHistorySection } = useNavigation();
  return <button className={toggleClass(className, 'active', activeHistorySection === section)} data-section={section} onClick={() => setActiveHistorySection(section)}>{children}</button>;
}

function ProfessionalNavigationButton({ section, className, children }: { section: string; className: string; children: ReactNode }) {
  const { activeProfessionalSection, setActiveProfessionalSection } = useNavigation();
  return <button className={toggleClass(className, 'active', activeProfessionalSection === section)} data-prof={section} onClick={() => setActiveProfessionalSection(section)}>{children}</button>;
}

function HistorySection({ id, className, children }: { id: string; className: string; children: ReactNode }) {
  const { activeHistorySection } = useNavigation();
  return <div id={id} className={toggleClass(className, 'active', activeHistorySection === id)}>{children}</div>;
}

function ProfessionalSection({ id, className, children }: { id: string; className: string; children: ReactNode }) {
  const { activeProfessionalSection } = useNavigation();
  return <div id={id} className={toggleClass(className, 'active', `prof-${activeProfessionalSection}` === id)}>{children}</div>;
}

export function LegacyDocument({ data }: { data: unknown }) {
  return useMemo(() => {
    const options: HTMLReactParserOptions = {
      replace(domNode) {
        if (!(domNode instanceof Element)) return undefined;
        const children = domToReact(domNode.children as DOMNode[], options);
        const className = domNode.attribs.class ?? '';

        if (domNode.name === 'div' && hasClass(domNode, 'shell')) return <Shell>{children}</Shell>;
        if (domNode.name === 'header' && hasClass(domNode, 'header')) return <Header>{children}</Header>;
        if (domNode.name === 'nav' && hasClass(domNode, 'nav')) return <MainNavigation>{children}</MainNavigation>;
        if (domNode.name === 'main') return <main><WorkspaceNavigation />{children}<ArchivePage data={data as ArchivePageData} /><AlgorithmPage data={data as AlgorithmPageData} /></main>;
        if (domNode.name === 'select' && domNode.attribs.id === 'drawPageSize') {
          return <select className={className} id="drawPageSize" defaultValue="50">{children}</select>;
        }
        if (domNode.name === 'option' && 'selected' in domNode.attribs) return <option>{children}</option>;

        if (domNode.name === 'button' && domNode.attribs['data-page']) {
          return <PageNavigationButton page={domNode.attribs['data-page'] as PageId} className={className}>{children}</PageNavigationButton>;
        }
        if (domNode.name === 'button' && domNode.attribs['data-section']) {
          return <HistoryNavigationButton section={domNode.attribs['data-section']} className={className}>{children}</HistoryNavigationButton>;
        }
        if (domNode.name === 'button' && domNode.attribs['data-prof']) {
          return <ProfessionalNavigationButton section={domNode.attribs['data-prof']} className={className}>{children}</ProfessionalNavigationButton>;
        }
        if (domNode.name === 'section' && hasClass(domNode, 'page')) {
          return <PageSection id={domNode.attribs.id} className={className}>{children}</PageSection>;
        }
        if (domNode.name === 'div' && hasClass(domNode, 'history-section')) {
          return <HistorySection id={domNode.attribs.id} className={className}>{children}</HistorySection>;
        }
        if (domNode.name === 'div' && hasClass(domNode, 'prof-section')) {
          return <ProfessionalSection id={domNode.attribs.id} className={className}>{children}</ProfessionalSection>;
        }

        return undefined;
      },
    };

    return parse(bodyTemplate, options);
  }, [data]);
}
