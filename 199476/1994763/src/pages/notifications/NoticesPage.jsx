import { useEffect } from 'react';
import { Bell } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './NoticesPage.css';
import { api } from '../../api/http.js';

export default function NoticesPage({
  go,
  notices,
  setNotices,
}) {
  useEffect(() => {
    if (!notices.some((notice) => !notice.read)) return;
    api.readAllNotifications()
      .then(() => setNotices((current) => current.map((notice) => ({ ...notice, read: true }))))
      .catch(() => {});
  }, [notices, setNotices]);

  return (
    <Page title="通知" back={() => go('home')}>
      <section className="notice-list">
        {notices.map((notice) => (
          <article key={notice.id}>
            <i>
              <Bell />
            </i>
            <div>
              <h3>{notice.title}</h3>
              <p>{notice.content}</p>
              <span>{notice.time}</span>
            </div>
          </article>
        ))}
        {notices.length === 0 && <p className="notice-empty">暂时没有通知</p>}
      </section>
    </Page>
  );
}
