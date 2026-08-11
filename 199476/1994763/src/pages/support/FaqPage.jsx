import { useState } from 'react';
import { ChevronDown } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './FaqPage.css';

const questions = [
  {
    title: '提现何时能到账？',
    answer: '提现申请提交后，预计1～3个工作日到账。\n到账时间以银行实际处理进度为准。',
  },
  {
    title: '违规处理方式？',
    answer:
      '违规共分为6个等级：\n0级：1次\n1级：3次\n2级：5次\n3级：10次\n4级：50次\n5级：100次\n\n对应次数扣完后，账号将被永久封禁。',
  },
];

export default function FaqPage({ go }) {
  const [opened, setOpened] = useState(0);

  return (
    <Page title="常见问题" back={() => go('profile')} className="faq-page">
      <section className="faq-list">
        {questions.map((item, index) => {
          const isOpen = opened === index;

          return (
            <article className={isOpen ? 'open' : ''} key={item.title}>
              <button type="button" onClick={() => setOpened(isOpen ? -1 : index)}>
                <b>{item.title}</b>
                <ChevronDown />
              </button>
              {isOpen && (
                <div className="faq-answer">
                  <p>{item.answer}</p>
                </div>
              )}
            </article>
          );
        })}
      </section>
    </Page>
  );
}
