import { appRepository } from "../../infrastructure/appRepository";
import { seedMockFixtures } from "./mockFixtures";
import { sessionRepository } from "../../infrastructure/sessionRepository";

export function initializeMockDatabase(): void {
  seedMockFixtures();
  sessionRepository.validate(appRepository.getSnapshot().users);
}
