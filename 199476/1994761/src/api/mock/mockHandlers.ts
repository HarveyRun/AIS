import {
  ADMIN_EMAIL,
  FEEDBACK_CATEGORIES,
  IDEA_STATUSES,
  IDEA_STATUS_TRANSITIONS,
  MASTER_INVITE,
  PACKAGES,
  type PackageId,
} from "../../domain/constants";
import type {
  ActivePackage,
  AppData,
  Feedback,
  FeedbackCategory,
  FeedbackMessage,
  Idea,
  Notification,
  PackageOrder,
  TeamStatus,
  Transaction,
  UserAccount,
} from "../../domain/types";
import { appRepository } from "../../infrastructure/appRepository";
import { sessionRepository } from "../../infrastructure/sessionRepository";
import {
  hashText,
  isSingleIdea,
  makeId,
  makeInviteCode,
} from "../../shared/lib/format";

const now = () => new Date().toISOString();

function auditOn(
  data: AppData,
  action: string,
  detail: string,
  actor: string,
  targetId?: string,
): void {
  data.auditLogs.unshift({
    id: makeId("audit"),
    action,
    detail,
    actor: actor || "系统",
    targetId: targetId || null,
    createdAt: now(),
  });
  data.auditLogs = data.auditLogs.slice(0, 500);
}

function addNotificationOn(
  data: AppData,
  email: string | null,
  specification: Omit<
    Partial<Notification>,
    "id" | "userEmail" | "read" | "createdAt"
  >,
): Notification | null {
  if (!email || !data.users[email]) return null;
  if (specification.dedupeKey) {
    const existing = data.notifications.find(
      (item) =>
        item.userEmail === email && item.dedupeKey === specification.dedupeKey,
    );
    if (existing) return existing;
  }
  const item: Notification = {
    id: makeId("notice"),
    userEmail: email,
    type: specification.type || "system",
    title: specification.title || "消息通知",
    content: specification.content || "",
    link: specification.link || "/center/notifications",
    businessId: specification.businessId || null,
    dedupeKey: specification.dedupeKey || null,
    read: false,
    createdAt: now(),
  };
  data.notifications.unshift(item);
  return item;
}

function ideaNoticeCopy(
  idea: Idea,
): Pick<Notification, "title" | "content"> | null {
  if (idea.status === "待付款")
    return {
      title: "想法等待付款",
      content: `“${idea.text}”需要支付 ¥${Number(idea.fee || 0).toFixed(2)} 后进入制作。`,
    };
  if (idea.status === "排队中")
    return {
      title: "想法已进入队列",
      content: `“${idea.text}”已经进入制作队列。`,
    };
  if (idea.status === "制作中")
    return { title: "想法正在制作", content: `“${idea.text}”已经开始制作。` };
  if (idea.status === "已完成")
    return {
      title: "想法已经完成",
      content: `“${idea.text}”已经完成，可以前往个人中心查看。`,
    };
  if (idea.status === "不制作")
    return {
      title: "想法评估完成",
      content: `“${idea.text}”本次暂不进入制作。`,
    };
  return null;
}

export const mockHandlers = {
  login(
    emailInput: string,
    password: string,
  ): { ok: true } | { ok: false; error: string } {
    const email = emailInput.trim().toLowerCase();
    if (email.length > 190 || !/^\S+@\S+\.\S+$/.test(email))
      return { ok: false, error: "请输入有效的邮箱地址。" };
    if (password.length < 8 || password.length > 72)
      return { ok: false, error: "密码长度应为 8—72 位。" };
    const data = appRepository.getSnapshot();
    const existing = data.users[email];
    if (!existing) return { ok: false, error: "账号不存在，请先注册。" };
    if (existing.passwordHash !== hashText(password))
      return { ok: false, error: "密码不正确。" };
    sessionRepository.set(email);
    return { ok: true };
  },

  register(
    emailInput: string,
    password: string,
    passwordConfirmation: string,
    inviteDigits: string,
  ): { ok: true } | { ok: false; error: string } {
    const email = emailInput.trim().toLowerCase();
    if (email.length > 190 || !/^\S+@\S+\.\S+$/.test(email))
      return { ok: false, error: "请输入有效的邮箱地址。" };
    if (password.length < 8 || password.length > 72)
      return { ok: false, error: "密码长度应为 8—72 位。" };
    if (password !== passwordConfirmation)
      return { ok: false, error: "两次输入的密码不一致。" };
    if (!/^\d{6}$/.test(inviteDigits.trim()))
      return { ok: false, error: "请输入 6 位数字邀请码。" };
    const inviteCode = `DC-${inviteDigits.trim()}`;
    const data = appRepository.getSnapshot();
    if (data.users[email])
      return { ok: false, error: "该邮箱已经注册，请直接登录。" };
    const validInvite =
      inviteCode === MASTER_INVITE ||
      Object.values(data.users).some(
        (account) => account.inviteCode === inviteCode,
      );
    if (!validInvite) return { ok: false, error: "邀请码无效。" };
    appRepository.update((draft) => {
      draft.users[email] = {
        email,
        name: "",
        passwordHash: hashText(password),
        inviteCode: makeInviteCode(email),
        usedInviteCode: inviteCode,
        createdAt: now(),
        ideas: [],
        products: [],
        teamApplication: null,
        balance: 0,
        transactions: [],
        packageOrders: [],
        activePackage: null,
        notifications: [],
      };
    });
    sessionRepository.set(email);
    return { ok: true };
  },

  logout(): void {
    sessionRepository.clear();
  },

  addIdea(
    email: string,
    type: Idea["type"],
    text: string,
    parentId: string | null,
    isPublic = true,
  ): Idea {
    return appRepository.update((data) => {
      const user = data.users[email];
      if (!user) throw new Error("账号不存在或登录已失效。");
      const minimumLength = type === "new" ? 10 : 6;
      if (!isSingleIdea(text, minimumLength))
        throw new Error("想法内容不符合提交规则。");
      if (type === "iteration") {
        const parent = user.ideas.find(
          (item) => item.id === parentId && item.type === "new",
        );
        if (!parent) throw new Error("请选择有效的原想法。");
      }
      const idea: Idea = {
        id: makeId("idea"),
        type,
        parentId,
        text: text.trim(),
        status: "待评估",
        level: null,
        fee: 0,
        paid: false,
        isPublic,
        likedBy: [],
        createdAt: now(),
      };
      data.users[email]!.ideas.unshift(idea);
      return idea;
    });
  },

  toggleIdeaVisibility(email: string, ideaId: string): void {
    appRepository.update((data) => {
      const idea = data.users[email]?.ideas.find((item) => item.id === ideaId);
      if (idea) idea.isPublic = !idea.isPublic;
    });
  },

  toggleLike(ownerEmail: string, ideaId: string, viewerEmail: string): boolean {
    return appRepository.update((data) => {
      const idea = data.users[ownerEmail]?.ideas.find(
        (item) => item.id === ideaId,
      );
      if (!idea) return false;
      const index = idea.likedBy.indexOf(viewerEmail);
      if (index >= 0) idea.likedBy.splice(index, 1);
      else idea.likedBy.push(viewerEmail);
      return index < 0;
    });
  },

  payIdea(email: string, ideaId: string): { ok: boolean; error?: string } {
    return appRepository.update((data) => {
      const user = data.users[email]!;
      const idea = user?.ideas.find((item) => item.id === ideaId);
      if (!user || !idea || idea.status !== "待付款")
        return { ok: false, error: "当前记录无需付款。" };
      if (user.balance < idea.fee) return { ok: false, error: "余额不足。" };
      user.balance = Number((user.balance - idea.fee).toFixed(2));
      idea.paid = true;
      idea.status = "排队中";
      idea.updatedAt = now();
      user.transactions.unshift({
        id: makeId("tx"),
        type: "debit",
        title: `想法费用 · ${idea.text.slice(0, 30)}`,
        amount: idea.fee,
        businessType: "IDEA",
        createdAt: now(),
      });
      return { ok: true };
    });
  },

  recharge(email: string, amount: number): void {
    if (!Number.isFinite(amount) || amount < 1 || amount > 99_999)
      throw new Error("充值金额应在 1—99999 元之间。");
    appRepository.update((data) => {
      const user = data.users[email]!;
      if (!user) throw new Error("账号不存在或登录已失效。");
      user.balance = Number((user.balance + amount).toFixed(2));
      user.transactions.unshift({
        id: makeId("tx"),
        type: "credit",
        title: "支付宝充值",
        amount,
        createdAt: now(),
      });
    });
  },

  purchasePackage(
    email: string,
    packageId: PackageId,
  ): { ok: boolean; error?: string; order?: PackageOrder } {
    return appRepository.update((data) => {
      const definition = PACKAGES.find((item) => item.id === packageId);
      const user = data.users[email];
      if (!definition || !user) return { ok: false, error: "套餐不存在。" };
      if (user.balance < definition.price)
        return {
          ok: false,
          error: `余额不足，还差 ¥${(definition.price - user.balance).toFixed(2)}。`,
        };
      const startedAt = new Date();
      const current = user.activePackage;
      const sameActive =
        current &&
        new Date(current.expiresAt).getTime() > Date.now() &&
        current.packageId === packageId;
      const base = sameActive ? new Date(current.expiresAt) : startedAt;
      const expiresAt = new Date(
        base.getTime() + definition.durationDays * 86_400_000,
      ).toISOString();
      const order: PackageOrder = {
        id: makeId("pkg"),
        packageId,
        packageName: definition.name,
        levelRange: definition.levelRange,
        amount: definition.price,
        payType: "BALANCE",
        status: "PAID",
        durationDays: definition.durationDays,
        projectQuota: definition.projectQuota,
        iterationQuota: definition.iterationQuota,
        benefits: [...definition.benefits],
        createdAt: startedAt.toISOString(),
        activatedAt: startedAt.toISOString(),
        expiresAt,
      };
      const transaction: Transaction = {
        id: makeId("tx"),
        type: "debit",
        title: `套餐开通 · ${definition.name}`,
        amount: definition.price,
        businessType: "PACKAGE",
        orderId: order.id,
        payType: "BALANCE",
        createdAt: startedAt.toISOString(),
      };
      user.balance = Number((user.balance - definition.price).toFixed(2));
      user.packageOrders.unshift(order);
      user.transactions.unshift(transaction);
      const activePackage: ActivePackage = {
        packageId,
        packageName: definition.name,
        levelRange: definition.levelRange,
        startedAt: startedAt.toISOString(),
        expiresAt,
        projectQuota:
          (sameActive ? Number(current.projectQuota) || 0 : 0) +
          definition.projectQuota,
        iterationQuota:
          (sameActive ? Number(current.iterationQuota) || 0 : 0) +
          definition.iterationQuota,
        orderId: order.id,
      };
      user.activePackage = activePackage;
      addNotificationOn(data, email, {
        type: "package",
        title: "套餐已生效",
        content: `${order.packageName}已开通，有效期至 ${new Date(order.expiresAt).toLocaleDateString("zh-CN")}。`,
        businessId: order.id,
        dedupeKey: `package-order-${order.id}`,
      });
      auditOn(
        data,
        "开通或续费套餐",
        `${order.packageName} · ¥${order.amount.toFixed(2)}`,
        email,
        order.id,
      );
      return { ok: true, order };
    });
  },

  updateProfile(email: string, name: string): void {
    if (name.trim().length > 30) throw new Error("显示名称不能超过 30 个字。");
    appRepository.update((data) => {
      data.users[email]!.name = name.trim();
    });
  },

  changePassword(
    email: string,
    current: string,
    next: string,
  ): { ok: boolean; error?: string } {
    if (
      hashText(current) !==
      appRepository.getSnapshot().users[email]?.passwordHash
    )
      return { ok: false, error: "当前密码不正确。" };
    if (next.length < 8 || next.length > 72)
      return { ok: false, error: "新密码长度应为 8—72 位。" };
    appRepository.update((data) => {
      data.users[email]!.passwordHash = hashText(next);
    });
    return { ok: true };
  },

  deleteAccount(email: string): void {
    appRepository.update((data) => {
      delete data.users[email];
    });
    sessionRepository.clear();
  },

  createFeedback(
    email: string | null,
    content: string,
    page: string,
    category: FeedbackCategory,
  ): Feedback {
    if (content.trim().length < 5 || content.trim().length > 500)
      throw new Error("反馈内容应为 5—500 个字。");
    return appRepository.update((data) => {
      const createdAt = now();
      const feedback: Feedback = {
        id: makeId("fb"),
        userEmail: email,
        page: page || "index.html",
        category: FEEDBACK_CATEGORIES.includes(category)
          ? category
          : "使用问题",
        status: "待管理员回复",
        createdAt,
        updatedAt: createdAt,
        messages: [],
      };
      feedback.messages.push({
        id: makeId("msg"),
        role: "user",
        email,
        content: content.trim(),
        createdAt,
      });
      data.feedbacks.unshift(feedback);
      addNotificationOn(data, ADMIN_EMAIL, {
        type: "feedback",
        title: "收到新的用户反馈",
        content: `${feedback.category} · ${content.trim().slice(0, 60)}`,
        businessId: feedback.id,
        dedupeKey: `feedback-new-${feedback.id}`,
      });
      auditOn(
        data,
        "提交反馈",
        `${feedback.category} · ${content.trim().slice(0, 80)}`,
        email || "游客",
        feedback.id,
      );
      return feedback;
    });
  },

  appendFeedbackMessage(
    feedbackId: string,
    role: "user" | "admin",
    email: string,
    content: string,
  ): FeedbackMessage {
    return appRepository.update((data) => {
      const feedback = data.feedbacks.find((item) => item.id === feedbackId);
      if (!feedback) throw new Error("FEEDBACK_NOT_FOUND");
      if (feedback.status === "已结束") throw new Error("FEEDBACK_CLOSED");
      if (content.trim().length < 2 || content.trim().length > 500)
        throw new Error("回复内容应为 2—500 个字。");
      if (role === "admin" && email !== ADMIN_EMAIL)
        throw new Error("FORBIDDEN");
      if (role === "user" && feedback.userEmail !== email)
        throw new Error("FORBIDDEN");
      const message: FeedbackMessage = {
        id: makeId("msg"),
        role,
        email,
        content: content.trim(),
        createdAt: now(),
      };
      feedback.messages.push(message);
      feedback.updatedAt = message.createdAt;
      feedback.status = role === "admin" ? "待用户回复" : "待管理员回复";
      if (role === "admin" && feedback.userEmail)
        addNotificationOn(data, feedback.userEmail, {
          type: "feedback",
          title: "你的反馈收到回复",
          content: message.content.slice(0, 80),
          businessId: feedback.id,
          dedupeKey: `feedback-message-${message.id}`,
        });
      if (role === "user")
        addNotificationOn(data, ADMIN_EMAIL, {
          type: "feedback",
          title: "用户追加了反馈回复",
          content: message.content.slice(0, 80),
          businessId: feedback.id,
          dedupeKey: `feedback-message-${message.id}`,
        });
      auditOn(
        data,
        role === "admin" ? "管理员回复反馈" : "用户回复反馈",
        message.content.slice(0, 100),
        email,
        feedback.id,
      );
      return message;
    });
  },

  closeFeedback(feedbackId: string, actor: string): void {
    appRepository.update((data) => {
      const feedback = data.feedbacks.find((item) => item.id === feedbackId);
      if (!feedback) return;
      feedback.status = "已结束";
      feedback.updatedAt = now();
      auditOn(
        data,
        "结束反馈会话",
        `${feedback.category} · ${feedback.messages[0]?.content.slice(0, 60) || ""}`,
        actor,
        feedback.id,
      );
      if (feedback.userEmail)
        addNotificationOn(data, feedback.userEmail, {
          type: "feedback",
          title: "反馈处理已结束",
          content: feedback.messages[0]?.content.slice(0, 80) || "",
          businessId: feedback.id,
          dedupeKey: `feedback-closed-${feedback.id}`,
        });
    });
  },

  markNotification(email: string, id: string): void {
    appRepository.update((data) => {
      const item = data.notifications.find(
        (entry) => entry.id === id && entry.userEmail === email,
      );
      if (item) item.read = true;
    });
  },

  markAllNotifications(email: string): void {
    appRepository.update((data) => {
      data.notifications.forEach((item) => {
        if (item.userEmail === email) item.read = true;
      });
    });
  },

  markBusinessNotifications(email: string, businessId: string): void {
    appRepository.update((data) => {
      data.notifications.forEach((item) => {
        if (item.userEmail === email && item.businessId === businessId)
          item.read = true;
      });
    });
  },

  ensureDerivedNotifications(email: string): void {
    appRepository.update((data) => {
      const user = data.users[email];
      if (!user) return;
      user.ideas.forEach((idea) => {
        const copy = ideaNoticeCopy(idea);
        if (copy)
          addNotificationOn(data, email, {
            type: idea.status === "待付款" ? "payment" : "idea",
            ...copy,
            businessId: idea.id,
            dedupeKey: `idea-${idea.id}-${idea.status}`,
            link: "/center/ideas",
          });
      });
      const active = user.activePackage;
      if (!active) return;
      const days = Math.ceil(
        (new Date(active.expiresAt).getTime() - Date.now()) / 86_400_000,
      );
      if (days >= 0 && days <= 7)
        addNotificationOn(data, email, {
          type: "package",
          title: "套餐即将到期",
          content: `${active.packageName}将在 ${days || "今天"}${days ? " 天后" : ""}到期，可前往套餐页面续费。`,
          businessId: active.orderId,
          dedupeKey: `package-expiry-${active.orderId}-${days}`,
          link: "/center/packages",
        });
      if (days < 0)
        addNotificationOn(data, email, {
          type: "package",
          title: "套餐已到期",
          content: `${active.packageName}已到期，可前往套餐页面重新开通。`,
          businessId: active.orderId,
          dedupeKey: `package-expired-${active.orderId}`,
          link: "/center/packages",
        });
      if (active.projectQuota <= 1 || active.iterationQuota <= 2)
        addNotificationOn(data, email, {
          type: "package",
          title: "套餐权益即将用完",
          content: `剩余 ${active.projectQuota || 0} 个项目、${active.iterationQuota || 0} 次迭代。`,
          businessId: active.orderId,
          dedupeKey: `package-quota-${active.orderId}-${active.projectQuota}-${active.iterationQuota}`,
          link: "/center/packages",
        });
    });
  },

  evaluateIdea(
    owner: string,
    ideaId: string,
    level: number,
    decision: "制作" | "不制作",
    fee: number,
  ): { ok: boolean; error?: string } {
    if (!Number.isInteger(level) || level < 1 || level > 6)
      return { ok: false, error: "评级必须是 1—6 级。" };
    if (decision === "制作" && level >= 4 && fee <= 0)
      return { ok: false, error: "4—6 级需要填写费用。" };
    return appRepository.update((data) => {
      const idea = data.users[owner]?.ideas.find((item) => item.id === ideaId);
      if (!idea) return { ok: false, error: "想法不存在。" };
      if (idea.status !== "待评估") {
        return { ok: false, error: "这条想法已经评估，不能重复评估。" };
      }
      idea.level = level;
      idea.decision = decision;
      idea.reviewedAt = now();
      if (decision === "不制作") {
        idea.fee = 0;
        idea.paid = false;
        idea.status = "不制作";
      } else if (level <= 3) {
        idea.fee = 0;
        idea.paid = true;
        idea.status = "排队中";
      } else {
        idea.fee = Number(fee.toFixed(2));
        idea.paid = false;
        idea.status = "待付款";
      }
      const copy = ideaNoticeCopy(idea);
      if (copy)
        addNotificationOn(data, owner, {
          type: idea.status === "待付款" ? "payment" : "idea",
          ...copy,
          businessId: idea.id,
          dedupeKey: `idea-${idea.id}-${idea.status}`,
          link: "/center/ideas",
        });
      auditOn(
        data,
        "更新想法状态",
        `${idea.status} · ${idea.text.slice(0, 70)}`,
        ADMIN_EMAIL,
        idea.id,
      );
      return { ok: true };
    });
  },

  updateIdeaStatus(
    owner: string,
    ideaId: string,
    status: Idea["status"],
  ): void {
    if (!IDEA_STATUSES.includes(status)) throw new Error("无效的想法状态。");
    appRepository.update((data) => {
      const idea = data.users[owner]?.ideas.find((item) => item.id === ideaId);
      if (!idea) throw new Error("没有找到这条想法。");
      if (!IDEA_STATUS_TRANSITIONS[idea.status].includes(status)) {
        throw new Error("当前状态不能执行该操作。");
      }
      idea.status = status;
      idea.updatedAt = now();
      const copy = ideaNoticeCopy(idea);
      if (copy)
        addNotificationOn(data, owner, {
          type: status === "待付款" ? "payment" : "idea",
          ...copy,
          businessId: idea.id,
          dedupeKey: `idea-${idea.id}-${status}`,
          link: "/center/ideas",
        });
      auditOn(
        data,
        "更新想法状态",
        `${status} · ${idea.text.slice(0, 70)}`,
        ADMIN_EMAIL,
        idea.id,
      );
    });
  },

  submitTeamApplication(
    email: string,
    application: UserAccount["teamApplication"],
  ): void {
    if (application) {
      if (application.skill.trim().length < 2)
        throw new Error("请填写可以负责的内容。");
      if (!application.resumeId || !application.resumeName)
        throw new Error("必须上传简历。");
      if ((application.resumeSize || 0) > 20 * 1024 * 1024)
        throw new Error("简历不能超过 20 MB。");
    }
    appRepository.update((data) => {
      data.users[email]!.teamApplication = application;
    });
  },

  updateTeamStatus(email: string, status: TeamStatus): void {
    appRepository.update((data) => {
      const application = data.users[email]?.teamApplication;
      if (!application) return;
      application.status = status;
      application.updatedAt = now();
      auditOn(data, "审核团队申请", `${email} · ${status}`, ADMIN_EMAIL, email);
    });
  },

  createDeposit(
    email: string,
    amount: number,
  ): { ok: boolean; error?: string } {
    if (!Number.isFinite(amount) || amount !== 2_000)
      return { ok: false, error: "商务合作押金固定为 ¥2000.00。" };
    return appRepository.update((data) => {
      const existing = data.cooperationDeposits.find(
        (item) =>
          item.userEmail === email &&
          !["已退回", "已取消"].includes(item.status),
      );
      if (existing) return { ok: true };
      const user = data.users[email];
      if (!user || user.balance < amount)
        return { ok: false, error: "余额不足。" };
      user.balance = Number((user.balance - amount).toFixed(2));
      user.transactions.unshift({
        id: makeId("tx"),
        type: "debit",
        title: "商务合作押金",
        amount,
        businessType: "COOPERATION_DEPOSIT",
        createdAt: now(),
      });
      const deposit = {
        id: makeId("deposit"),
        userEmail: email,
        amount,
        status: "已支付" as const,
        createdAt: now(),
        updatedAt: now(),
      };
      data.cooperationDeposits.unshift(deposit);
      auditOn(
        data,
        "支付商务合作押金",
        `¥${amount.toFixed(2)}`,
        email,
        deposit.id,
      );
      addNotificationOn(data, email, {
        type: "cooperation",
        title: "商务合作押金已支付",
        content: "合作联系人会根据提交顺序与你沟通，押金将在沟通结束后退回。",
        businessId: deposit.id,
        dedupeKey: `deposit-paid-${deposit.id}`,
      });
      return { ok: true };
    });
  },

  updateDeposit(depositId: string, status: "已联系" | "已退回"): void {
    appRepository.update((data) => {
      const deposit = data.cooperationDeposits.find(
        (item) => item.id === depositId,
      );
      if (!deposit || deposit.status === "已退回") return;
      const user = data.users[deposit.userEmail];
      if (status === "已退回" && user) {
        user.balance = Number((user.balance + deposit.amount).toFixed(2));
        user.transactions.unshift({
          id: makeId("tx"),
          type: "credit",
          title: "商务合作押金退回",
          amount: deposit.amount,
          businessType: "COOPERATION_REFUND",
          depositId: deposit.id,
          createdAt: now(),
        });
        deposit.refundedAt = now();
      }
      deposit.status = status;
      deposit.updatedAt = now();
      auditOn(
        data,
        status === "已退回" ? "退回商务合作押金" : "更新商务合作状态",
        status,
        ADMIN_EMAIL,
        deposit.id,
      );
      addNotificationOn(data, deposit.userEmail, {
        type: "cooperation",
        title: status === "已退回" ? "商务合作押金已退回" : "商务合作进度更新",
        content:
          status === "已退回"
            ? `¥${deposit.amount.toFixed(2)} 已退回账户余额。`
            : `当前状态：${status}`,
        businessId: deposit.id,
        dedupeKey: `deposit-${deposit.id}-${status}`,
      });
    });
  },
};
