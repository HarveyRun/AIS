import type { FileApi } from "../fileApi";
import { sessionRepository } from "../../infrastructure/sessionRepository";
import { apiUrl, getApiToken, httpRequest, setApiToken } from "./httpClient";

export const httpFileApi: FileApi = {
  async upload(record) {
    const form = new FormData();
    form.set("id", record.id);
    if (record.kind) form.set("kind", record.kind);
    const fileName = record.blob instanceof File ? record.blob.name : record.id;
    form.set("file", record.blob, fileName);
    await httpRequest<{ ok: boolean }>("/files", { method: "POST", body: form });
  },
  async remove(id) {
    await httpRequest<{ ok: boolean }>(`/files/${encodeURIComponent(id)}`, {
      method: "DELETE",
    });
  },
  async download(id, fileName) {
    const token = getApiToken();
    const response = await fetch(
      `${apiUrl(`/files/${encodeURIComponent(id)}`)}?name=${encodeURIComponent(fileName)}`,
      { headers: token ? { Authorization: `Bearer ${token}` } : {} },
    );
    if (response.status === 404) return false;
    if (response.status === 401) {
      setApiToken("");
      sessionRepository.clear();
    }
    if (!response.ok) throw new Error("文件下载失败。");
    const url = URL.createObjectURL(await response.blob());
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1_000);
    return true;
  },
};
