import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { AppRouter } from "./router";
import { appService } from "./application/appService";
import { StoreProvider } from "./application/store";
import { ToastProvider } from "./shared/ui/Toast";
import "./styles/base.css";
import "./styles/common.css";

async function bootstrap(): Promise<void> {
  await appService.initialize();
  createRoot(document.getElementById("root")!).render(
    <StrictMode>
      <StoreProvider>
        <ToastProvider>
          <AppRouter />
        </ToastProvider>
      </StoreProvider>
    </StrictMode>,
  );
}

void bootstrap().catch(() => {
  createRoot(document.getElementById("root")!).render(
    <main className="bootstrap-error" role="alert">
      <span>点</span>
      <h1>页面暂时无法加载</h1>
      <p>服务暂时不可用，请稍后重试。</p>
      <button type="button" onClick={() => window.location.reload()}>
        重新加载
      </button>
    </main>,
  );
});
