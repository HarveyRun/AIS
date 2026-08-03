import type { AppApi } from "./appApi";
import { httpAppApi } from "./http/httpAppApi";
import { httpFileApi } from "./http/httpFileApi";
import { mockAppApi } from "./mock/mockAppApi";
import { mockFileApi } from "./mock/mockFileApi";

// 自动化测试使用内存 Mock，开发与生产环境全部调用 Java HTTP API。
export const appApi: AppApi =
  import.meta.env.MODE === "test" ? mockAppApi : httpAppApi;
export const fileApi =
  import.meta.env.MODE === "test" ? mockFileApi : httpFileApi;
