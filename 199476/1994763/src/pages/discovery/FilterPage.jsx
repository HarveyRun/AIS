import { useEffect, useState } from 'react';
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
  experienceId = null,
  title = '找人',
  backScreen = 'home',
  catalog = { categories: [] },
  notify,
}) {
  const [matterDetail, setMatterDetail] = useState(null);
  const [matchedAnswerers, setMatchedAnswerers] = useState([]);
  const [selectedJobName, setSelectedJobName] = useState('');
  const selectedMatter = matterDetail || findMatter(catalog, matterId, problem);

  useEffect(() => {
    setSelectedJobName('');
  }, [matterId]);

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
    if (!experience || !experienceId) return undefined;
    let active = true;
    api
      .answerersByExperience(experienceId)
      .then((people) => {
        if (active) setMatchedAnswerers(people.map(answererFromApi));
      })
      .catch((error) => {
        if (active) {
          setMatchedAnswerers([]);
          notify(error.message, 'error');
        }
      });
    return () => {
      active = false;
    };
  }, [experience, experienceId, notify]);
  const participants = selectedMatter?.participants || [];
  const matterJobs = selectedMatter?.jobs || [];
  const participantPeople = participants
    .map((participant) => ({
      participant,
      person: matchedAnswerers.find((person) => String(person.uid) === String(participant.uid)),
    }))
    .filter((item) => item.person);
  const visibleParticipantPeople = selectedJobName
    ? participantPeople.filter(({ person }) => person.main === selectedJobName)
    : participantPeople;

  const experiencePeople = matchedAnswerers;

  const openTalent = (person) => {
    setTalent(person);
    go('talent');
  };

  if (experience) {
    return (
      <Page title={title} back={() => go(backScreen)}>
        <section className="result-title compact">
          <span>你想了解</span>
          <h2>{experience}</h2>
          <p>下面的人提交过相关经历证明，可以找他们聊聊。</p>
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
          {!experiencePeople.length && <p className="discovery-empty">暂无匹配用户</p>}
        </section>
      </Page>
    );
  }

  return (
    <Page title={title} back={() => go(backScreen)}>
      <section className="result-title compact">
        <span>你想做</span>
        <h2>{selectedMatter?.title || problem}</h2>
        <p>下面这些人的岗位与这件事有关，可以找他们聊聊。</p>
      </section>

      {matterJobs.length > 0 && (
        <section className="matter-participants-section mt20">
          <div className="section-head simple">
            <h2>可能会问到</h2>
          </div>
          <div className="matter-job-summary">
            {matterJobs.map((job) => (
              <button
                className={selectedJobName === job.name ? 'active' : ''}
                type="button"
                aria-pressed={selectedJobName === job.name}
                onClick={() => {
                  setSelectedJobName((current) => (current === job.name ? '' : job.name));
                }}
                key={job.id}
              >
                {job.name}
              </button>
            ))}
          </div>
        </section>
      )}

      {participants.length ? (
        <section className="matter-participants-section">
          <div className="matter-participant-list">
            {visibleParticipantPeople.map(({ person }) => (
              <TalentCard key={person.uid} p={person} onClick={() => openTalent(person)} />
            ))}
            {!visibleParticipantPeople.length && (
              <p className="discovery-empty">
                {selectedJobName ? '该岗位暂无可交流的人' : '暂无用户'}
              </p>
            )}
          </div>
        </section>
      ) : (
        <section className="matter-participants-unconfigured">暂无用户</section>
      )}
    </Page>
  );
}
