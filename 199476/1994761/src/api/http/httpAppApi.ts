import type { AppApi, ApiResult } from "../appApi";
import type { PackageId } from "../../domain/constants";
import type {
  AppData,
  Feedback,
  FeedbackCategory,
  FeedbackMessage,
  Idea,
  PackageOrder,
  TeamStatus,
  UserAccount,
} from "../../domain/types";
import { sessionRepository } from "../../infrastructure/sessionRepository";
import { httpRequest, jsonBody, setApiToken } from "./httpClient";

const emptyData = (): AppData => ({
  users: {},
  feedbacks: [],
  notifications: [],
  auditLogs: [],
  cooperationDeposits: [],
});

let snapshot = emptyData();
const listeners = new Set<() => void>();

function emit(): void {
  listeners.forEach((listener) => listener());
}

async function refresh(): Promise<void> {
  const response = await httpRequest<{ sessionEmail: string; data: AppData }>(
    "/state",
  );
  snapshot = response.data;
  if (response.sessionEmail) sessionRepository.set(response.sessionEmail);
  else sessionRepository.clear();
  emit();
}

async function mutate<T>(
  path: string,
  method: string,
  body?: unknown,
): Promise<T> {
  const result = await httpRequest<T>(path, {
    method,
    ...(body === undefined ? {} : jsonBody(body)),
  });
  await refresh();
  return result;
}

export const httpAppApi: AppApi = {
  initialize: refresh,
  getSnapshot: () => snapshot,
  subscribe(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
  async login(email, password) {
    const result = await httpRequest<
      ApiResult & { email?: string; token?: string }
    >("/auth/login", { method: "POST", ...jsonBody({ email, password }) });
    if (result.ok && result.token && result.email) {
      setApiToken(result.token);
      sessionRepository.set(result.email);
      await refresh();
    }
    return result;
  },
  async register(email, password, confirmation, inviteDigits) {
    const result = await httpRequest<
      ApiResult & { email?: string; token?: string }
    >("/auth/register", {
      method: "POST",
      ...jsonBody({ email, password, confirmation, inviteDigits }),
    });
    if (result.ok && result.token && result.email) {
      setApiToken(result.token);
      sessionRepository.set(result.email);
      await refresh();
    }
    return result;
  },
  async logout() {
    try {
      await httpRequest<ApiResult>("/auth/logout", { method: "POST" });
    } finally {
      setApiToken("");
      sessionRepository.clear();
      await refresh();
    }
  },
  addIdea: (_email, type, text, parentId, isPublic = true) =>
    mutate<Idea>("/ideas", "POST", { type, text, parentId, isPublic }),
  toggleIdeaVisibility: (_email, ideaId) =>
    mutate<void>(`/ideas/${ideaId}/visibility`, "PATCH"),
  async toggleLike(_ownerEmail, ideaId) {
    const result = await mutate<{ liked: boolean }>(
      `/ideas/${ideaId}/like`,
      "POST",
    );
    return result.liked;
  },
  payIdea: (_email, ideaId) =>
    mutate<ApiResult>(`/ideas/${ideaId}/pay`, "POST"),
  evaluateIdea: (owner, ideaId, level, decision, fee) =>
    mutate<ApiResult>(`/admin/ideas/${ideaId}/evaluate`, "POST", {
      owner,
      level,
      decision,
      fee,
    }),
  updateIdeaStatus: (owner, ideaId, status) =>
    mutate<void>(`/admin/ideas/${ideaId}/status`, "PATCH", { owner, status }),
  recharge: (_email, amount) =>
    mutate<void>("/wallet/recharge", "POST", { amount }),
  purchasePackage: (_email, packageId: PackageId) =>
    mutate<ApiResult & { order?: PackageOrder }>("/packages/purchase", "POST", {
      packageId,
    }),
  updateProfile: (_email, name) =>
    mutate<void>("/profile", "PUT", { name }),
  changePassword: (_email, current, next) =>
    mutate<ApiResult>("/profile/password", "PUT", { current, next }),
  async deleteAccount() {
    await httpRequest<ApiResult>("/profile", { method: "DELETE" });
    setApiToken("");
    sessionRepository.clear();
    await refresh();
  },
  createFeedback: (_email, content, page, category: FeedbackCategory) =>
    mutate<Feedback>("/feedback", "POST", { content, page, category }),
  appendFeedbackMessage: (feedbackId, role, _email, content) =>
    mutate<FeedbackMessage>(`/feedback/${feedbackId}/messages`, "POST", {
      role,
      content,
    }),
  closeFeedback: (feedbackId) =>
    mutate<void>(`/feedback/${feedbackId}/close`, "POST"),
  markNotification: (_email, id) =>
    mutate<void>(`/notifications/${id}/read`, "PATCH"),
  markAllNotifications: () =>
    mutate<void>("/notifications/read-all", "POST"),
  markBusinessNotifications: (_email, businessId) =>
    mutate<void>("/notifications/read-business", "POST", { businessId }),
  ensureDerivedNotifications: () =>
    mutate<void>("/notifications/derive", "POST"),
  submitTeamApplication: (_email, application: UserAccount["teamApplication"]) =>
    mutate<void>("/team/application", "PUT", application),
  updateTeamStatus: (email, status: TeamStatus) =>
    mutate<void>(`/admin/team/${encodeURIComponent(email)}/status`, "PATCH", {
      status,
    }),
  createDeposit: (_email, amount) =>
    mutate<ApiResult>("/deposits", "POST", { amount }),
  updateDeposit: (depositId, status) =>
    mutate<void>(`/admin/deposits/${depositId}`, "PATCH", { status }),
};
