import {
  AI_OWNERS,
  DEFAULT_THREE_STAR_TIME_SECONDS,
  DEFAULT_TWO_STAR_TIME_SECONDS,
  DEFAULT_MAX_LEVEL,
  LevelDocument,
  MAX_STAR_TIME_SECONDS,
  sanitizeCapLevel,
  sanitizeMaxLevel,
  WORLD_HEIGHT,
  WORLD_WIDTH
} from "./level-types";

export function parseLevelDocument(text: string): LevelDocument {
  return JSON.parse(text) as LevelDocument;
}

export function validateLevel(level: LevelDocument): string[] {
  const errors: string[] = [];

  if (level.schemaVersion !== 2) {
    errors.push("schemaVersion must be 2.");
  }
  if (!level.name.trim()) {
    errors.push("Level name is required.");
  }
  if (!level.description.trim()) {
    errors.push("Level description is required.");
  }
  if (level.twoStarTimeSeconds <= 0) {
    errors.push("Two-star time must be positive.");
  }
  if (level.twoStarTimeSeconds > MAX_STAR_TIME_SECONDS) {
    errors.push(`Two-star time must not exceed ${MAX_STAR_TIME_SECONDS} seconds.`);
  }
  if (level.threeStarTimeSeconds <= 0) {
    errors.push("Three-star time must be positive.");
  }
  if (level.threeStarTimeSeconds > MAX_STAR_TIME_SECONDS) {
    errors.push(`Three-star time must not exceed ${MAX_STAR_TIME_SECONDS} seconds.`);
  }
  if (level.threeStarTimeSeconds >= level.twoStarTimeSeconds) {
    errors.push("Three-star time must be lower than the two-star time.");
  }
  if (!level.introMessage.trim()) {
    errors.push("Intro message is required.");
  }
  if (level.worldWidth <= 0 || level.worldHeight <= 0) {
    errors.push("World size must be positive.");
  }
  if (!level.bases.some((base) => base.owner === "PLAYER")) {
    errors.push("At least one player base is required.");
  }
  if (!level.bases.some((base) => AI_OWNERS.includes(base.owner as (typeof AI_OWNERS)[number]))) {
    errors.push("At least one AI-owned base is required.");
  }

  const configuredAiOwners = level.aiControllers.map((controller) => controller.owner);
  if (configuredAiOwners.length !== new Set(configuredAiOwners).size) {
    errors.push("AI controller owners must be unique.");
  }

  const seenIds = new Set<number>();
  level.bases.forEach((base) => {
    if (seenIds.has(base.id)) {
      errors.push(`Base id ${base.id} is duplicated.`);
    }
    seenIds.add(base.id);
    if (base.x < 0 || base.x > level.worldWidth || base.y < 0 || base.y > level.worldHeight) {
      errors.push(`Base ${base.id} is outside the world bounds.`);
    }
    if (base.units < 0) {
      errors.push(`Base ${base.id} units must be non-negative.`);
    }
    if (base.capLevel < 1) {
      errors.push(`Base ${base.id} capLevel must be at least 1.`);
    }
    if (base.maxLevel < 1) {
      errors.push(`Base ${base.id} maxLevel must be at least 1.`);
    }
    if (base.capLevel > base.maxLevel) {
      errors.push(`Base ${base.id} capLevel cannot exceed maxLevel.`);
    }
    if (AI_OWNERS.includes(base.owner as (typeof AI_OWNERS)[number]) && !configuredAiOwners.includes(base.owner as (typeof AI_OWNERS)[number])) {
      errors.push(`Base ${base.id} uses ${base.owner} without an AI controller.`);
    }
  });

  level.aiControllers.forEach((controller) => {
    if (!level.bases.some((base) => base.owner === controller.owner)) {
      errors.push(`${controller.owner} is configured as an AI controller but owns no bases.`);
    }
  });

  level.obstacles.forEach((obstacle, index) => {
    if (
      obstacle.x < 0 ||
      obstacle.x > level.worldWidth ||
      obstacle.y < 0 ||
      obstacle.y > level.worldHeight
    ) {
      errors.push(`Obstacle ${index + 1} is outside the world bounds.`);
    }
    if (obstacle.radius <= 0) {
      errors.push(`Obstacle ${index + 1} radius must be positive.`);
    }
  });

  return errors;
}

export function formatLevelFile(level: LevelDocument): string {
  return `${JSON.stringify(level, null, 2)}\n`;
}

export function normalizeLevel(level: LevelDocument): LevelDocument {
  return {
    ...level,
    schemaVersion: 2,
    twoStarTimeSeconds: normalizePositiveInt(level.twoStarTimeSeconds, DEFAULT_TWO_STAR_TIME_SECONDS),
    threeStarTimeSeconds: normalizePositiveInt(level.threeStarTimeSeconds, DEFAULT_THREE_STAR_TIME_SECONDS),
    worldWidth: level.worldWidth || WORLD_WIDTH,
    worldHeight: level.worldHeight || WORLD_HEIGHT,
    bases: level.bases.map((base) => {
      const capLevel = sanitizeCapLevel(base.capLevel);
      const maxLevel = sanitizeMaxLevel(base.maxLevel ?? DEFAULT_MAX_LEVEL);
      return {
        ...base,
        capLevel,
        maxLevel: Math.max(maxLevel, capLevel),
        x: clamp(base.x, 0, level.worldWidth || WORLD_WIDTH),
        y: clamp(base.y, 0, level.worldHeight || WORLD_HEIGHT)
      };
    }),
    obstacles: level.obstacles.map((obstacle) => ({
      ...obstacle,
      x: clamp(obstacle.x, 0, level.worldWidth || WORLD_WIDTH),
      y: clamp(obstacle.y, 0, level.worldHeight || WORLD_HEIGHT)
    }))
  };
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

function normalizePositiveInt(value: number | undefined, fallback: number): number {
  const rounded = Math.round(value ?? fallback);
  return rounded > 0 ? rounded : fallback;
}
