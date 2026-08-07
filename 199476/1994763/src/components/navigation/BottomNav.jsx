import { CircleUserRound, FileCheck2, Home, MessageCircleMore } from 'lucide-react';

const NAV_ITEMS = [
  { id: 'home', label: '首页', Icon: Home },
  { id: 'requests', label: '我的事项', Icon: FileCheck2 },
  { id: 'messages', label: '消息', Icon: MessageCircleMore },
  { id: 'profile', label: '我的', Icon: CircleUserRound },
];

export default function BottomNav({ active, onChange, unreadCount = 0 }) {
  return (
    <nav className="bottom-nav" aria-label="主要导航">
      {NAV_ITEMS.map(({ Icon, id, label }) => (
        <button
          type="button"
          className={active === id ? 'active' : ''}
          onClick={() => onChange(id)}
          key={id}
        >
          <span className="nav-icon">
            <Icon />
            {id === 'messages' && unreadCount > 0 && (
              <em>{unreadCount > 99 ? '99+' : unreadCount}</em>
            )}
          </span>
          <span>{label}</span>
        </button>
      ))}
    </nav>
  );
}
