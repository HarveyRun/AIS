import { useEffect, useRef, useState } from 'react';
import { ChevronRight, HeartHandshake, LoaderCircle, Mail, Search, Sparkles, X } from 'lucide-react';
import UserAvatar from '../../components/profile/UserAvatar.jsx';
import TalentCard from '../../components/talent/TalentCard.jsx';
import './HomePage.css';

export default function HomePage({
  go,
  setTalent,
  userProfile,
  unreadNoticeCount = 0,
  answerers = [],
  answererKeyword = '',
  hasMore,
  loadMore,
  searchAnswerers,
  notify,
}) {
  const [slide, setSlide] = useState(0);
  const [keyword, setKeyword] = useState(answererKeyword);
  const [searching, setSearching] = useState(false);
  const searchReadyRef = useRef(false);
  const searchRequestRef = useRef(0);
  const visiblePeople = answerers;
  const banners = [
    [
      '大多数人都会遇到',
      '买房、装修，是生活里绕不开的一环',
      '先问问做过和经历过的人，别稀里糊涂花钱',
    ],
    ['上班总会遇到点糟心事', '离职、裁员，遇到容易慌？', '找经历过的人聊聊，心里就有底了'],
    ['家庭的担子', '照顾老人、孩子成长，没人天生就会', '问问过来人，听听日常里管用的经验'],
  ];
  useEffect(() => {
    const t = setInterval(() => setSlide((x) => (x + 1) % banners.length), 10000);
    const scroll = () => {
      if (!searching && innerHeight + scrollY >= document.body.offsetHeight - 180) loadMore?.();
    };
    addEventListener('scroll', scroll);
    return () => {
      clearInterval(t);
      removeEventListener('scroll', scroll);
    };
  }, [loadMore, searching]);

  useEffect(() => {
    if (!searchReadyRef.current) {
      searchReadyRef.current = true;
      return undefined;
    }
    const timer = window.setTimeout(async () => {
      const requestId = searchRequestRef.current + 1;
      searchRequestRef.current = requestId;
      try {
        setSearching(true);
        await searchAnswerers(keyword);
      } catch (error) {
        if (requestId === searchRequestRef.current) {
          notify?.(error.message, 'error');
        }
      } finally {
        if (requestId === searchRequestRef.current) {
          setSearching(false);
        }
      }
    }, 300);
    return () => window.clearTimeout(timer);
  }, [keyword, notify, searchAnswerers]);

  return (
    <>
      <header className="brand-top">
        <button className="home-profile-entry" type="button" onClick={() => go('profile')} aria-label="进入我的">
          <UserAvatar
            src={userProfile?.avatar}
            uid={userProfile?.uid}
            name={userProfile?.name}
          />
        </button>
        <div className="home-job-search">
          <Search />
          <input
            type="search"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索主职"
            aria-label="搜索主职"
          />
          {searching && <LoaderCircle className="home-search-loading" />}
          {!searching && keyword && (
            <button type="button" onClick={() => setKeyword('')} aria-label="清除搜索">
              <X />
            </button>
          )}
        </div>
        <button className="round" type="button" onClick={() => go('notices')} aria-label="消息">
          <Mail size={20} />
          {unreadNoticeCount > 0 && (
            <i className="notice-count">
              {unreadNoticeCount > 99 ? '99+' : unreadNoticeCount}
            </i>
          )}
        </button>
      </header>
      <section className="intro-section">
        <div className="pt15">
          <div className="intro-banner carousel">
            <div>
              <span>
                <Sparkles size={13} /> {banners[slide][0]}
              </span>
              <h2>{banners[slide][1]}</h2>
              <p>{banners[slide][2]}</p>
            </div>
            <nav>
              {banners.map((_, i) => (
                <button
                  className={slide === i ? 'active' : ''}
                  onClick={() => setSlide(i)}
                  key={i}
                />
              ))}
            </nav>
          </div>
        </div>
      </section>
      <section className="home-actions">
        <button onClick={() => go('knowledge')}>
          <Search size={19} />
          <div>
            <b>按事情</b>
            <small>先看看应该问哪些人</small>
          </div>
          <ChevronRight />
        </button>
        <button onClick={() => go('experiences')}>
          <HeartHandshake size={19} />
          <div>
            <b>按经历</b>
            <small>找真正经历过的人聊聊</small>
          </div>
          <ChevronRight />
        </button>
      </section>
      <section className="talent-section">
        <div className="section-head simple">
          <h2>可以帮你的人</h2>
        </div>
        <div className="talent-list">
          {visiblePeople.map((p) => (
            <TalentCard
              key={p.uid}
              p={p}
              onClick={() => {
                setTalent(p);
                go('talent');
              }}
            />
          ))}
          {!searching && keyword && visiblePeople.length === 0 && (
            <div className="home-search-empty">没有找到相关主职</div>
          )}
        </div>
      </section>
      <div className="endline">
        {searching ? '正在查找' : hasMore ? '继续下滑，看看更多人' : '已经到底啦'}
      </div>
    </>
  );
}
