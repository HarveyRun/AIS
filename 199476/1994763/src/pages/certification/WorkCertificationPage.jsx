import { ChevronRight } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './CertificationPages.css';

export default function WorkCertificationPage({ go, setCertType, certifications }) {
  const items = certifications.filter((item) => ['实名认证', '岗位认证'].includes(item.type));

  const openCertification = (id) => {
    setCertType(id);
    go('certBasicApply');
  };

  return (
    <Page title="基础信息" back={() => go('certs')}>
      <section className="certification-list standalone-certification-list no-leading-icon">
        {items.map((item) => (
          <button type="button" key={item.id} onClick={() => openCertification(item.id)}>
            <div>
              <h3>
                {item.title}
                <em className="required">必须</em>
              </h3>
              <p>{item.description}</p>
            </div>
            <strong className={`cert-status ${item.status}`}>{item.status}</strong>
            <ChevronRight />
          </button>
        ))}
      </section>
    </Page>
  );
}
