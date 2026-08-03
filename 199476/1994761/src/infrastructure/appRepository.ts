import { FEEDBACK_CATEGORIES, MOCK_API_STORAGE_KEY } from "../domain/constants";
import type { AppData, FeedbackCategory, UserAccount } from "../domain/types";
import { makeId, makeInviteCode } from "../shared/lib/format";
import { persistentStorage } from "./storage/browserStorage";
import { JsonStorage } from "./storage/jsonStorage";

const emptyData = (): AppData => ({
  users: {},
  feedbacks: [],
  notifications: [],
  auditLogs: [],
  cooperationDeposits: [],
});

export function normalizeAppData(candidate: unknown): AppData {
  const data =
    candidate && typeof candidate === "object"
      ? (candidate as Partial<AppData>)
      : {};
  const normalized: AppData = {
    users: data.users && typeof data.users === "object" ? data.users : {},
    feedbacks: Array.isArray(data.feedbacks) ? data.feedbacks : [],
    notifications: Array.isArray(data.notifications) ? data.notifications : [],
    auditLogs: Array.isArray(data.auditLogs) ? data.auditLogs : [],
    cooperationDeposits: Array.isArray(data.cooperationDeposits)
      ? data.cooperationDeposits
      : [],
    _demoSeedVersion: data._demoSeedVersion,
  };

  Object.entries(normalized.users).forEach(([email, value]) => {
    const user = value as UserAccount;
    user.email = user.email || email;
    user.name = user.name || "";
    user.passwordHash = user.passwordHash || "";
    user.inviteCode = /^DC-\d{6}$/.test(user.inviteCode || "")
      ? user.inviteCode
      : makeInviteCode(user.email);
    user.usedInviteCode = /^DC-\d{6}$/.test(user.usedInviteCode || "")
      ? user.usedInviteCode
      : user.inviteCode;
    user.createdAt = user.createdAt || new Date().toISOString();
    user.ideas = Array.isArray(user.ideas) ? user.ideas : [];
    user.products = Array.isArray(user.products) ? user.products : [];
    user.transactions = Array.isArray(user.transactions)
      ? user.transactions
      : [];
    user.packageOrders = Array.isArray(user.packageOrders)
      ? user.packageOrders
      : [];
    user.notifications = Array.isArray(user.notifications)
      ? user.notifications
      : [];
    user.teamApplication =
      user.teamApplication && typeof user.teamApplication === "object"
        ? user.teamApplication
        : null;
    user.activePackage =
      user.activePackage && typeof user.activePackage === "object"
        ? user.activePackage
        : null;
    user.balance = Number(user.balance) || 0;
    user.ideas.forEach((idea) => {
      idea.parentId = idea.parentId || null;
      idea.fee = Number(idea.fee) || 0;
      idea.paid = Boolean(idea.paid);
      idea.isPublic = idea.isPublic === true;
      idea.likedBy = Array.isArray(idea.likedBy) ? idea.likedBy : [];
      if (Number(idea.level) > 6) {
        idea.level = null;
        idea.fee = 0;
        idea.paid = false;
        idea.status = "待评估";
      }
    });
  });

  normalized.feedbacks.forEach((feedback) => {
    feedback.category = (FEEDBACK_CATEGORIES as readonly string[]).includes(
      feedback.category,
    )
      ? feedback.category
      : ("使用问题" as FeedbackCategory);
    feedback.messages = Array.isArray(feedback.messages)
      ? feedback.messages
      : [];
    if (!feedback.messages.length && feedback.content) {
      feedback.messages.push({
        id: makeId("msg"),
        role: "user",
        email: feedback.userEmail || null,
        content: String(feedback.content),
        createdAt: feedback.createdAt || new Date().toISOString(),
      });
    }
    if (
      feedback.reply &&
      !feedback.messages.some((message) => message.role === "admin")
    ) {
      feedback.messages.push({
        id: makeId("msg"),
        role: "admin",
        email: feedback.replyBy || null,
        content: String(feedback.reply),
        createdAt:
          feedback.repliedAt || feedback.updatedAt || new Date().toISOString(),
      });
    }
    feedback.updatedAt =
      feedback.updatedAt ||
      feedback.repliedAt ||
      feedback.createdAt ||
      new Date().toISOString();
    if (feedback.status !== "已结束") {
      feedback.status =
        feedback.messages.at(-1)?.role === "admin"
          ? "待用户回复"
          : "待管理员回复";
    }
  });

  return normalized;
}

const storage = new JsonStorage<unknown>(
  persistentStorage,
  MOCK_API_STORAGE_KEY,
  emptyData,
);

function readStorage(): AppData {
  return normalizeAppData(storage.read());
}

let snapshot = readStorage();
const listeners = new Set<() => void>();

function emit(): void {
  listeners.forEach((listener) => listener());
}

export const appRepository = {
  getSnapshot(): AppData {
    return snapshot;
  },

  subscribe(listener: () => void): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },

  replace(data: AppData): AppData {
    snapshot = normalizeAppData(data);
    storage.write(snapshot);
    emit();
    return snapshot;
  },

  update<T>(mutator: (draft: AppData) => T): T {
    const draft = structuredClone(snapshot);
    const result = mutator(draft);
    this.replace(draft);
    return result;
  },

  refresh(): void {
    snapshot = readStorage();
    emit();
  },
};

storage.subscribe(() => appRepository.refresh());
