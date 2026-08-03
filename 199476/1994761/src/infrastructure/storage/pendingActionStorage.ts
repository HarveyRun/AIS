import { PENDING_LOGIN_KEY } from "../../domain/constants";
import { transientStorage } from "./browserStorage";
import { JsonStorage } from "./jsonStorage";

export interface PendingIdeaAction {
  type: "idea";
  mode: "new" | "iteration";
  text: string;
  parentId: string | null;
  isPublic: boolean;
}

const storage = new JsonStorage<PendingIdeaAction | null>(
  transientStorage,
  PENDING_LOGIN_KEY,
  () => null,
);

export const pendingActionStorage = {
  read: () => storage.read(),
  save: (action: PendingIdeaAction) => storage.write(action),
  clear: () => storage.remove(),
};
