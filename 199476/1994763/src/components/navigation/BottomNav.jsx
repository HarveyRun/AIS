import { CircleUserRound, Home, MessagesSquare, MessageCircleMore } from 'lucide-react';

const NAV_ITEMS = [
  { id: 'home', label: '首页', Icon: Home },
  { id: 'inquiries', label: '我的询问', Icon: MessageCircleMore },
  { id: 'profile', label: '我的', Icon: CircleUserRound },
];

export default function BottomNav({ active, onChange, inquiryUnreadCount = 0 }) {
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
            {id === 'inquiries' && inquiryUnreadCount > 0 && (
              <em>{inquiryUnreadCount > 99 ? '99+' : inquiryUnreadCount}</em>
            )}
          </span>
          <span>{label}</span>
        </button>
      ))}
    </nav>
  );
}
