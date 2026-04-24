import { useEffect, useMemo, useRef, useState } from "react";
import { Circle, Layer, RegularPolygon, Stage, Text } from "react-konva";
import Konva from "konva";
import {
  AI_OWNERS,
  AI_TYPES,
  BASE_TYPES,
  DEFAULT_CAP_LEVEL,
  DEFAULT_MAX_LEVEL,
  MAX_STAR_TIME_SECONDS,
  LevelAiController,
  LevelBase,
  LevelDocument,
  LevelObstacle,
  OWNERS,
  radiusForLevel,
  Selection,
  createEmptyLevel
} from "./level-types";
import {
  deleteLevelDocument,
  fetchLevelDocument,
  fetchLevelFileNames,
  saveLevelDocument
} from "./level-api";
import { formatLevelFile, normalizeLevel, parseLevelDocument, validateLevel } from "./level-validation";

type Tool = "select" | "base" | "obstacle";

const DEFAULT_STAGE_HEIGHT = 760;
const PHONE_FRAME_WIDTH = 44;
const PHONE_FRAME_HEIGHT = 69;
export default function App() {
  const stageRef = useRef<Konva.Stage | null>(null);
  const stageCardRef = useRef<HTMLDivElement | null>(null);
  const [tool, setTool] = useState<Tool>("select");
  const [fileNames, setFileNames] = useState<string[]>([]);
  const [currentFileName, setCurrentFileName] = useState<string>("");
  const [level, setLevel] = useState<LevelDocument>(() => createEmptyLevel(1));
  const [selection, setSelection] = useState<Selection>(null);
  const [status, setStatus] = useState("Open a packaged level or create a new one.");
  const [placementBaseType, setPlacementBaseType] = useState<LevelBase["type"]>("COMMAND");
  const [placementOwner, setPlacementOwner] = useState<LevelBase["owner"]>("NEUTRAL");
  const [placementObstacleRadius, setPlacementObstacleRadius] = useState(70);
  const [stageBounds, setStageBounds] = useState({ width: 520, height: DEFAULT_STAGE_HEIGHT + PHONE_FRAME_HEIGHT });

  const errors = useMemo(() => validateLevel(level), [level]);
  const scale = useMemo(() => {
    const widthScale = Math.max((stageBounds.width - PHONE_FRAME_WIDTH) / level.worldWidth, 0.2);
    const heightScale = Math.max((stageBounds.height - PHONE_FRAME_HEIGHT) / level.worldHeight, 0.2);
    const preferredScale = DEFAULT_STAGE_HEIGHT / level.worldHeight;
    return Math.min(widthScale, heightScale, preferredScale);
  }, [level.worldHeight, level.worldWidth, stageBounds.height, stageBounds.width]);
  const stageHeight = Math.round(level.worldHeight * scale);
  const stageWidth = Math.round(level.worldWidth * scale);
  const selectedObstacleIndex = selection?.kind === "obstacle" ? selection.index : null;

  useEffect(() => {
    void refreshFiles(setFileNames, setStatus);
  }, []);

  useEffect(() => {
    const element = stageCardRef.current;
    if (!element) {
      return;
    }

    const observer = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (!entry) {
        return;
      }

      setStageBounds({
        width: Math.max(Math.floor(entry.contentRect.width) - 52, 320),
        height: Math.max(Math.floor(entry.contentRect.height) - 52, 500)
      });
    });

    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  async function openLevel(fileName: string): Promise<void> {
    try {
      const parsed = normalizeLevel(fetchAndParseLevel(await fetchLevelDocument(fileName)));
      setLevel(parsed);
      setCurrentFileName(fileName);
      setSelection(null);
      setStatus(`Opened ${fileName}.`);
    } catch (error) {
      setStatus(readErrorMessage(error, `Failed to open ${fileName}.`));
    }
  }

  async function saveLevel(): Promise<void> {
    if (errors.length > 0) {
      setStatus("Fix validation errors before saving.");
      return;
    }

    const fileName = normalizeFileName(currentFileName || `level_${level.levelId}.json`);
    try {
      const normalizedLevel = normalizeLevel(level);
      await saveLevelDocument(fileName, fetchAndParseLevel(normalizedLevel));
      setLevel(normalizedLevel);
      setCurrentFileName(fileName);
      setStatus(`Saved ${fileName}. The Android app will pick it up on the next run.`);
      await refreshFiles(setFileNames, setStatus);
    } catch (error) {
      setStatus(readErrorMessage(error, `Failed to save ${fileName}.`));
    }
  }

  async function deleteLevel(): Promise<void> {
    if (!currentFileName) {
      setStatus("Open a saved file before deleting.");
      return;
    }
    try {
      await deleteLevelDocument(currentFileName);
      const nextLevelId = nextUnusedLevelId(fileNames, currentFileName, level.levelId);
      setLevel(createEmptyLevel(nextLevelId));
      setCurrentFileName("");
      setSelection(null);
      setStatus("Deleted the current level file.");
      await refreshFiles(setFileNames, setStatus);
    } catch (error) {
      setStatus(readErrorMessage(error, `Failed to delete ${currentFileName}.`));
    }
  }

  function newLevel(): void {
    const nextId = nextUnusedLevelId(fileNames, null, level.levelId + 1);
    setLevel(createEmptyLevel(nextId));
    setCurrentFileName("");
    setSelection(null);
    setStatus("Created a new unsaved level.");
  }

  function duplicateLevel(): void {
    const nextId = nextUnusedLevelId(fileNames, null, level.levelId + 1);
    setLevel({
      ...level,
      levelId: nextId,
      sortOrder: nextId,
      name: `${level.name} Copy`,
      unlockAfterLevelId: nextId > 1 ? nextId - 1 : null,
      aiControllers: [...level.aiControllers]
    });
    setCurrentFileName(`level_${nextId}.json`);
    setSelection(null);
    setStatus("Duplicated the current level into a new unsaved copy.");
  }

  function deleteSelectedBase(baseId: number): void {
    setLevel((current) =>
      normalizeLevel({
        ...current,
        bases: current.bases.filter((base) => base.id !== baseId)
      })
    );
    setSelection(null);
    setStatus(`Deleted base ${baseId}.`);
  }

  function deleteSelectedObstacle(index: number): void {
    setLevel((current) =>
      normalizeLevel({
        ...current,
        obstacles: current.obstacles.filter((_, currentIndex) => currentIndex !== index)
      })
    );
    setSelection(null);
    setStatus("Deleted obstacle.");
  }

  function updateAiController(index: number, patch: Partial<LevelAiController>): void {
    setLevel((current) =>
      normalizeLevel({
        ...current,
        aiControllers: current.aiControllers.map((controller, currentIndex) =>
          currentIndex === index ? { ...controller, ...patch } : controller
        )
      })
    );
  }

  function addAiController(): void {
    const nextOwner = AI_OWNERS.find((owner) => !level.aiControllers.some((controller) => controller.owner === owner));
    if (!nextOwner) {
      setStatus("All AI owner slots are already in use.");
      return;
    }
    setLevel((current) =>
      normalizeLevel({
        ...current,
        aiControllers: [...current.aiControllers, { owner: nextOwner, type: "STANDARD" }]
      })
    );
    setStatus(`Added ${nextOwner} controller.`);
  }

  function deleteAiController(index: number): void {
    const owner = level.aiControllers[index]?.owner;
    if (!owner) {
      return;
    }
    setLevel((current) =>
      normalizeLevel({
        ...current,
        aiControllers: current.aiControllers.filter((_, currentIndex) => currentIndex !== index),
        bases: current.bases.map((base) => (base.owner === owner ? { ...base, owner: "NEUTRAL" } : base))
      })
    );
    setStatus(`Removed ${owner} controller and reassigned its bases to NEUTRAL.`);
  }

  function handleStageClick(): void {
    const stage = stageRef.current;
    const pointer = stage?.getPointerPosition();
    if (!pointer) {
      return;
    }

    const worldX = roundToWorld(pointer.x / scale);
    const worldY = roundToWorld(pointer.y / scale);

    if (tool === "base") {
      const base: LevelBase = {
        id: nextBaseId(level.bases),
        x: worldX,
        y: worldY,
        owner: placementOwner,
        type: placementBaseType,
        units: 10,
        capLevel: DEFAULT_CAP_LEVEL,
        maxLevel: DEFAULT_MAX_LEVEL
      };
      setLevel((current) => ({ ...current, bases: [...current.bases, base] }));
      setSelection({ kind: "base", id: base.id });
      setStatus(`Placed base ${base.id}.`);
      return;
    }

    if (tool === "obstacle") {
      const obstacle: LevelObstacle = { x: worldX, y: worldY, radius: placementObstacleRadius };
      setLevel((current) => ({ ...current, obstacles: [...current.obstacles, obstacle] }));
      setSelection({ kind: "obstacle", index: level.obstacles.length });
      setStatus("Placed obstacle.");
      return;
    }

    setSelection(null);
  }

  const selectedBase = selection?.kind === "base" ? level.bases.find((base) => base.id === selection.id) ?? null : null;
  const selectedObstacle =
    selection?.kind === "obstacle" ? level.obstacles[selection.index] ?? null : null;

  return (
    <div className="editor-shell">
      <aside className="panel">
        <h1>CW Level Editor</h1>
        <p className="hint">Edit the packaged JSON levels in `app/src/main/assets/levels` directly from this editor.</p>
        <div className="sidebar-actions">
          <button onClick={newLevel}>New Level</button>
          <button onClick={duplicateLevel}>Duplicate Level</button>
          <button onClick={() => void saveLevel()}>Save Level</button>
          <button onClick={() => void deleteLevel()}>Delete Level</button>
        </div>
        <h2>Files</h2>
        <div className="file-list">
          {fileNames.map((fileName) => (
            <button
              key={fileName}
              className={fileName === currentFileName ? "active" : ""}
              onClick={() => void openLevel(fileName)}
            >
              {fileName}
            </button>
          ))}
        </div>
      </aside>

      <main className="canvas-column">
        <div className="toolbar">
          <button className={tool === "select" ? "active" : ""} onClick={() => setTool("select")}>Select</button>
          <button className={tool === "base" ? "active" : ""} onClick={() => setTool("base")}>Place Base</button>
          <button className={tool === "obstacle" ? "active" : ""} onClick={() => setTool("obstacle")}>Place Obstacle</button>
        </div>

        <div ref={stageCardRef} className="stage-card">
          <div className="phone-frame" style={{ width: stageWidth + 44 }}>
            <div className="phone-top">
              <span className="phone-speaker" />
              <span className="phone-camera" />
            </div>
            <div className="phone-screen" style={{ width: stageWidth, height: stageHeight }}>
              <Stage
                ref={(node) => {
                  stageRef.current = node;
                }}
                width={stageWidth}
                height={stageHeight}
                onMouseDown={(event) => {
                  if (event.target === event.target.getStage()) {
                    handleStageClick();
                  }
                }}
              >
                <Layer>
                  <Circle
                    x={stageWidth / 2}
                    y={stageHeight / 2}
                    radius={Math.min(stageWidth, stageHeight) / 2.1}
                    fill="rgba(15,32,46,0.28)"
                    listening={false}
                  />
                  {level.obstacles.map((obstacle, index) => (
                    <Circle
                      key={`obstacle-${index}`}
                      x={toScreen(obstacle.x, scale)}
                      y={toScreen(obstacle.y, scale)}
                      radius={obstacle.radius * scale}
                      fill="rgba(44,61,76,0.88)"
                      stroke={selection?.kind === "obstacle" && selection.index === index ? "#f0c96d" : "#7f9ab1"}
                      strokeWidth={selection?.kind === "obstacle" && selection.index === index ? 4 : 2}
                      draggable
                      onClick={() => setSelection({ kind: "obstacle", index })}
                      onDragMove={(event) => {
                        updateObstacle(index, {
                          x: roundToWorld(event.target.x() / scale),
                          y: roundToWorld(event.target.y() / scale)
                        });
                      }}
                      onDragEnd={(event) => {
                        updateObstacle(index, {
                          x: roundToWorld(event.target.x() / scale),
                          y: roundToWorld(event.target.y() / scale)
                        });
                      }}
                    />
                  ))}
                  {level.bases.map((base) =>
                    base.type === "FAST" ? (
                      <RegularPolygon
                        key={base.id}
                        x={toScreen(base.x, scale)}
                        y={toScreen(base.y, scale)}
                        sides={4}
                        radius={radiusForLevel(base.capLevel) * scale}
                        rotation={45}
                        fill={ownerFill(base.owner)}
                        stroke={selection?.kind === "base" && selection.id === base.id ? "#f0c96d" : "#d7e6f2"}
                        strokeWidth={selection?.kind === "base" && selection.id === base.id ? 4 : 2}
                        draggable
                        onClick={() => setSelection({ kind: "base", id: base.id })}
                        onDragMove={(event) => {
                          updateBase(base.id, {
                            x: roundToWorld(event.target.x() / scale),
                            y: roundToWorld(event.target.y() / scale)
                          });
                        }}
                        onDragEnd={(event) => {
                          updateBase(base.id, {
                            x: roundToWorld(event.target.x() / scale),
                            y: roundToWorld(event.target.y() / scale)
                          });
                        }}
                      />
                    ) : (
                      <Circle
                        key={base.id}
                        x={toScreen(base.x, scale)}
                        y={toScreen(base.y, scale)}
                        radius={radiusForLevel(base.capLevel) * scale}
                        fill={ownerFill(base.owner)}
                        stroke={selection?.kind === "base" && selection.id === base.id ? "#f0c96d" : "#d7e6f2"}
                        strokeWidth={selection?.kind === "base" && selection.id === base.id ? 4 : 2}
                        draggable
                        onClick={() => setSelection({ kind: "base", id: base.id })}
                        onDragMove={(event) => {
                          updateBase(base.id, {
                            x: roundToWorld(event.target.x() / scale),
                            y: roundToWorld(event.target.y() / scale)
                          });
                        }}
                        onDragEnd={(event) => {
                          updateBase(base.id, {
                            x: roundToWorld(event.target.x() / scale),
                            y: roundToWorld(event.target.y() / scale)
                          });
                        }}
                      />
                    )
                  )}
                  {level.bases.map((base) => (
                    <Text
                      key={`label-${base.id}`}
                      x={toScreen(base.x, scale) - 24}
                      y={toScreen(base.y, scale) - 8}
                      width={48}
                      align="center"
                      text={String(base.units)}
                      fill="#041018"
                      fontStyle="bold"
                      listening={false}
                    />
                  ))}
                </Layer>
              </Stage>
              <div className="screen-boundary-label">Playable phone screen</div>
            </div>
            <div className="phone-home-indicator" />
          </div>
        </div>

        <div className="status-bar">
          <span>{status}</span>
          <span>{errors.length === 0 ? "Ready to save" : `${errors.length} validation issue(s)`}</span>
        </div>
      </main>

      <aside className="panel right">
        {selectedBase && (
          <>
            <h2>Base Properties</h2>
            <div className="property-grid">
              <label>
                Base Id
                <input value={selectedBase.id} disabled />
              </label>
              <label>
                Owner
                <select value={selectedBase.owner} onChange={(event) => updateBase(selectedBase.id, { owner: event.target.value as LevelBase["owner"] })}>
                  {OWNERS.map((owner) => (
                    <option key={owner} value={owner}>
                      {owner}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Type
                <select value={selectedBase.type} onChange={(event) => updateBase(selectedBase.id, { type: event.target.value as LevelBase["type"] })}>
                  {BASE_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Units
                <input type="number" value={selectedBase.units} onChange={(event) => updateBase(selectedBase.id, { units: Number(event.target.value) })} />
              </label>
              <label>
                Cap Level
                <input
                  type="number"
                  min={1}
                  value={selectedBase.capLevel}
                  onChange={(event) => {
                    const nextCapLevel = Math.max(1, Math.round(Number(event.target.value) || 1));
                    updateBase(selectedBase.id, { capLevel: nextCapLevel });
                  }}
                />
              </label>
              <label>
                Max Level
                <input
                  type="number"
                  min={1}
                  value={selectedBase.maxLevel}
                  onChange={(event) => updateBase(selectedBase.id, { maxLevel: Math.max(1, Math.round(Number(event.target.value) || 1)) })}
                />
              </label>
              <label>
                X
                <input type="number" value={selectedBase.x} onChange={(event) => updateBase(selectedBase.id, { x: Number(event.target.value) })} />
              </label>
              <label>
                Y
                <input type="number" value={selectedBase.y} onChange={(event) => updateBase(selectedBase.id, { y: Number(event.target.value) })} />
              </label>
            </div>
            <div className="property-actions">
              <button onClick={() => deleteSelectedBase(selectedBase.id)}>Delete Base</button>
            </div>
          </>
        )}

        {selectedObstacle && (
          <>
            <h2>Obstacle Properties</h2>
            <div className="property-grid">
              <label>
                X
                <input type="number" value={selectedObstacle.x} onChange={(event) => updateObstacle(selectedObstacleIndex!, { x: Number(event.target.value) })} />
              </label>
              <label>
                Y
                <input type="number" value={selectedObstacle.y} onChange={(event) => updateObstacle(selectedObstacleIndex!, { y: Number(event.target.value) })} />
              </label>
              <label>
                Radius
                <input type="number" value={selectedObstacle.radius} onChange={(event) => updateObstacle(selectedObstacleIndex!, { radius: Number(event.target.value) })} />
              </label>
            </div>
            <div className="property-actions">
              <button onClick={() => deleteSelectedObstacle(selectedObstacleIndex!)}>Delete Obstacle</button>
            </div>
          </>
        )}

        {!selectedBase && !selectedObstacle && tool === "base" && (
          <>
            <h2>New Base</h2>
            <p className="hint">Choose the defaults for the next base you place on the phone screen.</p>
            <div className="property-grid">
              <label>
                Type
                <select value={placementBaseType} onChange={(event) => setPlacementBaseType(event.target.value as LevelBase["type"])}>
                  {BASE_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Owner
                <select value={placementOwner} onChange={(event) => setPlacementOwner(event.target.value as LevelBase["owner"])}>
                  {OWNERS.map((owner) => (
                    <option key={owner} value={owner}>
                      {owner}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </>
        )}

        {!selectedBase && !selectedObstacle && tool === "obstacle" && (
          <>
            <h2>New Obstacle</h2>
            <p className="hint">Choose the defaults for the next obstacle you place on the phone screen.</p>
            <div className="property-grid">
              <label>
                Radius
                <input
                  type="number"
                  min={10}
                  value={placementObstacleRadius}
                  onChange={(event) => setPlacementObstacleRadius(Number(event.target.value))}
                />
              </label>
            </div>
          </>
        )}

        {!selectedBase && !selectedObstacle && tool === "select" && (
          <>
            <h2>Level Metadata</h2>
            <p className="hint">Select a node to edit its properties. Level metadata and AI setup are shown only when nothing is selected.</p>
            <div className="field-grid">
              <label>
                File Name
                <input value={currentFileName} onChange={(event) => setCurrentFileName(event.target.value)} placeholder="level_3.json" />
              </label>
              <label>
                Level Id
                <input type="number" value={level.levelId} onChange={(event) => updateLevel({ levelId: Number(event.target.value) })} />
              </label>
              <label>
                Name
                <input value={level.name} onChange={(event) => updateLevel({ name: event.target.value })} />
              </label>
              <label>
                Description
                <textarea value={level.description} onChange={(event) => updateLevel({ description: event.target.value })} />
              </label>
              <label>
                Sort Order
                <input type="number" value={level.sortOrder} onChange={(event) => updateLevel({ sortOrder: Number(event.target.value) })} />
              </label>
              <label>
                Unlock After Level
                <input
                  type="number"
                  value={level.unlockAfterLevelId ?? ""}
                  onChange={(event) =>
                    updateLevel({
                      unlockAfterLevelId: event.target.value === "" ? null : Number(event.target.value)
                    })
                  }
                />
              </label>
              <label>
                Two-Star Time (s)
                <input
                  type="number"
                  min={1}
                  max={MAX_STAR_TIME_SECONDS}
                  value={level.twoStarTimeSeconds}
                  onChange={(event) => updateLevel({ twoStarTimeSeconds: Number(event.target.value) })}
                />
              </label>
              <label>
                Three-Star Time (s)
                <input
                  type="number"
                  min={1}
                  max={MAX_STAR_TIME_SECONDS}
                  value={level.threeStarTimeSeconds}
                  onChange={(event) => updateLevel({ threeStarTimeSeconds: Number(event.target.value) })}
                />
              </label>
              <label>
                Intro Message
                <textarea value={level.introMessage} onChange={(event) => updateLevel({ introMessage: event.target.value })} />
              </label>
            </div>
            <h3>AI Controllers</h3>
            <div className="property-grid">
              {level.aiControllers.map((controller, index) => (
                <div key={`${controller.owner}-${index}`} className="ai-controller-card">
                  <label>
                    AI Owner
                    <select value={controller.owner} onChange={(event) => updateAiController(index, { owner: event.target.value as LevelAiController["owner"] })}>
                      {AI_OWNERS.map((owner) => (
                        <option key={owner} value={owner}>
                          {owner}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    AI Type
                    <select value={controller.type} onChange={(event) => updateAiController(index, { type: event.target.value as LevelAiController["type"] })}>
                      {AI_TYPES.map((type) => (
                        <option key={type} value={type}>
                          {type}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button type="button" onClick={() => deleteAiController(index)}>Delete AI</button>
                </div>
              ))}
            </div>
            <div className="property-actions">
              <button type="button" onClick={addAiController}>Add AI Controller</button>
            </div>
          </>
        )}

        {errors.length > 0 && (
          <>
            <h3>Validation</h3>
            <div className="error-list">
              {errors.map((error) => (
                <div key={error} className="error-item">
                  {error}
                </div>
              ))}
            </div>
          </>
        )}
      </aside>
    </div>
  );

  function updateLevel(patch: Partial<LevelDocument>): void {
    setLevel((current) => normalizeLevel({ ...current, ...patch }));
  }

  function updateBase(baseId: number, patch: Partial<LevelBase>): void {
    setLevel((current) =>
      normalizeLevel({
        ...current,
        bases: current.bases.map((base) => (base.id === baseId ? { ...base, ...patch } : base))
      })
    );
  }

  function updateObstacle(index: number, patch: Partial<LevelObstacle>): void {
    setLevel((current) =>
      normalizeLevel({
        ...current,
        obstacles: current.obstacles.map((obstacle, currentIndex) =>
          currentIndex === index ? { ...obstacle, ...patch } : obstacle
        )
      })
    );
  }
}

async function refreshFiles(
  setFileNames: (names: string[]) => void,
  setStatus: (status: string) => void
): Promise<void> {
  try {
    setFileNames(await fetchLevelFileNames());
  } catch (error) {
    setFileNames([]);
    setStatus(readErrorMessage(error, "Failed to load the packaged levels."));
  }
}

function nextBaseId(bases: LevelBase[]): number {
  return bases.reduce((max, base) => Math.max(max, base.id), 0) + 1;
}

function nextUnusedLevelId(fileNames: string[], currentFileName: string | null, fallback: number): number {
  const ids = fileNames
    .filter((name) => name !== currentFileName)
    .map((name) => Number(name.replace(/\D/g, "")))
    .filter((value) => Number.isFinite(value) && value > 0);
  let nextId = Math.max(fallback, ...ids, 0);
  while (ids.includes(nextId)) {
    nextId += 1;
  }
  return nextId;
}

function toScreen(value: number, scale: number): number {
  return value * scale;
}

function normalizeFileName(fileName: string): string {
  return fileName.endsWith(".json") ? fileName : `${fileName}.json`;
}

function fetchAndParseLevel(level: LevelDocument): LevelDocument {
  return parseLevelDocument(formatLevelFile(level));
}

function roundToWorld(value: number): number {
  return Math.round(value);
}

function readErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

function ownerFill(owner: LevelBase["owner"]): string {
  switch (owner) {
    case "PLAYER":
      return "#59d0ff";
    case "AI_1":
      return "#ff7868";
    case "AI_2":
      return "#ffb347";
    case "AI_3":
      return "#be8cff";
    case "AI_4":
      return "#5de2a5";
    default:
      return "#8999a8";
  }
}
