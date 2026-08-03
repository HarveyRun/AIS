import { Link } from "@tanstack/react-router";
import { useEffect, useMemo, useRef, useState } from "react";
import { appService } from "../application/appService";
import { useAppNavigate } from "../application/navigation";
import { useAppStore } from "../application/store";
import { pendingActionStorage } from "../infrastructure/storage/pendingActionStorage";
import { PublicLayout } from "../layouts/PublicLayout";
import { isSingleIdea } from "../shared/lib/format";
import { useAsyncAction } from "../shared/lib/useAsyncAction";
import { useToast } from "../shared/ui/Toast";
import { Icon, type IconName } from "../shared/ui/Icon";
import "../styles/pages/home.css";

const deliverables = [
  ["01", "功能设计", "free", "layout"],
  ["02", "界面设计", "free", "palette"],
  ["03", "代码开发", "free", "code"],
  ["04", "免费域名", "free", "globe"],
  ["05", "免费提供空间与托管服务", "free", "server"],
  ["06", "专属定制", "business", "sparkles"],
] as const;
const rules = [
  ["RULE 01", "只接新的", "我们只期待从0到1的全新想法。", "wide highlight"],
  [
    "RULE 02",
    "一次只说一件事",
    "一句话说明要做什么，不把另一件事放进来。后续可以继续补充，但要始终围绕原来的想法。",
    "narrow",
  ],
  [
    "RULE 03",
    "提交不等于制作",
    "提交后我们会先评估。做不做、什么时候做，由我们决定。",
    "half",
  ],
  ["RULE 04", "不必参与制作", "不需要您参与，我们来实现。", "half"],
  [
    "RULE 05",
    "迭代可以继续提",
    "同一个想法可以持续提出更新。每次提交都会单独记录，也会单独决定是否处理。",
    "narrow",
  ],
  [
    "RULE 06",
    "订阅到期",
    "月订阅到期，若项目已完成交付，您将无法继续访问。",
    "wide",
  ],
] as const;

interface HomeSearch {
  mode?: "iteration";
  resume?: "idea";
  parent?: string;
}

function IdeaForm({ search }: { search: HomeSearch }) {
  const { user, sessionEmail } = useAppStore();
  const notify = useToast();
  const navigate = useAppNavigate();
  const [mode, setMode] = useState<"new" | "iteration">(
    search.mode === "iteration" ? "iteration" : "new",
  );
  const [text, setText] = useState("");
  const [parentId, setParentId] = useState(search.parent || "");
  const [isPublic, setPublic] = useState(true);
  const { isPending, run } = useAsyncAction();
  const validation = useMemo(
    () => ({
      clear: text.trim().length >= 10,
      single:
        !/(并且|同时|以及|另外|还要|顺便)/.test(text) &&
        !/[。；;！!？?]/.test(text.trim().slice(0, -1)),
    }),
    [text],
  );
  // const valid = isSingleIdea(text, 10) && (mode === "new" || Boolean(parentId));
  const status = !text
    ? mode === "new"
      ? "小站马上安排~"
      : "只写这次要更新的内容，不要偏离原来的想法。" : "";

  useEffect(() => {
    if (!user || search.resume !== "idea") return;
    const action = pendingActionStorage.read();
    if (action?.type === "idea" && action.text) {
      void (async () => {
        await appService.addIdea(
          sessionEmail,
          action.mode,
          action.text,
          action.parentId,
          action.isPublic,
        );
        pendingActionStorage.clear();
        await navigate("/center/ideas", { replace: true });
      })();
    }
  }, [navigate, search.resume, sessionEmail, user]);

  const roots = user?.ideas.filter((idea) => idea.type === "new") || [];
  return (
    <form
      className="idea-card"
      onSubmit={async (event) => {
        event.preventDefault();
        if (!user) {
          pendingActionStorage.save({
            type: "idea",
            mode,
            text: text.trim(),
            parentId: parentId || null,
            isPublic: mode === "new" ? isPublic : false,
          });
          void navigate("/login", { redirect: "/?resume=idea" });
          return;
        }
        await run(() =>
          appService.addIdea(
            sessionEmail,
            mode,
            text,
            parentId || null,
            mode === "new" ? isPublic : false,
          ),
        );
        notify(
          mode === "new"
            ? "已提交，当前状态：待评估。"
            : "迭代已记录，当前状态：待评估。",
        );
        void navigate("/center/ideas");
      }}
    >
      <div className="idea-top">
        <Link className="idea-label" to="/cases">
          案例列表{" "}
          <Icon name="arrow-up-right" />
        </Link>
        <div className="mode" role="tablist" aria-label="提交类型">
          <button
            className={mode === "new" ? "active" : ""}
            type="button"
            role="tab"
            aria-selected={mode === "new"}
            onClick={() => {
              setMode("new");
              setParentId("");
            }}
          >
            新想法
          </button>
          <button
            className={mode === "iteration" ? "active" : ""}
            type="button"
            role="tab"
            aria-selected={mode === "iteration"}
            onClick={() => setMode("iteration")}
          >
            我要迭代
          </button>
        </div>
      </div>
      {mode === "iteration" && (
        <div className="iteration-context show" aria-hidden="false">
          {!user ? (
            <button
              className="iteration-select"
              type="button"
              onClick={() => {
                void navigate("/login", { redirect: "/?mode=iteration" });
              }}
            >
              <span>
                <b>选择原内容</b>
                <small>登录后从已提交内容中选择</small>
              </span>
              <span className="arrow"><Icon name="arrow-up-right" /></span>
            </button>
          ) : roots.length ? (
            <select
              className="iteration-select"
              aria-label="选择原内容"
              value={parentId}
              onChange={(event) => setParentId(event.target.value)}
            >
              <option value="">选择要继续的原内容</option>
              {roots.map((idea) => (
                <option key={idea.id} value={idea.id}>
                  {idea.text}
                </option>
              ))}
            </select>
          ) : (
            <button
              className="iteration-select"
              type="button"
              onClick={() => setMode("new")}
            >
              <span>
                <b>还没有可迭代的内容</b>
                <small>请先提出一个新想法</small>
              </span>
              <span className="arrow"><Icon name="plus" /></span>
            </button>
          )}
        </div>
      )}
      <div className="input-wrap">
        <label htmlFor="idea">
          {mode === "iteration"
            ? "用一句话说明这次要更新什么"
            : "用一句话说明要做什么"}
        </label>
        <textarea
          className="idea-input"
          id="idea"
          maxLength={80}
          placeholder={
            mode === "iteration"
              ? "例如：让清单可以按用餐人数调整"
              : "例如：我想要一个个人专属相册网站"
          }
          value={text}
          onChange={(event) => setText(event.target.value)}
        />
        <div className="idea-meta">
          <span
            className={`idea-status ${text ? "warn" : ""}`}
            aria-live="polite"
          >
            {status}
          </span>
          <span>
            <b>{text.length}</b>/80
          </span>
        </div>
      </div>
      {mode === "new" && (
        <label className="visibility-row">
          <input
            type="checkbox"
            checked={isPublic}
            onChange={(event) => setPublic(event.target.checked)}
          />
          <span>
            <b>公开到“用户想法”</b>公开后，其他登录用户可以看到并点赞。
          </span>
        </label>
      )}
      <div className="idea-foot">
        <div className="three-rules" aria-label="提交检查">
          <span className={validation.clear ? "passed" : ""}>
            <i><Icon name="check" /></i>表达清楚
          </span>
          <span className={validation.clear ? "passed" : ""}>
            <i><Icon name="check" /></i>一次一件事
          </span>
          <span className={mode === "new" || parentId ? "passed" : ""}>
            <i><Icon name="check" /></i>
            {mode === "new" ? "全新想法" : "已选原内容"}
          </span>
        </div>
        <button className="button primary" type="submit" disabled={isPending}>
          {isPending
            ? "提交中…"
            : mode === "iteration"
              ? "提交迭代"
              : "提交想法"}{" "}
          <span className="arrow"><Icon name="arrow-up-right" /></span>
        </button>
      </div>
    </form>
  );
}

function Pricing() {
  const { user } = useAppStore();
  const navigate = useAppNavigate();
  const openPackages = () => {
    void navigate(
      user ? "/center/packages" : "/login",
      user ? undefined : { redirect: "/center/packages" },
    );
  };
  return (
    <section className="section" id="price">
      <div className="shell">
        <div className="price-header reveal">
          <div>
            <div className="eyebrow">费用</div>
            <h2>评级与套餐</h2>
          </div>
          <p className="section-intro">提交后由我们评级。</p>
        </div>
        <div className="price-shell reveal">
          <div className="price-grid">
            <article className="price-card">
              <span className="price-range">LEVEL 1—3</span>
              <h3>标准套餐</h3>
              <div className="big">25元/月</div>
              {[
                "每月2个微型项目",
                "每月4次迭代调整",
                "支持完整源码交付",
                "不支持模块概述梳理、细节定制",
                "不支持对接第三方与需求方",
                "最终成品交付标准由平台审核确认",
              ].map((item, index) => (
                <div
                  className="cir-warp-open"
                  style={index === 0 ? { marginTop: 60 } : undefined}
                  key={item}
                >
                  <span className="cir-open"><Icon name="check" /></span>
                  {item}
                </div>
              ))}
              <button
                className="button"
                style={{ marginTop: 20 }}
                type="button"
                onClick={openPackages}
              >
                立即开通
              </button>
            </article>
            <article className="price-card featured">
              <span className="price-range">LEVEL 4—6</span>
              <h3>升级套餐</h3>
              <div className="big">99元/月</div>
              {[
                "每月2个小型项目",
                "每月15次迭代调整",
                "支持完整源码交付",
                "仅支持模块概述梳理，不支持细节定制",
                "不支持对接第三方与需求方",
                "最终成品交付标准由平台审核确认",
              ].map((item, index) => (
                <div
                  className="cir-warp-open"
                  style={index === 0 ? { marginTop: 60 } : undefined}
                  key={item}
                >
                  <span className="cirs-open"><Icon name="check" /></span>
                  {item}
                </div>
              ))}
              <button
                className="button primary"
                style={{ marginTop: 20 }}
                type="button"
                onClick={openPackages}
              >
                立即开通
              </button>
            </article>
          </div>
          <p className="price-note">
            开通后，可在个人中心查看套餐有效期和剩余权益。
          </p>
        </div>
      </div>
    </section>
  );
}

export function HomePage({ search }: { search: HomeSearch }) {
  const rootRef = useRef<HTMLDivElement>(null);
  const [scrollProgress, setScrollProgress] = useState(0);
  useEffect(() => {
    document.title = "点成｜助力小想法建站";
    const root = rootRef.current;
    const elements = root
      ? [...root.querySelectorAll<HTMLElement>(".reveal")]
      : [];
    const observer = new IntersectionObserver(
      (entries) =>
        entries.forEach(
          (entry) =>
            entry.isIntersecting && entry.target.classList.add("visible"),
        ),
      { threshold: 0.12 },
    );
    elements.forEach((element) => observer.observe(element));
    const updateScroll = () => {
      const maximum =
        document.documentElement.scrollHeight - window.innerHeight;
      setScrollProgress(
        maximum > 0 ? Math.min(100, (window.scrollY / maximum) * 100) : 0,
      );
    };
    updateScroll();
    window.addEventListener("scroll", updateScroll, { passive: true });
    return () => {
      observer.disconnect();
      window.removeEventListener("scroll", updateScroll);
    };
  }, []);
  return (
    <div className="home-page global-shell-page" ref={rootRef}>
      <div
        className="scroll-progress"
        style={{ width: `${scrollProgress}%` }}
        aria-hidden="true"
      />
      <a className="skip" href="#main">
        跳到正文
      </a>
      <PublicLayout active="home">
        <main id="main">
          <section className="hero" id="top">
            <div className="aurora" />
            <div className="shell hero-inner">
              <div className="availability">
                <span className="live-dot" />
                接受网站制作中
              </div>
              <h1>
                <span className="gradient-text">
                  一句话说出想要的
                  <br />
                  我们来实现
                </span>
              </h1>
              <div className="idea-stage reveal" id="submit">
                <IdeaForm search={search} />
              </div>
            </div>
            <div className="trust-strip">
              <div className="shell trust-inner">
                <div className="trust-title">先了解三件事</div>
                <div className="trust-item">
                  <b>提交不等于制作</b>我们会先评估
                </div>
                <div className="trust-item">
                  <b>不需要您参与</b>我们全权实现
                </div>
                <div className="trust-item">
                  <b>站内问题</b>以留言为主要方式
                </div>
              </div>
            </div>
          </section>
          <section className="section" id="how">
            <div className="shell process-layout">
              <div className="sticky-copy reveal">
                <div className="eyebrow">流程</div>
                <h2>只需四步</h2>
                <p className="section-intro">想法无分大小，排期不论高低。</p>
              </div>
              <div className="process-list reveal">
                {[
                  ["01", "提交", "提交新想法或迭代。"],
                  ["02", "评估", "进行等级评估。"],
                  [
                    "03",
                    "决定",
                    "做不做、什么时候做、何时做完，我们一揽子提供。",
                  ],
                  ["04", "查看", "在个人中心查看结果与进度。"],
                ].map(([number, title, copy]) => (
                  <article className="process-item" key={number}>
                    <span className="process-no">{number}</span>
                    <div>
                      <h3>{title}</h3>
                      <p>
                        {copy}
                        {number === "02" && (
                          <>
                            <br />
                            <br />
                            微型：1-3级
                            <br />
                            小型：4-6级
                            <br />
                            中|大型：商务合作
                          </>
                        )}
                      </p>
                    </div>
                  </article>
                ))}
              </div>
            </div>
          </section>
          <section className="deliverables-section" id="why-us">
            <div className="shell">
              <div
                className="deliverables-card reveal"
                style={{ paddingBottom: 30 }}
              >
                <div className="deliverables-head">
                  <div>
                    <div className="eyebrow">一站式全包服务</div>
                    <h2>我们会帮您做什么事</h2>
                  </div>
                  <p>价格是很低的，服务是大胆的。</p>
                </div>
                <div className="deliverables-grid">
                  {deliverables.map(([number, title, kind, icon]) => (
                    <article
                      className="deliverable"
                      data-no={number}
                      key={number}
                    >
                      <span
                        className={
                          kind === "free" ? "free-badge" : "support-badge"
                        }
                      >
                        {kind === "free" ? "免费" : "商务合作"}
                      </span>
                      <span className="deliverable-icon"><Icon name={icon as IconName} /></span>
                      <h3>{title}</h3>
                    </article>
                  ))}
                </div>
                <p
                  className="gradient-text"
                  style={{ fontSize: 18, marginTop: 40, fontWeight: "bold" }}
                >
                  AI
                  的价值是赋能专业人才，使其发挥十倍效能；但无法消除行业专业门槛
                </p>
              </div>
            </div>
          </section>
          <section className="scope-section" id="scope">
            <div className="shell">
              <div className="scope-head reveal">
                <div>
                  <div className="eyebrow">两类内容</div>
                  <h2>提交范围</h2>
                </div>
                <p className="section-intro">按提交时间先后，依次排期。</p>
              </div>
              <div className="scope-grid reveal">
                <article className="scope-card accept">
                  <div className="scope-icon"><Icon name="lightbulb" /></div>
                  <h3>新想法</h3>
                  <div className="scope-list">
                    <div className="scope-row"><Icon name="check" />从0开始</div>
                    <div className="scope-row"><Icon name="check" />一次只说一件事</div>
                    <div className="scope-row"><Icon name="check" />我们全权实现</div>
                  </div>
                </article>
                <article className="scope-card">
                  <div className="scope-icon"><Icon name="refresh" /></div>
                  <h3>后续迭代</h3>
                  <div className="scope-list">
                    <div className="scope-row"><Icon name="check" />只针对已提交的想法</div>
                    <div className="scope-row"><Icon name="check" />在个人中心管理</div>
                    <div className="scope-row"><Icon name="check" />继续原来的方向</div>
                  </div>
                </article>
              </div>
            </div>
          </section>
          <section className="rules-section" id="rules">
            <div className="shell">
              <div className="eyebrow">规则</div>
              <h2>六条规则</h2>
              <p className="section-intro">提交前请先了解这些安排。</p>
              <div className="rules-grid reveal">
                {rules.map(([id, title, copy, classes]) => (
                  <article className={`rule-card ${classes}`} key={id}>
                    <span className="rule-id">{id}</span>
                    <h3>{title}</h3>
                    <p>{copy}</p>
                  </article>
                ))}
                <article className="rule-card full">
                  <span className="rule-id">RULE 07</span>
                  <span className="support-badge">等级标准</span>
                  <h3>服务标准</h3>
                  <p>
                    微型项目：采用标准化交付模式。不支持模块概述梳理、不支持细节定制，亦不支持对接第三方与需求方
                    <br />
                    小型项目：提供基础框架服务。仅支持模块概述梳理，不支持细节定制，亦不支持对接第三方与需求方。
                    <br />
                    中|大型项目：提供深度定制服务及专属需求，请通过商务合作渠道与我们沟通。
                  </p>
                </article>
              </div>
            </div>
          </section>
          <Pricing />
          <div className="more-closing-zone">
            <section className="section" id="more">
              <div className="shell">
                <div className="eyebrow">个人中心</div>
                <h2>更多可能</h2>
                <div className="more-grid reveal">
                  <article className="more-card submit-card">
                    <div className="more-icon"><Icon name="user-plus" /></div>
                    <h3>加入我们</h3>
                    <p>加入我们指成为平台的长期合作者。</p>
                    <Link
                      className="text-link"
                      to="/center/$tab"
                      params={{ tab: "team" }}
                    >
                      加入我们 <Icon name="arrow-up" />
                    </Link>
                  </article>
                  <article className="more-card dark">
                    <div className="more-icon"><Icon name="sparkles" /></div>
                    <h3>商务合作</h3>
                    <p>提供深度定制服务及专属需求。</p>
                    <Link
                      className="text-link"
                      to="/center/$tab"
                      params={{ tab: "ideas" }}
                    >
                      立即前往 <Icon name="arrow-up-right" />
                    </Link>
                  </article>
                </div>
              </div>
            </section>
          </div>
          <section className="faq-section" id="faq">
            <div className="shell faq-layout">
              <div className="faq-heading reveal">
                <div className="eyebrow">FAQ</div>
                <h2>常见问题</h2>
                <p>遇到任何问题，先看这里</p>
              </div>
              <div className="faq-list reveal">
                <details name="faq" open>
                  <summary>
                    您需要配合什么？<span><Icon name="plus" /></span>
                  </summary>
                  <p>无需任何配合，打开平台交付时的访问地址即可直接使用</p>
                </details>
                <details name="faq">
                  <summary>
                    如何确保“所想即所得”？<span><Icon name="plus" /></span>
                  </summary>
                  <p>与我们进行商务合作</p>
                </details>
                <details name="faq">
                  <summary>
                    关于交付时间？<span><Icon name="plus" /></span>
                  </summary>
                  <p>
                    1、微型项目平均 2~7 个工作日
                    <br />
                    2、小型项目平均 5~14 个工作日
                    <br />
                    3、功能迭代平均 1~10 个工作日
                  </p>
                </details>
                <details name="faq">
                  <summary>
                    关于订阅费用？<span><Icon name="plus" /></span>
                  </summary>
                  <p>
                    1、平台不会自动扣除用户任何费用，如需续费，请自行手动操作。
                    <br />
                    2、若订阅到期，项目推进、迭代支持及相关访问权限将同步终止。
                    <br />
                    3、提交想法或迭代后，平台将于24小时内反馈具体排期，请据此合理安排计划。
                  </p>
                </details>
                <details name="faq">
                  <summary>
                    支持申请退款吗？<span><Icon name="plus" /></span>
                  </summary>
                  <p>无特殊原因，暂不支持申请退款。</p>
                </details>
                <details name="faq">
                  <summary>
                    关于邀请码？<span><Icon name="plus" /></span>
                  </summary>
                  <p>
                    1、平台非接单平台，所以价格是很低的，服务是大胆的。
                    <br />
                    2、邀请机制用于抵御恶意行为，违规账号及其邀请人将受连带处罚并永久封号。
                    <br />
                    3、无论您最终是否选择我们，我们都充分尊重您的决定。
                    <br />
                    4、平台未来，不排除开放对邀请者的分成激励。
                  </p>
                </details>
                <details name="faq">
                  <summary>
                    关于获取源码？<span><Icon name="plus" /></span>
                  </summary>
                  <p>
                    1、用户可通过平台的个人中心随时下载源码，每180天可获取一次
                    <br />
                    2、源码包含完整的前端页面、后端服务及数据库数据（实际交付内容，均以您下载时的版本为准）
                    <br />
                    3、提供源码下载仅为平台的一项增值权益，不构成其他任何形式的服务承诺
                  </p>
                </details>
                <details name="faq">
                  <summary>
                    关于定制化服务？<span><Icon name="plus" /></span>
                  </summary>
                  <p>
                    除商务合作外，仅提供平台内的标准化流程，暂不提供任何形式的定制或特殊要求。
                  </p>
                </details>
                <details name="faq">
                  <summary>
                    支持与帮助<span><Icon name="plus" /></span>
                  </summary>
                  <p>
                    1、目前暂不提供在线客服及电话热线服务，您可通过【留言】功能提交建议或问题，敬请谅解
                    <br />
                    2、如需商务合作或特殊事务，请前往个人中心通过邮箱与我们联系，我们将及时予以回复
                  </p>
                </details>
              </div>
            </div>
          </section>
        </main>
      </PublicLayout>
    </div>
  );
}
