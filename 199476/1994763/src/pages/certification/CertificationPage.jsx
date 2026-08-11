import {
  BriefcaseBusiness,
  ChevronRight,
  Footprints,
  MessageCircleQuestion,
  MessagesSquare,
} from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './CertificationPages.css';

export default function CertificationPage({
  go,
  certifications,
  acceptingInquiries,
  setAcceptingInquiries,
}) {
  const workItems = certifications.filter((item) => ['实名认证', '岗位认证'].includes(item.type));
  const hasJoined = workItems
    .filter((item) => item.required)
    .every((item) => item.status === '已认证');

  return (
    <Page title={hasJoined ? '答主信息' : '成为答主'} back={() => go('profile', 'profile')}>
      <section className="cert-profile-hero">
        <div className="cert-profile-icon">
          <MessageCircleQuestion />
        </div>
        <div>
          <h1>{hasJoined ? '你已成为答主' : '成为答主'}</h1>
          <p>
            {hasJoined
              ? '你可以在这里查看自己的认证信息。'
              : '完成基础信息认证后，即可回答他人的询问。'}
          </p>
        </div>
      </section>

      <section className={`answer-availability ${acceptingInquiries ? 'active' : 'paused'}`}>
        <i>
          <MessagesSquare />
        </i>
        <div>
          <h2>接受新询问</h2>
          <p>
            {!hasJoined
              ? '完成基础信息认证后可以开启'
              : acceptingInquiries
                ? '其他人可以向你发起询问'
                : '暂停后不会收到新的询问'}
          </p>
        </div>
        <button
          type="button"
          className={acceptingInquiries ? 'on' : ''}
          disabled={!hasJoined}
          onClick={() => setAcceptingInquiries((current) => !current)}
          aria-label={acceptingInquiries ? '暂停接受询问' : '开始接受询问'}
        >
          <span />
        </button>
      </section>

      <section className="certification-entry-list">
        <button type="button" onClick={() => go('certWork')}>
          <i className="work">
            <BriefcaseBusiness />
          </i>
          <div>
            <h2>基础信息</h2>
            <p>身份信息和我的岗位</p>
          </div>
          <ChevronRight />
        </button>

        <button type="button" onClick={() => go('certExperience')}>
          <i className="experience">
            <Footprints />
          </i>
          <div>
            <h2>亲身经历</h2>
            <p>亲自经历过的事情</p>
          </div>
          <ChevronRight />
        </button>
      </section>
    </Page>
  );
}
