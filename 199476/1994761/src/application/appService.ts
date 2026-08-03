import { appApi } from "../api/client";

// 应用层只暴露接口契约，不再直接读写 Mock 数据库。
export const appService = appApi;
