import { ADMIN_EMAIL, MASTER_INVITE } from "../../domain/constants";
import type { AppData, FeedbackMessage, UserAccount } from "../../domain/types";
import { hashText, makeInviteCode } from "../../shared/lib/format";
import { appRepository } from "../../infrastructure/appRepository";
import { sessionRepository } from "../../infrastructure/sessionRepository";

const SEED_VERSION = "demo-2026-08-v3";
const ago = (hours: number) =>
  new Date(Date.now() - hours * 3_600_000).toISOString();
const later = (days: number) =>
  new Date(Date.now() + days * 86_400_000).toISOString();
const has = <T extends { id: string }>(rows: T[], id: string) =>
  rows.some((item) => item.id === id);

const names = [
  "林一",
  "周舟",
  "陈默",
  "小满",
  "阿禾",
  "木子",
  "南星",
  "言午",
  "可乐",
  "秋池",
  "江野",
  "白露",
  "知夏",
  "小川",
  "安然",
  "晴川",
  "许愿",
  "听雨",
  "山止",
  "向晚",
  "新禾",
  "拾光",
  "青屿",
  "星野",
];
const ideaCopies = [
  "做一个每天只推荐一道家常菜的网站",
  "给独立摄影师做一个预约作品集",
  "做一个能记录植物浇水时间的小工具",
  "为社区活动做一个报名与签到页面",
  "做一个适合长辈使用的用药提醒页",
  "为宠物店做一个洗护预约小站",
  "做一个会议结论自动归档的看板",
  "为手作店设计一个新品展示网站",
  "做一个旅行照片自动生成路线图的页面",
  "为读书会做一个书单投票工具",
  "做一个家庭共享购物清单",
  "为咖啡店做一个今日豆单页面",
  "做一个自由职业者报价单生成器",
  "为公益活动做一个物资认领页面",
  "做一个课程作业提交进度看板",
  "为民宿做一个周边路线推荐页",
  "做一个跑步训练打卡页面",
  "为工作室做一个客户排期看板",
  "做一个每日情绪记录小站",
  "为乐队做一个演出信息聚合页",
  "做一个宠物成长相册",
  "为展览做一个语音导览页面",
  "做一个团队午餐随机选择器",
  "为婚礼做一个宾客信息页面",
];
const statuses = [
  "待评估",
  "待付款",
  "排队中",
  "制作中",
  "已完成",
  "不制作",
] as const;

function ensureUser(data: AppData, email: string, name: string): UserAccount {
  const existing = data.users[email];
  if (existing) {
    existing.name = existing.name || name;
    if (!existing.passwordHash) existing.passwordHash = hashText("12345678");
    return existing;
  }
  const user: UserAccount = {
    email,
    name,
    passwordHash: hashText("12345678"),
    inviteCode: makeInviteCode(email),
    usedInviteCode: MASTER_INVITE,
    createdAt: ago(900),
    ideas: [],
    products: [],
    teamApplication: null,
    balance: 3_688,
    transactions: [],
    packageOrders: [],
    activePackage: null,
    notifications: [],
  };
  data.users[email] = user;
  return user;
}

function seedCurrentUser(data: AppData, email: string): void {
  const user = data.users[email];
  if (!email.endsWith("@diancheng.test") && email !== ADMIN_EMAIL) return;
  if (!user || user._demoSeedVersion === SEED_VERSION) return;
  user.balance = Math.max(Number(user.balance) || 0, 5_688);
  for (let index = 1; index <= 20; index += 1) {
    const number = String(index).padStart(2, "0");
    const ideaId = `personal-demo-idea-${number}`;
    const orderId = `personal-demo-order-${number}`;
    const transactionId = `personal-demo-tx-${number}`;
    const feedbackId = `personal-demo-feedback-${number}`;
    if (!has(user.ideas, ideaId))
      user.ideas.push({
        id: ideaId,
        type: "new",
        parentId: null,
        text: `我的示例想法 ${number}：${ideaCopies[(index - 1) % ideaCopies.length]}`,
        status: statuses[(index - 1) % statuses.length]!,
        level: (index % 6) + 1,
        fee: index % 3 === 0 ? 25 : 5,
        paid: index % 3 > 1,
        isPublic: index % 2 === 0,
        likedBy: [],
        createdAt: ago(260 - index * 4),
        updatedAt: ago(80 - index),
      });
    if (!has(user.packageOrders, orderId))
      user.packageOrders.push({
        id: orderId,
        packageId: index % 2 ? "standard" : "upgrade",
        packageName: index % 2 ? "标准套餐" : "升级套餐",
        levelRange: index % 2 ? "LEVEL 1—3" : "LEVEL 4—6",
        amount: index % 2 ? 25 : 99,
        payType: "BALANCE",
        status: "PAID",
        durationDays: 30,
        projectQuota: 2,
        iterationQuota: index % 2 ? 4 : 15,
        createdAt: ago(250 - index * 5),
        activatedAt: ago(250 - index * 5),
        expiresAt: ago(250 - index * 5 - 720),
      });
    if (!has(user.transactions, transactionId))
      user.transactions.push({
        id: transactionId,
        type: index % 4 === 0 ? "credit" : "debit",
        title:
          index % 4 === 0
            ? "支付宝充值"
            : index % 2
              ? "标准套餐开通"
              : "升级套餐开通",
        amount: index % 4 === 0 ? 300 : index % 2 ? 25 : 99,
        businessType: index % 4 === 0 ? "RECHARGE" : "PACKAGE",
        createdAt: ago(250 - index * 5),
      });
    if (!has(data.feedbacks, feedbackId)) {
      const createdAt = ago(230 - index * 4);
      const messages: FeedbackMessage[] = [
        {
          id: `personal-demo-msg-${number}-1`,
          role: "user",
          email,
          content: `示例反馈 ${number}：希望这里的消息状态和处理进度更清楚。`,
          createdAt,
        },
      ];
      if (index % 3 !== 0)
        messages.push({
          id: `personal-demo-msg-${number}-2`,
          role: "admin",
          email: ADMIN_EMAIL,
          content: "已经记录。你可以继续回复，新的进展也会保留在当前会话中。",
          createdAt: ago(220 - index * 4),
        });
      data.feedbacks.push({
        id: feedbackId,
        userEmail: email,
        page: "center.html",
        category: ["功能问题", "使用问题", "建议"][index % 3] as
          "功能问题" | "使用问题" | "建议",
        status:
          messages.at(-1)?.role === "admin" ? "待用户回复" : "待管理员回复",
        createdAt,
        updatedAt: messages.at(-1)!.createdAt,
        messages,
      });
    }
    const noticeId = `personal-demo-notice-${number}`;
    if (!has(data.notifications, noticeId))
      data.notifications.push({
        id: noticeId,
        userEmail: email,
        type: ["idea", "payment", "package", "cooperation"][index % 4] as
          "idea" | "payment" | "package" | "cooperation",
        title: [
          "想法进度已更新",
          "有一笔费用等待处理",
          "套餐权益提醒",
          "商务合作进度更新",
        ][index % 4]!,
        content: `这是第 ${number} 条示例业务消息，可用于测试筛选、已读状态和跳转。`,
        link:
          index % 4 === 1
            ? "/center/ideas"
            : index % 4 === 2
              ? "/center/packages"
              : "/center/notifications",
        businessId: `personal-demo-business-${number}`,
        dedupeKey: noticeId,
        read: index % 5 === 0,
        createdAt: ago(210 - index * 3),
      });
    const replyNoticeId = `personal-demo-reply-notice-${number}`;
    if (!has(data.notifications, replyNoticeId) && index % 3 !== 0)
      data.notifications.push({
        id: replyNoticeId,
        userEmail: email,
        type: "feedback",
        title: "你的反馈收到回复",
        content: "管理员已经回复了这条示例反馈。",
        link: "/center/notifications",
        businessId: feedbackId,
        dedupeKey: replyNoticeId,
        read: index % 4 === 0,
        createdAt: ago(200 - index * 3),
      });
  }
  if (!user.activePackage)
    user.activePackage = {
      packageId: "standard",
      packageName: "标准套餐",
      levelRange: "LEVEL 1—3",
      startedAt: ago(576),
      expiresAt: later(6),
      projectQuota: 1,
      iterationQuota: 2,
      orderId: "personal-demo-order-01",
    };
  user._demoSeedVersion = SEED_VERSION;
}

export function seedMockFixtures(): void {
  appRepository.update((data) => {
    for (let index = 1; index <= 24; index += 1) {
      const number = String(index).padStart(2, "0");
      const email = `demo${number}@diancheng.test`;
      const user = ensureUser(data, email, names[index - 1]!);
      const ideaId = `demo-idea-${number}`;
      if (!has(user.ideas, ideaId))
        user.ideas.push({
          id: ideaId,
          type: "new",
          parentId: null,
          text: ideaCopies[index - 1]!,
          status: statuses[(index - 1) % statuses.length]!,
          level: (index % 6) + 1,
          fee: index % 6 < 3 ? 5 : Number((index * 7.5).toFixed(2)),
          paid: index % 3 === 0,
          isPublic: true,
          likedBy: Array.from(
            { length: index % 9 },
            (_, like) => `visitor-${like}`,
          ),
          createdAt: ago(740 - index * 9),
          updatedAt: ago(120 - index),
        });
      const caseId = `demo-case-${number}`;
      if (index <= 20 && !has(user.ideas, caseId))
        user.ideas.push({
          id: caseId,
          type: "new",
          parentId: null,
          text: `示例案例 ${number}｜${ideaCopies[(index + 5) % ideaCopies.length]}`,
          status: "已完成",
          level: (index % 6) + 1,
          fee: index % 3 === 0 ? 25 : 5,
          paid: true,
          isPublic: true,
          likedBy: Array.from(
            { length: 5 + (index % 12) },
            (_, like) => `case-like-${like}`,
          ),
          createdAt: ago(1300 - index * 16),
          updatedAt: ago(70 - index),
        });
      if (index <= 20 && !user.teamApplication)
        user.teamApplication = {
          skill: ["前端开发", "视觉设计", "内容策划", "产品运营"][index % 4]!,
          time: ["偶尔参与", "每周少量时间", "可以稳定参与"][index % 3]!,
          resumeId: `demo-resume-${number}`,
          resumeName: `示例简历-${number}.pdf`,
          resumeSize: 180_000 + index * 1_200,
          status: ["待审核", "已通过", "未通过"][index % 3] as
            "待审核" | "已通过" | "未通过",
          createdAt: ago(600 - index * 8),
        };
      const orderId = `demo-order-${number}`;
      if (!has(user.packageOrders, orderId))
        user.packageOrders.push({
          id: orderId,
          packageId: index % 2 ? "standard" : "upgrade",
          packageName: index % 2 ? "标准套餐" : "升级套餐",
          levelRange: index % 2 ? "LEVEL 1—3" : "LEVEL 4—6",
          amount: index % 2 ? 25 : 99,
          payType: "BALANCE",
          status: "PAID",
          durationDays: 30,
          projectQuota: 2,
          iterationQuota: index % 2 ? 4 : 15,
          createdAt: ago(500 - index * 6),
          activatedAt: ago(500 - index * 6),
          expiresAt: ago(500 - index * 6 - 720),
        });
      const transactionId = `demo-transaction-${number}`;
      if (!has(user.transactions, transactionId))
        user.transactions.push({
          id: transactionId,
          type: index % 3 === 0 ? "credit" : "debit",
          title: index % 3 === 0 ? "支付宝充值" : "套餐开通",
          amount: index % 3 === 0 ? 100 + index * 10 : index % 2 ? 25 : 99,
          businessType: index % 3 === 0 ? "RECHARGE" : "PACKAGE",
          createdAt: ago(520 - index * 6),
        });
    }

    for (let index = 1; index <= 24; index += 1) {
      const number = String(index).padStart(2, "0");
      const email = `demo${number}@diancheng.test`;
      const feedbackId = `demo-feedback-${number}`;
      const createdAt = ago(430 - index * 5);
      if (!has(data.feedbacks, feedbackId)) {
        const messages: FeedbackMessage[] = [
          {
            id: `demo-message-${number}-1`,
            role: "user",
            email,
            content: [
              `页面在手机上滚动时，第 ${index} 个卡片显示不完整。`,
              `希望增加第 ${index} 条使用建议的处理进度。`,
              `提交后想确认第 ${index} 条记录应该在哪里查看。`,
            ][index % 3]!,
            createdAt,
          },
        ];
        if (index % 3 !== 0)
          messages.push({
            id: `demo-message-${number}-2`,
            role: "admin",
            email: ADMIN_EMAIL,
            content: "已经收到，我们会结合当前页面流程检查并同步处理结果。",
            createdAt: ago(420 - index * 5),
          });
        if (index % 4 === 0)
          messages.push({
            id: `demo-message-${number}-3`,
            role: "user",
            email,
            content: "补充说明：这个问题在移动端更容易出现。",
            createdAt: ago(410 - index * 5),
          });
        if (index % 5 === 0)
          messages.push({
            id: `demo-message-${number}-4`,
            role: "admin",
            email: ADMIN_EMAIL,
            content: "补充信息已记录，新的处理进展会继续显示在这里。",
            createdAt: ago(400 - index * 5),
          });
        const last = messages.at(-1)!;
        data.feedbacks.push({
          id: feedbackId,
          userEmail: email,
          page: index % 2 ? "index.html" : "center.html",
          category: ["功能问题", "使用问题", "建议"][index % 3] as
            "功能问题" | "使用问题" | "建议",
          status:
            index % 7 === 0
              ? "已结束"
              : last.role === "admin"
                ? "待用户回复"
                : "待管理员回复",
          createdAt,
          updatedAt: last.createdAt,
          messages,
        });
      }
      const noticeId = `demo-admin-notice-${number}`;
      if (!has(data.notifications, noticeId))
        data.notifications.push({
          id: noticeId,
          userEmail: ADMIN_EMAIL,
          type: "feedback",
          title: "收到示例用户反馈",
          content: `示例反馈 ${number} 等待处理。`,
          link: "/center/admin",
          businessId: feedbackId,
          dedupeKey: noticeId,
          read: index % 4 === 0,
          createdAt,
        });
    }

    for (let index = 1; index <= 20; index += 1) {
      const number = String(index).padStart(2, "0");
      const id = `demo-deposit-${number}`;
      const status = ["已支付", "已联系", "已退回"][index % 3] as
        "已支付" | "已联系" | "已退回";
      if (!has(data.cooperationDeposits, id))
        data.cooperationDeposits.push({
          id,
          userEmail: `demo${number}@diancheng.test`,
          amount: 2_000,
          status,
          createdAt: ago(360 - index * 7),
          updatedAt: ago(90 - index * 2),
          refundedAt: status === "已退回" ? ago(90 - index * 2) : null,
        });
    }
    for (let index = 1; index <= 40; index += 1) {
      const number = String(index).padStart(2, "0");
      const id = `demo-audit-${number}`;
      if (!has(data.auditLogs, id))
        data.auditLogs.push({
          id,
          action: [
            "更新想法状态",
            "管理员回复反馈",
            "审核团队申请",
            "更新商务合作状态",
          ][index % 4]!,
          detail: `示例操作记录 ${number} · 已完成`,
          actor:
            index % 3 === 0
              ? ADMIN_EMAIL
              : `demo${String((index % 24) + 1).padStart(2, "0")}@diancheng.test`,
          targetId: `demo-target-${number}`,
          createdAt: ago(300 - index * 3),
        });
    }

    ensureUser(data, ADMIN_EMAIL, "timeline.1994.1976");
    const sessionEmail = sessionRepository.getSnapshot();
    if (sessionEmail && data.users[sessionEmail])
      seedCurrentUser(data, sessionEmail);
    data._demoSeedVersion = SEED_VERSION;
  });
}
