import { useMemo, useState } from 'react';
import { Search, UsersRound } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import TalentCard from '../../components/talent/TalentCard.jsx';
import './DiscoveryPages.css';

function findMatter(catalog, matterId, title) {
  const allMatters = (catalog?.categories || []).flatMap((main) =>
    (main.subcategories || []).flatMap((subcategory) => subcategory.matters || []),
  );
  return allMatters.find((matter) => matter.id === Number(matterId))
    || allMatters.find((matter) => matter.title === title)
    || null;
}

export default function FilterPage({
  go,
  setTalent,
  problem = '',
  matterId = null,
  experience = '',
  experienceCategoryId = null,
  title = '找人',
  backScreen = 'home',
  answerers = [],
  catalog = { categories: [] },
}) {
  const [keyword, setKeyword] = useState('');
  const [participantType, setParticipantType] = useState('ALL');
  const selectedMatter = useMemo(() => findMatter(catalog, matterId, problem), [catalog, matterId, problem]);
  const participants = selectedMatter?.participants || [];
  const normalizedKeyword = keyword.trim().toLowerCase();
  const participantPeople = participants
    .filter((participant) => participantType === 'ALL' || participant.type === participantType)
    .map((participant) => ({
      participant,
      person: answerers.find((person) => String(person.uid) === String(participant.uid)),
    }))
    .filter((item) => item.person);

  const experiencePeople = answerers.filter((person) => {
    const matchesExperience = (person.experienceDetails || []).some((item) =>
      item.discoveryCategoryId === experienceCategoryId && item.title === experience,
    );
    const matchesKeyword = !normalizedKeyword
      || `${person.name} ${person.main}`.toLowerCase().includes(normalizedKeyword);
    return matchesExperience && matchesKeyword;
  });

  const openTalent = (person) => {
    setTalent(person);
    go('talent');
  };

  if (experience) {
    return (
      <Page title={title} back={() => go(backScreen)}>
        <section className="result-title compact">
          <span>你想了解的经历</span>
          <h2>{experience}</h2>
          <p>下面的人提交过相关经历证明，可以直接找他们聊聊。</p>
        </section>
        <section className="filter-panel"><div className="searchbox"><Search size={18} /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索姓名或岗位" /></div></section>
        <section className="talent-section results">
          <div className="section-head simple"><h2>找到这些人</h2><small>{experiencePeople.length}人</small></div>
          <div className="talent-list">{experiencePeople.map((person) => <TalentCard key={person.uid} p={person} onClick={() => openTalent(person)} />)}</div>
          {!experiencePeople.length && <p className="discovery-empty">暂时没有匹配的认证用户</p>}
        </section>
      </Page>
    );
  }

  return (
    <Page title={title} back={() => go(backScreen)}>
      <section className="matter-result-hero">
        <span>你想做</span>
        <h1>{selectedMatter?.title || problem}</h1>
      </section>

      {participants.length ? (
        <section className="matter-participants-section">
          <div className="participant-type-tabs">
            <button className={participantType === 'ALL' ? 'active' : ''} onClick={() => setParticipantType('ALL')}>全部</button>
            <button className={participantType === 'PRIMARY' ? 'active' : ''} onClick={() => setParticipantType('PRIMARY')}>主要参与</button>
            <button className={participantType === 'SUPPORTING' ? 'active' : ''} onClick={() => setParticipantType('SUPPORTING')}>辅助参与</button>
          </div>
          <div className="matter-participant-list">
            {participantPeople.map(({ participant, person }) => (
              <div className="matter-participant-item" key={person.uid}>
                <div className={`participation-tag ${participant.type === 'PRIMARY' ? 'primary' : 'supporting'}`}>{participant.type === 'PRIMARY' ? '主要参与' : '辅助参与'}</div>
                <p>{person.capabilityDescription || '暂未填写介绍'}</p>
                <TalentCard p={person} onClick={() => openTalent(person)} />
              </div>
            ))}
            {!participantPeople.length && <p className="discovery-empty">暂无对应的认证用户</p>}
          </div>
        </section>
      ) : (
        <section className="matter-participants-unconfigured">
          <UsersRound />
          <h2>该事情暂未配置参与人员</h2>
        </section>
      )}
    </Page>
  );
}
