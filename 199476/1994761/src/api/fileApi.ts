import type { StoredFile } from "../domain/types";

export interface FileApi {
  upload(record: StoredFile): Promise<void>;
  remove(id: string): Promise<void>;
  download(id: string, fileName: string): Promise<boolean>;
}
