import type { PackageId } from "../domain/constants";
import type {
  AppData,
  Feedback,
  FeedbackCategory,
  FeedbackMessage,
  Idea,
  PackageOrder,
  TeamStatus,
  UserAccount,
} from "../domain/types";

export interface ApiResult {
  ok: boolean;
  error?: string;
}

export interface AppApi {
  initialize(): Promise<void>;
  getSnapshot(): AppData;
  subscribe(listener: () => void): () => void;

  login(email: string, password: string): Promise<ApiResult>;
  register(
    email: string,
    password: string,
    confirmation: string,
    inviteDigits: string,
  ): Promise<ApiResult>;
  logout(): Promise<void>;

  addIdea(
    email: string,
    type: Idea["type"],
    text: string,
    parentId: string | null,
    isPublic?: boolean,
  ): Promise<Idea>;
  toggleIdeaVisibility(email: string, ideaId: string): Promise<void>;
  toggleLike(
    ownerEmail: string,
    ideaId: string,
    viewerEmail: string,
  ): Promise<boolean>;
  payIdea(email: string, ideaId: string): Promise<ApiResult>;
  evaluateIdea(
    owner: string,
    ideaId: string,
    level: number,
    decision: "制作" | "不制作",
    fee: number,
  ): Promise<ApiResult>;
  updateIdeaStatus(
    owner: string,
    ideaId: string,
    status: Idea["status"],
  ): Promise<void>;

  recharge(email: string, amount: number): Promise<void>;
  purchasePackage(
    email: string,
    packageId: PackageId,
  ): Promise<ApiResult & { order?: PackageOrder }>;

  updateProfile(email: string, name: string): Promise<void>;
  changePassword(
    email: string,
    current: string,
    next: string,
  ): Promise<ApiResult>;
  deleteAccount(email: string): Promise<void>;

  createFeedback(
    email: string | null,
    content: string,
    page: string,
    category: FeedbackCategory,
  ): Promise<Feedback>;
  appendFeedbackMessage(
    feedbackId: string,
    role: "user" | "admin",
    email: string,
    content: string,
  ): Promise<FeedbackMessage>;
  closeFeedback(feedbackId: string, actor: string): Promise<void>;

  markNotification(email: string, id: string): Promise<void>;
  markAllNotifications(email: string): Promise<void>;
  markBusinessNotifications(email: string, businessId: string): Promise<void>;
  ensureDerivedNotifications(email: string): Promise<void>;

  submitTeamApplication(
    email: string,
    application: UserAccount["teamApplication"],
  ): Promise<void>;
  updateTeamStatus(email: string, status: TeamStatus): Promise<void>;
  createDeposit(email: string, amount: number): Promise<ApiResult>;
  updateDeposit(depositId: string, status: "已联系" | "已退回"): Promise<void>;
}
