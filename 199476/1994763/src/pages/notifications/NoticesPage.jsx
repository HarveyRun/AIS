import { Bell } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './NoticesPage.css';

export default function NoticesPage({ go, notices, setNotices }) {
  const openNotice = (notice) => {
    setNotices((current) =>
      current.map((item) => (item.id === notice.id ? { ...item, read: true } : item)),
    );
    if (notice.screen) go(notice.screen);
  };

  return (
    <Page title="通知" back={() => go('home')}>
      <section className="notice-list">
        {notices.map((notice) => (
          <button
            className={notice.read ? 'read' : ''}
            key={notice.id}
            onClick={() => openNotice(notice)}
          >
            <i>
              <Bell />
            </i>
            <div>
              <h3>{notice.title}</h3>
              <p>{notice.content}</p>
              <span>{notice.time}</span>
            </div>
            {!notice.read && <b />}
          </button>
        ))}
        {notices.length === 0 && <p className="notice-empty">暂时没有通知</p>}
      </section>
    </Page>
  );
}
