import { useEffect, useState } from 'react';
import { Search, UsersRound } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import TalentCard from '../../components/talent/TalentCard.jsx';
import './DiscoveryPages.css';
import { api } from '../../api/http.js';

function findMatter(catalog, matterId, title) {
  const allMatters = (catalog?.categories || []).flatMap((main) =>
    (main.subcategories || []).flatMap((subcategory) => subcategory.matters || []),
  );
  return (
    allMatters.find((matter) => matter.id === Number(matterId)) ||
    allMatters.find((matter) => matter.title === title) ||
    null
  );
}

function answererFromApi(item) {
  return {
    id: item.id,
    uid: item.uid,
    name: item.nickname || `UID ${item.uid}`,
    avatar: item.avatarUrl || '',
    acceptingInquiries: item.acceptingInquiries,
    main: item.mainJob || '-',
    mainYears: item.mainJobYears || 0,
    capabilityDescription: item.capabilityDescription || '',
    experiences: item.experiences.map((entry) => entry.title),
    experienceDetails: item.experiences,
  };
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
  catalog = { categories: [] },
  notify,
}) {
  const [keyword, setKeyword] = useState('');
  const [participantType, setParticipantType] = useState('PRIMARY');
  const [matterDetail, setMatterDetail] = useState(null);
  const [matchedAnswerers, setMatchedAnswerers] = useState([]);
  const selectedMatter = matterDetail || findMatter(catalog, matterId, problem);
  useEffect(() => {
    setParticipantType('PRIMARY');
  }, [matterId, experience]);

  useEffect(() => {
    if (!matterId || experience) {
      setMatterDetail(null);
      return;
    }
    let active = true;
    Promise.all([api.discoveryMatter(matterId), api.answerersByMatter(matterId)])
      .then(([detail, people]) => {
        if (!active) return;
        setMatterDetail(detail);
        setMatchedAnswerers(people.map(answererFromApi));
      })
      .catch((error) => {
        if (active) {
          setMatterDetail(null);
          notify(error.message, 'error');
        }
      });
    return () => {
      active = false;
    };
  }, [experience, matterId, notify]);
  useEffect(() => {
    if (!experience || !experienceCategoryId) return undefined;
    let active = true;
    const timer = window.setTimeout(() => {
      api
        .answerersByExperience(experienceCategoryId, experience, keyword)
        .then((people) => {
          if (active) setMatchedAnswerers(people.map(answererFromApi));
        })
        .catch((error) => {
          if (active) {
            setMatchedAnswerers([]);
            notify(error.message, 'error');
          }
        });
    }, 250);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [experience, experienceCategoryId, keyword, notify]);
  const participants = selectedMatter?.participants || [];
  const matterJobs = selectedMatter?.jobs || [];
  const participantPeople = participants
    .filter((participant) => participant.type === participantType)
    .map((participant) => ({
      participant,
      person: matchedAnswerers.find((person) => String(person.uid) === String(participant.uid)),
    }))
    .filter((item) => item.person);

  const experiencePeople = matchedAnswerers;

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
        <section className="filter-panel">
          <div className="searchbox">
            <Search size={18} />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="搜索姓名或岗位"
            />
          </div>
        </section>
        <section className="talent-section results">
          <div className="section-head simple">
            <h2>找到这些人</h2>
            <small>{experiencePeople.length}人</small>
          </div>
          <div className="talent-list">
            {experiencePeople.map((person) => (
              <TalentCard key={person.uid} p={person} onClick={() => openTalent(person)} />
            ))}
          </div>
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

      {matterJobs.length > 0 && (
        <section className="matter-participants-section">
          <div className="section-head simple">
            <h2>可能会问到</h2>
          </div>
          <div className="matter-job-summary">
            {matterJobs.map((job) => (
              <span key={job.id}>{job.name}</span>
            ))}
          </div>
        </section>
      )}

      {participants.length ? (
        <section className="matter-participants-section">
          <div className="participant-type-tabs">
            <button
              className={participantType === 'PRIMARY' ? 'active' : ''}
              onClick={() => setParticipantType('PRIMARY')}
            >
              重点问
            </button>
            <button
              className={participantType === 'SUPPORTING' ? 'active' : ''}
              onClick={() => setParticipantType('SUPPORTING')}
            >
              顺便问
            </button>
          </div>
          <div className="matter-participant-list">
            {participantPeople.map(({ person }) => (
              <TalentCard key={person.uid} p={person} onClick={() => openTalent(person)} />
            ))}
            {!participantPeople.length && <p className="discovery-empty">暂无用户</p>}
          </div>
        </section>
      ) : (
        <section className="matter-participants-unconfigured">暂无用户</section>
      )}
    </Page>
  );
}
