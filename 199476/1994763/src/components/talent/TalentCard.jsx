import { ChevronRight } from 'lucide-react';
import UserAvatar from '../profile/UserAvatar.jsx';

export default function TalentCard({ p, onClick }) {
  return (
    <button className="person-card" onClick={onClick}>
      <div className="person-top">
        <UserAvatar src={p.avatar} uid={p.uid} name={p.name} verified />
        <div className="identity"><h3>{p.name}</h3><span>UID {p.uid}</span></div>
        <ChevronRight className="chev" />
      </div>
      <div className="talent-card-details">
        <Career label="主职" name={p.main} years={p.mainYears} />
        <div className="talent-experiences">
          <small>亲身经历</small>
          <div>
            {(p.experiences || []).slice(0, 3).map((experience) => <span key={experience}>{experience.replace(/^经历过/, '')}</span>)}
            {(p.experiences || []).length > 3 && <em>+{p.experiences.length - 3}</em>}
            {(p.experiences || []).length === 0 && <em>暂未填写亲身经历</em>}
          </div>
        </div>
      </div>
    </button>
  );
}

export function Career({ label, name, years }) {
  return <div><small>{label}</small><b>{name || '-'}</b><span>{name ? `${years || 0}年经验` : '暂未认证'}</span></div>;
}
