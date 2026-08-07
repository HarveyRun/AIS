import { BadgeCheck, ChevronRight, Clock3 } from 'lucide-react';

export default function TalentCard({ p, onClick, index }) {
  return (
    <button className="person-card" onClick={onClick}>
      <div className="person-top">
        <div className="avatar" style={{ background: p.color }}>
          {p.name.slice(-1)}
          <i>
            <BadgeCheck size={15} />
          </i>
        </div>
        <div className="identity">
          <h3>{p.name}</h3>
          <span>UID {p.uid}</span>
        </div>
        <ChevronRight className="chev" />
      </div>
      <div className="talent-card-details">
        <Career label="主职" name={p.main} years={p.mainYears} />
        <div className="talent-service-time">
          <small>可服务时间</small>
          <span>
            <Clock3 />
            {p.serviceTime}
          </span>
        </div>
      </div>
    </button>
  );
}

export function Career({ label, name, years }) {
  return (
    <div>
      <small>{label}</small>
      <b>{name || '-'}</b>
      <span>{name ? `${years}年经历` : '暂未认证'}</span>
    </div>
  );
}
