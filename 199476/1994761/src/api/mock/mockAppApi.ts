import type { AppApi } from "../appApi";
import type { PackageId } from "../../domain/constants";
import type {
  FeedbackCategory,
  TeamStatus,
  UserAccount,
} from "../../domain/types";
import { appRepository } from "../../infrastructure/appRepository";
import { initializeMockDatabase } from "./mockDatabase";
import { mockHandlers } from "./mockHandlers";

const MOCK_LATENCY_MS = import.meta.env.MODE === "test" ? 0 : 160;

function respond<T>(operation: () => T): Promise<T> {
  if (!MOCK_LATENCY_MS) return Promise.resolve().then(operation);
  return new Promise((resolve, reject) => {
    window.setTimeout(() => {
      try {
        resolve(operation());
      } catch (error) {
        reject(error);
      }
    }, MOCK_LATENCY_MS);
  });
}

export const mockAppApi: AppApi = {
  async initialize() {
    await respond(initializeMockDatabase);
  },
  getSnapshot: appRepository.getSnapshot,
  subscribe: appRepository.subscribe,
  login: (email, password) =>
    respond(() => mockHandlers.login(email, password)),
  register: (email, password, confirmation, inviteDigits) =>
    respond(() =>
      mockHandlers.register(email, password, confirmation, inviteDigits),
    ),
  logout: () => respond(() => mockHandlers.logout()),
  addIdea: (email, type, text, parentId, isPublic) =>
    respond(() => mockHandlers.addIdea(email, type, text, parentId, isPublic)),
  toggleIdeaVisibility: (email, ideaId) =>
    respond(() => mockHandlers.toggleIdeaVisibility(email, ideaId)),
  toggleLike: (ownerEmail, ideaId, viewerEmail) =>
    respond(() => mockHandlers.toggleLike(ownerEmail, ideaId, viewerEmail)),
  payIdea: (email, ideaId) =>
    respond(() => mockHandlers.payIdea(email, ideaId)),
  evaluateIdea: (owner, ideaId, level, decision, fee) =>
    respond(() =>
      mockHandlers.evaluateIdea(owner, ideaId, level, decision, fee),
    ),
  updateIdeaStatus: (owner, ideaId, status) =>
    respond(() => mockHandlers.updateIdeaStatus(owner, ideaId, status)),
  recharge: (email, amount) =>
    respond(() => mockHandlers.recharge(email, amount)),
  purchasePackage: (email, packageId: PackageId) =>
    respond(() => mockHandlers.purchasePackage(email, packageId)),
  updateProfile: (email, name) =>
    respond(() => mockHandlers.updateProfile(email, name)),
  changePassword: (email, current, next) =>
    respond(() => mockHandlers.changePassword(email, current, next)),
  deleteAccount: (email) => respond(() => mockHandlers.deleteAccount(email)),
  createFeedback: (email, content, page, category: FeedbackCategory) =>
    respond(() => mockHandlers.createFeedback(email, content, page, category)),
  appendFeedbackMessage: (feedbackId, role, email, content) =>
    respond(() =>
      mockHandlers.appendFeedbackMessage(feedbackId, role, email, content),
    ),
  closeFeedback: (feedbackId, actor) =>
    respond(() => mockHandlers.closeFeedback(feedbackId, actor)),
  markNotification: (email, id) =>
    respond(() => mockHandlers.markNotification(email, id)),
  markAllNotifications: (email) =>
    respond(() => mockHandlers.markAllNotifications(email)),
  markBusinessNotifications: (email, businessId) =>
    respond(() => mockHandlers.markBusinessNotifications(email, businessId)),
  ensureDerivedNotifications: (email) =>
    respond(() => mockHandlers.ensureDerivedNotifications(email)),
  submitTeamApplication: (email, application: UserAccount["teamApplication"]) =>
    respond(() => mockHandlers.submitTeamApplication(email, application)),
  updateTeamStatus: (email, status: TeamStatus) =>
    respond(() => mockHandlers.updateTeamStatus(email, status)),
  createDeposit: (email, amount) =>
    respond(() => mockHandlers.createDeposit(email, amount)),
  updateDeposit: (depositId, status) =>
    respond(() => mockHandlers.updateDeposit(depositId, status)),
};
