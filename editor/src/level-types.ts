export const WORLD_WIDTH = 1000;
export const WORLD_HEIGHT = 1600;
export const BASE_RADIUS_MIN = 36;
export const RADIUS_PER_LEVEL = 6;
export const DEFAULT_CAP_LEVEL = 4;
export const DEFAULT_MAX_LEVEL = 10;

export type Owner = "PLAYER" | "AI_1" | "AI_2" | "AI_3" | "AI_4" | "NEUTRAL";
export type BaseType =
  | "COMMAND"
  | "FAST"
  | "FACTORY"
  | "RELAY"
  | "ASSAULT"
  | "BATTERY";
export type AiType = "STANDARD";

export type LevelAiController = {
  owner: Extract<Owner, "AI_1" | "AI_2" | "AI_3" | "AI_4">;
  type: AiType;
};

export type LevelBase = {
  id: number;
  x: number;
  y: number;
  owner: Owner;
  type: BaseType;
  units: number;
  capLevel: number;
  maxLevel: number;
};

export type LevelObstacle = {
  x: number;
  y: number;
  radius: number;
};

export type LevelDocument = {
  schemaVersion: number;
  levelId: number;
  name: string;
  description: string;
  sortOrder: number;
  unlockAfterLevelId: number | null;
  worldWidth: number;
  worldHeight: number;
  introMessage: string;
  aiControllers: LevelAiController[];
  bases: LevelBase[];
  obstacles: LevelObstacle[];
};

export type Selection =
  | { kind: "base"; id: number }
  | { kind: "obstacle"; index: number }
  | null;

export const OWNERS: Owner[] = ["PLAYER", "AI_1", "AI_2", "AI_3", "AI_4", "NEUTRAL"];
export const AI_OWNERS: LevelAiController["owner"][] = ["AI_1", "AI_2", "AI_3", "AI_4"];
export const BASE_TYPES: BaseType[] = [
  "COMMAND",
  "FAST",
  "FACTORY",
  "RELAY",
  "ASSAULT",
  "BATTERY"
];
export const AI_TYPES: AiType[] = ["STANDARD"];

export function radiusForLevel(capLevel: number): number {
  return BASE_RADIUS_MIN + (sanitizeCapLevel(capLevel) - 1) * RADIUS_PER_LEVEL;
}

export function sanitizeCapLevel(capLevel: number): number {
  return Math.max(1, Math.round(capLevel));
}

export function sanitizeMaxLevel(maxLevel: number): number {
  return Math.max(1, Math.round(maxLevel));
}

export function createEmptyLevel(nextLevelId: number): LevelDocument {
  return {
    schemaVersion: 2,
    levelId: nextLevelId,
    name: `Level ${nextLevelId}`,
    description: "Describe the level objective here.",
    sortOrder: nextLevelId,
    unlockAfterLevelId: nextLevelId > 1 ? nextLevelId - 1 : null,
    worldWidth: WORLD_WIDTH,
    worldHeight: WORLD_HEIGHT,
    introMessage: "Capture nearby structures and push forward",
    aiControllers: [{ owner: "AI_1", type: "STANDARD" }],
    bases: [
      {
        id: 1,
        x: 180,
        y: 1350,
        owner: "PLAYER",
        type: "COMMAND",
        units: 32,
        capLevel: DEFAULT_CAP_LEVEL,
        maxLevel: DEFAULT_MAX_LEVEL
      },
      {
        id: 2,
        x: 820,
        y: 250,
        owner: "AI_1",
        type: "COMMAND",
        units: 32,
        capLevel: DEFAULT_CAP_LEVEL,
        maxLevel: DEFAULT_MAX_LEVEL
      }
    ],
    obstacles: []
  };
}
