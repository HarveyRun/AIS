import {
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  Banknote,
  Bell,
  BriefcaseBusiness,
  Building2,
  CalendarDays,
  Camera,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleUserRound,
  Clock3,
  Coins,
  FileCheck2,
  HeartHandshake,
  Info,
  Landmark,
  LockKeyhole,
  MessageCircleMore,
  MessageSquareWarning,
  MoreHorizontal,
  PlusCircle,
  Search,
  Send,
  ShieldCheck,
  SlidersHorizontal,
  Smartphone,
  Sparkles,
  Star,
  UsersRound,
  WalletCards,
  X,
  Zap,
} from 'lucide-react';

export default function RequestCard({ title, people, status, color, onClick }) {
  return (
    <button onClick={onClick}>
      <i style={{ background: color }}>
        <FileCheck2 />
      </i>
      <div>
        <h3>{title}</h3>
        <p>{people}</p>
      </div>
      <span>{status}</span>
      <ChevronRight />
    </button>
  );
}
