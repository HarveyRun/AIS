import { beforeEach, describe, expect, it } from "vitest";
import { appService } from "./appService";
import { MASTER_INVITE } from "../domain/constants";
import type { AppData, UserAccount } from "../domain/types";
import { appRepository } from "../infrastructure/appRepository";
import { sessionRepository } from "../infrastructure/sessionRepository";
import { hashText, makeInviteCode } from "../shared/lib/format";

function account(email = "user@example.com"): UserAccount {
  return {
    email,
    name: "",
    passwordHash: hashText("12345678"),
    inviteCode: makeInviteCode(email),
    usedInviteCode: MASTER_INVITE,
    createdAt: "2026-08-01T00:00:00.000Z",
    ideas: [],
    products: [],
    teamApplication: null,
    balance: 200,
    transactions: [],
    packageOrders: [],
    activePackage: null,
    notifications: [],
  };
}

function dataWith(user = account()): AppData {
  return {
    users: { [user.email]: user },
    feedbacks: [],
    notifications: [],
    auditLogs: [],
    cooperationDeposits: [],
  };
}

beforeEach(() => {
  appRepository.replace(dataWith());
  sessionRepository.clear();
});

describe("appService", () => {
  it("登录只校验已有账号，不再隐式创建用户", async () => {
    await expect(
      appService.login("new@example.com", "12345678"),
    ).resolves.toEqual({
      ok: false,
      error: "账号不存在，请先注册。",
    });
    await expect(
      appService.login("user@example.com", "wrong-password"),
    ).resolves.toEqual({
      ok: false,
      error: "密码不正确。",
    });
    await expect(
      appService.login("user@example.com", "12345678"),
    ).resolves.toEqual({
      ok: true,
    });
    expect(sessionRepository.getSnapshot()).toBe("user@example.com");
  });

  it("注册校验确认密码、邀请码和重复邮箱", async () => {
    await expect(
      appService.register("new@example.com", "12345678", "87654321", "199476"),
    ).resolves.toEqual({ ok: false, error: "两次输入的密码不一致。" });
    await expect(
      appService.register("new@example.com", "12345678", "12345678", "000000"),
    ).resolves.toEqual({ ok: false, error: "邀请码无效。" });
    await expect(
      appService.register("user@example.com", "12345678", "12345678", "199476"),
    ).resolves.toEqual({ ok: false, error: "该邮箱已经注册，请直接登录。" });
  });

  it("注册成功后保持原账号模型并建立会话", async () => {
    await expect(
      appService.register("NEW@example.com", "12345678", "12345678", "199476"),
    ).resolves.toEqual({ ok: true });
    expect(appRepository.getSnapshot().users["new@example.com"]).toMatchObject({
      email: "new@example.com",
      usedInviteCode: MASTER_INVITE,
      balance: 0,
      ideas: [],
      products: [],
    });
    expect(sessionRepository.getSnapshot()).toBe("new@example.com");
  });

  it("新增想法时保持原数据模型和初始状态", async () => {
    const idea = await appService.addIdea(
      "user@example.com",
      "new",
      "做一个可以记录每日饮水情况的小工具",
      null,
      true,
    );
    expect(idea).toMatchObject({
      type: "new",
      status: "待评估",
      level: null,
      fee: 0,
      paid: false,
      isPublic: true,
    });
    expect(
      appRepository.getSnapshot().users["user@example.com"]?.ideas[0]?.id,
    ).toBe(idea.id);
  });

  it("套餐开通使用余额并生成订单、交易和有效权益", async () => {
    const result = await appService.purchasePackage(
      "user@example.com",
      "standard",
    );
    const user = appRepository.getSnapshot().users["user@example.com"]!;
    expect(result.ok).toBe(true);
    expect(user.balance).toBe(175);
    expect(user.packageOrders).toHaveLength(1);
    expect(user.transactions[0]).toMatchObject({
      type: "debit",
      amount: 25,
      businessType: "PACKAGE",
    });
    expect(user.activePackage).toMatchObject({
      packageId: "standard",
      projectQuota: 2,
      iterationQuota: 4,
    });
  });

  it("商务押金扣除余额，退回时完整返还", async () => {
    const user = account();
    user.balance = 2_500;
    appRepository.replace(dataWith(user));
    expect((await appService.createDeposit(user.email, 2_000)).ok).toBe(true);
    expect(appRepository.getSnapshot().users[user.email]?.balance).toBe(500);
    const deposit = appRepository.getSnapshot().cooperationDeposits[0]!;
    await appService.updateDeposit(deposit.id, "已退回");
    expect(appRepository.getSnapshot().users[user.email]?.balance).toBe(2_500);
    expect(appRepository.getSnapshot().cooperationDeposits[0]?.status).toBe(
      "已退回",
    );
  });
});
