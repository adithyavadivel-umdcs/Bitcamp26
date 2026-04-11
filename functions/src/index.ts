import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import {
  DEFAULT_MAP_RADIUS_METERS,
  SHRINK_GRACE_METERS,
  STALE_LOCATION_MS,
  catchEligibilityRadiusMeters,
  defaultConfig,
  headStartSeconds,
  hiderInvisibilityDurationSeconds,
  hotspotCount,
  hotspotRadiusMeters,
  minimumPlayableRadiusMeters,
  seekerRevealAllDurationSeconds,
  shrinkIntervalSeconds,
  shrinkStepMeters,
} from "./gameBalance";

admin.initializeApp();
const db = admin.database();

type LobbyState = "WAITING" | "RUNNING" | "ENDED";
type PlayerRole = "SEEKER" | "HIDER";
type PowerupType = "NONE" | "HIDER_INVISIBILITY" | "SEEKER_REVEAL_ALL";
type Winner = "SEEKER" | "HIDERS";

export type LobbyPlayer = {
  userId: string;
  displayName: string;
  role: PlayerRole;
  alive: boolean;
  connected: boolean;
  ready: boolean;
  lat: number;
  lng: number;
  accuracyMeters: number;
  lastLocationAt: number;
  stepsAtGameStart: number;
  currentMatchSteps: number;
  activePowerup: PowerupType;
  powerupEndsAt: number | null;
  catchCode: string | null;
  catchEligible: boolean;
  eliminatedAt?: number;
};

type Lobby = {
  hostId: string;
  inviteCode: string;
  state: LobbyState;
  createdAt: number;
  mapCenterLat: number;
  mapCenterLng: number;
  initialRadiusMeters: number;
  currentRadiusMeters: number;
  seekerId: string;
  config: Record<string, number>;
  timing?: {
    gameStartAt: number;
    nextShrinkAt: number;
  };
  results?: {
    winner: Winner;
    endedAt: number;
  };
};

type Hotspot = {
  id: string;
  lat: number;
  lng: number;
  radiusMeters: number;
  active: boolean;
  claimedBy: string | null;
  claimedAt: number | null;
  powerupType: PowerupType;
};

type StartGamePlan = {
  playerUpdates: Record<string, unknown>;
  hotspots: Record<string, Hotspot>;
  gameStartAt: number;
  nextShrinkAt: number;
};

type CatchResult =
  | { success: true; targetId: string }
  | { success: false; reason: "NO_ELIGIBLE_MATCH" };

type HotspotClaimResult = {
  hotspot: Hotspot;
  grantedPowerup: PowerupType;
};

type PowerupActivationResult = {
  powerup: PowerupType;
  powerupEndsAt: number;
  durationSeconds: number;
};

export function distanceMeters(
  aLat: number,
  aLng: number,
  bLat: number,
  bLng: number
): number {
  const earthRadius = 6_371_000;
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(bLat - aLat);
  const dLng = toRad(bLng - aLng);
  const sinHalfDLat = Math.sin(dLat / 2);
  const sinHalfDLng = Math.sin(dLng / 2);
  const a =
    sinHalfDLat * sinHalfDLat +
    sinHalfDLng * sinHalfDLng * Math.cos(toRad(aLat)) * Math.cos(toRad(bLat));
  return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function findEligibleTarget(
  players: Record<string, LobbyPlayer>,
  code: string
): string | null {
  const normalized = code.trim().toUpperCase();
  const entry = Object.entries(players).find(
    ([, player]) =>
      player.role === "HIDER" &&
      player.alive &&
      player.catchEligible &&
      player.catchCode === normalized
  );
  return entry ? entry[0] : null;
}

export function computeShrinkRadius(
  currentRadius: number,
  initialRadius: number
): number {
  const step = shrinkStepMeters(initialRadius);
  const floor = minimumPlayableRadiusMeters(initialRadius);
  return Math.max(currentRadius - step, floor);
}

export function findOobPlayers(
  players: Record<string, LobbyPlayer>,
  centerLat: number,
  centerLng: number,
  newRadius: number,
  graceMeters: number,
  staleThresholdMs: number,
  now: number
): string[] {
  const outOfBounds: string[] = [];
  for (const [id, player] of Object.entries(players)) {
    if (!player.alive) continue;
    if (now - player.lastLocationAt > staleThresholdMs) continue;
    const dist = distanceMeters(centerLat, centerLng, player.lat, player.lng);
    if (dist > newRadius + graceMeters) {
      outOfBounds.push(id);
    }
  }
  return outOfBounds;
}

export function resolveWinner(
  players: Record<string, LobbyPlayer>
): Winner | null {
  const aliveHiders = Object.values(players).filter(
    (player) => player.role === "HIDER" && player.alive
  );
  const seeker = Object.values(players).find((player) => player.role === "SEEKER");
  if (seeker && !seeker.alive) return "HIDERS";
  if (aliveHiders.length === 0) return "SEEKER";
  return null;
}

const CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

function generateCode(): string {
  let value = "";
  for (let i = 0; i < 6; i++) {
    value += CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)];
  }
  return value;
}

function generateUniqueCatchCodes(count: number): string[] {
  const codes = new Set<string>();
  while (codes.size < count) {
    codes.add(generateCode());
  }
  return Array.from(codes);
}

function generateHotspots(
  centerLat: number,
  centerLng: number,
  mapRadius: number,
  count: number,
  hotspotRadius: number
): Hotspot[] {
  const types: PowerupType[] = ["HIDER_INVISIBILITY", "SEEKER_REVEAL_ALL"];
  return Array.from({ length: count }, (_, index) => {
    const r = mapRadius * 0.8 * Math.sqrt(Math.random());
    const theta = Math.random() * 2 * Math.PI;
    const lat = centerLat + (r / 111_111) * Math.cos(theta);
    const lng =
      centerLng +
      (r / (111_111 * Math.cos((centerLat * Math.PI) / 180))) * Math.sin(theta);
    return {
      id: `hs_${index}`,
      lat,
      lng,
      radiusMeters: hotspotRadius,
      active: true,
      claimedBy: null,
      claimedAt: null,
      powerupType: types[index % types.length],
    };
  });
}

export function buildLobbyPlayer(
  uid: string,
  displayName: string,
  role: PlayerRole,
  now: number
): LobbyPlayer {
  return {
    userId: uid,
    displayName,
    role,
    alive: true,
    connected: true,
    ready: false,
    lat: 0,
    lng: 0,
    accuracyMeters: 0,
    lastLocationAt: now,
    stepsAtGameStart: 0,
    currentMatchSteps: 0,
    activePowerup: "NONE",
    powerupEndsAt: null,
    catchCode: null,
    catchEligible: false,
  };
}

export function buildCreateLobbyState(
  uid: string,
  displayName: string,
  mapCenterLat = 0,
  mapCenterLng = 0,
  initialRadiusMeters = DEFAULT_MAP_RADIUS_METERS,
  now = Date.now()
): { lobby: Lobby; hostPlayer: LobbyPlayer } {
  return {
    lobby: {
      hostId: uid,
      inviteCode: "",
      state: "WAITING",
      createdAt: now,
      mapCenterLat,
      mapCenterLng,
      initialRadiusMeters,
      currentRadiusMeters: initialRadiusMeters,
      seekerId: uid,
      config: defaultConfig(initialRadiusMeters),
    },
    hostPlayer: buildLobbyPlayer(uid, displayName, "SEEKER", now),
  };
}

export function prepareStartGame(
  lobby: Lobby,
  players: Record<string, LobbyPlayer>,
  now = Date.now(),
  generatedHotspots?: Hotspot[]
): StartGamePlan {
  const playerEntries = Object.entries(players);
  if (playerEntries.length < 2) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Need at least 2 players to start"
    );
  }
  if (playerEntries.some(([, player]) => !player.ready)) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "All players must be ready before the game can start"
    );
  }

  const hiders = playerEntries.filter(([, player]) => player.role === "HIDER");
  const hasSeeker = playerEntries.some(([, player]) => player.role === "SEEKER");
  if (!hasSeeker || hiders.length === 0) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Need at least one SEEKER and one HIDER"
    );
  }

  const catchCodes = generateUniqueCatchCodes(hiders.length);
  const playerUpdates: Record<string, unknown> = {};
  let hiderIndex = 0;
  for (const [playerId, player] of playerEntries) {
    playerUpdates[`${playerId}/alive`] = true;
    playerUpdates[`${playerId}/catchEligible`] = false;
    playerUpdates[`${playerId}/powerupEndsAt`] = null;
    playerUpdates[`${playerId}/activePowerup`] = "NONE";
    if (player.role === "HIDER") {
      playerUpdates[`${playerId}/catchCode`] = catchCodes[hiderIndex++];
    } else {
      playerUpdates[`${playerId}/catchCode`] = null;
    }
  }

  const radius = lobby.currentRadiusMeters || lobby.initialRadiusMeters;
  const hotspots = generatedHotspots ?? generateHotspots(
    lobby.mapCenterLat,
    lobby.mapCenterLng,
    radius,
    hotspotCount(radius),
    hotspotRadiusMeters(radius)
  );
  const hotspotRecord: Record<string, Hotspot> = {};
  for (const hotspot of hotspots) {
    hotspotRecord[hotspot.id] = hotspot;
  }

  const gameStartAt = now + headStartSeconds(radius) * 1000;
  const nextShrinkAt = gameStartAt + shrinkIntervalSeconds(radius) * 1000;
  return { playerUpdates, hotspots: hotspotRecord, gameStartAt, nextShrinkAt };
}

export function evaluateCatchAttempt(
  players: Record<string, LobbyPlayer>,
  code: string
): CatchResult {
  const targetId = findEligibleTarget(players, code);
  if (!targetId) {
    return { success: false, reason: "NO_ELIGIBLE_MATCH" };
  }
  return { success: true, targetId };
}

export function resolveHotspotClaimAttempt(
  player: LobbyPlayer,
  hotspot: Hotspot,
  now = Date.now()
): HotspotClaimResult {
  if (!hotspot.active) {
    throw new functions.https.HttpsError("aborted", "Hotspot already claimed");
  }
  const dist = distanceMeters(player.lat, player.lng, hotspot.lat, hotspot.lng);
  if (dist > hotspot.radiusMeters) {
    throw new functions.https.HttpsError(
      "aborted",
      "Hotspot already claimed or player not in range"
    );
  }
  return {
    hotspot: {
      ...hotspot,
      active: false,
      claimedBy: player.userId,
      claimedAt: now,
    },
    grantedPowerup: hotspot.powerupType ?? "NONE",
  };
}

export function resolvePowerupActivation(
  lobby: Lobby,
  player: LobbyPlayer,
  now = Date.now()
): PowerupActivationResult {
  if (!player.activePowerup || player.activePowerup === "NONE") {
    throw new functions.https.HttpsError("failed-precondition", "No powerup to activate");
  }
  if (player.powerupEndsAt !== null) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Powerup already activated — claim a new hotspot to get another"
    );
  }
  if (player.activePowerup === "HIDER_INVISIBILITY" && player.role !== "HIDER") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Only hiders can use HIDER_INVISIBILITY"
    );
  }
  if (player.activePowerup === "SEEKER_REVEAL_ALL" && player.role !== "SEEKER") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Only the seeker can use SEEKER_REVEAL_ALL"
    );
  }

  const radius = lobby.currentRadiusMeters || lobby.initialRadiusMeters;
  const durationSeconds =
    player.activePowerup === "HIDER_INVISIBILITY"
      ? hiderInvisibilityDurationSeconds(radius)
      : seekerRevealAllDurationSeconds(radius);
  return {
    powerup: player.activePowerup,
    powerupEndsAt: now + durationSeconds * 1000,
    durationSeconds,
  };
}

function clearExpiredPowerups(
  players: Record<string, LobbyPlayer>,
  now = Date.now()
): Record<string, unknown> {
  const updates: Record<string, unknown> = {};
  for (const [playerId, player] of Object.entries(players)) {
    if (
      player.activePowerup !== "NONE" &&
      player.powerupEndsAt !== null &&
      player.powerupEndsAt <= now
    ) {
      updates[`${playerId}/activePowerup`] = "NONE";
      updates[`${playerId}/powerupEndsAt`] = null;
    }
  }
  return updates;
}

async function checkAndWriteWin(
  lobbyId: string,
  players: Record<string, LobbyPlayer>
): Promise<boolean> {
  const winner = resolveWinner(players);
  if (!winner) return false;
  await db.ref(`lobbies/${lobbyId}`).update({
    state: "ENDED",
    "results/winner": winner,
    "results/endedAt": Date.now(),
  });
  return true;
}

export const createLobby = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth");
  }

  const {
    displayName,
    mapCenterLat = 0,
    mapCenterLng = 0,
    initialRadiusMeters = DEFAULT_MAP_RADIUS_METERS,
  } = data as {
    displayName: string;
    mapCenterLat?: number;
    mapCenterLng?: number;
    initialRadiusMeters?: number;
  };

  if (!displayName || typeof displayName !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "displayName required");
  }

  let inviteCode = generateCode();
  for (let attempt = 0; attempt < 5; attempt++) {
    const existing = await db.ref(`lobbies/${inviteCode}`).once("value");
    if (!existing.exists()) break;
    inviteCode = generateCode();
  }

  const { lobby, hostPlayer } = buildCreateLobbyState(
    uid,
    displayName,
    mapCenterLat,
    mapCenterLng,
    initialRadiusMeters,
    Date.now()
  );
  lobby.inviteCode = inviteCode;

  await db.ref(`lobbies/${inviteCode}`).set(lobby);
  await db.ref(`lobbies/${inviteCode}/players/${uid}`).set(hostPlayer);
  return { lobbyId: inviteCode, inviteCode };
});

export const joinLobby = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth");
  }

  const { inviteCode, displayName } = data as { inviteCode: string; displayName: string };
  if (!inviteCode || !displayName) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "inviteCode and displayName required"
    );
  }

  const normalizedCode = inviteCode.trim().toUpperCase();
  const lobbySnap = await db.ref(`lobbies/${normalizedCode}`).once("value");
  if (!lobbySnap.exists()) {
    throw new functions.https.HttpsError("not-found", "Lobby not found");
  }
  const lobby = lobbySnap.val() as Lobby;
  if (lobby.state !== "WAITING") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Lobby is not accepting new players"
    );
  }

  const playerRef = db.ref(`lobbies/${normalizedCode}/players/${uid}`);
  const existingSnap = await playerRef.once("value");
  if (existingSnap.exists()) {
    await playerRef.update({ displayName, connected: true });
    const existing = existingSnap.val() as LobbyPlayer;
    return { lobbyId: normalizedCode, role: existing.role };
  }

  const player = buildLobbyPlayer(uid, displayName, "HIDER", Date.now());
  await playerRef.set(player);
  return { lobbyId: normalizedCode, role: "HIDER" };
});

export const startGame = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth");
  }

  const { lobbyId } = data as { lobbyId: string };
  if (!lobbyId) {
    throw new functions.https.HttpsError("invalid-argument", "lobbyId required");
  }

  const lobbySnap = await db.ref(`lobbies/${lobbyId}`).once("value");
  if (!lobbySnap.exists()) {
    throw new functions.https.HttpsError("not-found", "Lobby not found");
  }
  const lobby = lobbySnap.val() as Lobby;
  if (lobby.hostId !== uid) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only the host can start the game"
    );
  }
  if (lobby.state !== "WAITING") {
    throw new functions.https.HttpsError("failed-precondition", "Game has already started");
  }

  const playersSnap = await db.ref(`lobbies/${lobbyId}/players`).once("value");
  const players = (playersSnap.val() ?? {}) as Record<string, LobbyPlayer>;
  const plan = prepareStartGame(lobby, players, Date.now());

  await db.ref(`lobbies/${lobbyId}/players`).update(plan.playerUpdates);
  await db.ref(`lobbies/${lobbyId}/hotspots`).set(plan.hotspots);
  await db.ref(`lobbies/${lobbyId}`).update({
    state: "RUNNING",
    currentRadiusMeters: lobby.currentRadiusMeters || lobby.initialRadiusMeters,
    "timing/gameStartAt": plan.gameStartAt,
    "timing/nextShrinkAt": plan.nextShrinkAt,
  });

  return { success: true, gameStartAt: plan.gameStartAt, nextShrinkAt: plan.nextShrinkAt };
});

export const submitCatchCode = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth");
  }

  const { lobbyId, code } = data as { lobbyId: string; code: string };
  if (!lobbyId || !code) {
    throw new functions.https.HttpsError("invalid-argument", "lobbyId and code required");
  }

  const lobbySnap = await db.ref(`lobbies/${lobbyId}`).once("value");
  const lobby = lobbySnap.val() as Lobby | null;
  if (!lobby) {
    throw new functions.https.HttpsError("not-found", "Lobby not found");
  }
  if (lobby.state !== "RUNNING") {
    throw new functions.https.HttpsError("failed-precondition", "Game is not running");
  }

  const now = Date.now();
  if (lobby.timing && now < lobby.timing.gameStartAt) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Head start still active — seeker is frozen"
    );
  }

  const playersSnap = await db.ref(`lobbies/${lobbyId}/players`).once("value");
  const players = (playersSnap.val() ?? {}) as Record<string, LobbyPlayer>;
  const seeker = players[uid];
  if (!seeker || seeker.role !== "SEEKER" || !seeker.alive) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Only the active seeker can submit catch codes"
    );
  }

  const catchResult = evaluateCatchAttempt(players, code);
  if (!catchResult.success) {
    return catchResult;
  }

  await db.ref(`lobbies/${lobbyId}/players/${catchResult.targetId}`).update({
    alive: false,
    catchEligible: false,
    eliminatedAt: now,
  });

  const updatedPlayers = {
    ...players,
    [catchResult.targetId]: {
      ...players[catchResult.targetId],
      alive: false,
      catchEligible: false,
      eliminatedAt: now,
    },
  };
  await checkAndWriteWin(lobbyId, updatedPlayers);
  return { success: true, eliminatedUserId: catchResult.targetId };
});

export const shrinkCircle = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth");
  }

  const { lobbyId } = data as { lobbyId: string };
  if (!lobbyId) {
    throw new functions.https.HttpsError("invalid-argument", "lobbyId required");
  }

  const lobbySnap = await db.ref(`lobbies/${lobbyId}`).once("value");
  const lobby = lobbySnap.val() as Lobby | null;
  if (!lobby) {
    throw new functions.https.HttpsError("not-found", "Lobby not found");
  }
  if (lobby.hostId !== uid) {
    throw new functions.https.HttpsError("permission-denied", "Only host can trigger shrink");
  }
  if (lobby.state !== "RUNNING") {
    throw new functions.https.HttpsError("failed-precondition", "Game is not running");
  }
  if (lobby.timing?.nextShrinkAt && Date.now() < lobby.timing.nextShrinkAt) {
    throw new functions.https.HttpsError("failed-precondition", "Shrink is not due yet");
  }

  const playersSnap = await db.ref(`lobbies/${lobbyId}/players`).once("value");
  const players = (playersSnap.val() ?? {}) as Record<string, LobbyPlayer>;
  const now = Date.now();
  const newRadius = computeShrinkRadius(
    lobby.currentRadiusMeters,
    lobby.initialRadiusMeters
  );
  const eliminatedIds = findOobPlayers(
    players,
    lobby.mapCenterLat,
    lobby.mapCenterLng,
    newRadius,
    SHRINK_GRACE_METERS,
    STALE_LOCATION_MS,
    now
  );

  await db.ref(`lobbies/${lobbyId}`).update({
    currentRadiusMeters: newRadius,
    "timing/nextShrinkAt": now + shrinkIntervalSeconds(lobby.initialRadiusMeters) * 1000,
  });

  if (eliminatedIds.length > 0) {
    const eliminationUpdates: Record<string, unknown> = {};
    for (const playerId of eliminatedIds) {
      eliminationUpdates[`${playerId}/alive`] = false;
      eliminationUpdates[`${playerId}/catchEligible`] = false;
      eliminationUpdates[`${playerId}/eliminatedAt`] = now;
    }
    await db.ref(`lobbies/${lobbyId}/players`).update(eliminationUpdates);
  }

  const updatedPlayers = { ...players };
  for (const playerId of eliminatedIds) {
    updatedPlayers[playerId] = {
      ...updatedPlayers[playerId],
      alive: false,
      catchEligible: false,
      eliminatedAt: now,
    };
  }
  const gameEnded = await checkAndWriteWin(lobbyId, updatedPlayers);
  return { success: true, newRadiusMeters: newRadius, eliminated: eliminatedIds, gameEnded };
});

export const claimHotspot = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth");
  }

  const { lobbyId, hotspotId } = data as { lobbyId: string; hotspotId: string };
  if (!lobbyId || !hotspotId) {
    throw new functions.https.HttpsError("invalid-argument", "lobbyId and hotspotId required");
  }

  const lobbySnap = await db.ref(`lobbies/${lobbyId}`).once("value");
  const lobby = lobbySnap.val() as Lobby | null;
  if (!lobby) {
    throw new functions.https.HttpsError("not-found", "Lobby not found");
  }
  if (lobby.state !== "RUNNING") {
    throw new functions.https.HttpsError("failed-precondition", "Game is not running");
  }

  const playerSnap = await db.ref(`lobbies/${lobbyId}/players/${uid}`).once("value");
  const player = playerSnap.val() as LobbyPlayer | null;
  if (!player || !player.alive) {
    throw new functions.https.HttpsError("failed-precondition", "Player not active");
  }

  const hotspotRef = db.ref(`lobbies/${lobbyId}/hotspots/${hotspotId}`);
  const transaction = await hotspotRef.transaction((current: Hotspot | null) => {
    if (!current) return current;
    try {
      const resolved = resolveHotspotClaimAttempt(player, current, Date.now());
      return resolved.hotspot;
    } catch {
      return undefined;
    }
  });

  if (!transaction.committed) {
    throw new functions.https.HttpsError(
      "aborted",
      "Hotspot already claimed or player not in range"
    );
  }

  const claimed = transaction.snapshot.val() as Hotspot;
  await db.ref(`lobbies/${lobbyId}/players/${uid}`).update({
    activePowerup: claimed.powerupType ?? "NONE",
    powerupEndsAt: null,
  });

  return { success: true, powerupType: claimed.powerupType ?? "NONE" };
});

export const activatePowerup = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth");
  }

  const { lobbyId } = data as { lobbyId: string };
  if (!lobbyId) {
    throw new functions.https.HttpsError("invalid-argument", "lobbyId required");
  }

  const lobbySnap = await db.ref(`lobbies/${lobbyId}`).once("value");
  const lobby = lobbySnap.val() as Lobby | null;
  if (!lobby) {
    throw new functions.https.HttpsError("not-found", "Lobby not found");
  }
  if (lobby.state !== "RUNNING") {
    throw new functions.https.HttpsError("failed-precondition", "Game is not running");
  }

  const playerSnap = await db.ref(`lobbies/${lobbyId}/players/${uid}`).once("value");
  const player = playerSnap.val() as LobbyPlayer | null;
  if (!player || !player.alive) {
    throw new functions.https.HttpsError("failed-precondition", "Player not active");
  }

  const result = resolvePowerupActivation(lobby, player, Date.now());
  await db.ref(`lobbies/${lobbyId}/players/${uid}`).update({
    powerupEndsAt: result.powerupEndsAt,
  });

  return {
    success: true,
    powerup: result.powerup,
    powerupEndsAt: result.powerupEndsAt,
    durationSeconds: result.durationSeconds,
  };
});

export const refreshCatchEligibility = functions.database
  .ref("/lobbies/{lobbyId}/players/{playerId}/lastLocationAt")
  .onWrite(async (_, context) => {
    const { lobbyId } = context.params;

    const lobbySnap = await db.ref(`lobbies/${lobbyId}`).once("value");
    const lobby = lobbySnap.val() as Lobby | null;
    if (!lobby || lobby.state !== "RUNNING") {
      return;
    }

    const playersSnap = await db.ref(`lobbies/${lobbyId}/players`).once("value");
    const players = (playersSnap.val() ?? {}) as Record<string, LobbyPlayer>;
    const now = Date.now();

    const expiredUpdates = clearExpiredPowerups(players, now);
    if (Object.keys(expiredUpdates).length > 0) {
      await db.ref(`lobbies/${lobbyId}/players`).update(expiredUpdates);
      for (const [path, value] of Object.entries(expiredUpdates)) {
        const [playerId, field] = path.split("/");
        players[playerId] = { ...players[playerId], [field]: value } as LobbyPlayer;
      }
    }

    const seeker = Object.values(players).find(
      (player) => player.role === "SEEKER" && player.alive
    );
    if (!seeker) {
      return;
    }

    const catchRadius =
      lobby.config?.catchEligibilityRadiusMeters ??
      catchEligibilityRadiusMeters(lobby.currentRadiusMeters);

    const updates: Record<string, boolean> = {};
    for (const [playerId, player] of Object.entries(players)) {
      if (player.role !== "HIDER" || !player.alive) {
        updates[`${playerId}/catchEligible`] = false;
        continue;
      }
      const dist = distanceMeters(seeker.lat, seeker.lng, player.lat, player.lng);
      updates[`${playerId}/catchEligible`] = dist <= catchRadius;
    }

    await db.ref(`lobbies/${lobbyId}/players`).update(updates);
  });
