import {
  AI_OWNERS,
  capacityForLevel,
  LevelDocument,
  sanitizeCapLevel,
  WORLD_HEIGHT,
  WORLD_WIDTH
} from "./level-types";

export function parseLevelDocument(text: string): LevelDocument {
  return JSON.parse(text) as LevelDocument;
}

export function validateLevel(level: LevelDocument): string[] {
  const errors: string[] = [];

  if (level.schemaVersion !== 1) {
    errors.push("schemaVersion must be 1.");
  }
  if (!level.name.trim()) {
    errors.push("Level name is required.");
  }
  if (!level.description.trim()) {
    errors.push("Level description is required.");
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
    if (base.radius <= 0) {
      errors.push(`Base ${base.id} radius must be positive.`);
    }
    if (base.units < 0) {
      errors.push(`Base ${base.id} units must be non-negative.`);
    }
    if (base.cap < 1) {
      errors.push(`Base ${base.id} cap must be at least 1.`);
    }
    if (base.capLevel < 1) {
      errors.push(`Base ${base.id} capLevel must be at least 1.`);
    }
    if (base.cap !== capacityForLevel(base.capLevel)) {
      errors.push(`Base ${base.id} cap must equal capLevel * 10.`);
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
    worldWidth: level.worldWidth || WORLD_WIDTH,
    worldHeight: level.worldHeight || WORLD_HEIGHT,
    bases: level.bases.map((base) => ({
      ...base,
      capLevel: sanitizeCapLevel(base.capLevel),
      cap: capacityForLevel(base.capLevel),
      x: clamp(base.x, 0, level.worldWidth || WORLD_WIDTH),
      y: clamp(base.y, 0, level.worldHeight || WORLD_HEIGHT)
    })),
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
