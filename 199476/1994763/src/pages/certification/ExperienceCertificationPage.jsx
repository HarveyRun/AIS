import { ChevronRight, Footprints, Plus, Store } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import { api } from '../../api/http.js';
import './CertificationPages.css';

export default function ExperienceCertificationPage({
  go,
  setCertType,
  certifications,
  notify,
}) {
  const items = certifications.filter((item) =>
    ['个人事业认证', '其它经历认证'].includes(item.type),
  );

  const openCertification = (id) => {
    setCertType(id);
    go('certExperienceApply');
  };

  const createExperience = async () => {
    try {
      const eligibility = await api.answererEligibility();
      if (!eligibility.basicInformationApproved) {
        notify('完成基础信息认证后才能添加亲身经历', 'warning');
        return;
      }
      setCertType('new-experience');
      go('certExperienceApply');
    } catch (error) {
      notify(error.message, 'error');
    }
  };

  return (
    <Page title="亲身经历" back={() => go('certs')}>
      <section className="certification-list standalone-certification-list">
        {items.map((item) => (
          <button type="button" key={item.id} onClick={() => openCertification(item.id)}>
            <i>{item.type === '个人事业认证' ? <Store /> : <Footprints />}</i>
            <div>
              <h3>{item.name || item.title}</h3>
              <p>{item.description}</p>
            </div>
            <strong className={`cert-status ${item.status}`}>{item.status}</strong>
            <ChevronRight />
          </button>
        ))}

        <button type="button" className="certification-add-row" onClick={createExperience}>
          <i>
            <Plus />
          </i>
          <div>
            <h3>添加一段亲身经历</h3>
            <p>填写一件你亲自经历过的事情</p>
          </div>
          <ChevronRight />
        </button>
      </section>
    </Page>
  );
}
