import { BadgeCheck, BriefcaseBusiness, ChevronRight, ShieldCheck, Star } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './CertificationPages.css';

const platformCertifications = [
  {
    type: '实名认证',
    title: '实名认证',
    description: '身份证正面、反面及手持身份证',
    required: true,
    status: '已认证',
  },
  {
    type: '主岗位认证',
    title: '主岗位',
    description: '软件工程师 · 11年经历',
    required: true,
    status: '已认证',
  },
  {
    type: '辅助岗位认证',
    title: '辅助岗位',
    description: '产品设计 · 6年经历',
    required: false,
    status: '已认证',
  },
  {
    type: '个人事业认证',
    title: '个人事业',
    description: '独立应用开发 · 持续4年',
    required: false,
    status: '已认证',
  },
];

export default function CertificationPage({ go, setCertType }) {
  const openCertification = (type) => {
    setCertType(type);
    go('certApply');
  };

  return (
    <Page title="我的档案" back={() => go('profile', 'profile')}>
      <section className="cert-profile-hero">
        <div className="cert-profile-icon">
          <BadgeCheck />
        </div>
        <div>
          <span>这些信息已经核实</span>
          <h1>安然的档案</h1>
          <p>实名与主岗位必须完成，其余认证可按实际情况补充。</p>
        </div>
      </section>

      <section className="certification-group">
        <header className="certification-group-title">
          <ShieldCheck />
          <div>
            <h2>身份和工作经历</h2>
            <p>让别人知道你是谁、做过什么工作</p>
          </div>
          <span>2项必需</span>
        </header>

        <div className="certification-list">
          {platformCertifications.map((certification) => (
            <button
              type="button"
              key={certification.type}
              onClick={() => openCertification(certification.type)}
            >
              <i>
                <BriefcaseBusiness />
              </i>
              <div>
                <h3>
                  {certification.title}
                  <em className={certification.required ? 'required' : 'optional'}>
                    {certification.required ? '必须' : '可选'}
                  </em>
                </h3>
                <p>{certification.description}</p>
              </div>
              <strong>{certification.status}</strong>
              <ChevronRight />
            </button>
          ))}
        </div>
      </section>

      <section className="certification-group experience-group">
        <header className="certification-group-title">
          <Star />
          <div>
            <h2>我的人生经历</h2>
            <p>核实以后，别人可以在你的档案里看到</p>
          </div>
        </header>

        <div className="verified-experiences">
          <span>
            创过业 <b>已认证</b>
          </span>
          <span>
            做过产品 <b>已认证</b>
          </span>
        </div>

        <button
          type="button"
          className="experience-entry"
          onClick={() => openCertification('其它经历认证')}
        >
          <div>
            <b>添加其它经历认证</b>
            <p>先说说这段经历，再按提示准备证明材料</p>
          </div>
          <ChevronRight />
        </button>
      </section>
    </Page>
  );
}
