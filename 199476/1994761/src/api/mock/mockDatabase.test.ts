import { beforeEach, describe, expect, it } from "vitest";
import type { AppData, UserAccount } from "../../domain/types";
import { appRepository } from "../../infrastructure/appRepository";
import { sessionRepository } from "../../infrastructure/sessionRepository";
import { hashText, makeInviteCode } from "../../shared/lib/format";
import { initializeMockDatabase } from "./mockDatabase";

const emptyData = (): AppData => ({
  users: {},
  feedbacks: [],
  notifications: [],
  auditLogs: [],
  cooperationDeposits: [],
});

function customAccount(): UserAccount {
  const email = "custom@example.com";
  return {
    email,
    name: "自定义用户",
    passwordHash: hashText("12345678"),
    inviteCode: makeInviteCode(email),
    usedInviteCode: "DC-199476",
    createdAt: new Date().toISOString(),
    ideas: [],
    products: [],
    teamApplication: null,
    balance: 0,
    transactions: [],
    packageOrders: [],
    activePackage: null,
    notifications: [],
  };
}

beforeEach(() => {
  appRepository.replace(emptyData());
  sessionRepository.clear();
});

describe("Mock database", () => {
  it("初始化固定的普通用户、管理员和业务样例", () => {
    initializeMockDatabase();
    const data = appRepository.getSnapshot();
    expect(data.users["demo01@diancheng.test"]).toBeDefined();
    expect(data.users["timeline.1994.1976@gmail.com"]).toBeDefined();
    expect(data.feedbacks.length).toBeGreaterThan(20);
    expect(data.cooperationDeposits.length).toBeGreaterThanOrEqual(20);
  });

  it("不会向新注册或自定义用户灌入个人示例记录", () => {
    const account = customAccount();
    appRepository.replace({
      ...emptyData(),
      users: { [account.email]: account },
    });
    sessionRepository.set(account.email);
    initializeMockDatabase();
    const stored = appRepository.getSnapshot().users[account.email]!;
    expect(stored.ideas).toEqual([]);
    expect(stored.transactions).toEqual([]);
    expect(stored.packageOrders).toEqual([]);
  });
});
