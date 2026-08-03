import type {
  FEEDBACK_CATEGORIES,
  IDEA_STATUSES,
  PackageId,
  TEAM_STATUSES,
} from "./constants";

export type IsoDate = string;
export type FeedbackCategory = (typeof FEEDBACK_CATEGORIES)[number];
export type IdeaStatus = (typeof IDEA_STATUSES)[number];
export type TeamStatus = (typeof TEAM_STATUSES)[number];

export interface Idea {
  id: string;
  type: "new" | "iteration";
  parentId: string | null;
  text: string;
  status: IdeaStatus;
  level: number | null;
  fee: number;
  paid: boolean;
  decision?: "制作" | "不制作";
  isPublic: boolean;
  likedBy: string[];
  createdAt: IsoDate;
  updatedAt?: IsoDate;
  reviewedAt?: IsoDate;
}

export interface Product {
  id: string;
  name: string;
  type: string;
  summary: string;
  fileName: string;
  size: number;
  status?: string;
  createdAt: IsoDate;
}

export interface TeamApplication {
  skill: string;
  intro?: string;
  time: string;
  resumeId?: string;
  resumeName?: string;
  resumeSize?: number;
  status: TeamStatus;
  createdAt: IsoDate;
  updatedAt?: IsoDate;
}

export interface Transaction {
  id: string;
  type: "credit" | "debit";
  title: string;
  amount: number;
  businessType?: string;
  orderId?: string | null;
  depositId?: string;
  payType?: "BALANCE";
  createdAt: IsoDate;
}

export interface PackageOrder {
  id: string;
  packageId: PackageId;
  packageName: string;
  levelRange: string;
  amount: number;
  payType: "BALANCE";
  status: "PAID";
  durationDays: number;
  projectQuota: number;
  iterationQuota: number;
  benefits?: string[];
  createdAt: IsoDate;
  activatedAt: IsoDate;
  expiresAt: IsoDate;
}

export interface ActivePackage {
  packageId: PackageId;
  packageName: string;
  levelRange: string;
  startedAt: IsoDate;
  expiresAt: IsoDate;
  projectQuota: number;
  iterationQuota: number;
  orderId: string;
}

export interface UserAccount {
  email: string;
  name: string;
  passwordHash: string;
  inviteCode: string;
  usedInviteCode: string;
  createdAt: IsoDate;
  ideas: Idea[];
  products: Product[];
  teamApplication: TeamApplication | null;
  balance: number;
  transactions: Transaction[];
  packageOrders: PackageOrder[];
  activePackage: ActivePackage | null;
  notifications: Notification[];
  _demoSeedVersion?: string;
}

export interface FeedbackMessage {
  id: string;
  role: "user" | "admin";
  email: string | null;
  content: string;
  createdAt: IsoDate;
}

export interface Feedback {
  id: string;
  userEmail: string | null;
  page: string;
  category: FeedbackCategory;
  status: "待管理员回复" | "待用户回复" | "已结束";
  createdAt: IsoDate;
  updatedAt: IsoDate;
  messages: FeedbackMessage[];
  content?: string;
  reply?: string;
  replyBy?: string;
  repliedAt?: IsoDate;
}

export interface Notification {
  id: string;
  userEmail: string;
  type: "idea" | "payment" | "package" | "feedback" | "cooperation" | "system";
  title: string;
  content: string;
  link: string;
  businessId: string | null;
  dedupeKey: string | null;
  read: boolean;
  createdAt: IsoDate;
}

export interface AuditLog {
  id: string;
  action: string;
  detail: string;
  actor: string;
  targetId: string | null;
  createdAt: IsoDate;
}

export interface CooperationDeposit {
  id: string;
  userEmail: string;
  amount: number;
  status: "已支付" | "已联系" | "已退回" | "已取消";
  createdAt: IsoDate;
  updatedAt: IsoDate;
  refundedAt?: IsoDate | null;
}

export interface AppData {
  users: Record<string, UserAccount>;
  feedbacks: Feedback[];
  notifications: Notification[];
  auditLogs: AuditLog[];
  cooperationDeposits: CooperationDeposit[];
  _demoSeedVersion?: string;
}

export interface StoredFile {
  id: string;
  email: string;
  kind?: "resume" | "product";
  blob: Blob;
}
