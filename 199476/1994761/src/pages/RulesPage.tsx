import { useEffect, type ReactNode } from "react";
import { PublicLayout } from "../layouts/PublicLayout";
import "../styles/pages/rules.css";

function RuleCard({
  id,
  title,
  children,
}: {
  id: string;
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="rule-card" id={id}>
      <h2>{title}</h2>
      {children}
    </section>
  );
}

export function RulesPage() {
  useEffect(() => {
    document.title = "服务规则｜点成";
  }, []);
  return (
    <div className="rules-page global-shell-page">
      <PublicLayout>
        <main>
          <section className="rules-hero">
            <div className="shell">
              <div className="eyebrow">Service Rules</div>
              <h1>服务规则</h1>
              <p>
                在提交想法或开通套餐前，先了解双方的权利、责任与交付边界，让每一次合作都有清晰预期。
              </p>
            </div>
          </section>
          <div className="shell rules-layout">
            <nav className="rules-nav" aria-label="服务规则目录">
              <a href="#agreement">用户协议</a>
              <a href="#privacy">隐私说明</a>
              <a href="#refund">退款规则</a>
              <a href="#packages">套餐权益</a>
              <a href="#delivery">交付标准</a>
            </nav>
            <div className="rules-content">
              <RuleCard id="agreement" title="用户协议">
                <p>
                  使用点成提交想法、购买套餐或参与合作，即表示你同意遵守本页规则。请确保提交内容真实、合法，并拥有相关文字、图片、品牌和业务材料的使用权。
                </p>
                <h3>账号与内容</h3>
                <ul>
                  <li>
                    请妥善保管账号信息，不要冒用他人身份或提交侵权、违法内容。
                  </li>
                  <li>
                    需求确认前可以补充背景；进入制作后，新增范围会单独评估。
                  </li>
                  <li>
                    恶意刷单、骚扰、攻击服务或绕过平台规则的行为可能导致服务暂停。
                  </li>
                </ul>
              </RuleCard>
              <RuleCard id="privacy" title="隐私说明">
                <p>
                  我们仅使用完成服务所需的信息，包括账号资料、提交的想法、反馈记录、套餐与余额记录。它们用于身份识别、需求评估、进度通知和交付沟通。
                </p>
                <ul>
                  <li>不会将联系方式出售给无关第三方。</li>
                  <li>公开展示案例前，会隐藏不适合公开的个人与业务信息。</li>
                  <li>请不要在反馈或想法中提交密码、银行卡号等敏感信息。</li>
                </ul>
              </RuleCard>
              <RuleCard id="refund" title="退款规则">
                <h3>套餐与项目费用</h3>
                <p>
                  套餐开通后即获得对应有效期与权益。未使用权益的退款申请会根据实际使用情况、已投入工作量和已产生费用进行核对；已完成或已交付的工作不重复退款。
                </p>
                <h3>商务合作押金</h3>
                <p>
                  ¥2,000
                  商务合作押金用于确认真实合作意向、减少无效沟通。完成沟通后，无论最终是否合作，押金都会全额退回站内余额。
                </p>
                <div className="rule-note">
                  如对订单或退回金额有疑问，可通过右下角“反馈”提交记录，处理进展会出现在个人中心的消息通知中。
                </div>
              </RuleCard>
              <RuleCard id="packages" title="套餐权益">
                <p>
                  套餐仅使用站内余额开通，不自动续费。续费会顺延有效期，并累加对应的项目与迭代额度。
                </p>
                <div className="package-table">
                  <div className="package-row">
                    <b>有效期</b>
                    <span>
                      相同套餐在有效期内续费时，从原到期日继续顺延（非自然月计算方式）。
                    </span>
                  </div>
                  <div className="package-row">
                    <b>项目额度</b>
                    <span>
                      用于套餐范围内的新项目制作；具体数量以购买页展示为准。
                    </span>
                  </div>
                  <div className="package-row">
                    <b>迭代额度</b>
                    <span>
                      用于已确认范围内的调整与优化；新的功能范围需要重新评估。
                    </span>
                  </div>
                  <div className="package-row">
                    <b>提醒与续费</b>
                    <span>
                      临近到期或额度不足时，个人中心会给出消息提醒和续费入口。
                    </span>
                  </div>
                </div>
              </RuleCard>
              <RuleCard id="delivery" title="交付标准">
                <p>
                  需求经过评估与确认后进入制作。制作进度会通过个人中心更新，交付内容以双方确认的范围为准。
                </p>
                <ul>
                  <li>交付前完成主要页面与核心流程的可用性检查。</li>
                  <li>页面适配范围、功能边界和验收方式在启动前确认。</li>
                  <li>
                    发现影响使用的问题，可通过反馈入口提交，后续回复统一进入消息通知。
                  </li>
                  <li>超出原范围的新需求会重新评估等级、时间与费用。</li>
                </ul>
              </RuleCard>
              <div className="updated">最近更新：2026 年 8 月</div>
            </div>
          </div>
        </main>
      </PublicLayout>
    </div>
  );
}
