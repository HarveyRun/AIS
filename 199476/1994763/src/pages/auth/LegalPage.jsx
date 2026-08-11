import Page from '../../components/layout/Page.jsx';
import './AuthPages.css';

const content = {
  terms: {
    title: '用户协议',
    sections: [
      ['账号使用', '请使用本人手机号注册和登录，并妥善保管账号。'],
      ['交流规则', '请围绕询问内容如实交流，不得骚扰、欺骗或发布违法内容。'],
      ['费用处理', '发起询问后金额暂存于平台余额，对方未接受时原路退回余额。'],
    ],
  },
  privacy: {
    title: '隐私政策',
    sections: [
      ['必要信息', '平台仅在提供登录、认证、交流和结算功能时使用必要信息。'],
      ['认证材料', '认证材料仅用于核实身份、岗位和亲身经历，不公开原始材料。'],
      ['信息保护', '未经允许，平台不会向无关第三方公开你的手机号和认证材料。'],
    ],
  },
};

export default function LegalPage({ go, type }) {
  const pageContent = content[type];

  return (
    <Page title={pageContent.title} back={() => go('login')}>
      <section className="legal-content">
        {pageContent.sections.map(([title, description]) => (
          <article key={title}>
            <h2>{title}</h2>
            <p>{description}</p>
          </article>
        ))}
      </section>
    </Page>
  );
}
