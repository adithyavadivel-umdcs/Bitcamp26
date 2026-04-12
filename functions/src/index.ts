import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import {
  DEFAULT_MAP_RADIUS_METERS,
  HOTSPOT_DWELL_SECONDS,
  DISCONNECT_STALE_MS,
  HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
  HOTSPOT_SIDE_LENGTH_METERS,
  SHRINK_GRACE_METERS,
  STALE_LOCATION_MS,
  catchEligibilityRadiusMeters,
  defaultConfig,
  headStartSeconds,
  hiderVisionReductionDurationSeconds,
  hotspotCount,
  hotspotRadiusMeters,
  minimumPlayableRadiusMeters,
  seekerMinimapBoostDurationSeconds,
  shrinkIntervalSeconds,
  shrinkStepMeters,
} from "./gameBalance";

if (!admin.apps.length) {
  admin.initializeApp({
    databaseURL:
      process.env.FIREBASE_DATABASE_URL ||
      "https://bitcamp26-default-rtdb.firebaseio.com",
  });
}

const getDb = () => admin.database();

type LobbyState = "WAITING" | "RUNNING" | "ENDED";
type PlayerRole = "SEEKER" | "HIDER" | "UNASSIGNED";
type PowerupType = "NONE" | "HIDER_VISION_REDUCTION" | "SEEKER_MINIMAP_BOOST";
type StoredPowerupType =
  | PowerupType
  | "HIDER_INVISIBILITY"
  | "SEEKER_REVEAL_ALL";
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
  storedPowerupType: StoredPowerupType;
  activePowerup: StoredPowerupType;
  powerupEndsAt: number | null;
  nextPowerupEligibleAt: number;
  currentHotspotId: string | null;
  hotspotEnteredAt: number | null;
  hotspotRewardGrantedForCurrentStay: boolean;
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
  allReady: boolean;
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
  powerupType: StoredPowerupType;
};

type StartGamePlan = {
  playerUpdates: Record<string, unknown>;
  hotspots: Record<string, Hotspot>;
  gameStartAt: number;
  nextShrinkAt: number;
  seekerId: string;
};

type CatchResult =
  | { success: true; targetId: string }
  | { success: false; reason: "NO_ELIGIBLE_MATCH" };

type PowerupActivationResult = {
  powerup: PowerupType;
  powerupEndsAt: number;
  durationSeconds: number;
};

type HotspotProgressResolution = {
  updates: Record<string, unknown>;
  grantedPowerup: PowerupType | null;
  currentHotspotId: string | null;
};

function normalizePowerupType(value: StoredPowerupType | string | null | undefined): PowerupType {
  switch (value) {
    case "HIDER_INVISIBILITY":
    case "HIDER_VISION_REDUCTION":
      return "HIDER_VISION_REDUCTION";
    case "SEEKER_REVEAL_ALL":
    case "SEEKER_MINIMAP_BOOST":
      return "SEEKER_MINIMAP_BOOST";
    default:
      return "NONE";
  }
}

function normalizeStoredPowerup(value: StoredPowerupType | string | null | undefined): PowerupType {
  return normalizePowerupType(value);
}

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

function normalizeLongitudeDelta(delta: number): number {
  let normalized = delta;
  while (normalized > 180) normalized -= 360;
  while (normalized < -180) normalized += 360;
  return normalized;
}

function latitudeDeltaMeters(aLat: number, bLat: number): number {
  return Math.abs(bLat - aLat) * 111_111;
}

function longitudeDeltaMeters(centerLat: number, aLng: number, bLng: number): number {
  const metersPerDegree =
    111_111 * Math.max(Math.cos((centerLat * Math.PI) / 180), 0.000001);
  return Math.abs(normalizeLongitudeDelta(bLng - aLng)) * metersPerDegree;
}

export function isWithinSquareBounds(
  centerLat: number,
  centerLng: number,
  targetLat: number,
  targetLng: number,
  halfWidthMeters: number
): boolean {
  if (halfWidthMeters < 0) {
    return false;
  }

  return (
    latitudeDeltaMeters(centerLat, targetLat) <= halfWidthMeters &&
    longitudeDeltaMeters(centerLat, centerLng, targetLng) <= halfWidthMeters
  );
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
    if (
      !isWithinSquareBounds(
        centerLat,
        centerLng,
        player.lat,
        player.lng,
        newRadius + graceMeters
      )
    ) {
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
  if (!seeker) return null; // roles not yet assigned
  if (!seeker.alive) return "HIDERS";
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
  const types: PowerupType[] = ["HIDER_VISION_REDUCTION", "SEEKER_MINIMAP_BOOST"];
  const metersPerLngDegree =
    111_111 * Math.max(Math.cos((centerLat * Math.PI) / 180), 0.000001);
  const usableHalfWidth = Math.max(0, mapRadius - hotspotRadius);
  const cellsPerSide = Math.floor((usableHalfWidth * 2) / HOTSPOT_SIDE_LENGTH_METERS);
  const candidateCenters: Array<{ xMeters: number; yMeters: number }> = [];

  if (cellsPerSide > 0) {
    const start = -usableHalfWidth + hotspotRadius;
    for (let row = 0; row < cellsPerSide; row++) {
      for (let col = 0; col < cellsPerSide; col++) {
        candidateCenters.push({
          xMeters: start + col * HOTSPOT_SIDE_LENGTH_METERS,
          yMeters: start + row * HOTSPOT_SIDE_LENGTH_METERS,
        });
      }
    }
  }

  for (let index = candidateCenters.length - 1; index > 0; index--) {
    const swapIndex = Math.floor(Math.random() * (index + 1));
    const current = candidateCenters[index];
    candidateCenters[index] = candidateCenters[swapIndex];
    candidateCenters[swapIndex] = current;
  }

  return candidateCenters.slice(0, count).map((candidate, index) => {
    const lat = centerLat + candidate.yMeters / 111_111;
    const lng = centerLng + candidate.xMeters / metersPerLngDegree;
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
  role: PlayerRole = "UNASSIGNED",
  now: number = Date.now()
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
    storedPowerupType: "NONE",
    activePowerup: "NONE",
    powerupEndsAt: null,
    nextPowerupEligibleAt: 0,
    currentHotspotId: null,
    hotspotEnteredAt: null,
    hotspotRewardGrantedForCurrentStay: false,
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
      seekerId: "",
      allReady: false,
      config: defaultConfig(initialRadiusMeters),
    },
    hostPlayer: buildLobbyPlayer(uid, displayName, "UNASSIGNED", now),
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

  // Randomly assign one player as SEEKER, rest are HIDERS
  const seekerIndex = Math.floor(Math.random() * playerEntries.length);
  const seekerId = playerEntries[seekerIndex][0];
  const hiderEntries = playerEntries.filter(([id]) => id !== seekerId);
  const catchCodes = generateUniqueCatchCodes(hiderEntries.length);

  const playerUpdates: Record<string, unknown> = {};
  let hiderIndex = 0;
  for (const [playerId] of playerEntries) {
    const isSeeker = playerId === seekerId;
    playerUpdates[`${playerId}/role`] = isSeeker ? "SEEKER" : "HIDER";
    playerUpdates[`${playerId}/alive`] = true;
    playerUpdates[`${playerId}/catchEligible`] = false;
    playerUpdates[`${playerId}/storedPowerupType`] = "NONE";
    playerUpdates[`${playerId}/powerupEndsAt`] = null;
    playerUpdates[`${playerId}/activePowerup`] = "NONE";
    playerUpdates[`${playerId}/nextPowerupEligibleAt`] = 0;
    playerUpdates[`${playerId}/currentHotspotId`] = null;
    playerUpdates[`${playerId}/hotspotEnteredAt`] = null;
    playerUpdates[`${playerId}/hotspotRewardGrantedForCurrentStay`] = false;
    playerUpdates[`${playerId}/catchCode`] = isSeeker ? null : catchCodes[hiderIndex++];
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
  const nextShrinkAt = gameStartAt + shrinkIntervalSeconds() * 1000;
  return { playerUpdates, hotspots: hotspotRecord, gameStartAt, nextShrinkAt, seekerId };
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

function findContainingHotspot(
  hotspots: Record<string, Hotspot>,
  player: LobbyPlayer
): Hotspot | null {
  const sortedHotspots = Object.values(hotspots).sort((a, b) => a.id.localeCompare(b.id));
  for (const hotspot of sortedHotspots) {
    if (!hotspot.active) {
      continue;
    }
    if (
      isWithinSquareBounds(
        hotspot.lat,
        hotspot.lng,
        player.lat,
        player.lng,
        hotspot.radiusMeters
      )
    ) {
      return hotspot;
    }
  }
  return null;
}

export function resolveHotspotProgressForPlayer(
  player: LobbyPlayer,
  hotspots: Record<string, Hotspot>,
  hotspotDwellSeconds: number,
  personalCooldownSeconds: number,
  now = Date.now()
): HotspotProgressResolution {
  const hotspot = findContainingHotspot(hotspots, player);
  if (!hotspot) {
    return {
      updates: {
        currentHotspotId: null,
        hotspotEnteredAt: null,
        hotspotRewardGrantedForCurrentStay: false,
      },
      grantedPowerup: null,
      currentHotspotId: null,
    };
  }

  if (player.currentHotspotId !== hotspot.id) {
    return {
      updates: {
        currentHotspotId: hotspot.id,
        hotspotEnteredAt: now,
        hotspotRewardGrantedForCurrentStay: false,
      },
      grantedPowerup: null,
      currentHotspotId: hotspot.id,
    };
  }

  if (player.nextPowerupEligibleAt > now || player.hotspotRewardGrantedForCurrentStay) {
    return { updates: {}, grantedPowerup: null, currentHotspotId: hotspot.id };
  }

  if (normalizeStoredPowerup(player.storedPowerupType) !== "NONE") {
    return { updates: {}, grantedPowerup: null, currentHotspotId: hotspot.id };
  }

  const hotspotEnteredAt = player.hotspotEnteredAt ?? now;
  if (now - hotspotEnteredAt < hotspotDwellSeconds * 1000) {
    return { updates: {}, grantedPowerup: null, currentHotspotId: hotspot.id };
  }

  const grantedPowerup = normalizePowerupType(hotspot.powerupType);
  return {
    updates: {
      storedPowerupType: grantedPowerup,
      nextPowerupEligibleAt: now + personalCooldownSeconds * 1000,
      hotspotRewardGrantedForCurrentStay: true,
    },
    grantedPowerup,
    currentHotspotId: hotspot.id,
  };
}

export function resolvePowerupActivation(
  lobby: Lobby,
  player: LobbyPlayer,
  now = Date.now()
): PowerupActivationResult {
  const storedPowerup = normalizeStoredPowerup(player.storedPowerupType);
  const activePowerup = normalizePowerupType(player.activePowerup);
  if (storedPowerup === "NONE") {
    throw new functions.https.HttpsError("failed-precondition", "No powerup to activate");
  }
  if (activePowerup !== "NONE" && player.powerupEndsAt !== null && player.powerupEndsAt > now) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Powerup already activated — claim a new hotspot to get another"
    );
  }
  if (storedPowerup === "HIDER_VISION_REDUCTION" && player.role !== "HIDER") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Only hiders can use HIDER_VISION_REDUCTION"
    );
  }
  if (storedPowerup === "SEEKER_MINIMAP_BOOST" && player.role !== "SEEKER") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Only the seeker can use SEEKER_MINIMAP_BOOST"
    );
  }

  const radius = lobby.currentRadiusMeters || lobby.initialRadiusMeters;
  const durationSeconds =
    storedPowerup === "HIDER_VISION_REDUCTION"
      ? hiderVisionReductionDurationSeconds(radius)
      : seekerMinimapBoostDurationSeconds(radius);
  return {
    powerup: storedPowerup,
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
  await getDb().ref(`lobbies/${lobbyId}`).update({
    state: "ENDED",
    "results/winner": winner,
    "results/endedAt": Date.now(),
  });
  return true;
}

export function shouldHidersWinByShrinkFloor(
  lobby: Pick<Lobby, "currentRadiusMeters" | "initialRadiusMeters">,
  players: Record<string, LobbyPlayer>
): boolean {
  if (lobby.currentRadiusMeters > minimumPlayableRadiusMeters(lobby.initialRadiusMeters)) {
    return false;
  }

  const seekerAlive = Object.values(players).some(
    (player) => player.role === "SEEKER" && player.alive
  );
  const aliveHiders = Object.values(players).some(
    (player) => player.role === "HIDER" && player.alive
  );

  return seekerAlive && aliveHiders;
}

async function checkAndWriteHiderShrinkWin(
  lobbyId: string,
  lobby: Lobby,
  players: Record<string, LobbyPlayer>,
  now: number
): Promise<boolean> {
  if (!shouldHidersWinByShrinkFloor(lobby, players)) {
    return false;
  }

  await getDb().ref(`lobbies/${lobbyId}`).update({
    state: "ENDED",
    "results/winner": "HIDERS",
    "results/endedAt": now,
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
    const existing = await getDb().ref(`lobbies/${inviteCode}`).once("value");
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

  await getDb().ref(`lobbies/${inviteCode}`).set(lobby);
  await getDb().ref(`lobbies/${inviteCode}/players/${uid}`).set(hostPlayer);
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
  const lobbySnap = await getDb().ref(`lobbies/${normalizedCode}`).once("value");
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

  const playerRef = getDb().ref(`lobbies/${normalizedCode}/players/${uid}`);
  const existingSnap = await playerRef.once("value");
  if (existingSnap.exists()) {
    await playerRef.update({ displayName, connected: true });
    const existing = existingSnap.val() as LobbyPlayer;
    return { lobbyId: normalizedCode, role: existing.role };
  }

  const player = buildLobbyPlayer(uid, displayName, "UNASSIGNED", Date.now());
  await playerRef.set(player);
  return { lobbyId: normalizedCode, role: "UNASSIGNED" };
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

  const lobbySnap = await getDb().ref(`lobbies/${lobbyId}`).once("value");
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

  const playersSnap = await getDb().ref(`lobbies/${lobbyId}/players`).once("value");
  const players = (playersSnap.val() ?? {}) as Record<string, LobbyPlayer>;
  const plan = prepareStartGame(lobby, players, Date.now());

  await getDb().ref(`lobbies/${lobbyId}/players`).update(plan.playerUpdates);
  await getDb().ref(`lobbies/${lobbyId}/hotspots`).set(plan.hotspots);
  await getDb().ref(`lobbies/${lobbyId}`).update({
    state: "RUNNING",
    seekerId: plan.seekerId,
    allReady: false,
    currentRadiusMeters: lobby.currentRadiusMeters || lobby.initialRadiusMeters,
    "timing/gameStartAt": plan.gameStartAt,
    "timing/nextShrinkAt": plan.nextShrinkAt,
  });

  return { success: true, gameStartAt: plan.gameStartAt, nextShrinkAt: plan.nextShrinkAt, seekerId: plan.seekerId };
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

  const lobbySnap = await getDb().ref(`lobbies/${lobbyId}`).once("value");
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

  const playersSnap = await getDb().ref(`lobbies/${lobbyId}/players`).once("value");
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

  await getDb().ref(`lobbies/${lobbyId}/players/${catchResult.targetId}`).update({
    alive: false,
    catchEligible: false,
    currentHotspotId: null,
    hotspotEnteredAt: null,
    hotspotRewardGrantedForCurrentStay: false,
    eliminatedAt: now,
  });

  const updatedPlayers = {
    ...players,
    [catchResult.targetId]: {
      ...players[catchResult.targetId],
      alive: false,
      catchEligible: false,
      currentHotspotId: null,
      hotspotEnteredAt: null,
      hotspotRewardGrantedForCurrentStay: false,
      eliminatedAt: now,
    },
  };
  await checkAndWriteWin(lobbyId, updatedPlayers);
  return { success: true, eliminatedUserId: catchResult.targetId };
});

async function executeShrink(lobbyId: string, lobby: Lobby): Promise<void> {
  const now = Date.now();
  const playersSnap = await getDb().ref(`lobbies/${lobbyId}/players`).once("value");
  const players = (playersSnap.val() ?? {}) as Record<string, LobbyPlayer>;

  const newRadius = computeShrinkRadius(lobby.currentRadiusMeters, lobby.initialRadiusMeters);
  const eliminatedIds = findOobPlayers(
    players,
    lobby.mapCenterLat,
    lobby.mapCenterLng,
    newRadius,
    SHRINK_GRACE_METERS,
    STALE_LOCATION_MS,
    now
  );

  await getDb().ref(`lobbies/${lobbyId}`).update({
    currentRadiusMeters: newRadius,
    "timing/nextShrinkAt": now + shrinkIntervalSeconds() * 1000,
  });

  const updatedPlayers = { ...players };
  if (eliminatedIds.length > 0) {
    const eliminationUpdates: Record<string, unknown> = {};
    for (const playerId of eliminatedIds) {
      eliminationUpdates[`${playerId}/alive`] = false;
      eliminationUpdates[`${playerId}/catchEligible`] = false;
      eliminationUpdates[`${playerId}/currentHotspotId`] = null;
      eliminationUpdates[`${playerId}/hotspotEnteredAt`] = null;
      eliminationUpdates[`${playerId}/hotspotRewardGrantedForCurrentStay`] = false;
      eliminationUpdates[`${playerId}/eliminatedAt`] = now;
      updatedPlayers[playerId] = {
        ...updatedPlayers[playerId],
        alive: false,
        catchEligible: false,
        currentHotspotId: null,
        hotspotEnteredAt: null,
        hotspotRewardGrantedForCurrentStay: false,
        eliminatedAt: now,
      };
    }
    await getDb().ref(`lobbies/${lobbyId}/players`).update(eliminationUpdates);
  }

  const nextLobbyState = { ...lobby, currentRadiusMeters: newRadius };
  const gameEnded = await checkAndWriteWin(lobbyId, updatedPlayers);
  if (!gameEnded) {
    await checkAndWriteHiderShrinkWin(lobbyId, nextLobbyState, updatedPlayers, now);
  }
}

export const scheduledShrink = functions.pubsub
  .schedule("every 1 minutes")
  .onRun(async () => {
    const now = Date.now();
    const lobbiesSnap = await getDb().ref("lobbies").once("value");
    const allLobbies = lobbiesSnap.val() as Record<string, Lobby> | null;
    if (!allLobbies) return;

    const shrinkPromises = Object.entries(allLobbies)
      .filter(([, lobby]) =>
        lobby.state === "RUNNING" &&
        lobby.timing?.nextShrinkAt != null &&
        now >= lobby.timing.nextShrinkAt
      )
      .map(([lobbyId, lobby]) => executeShrink(lobbyId, lobby));

    await Promise.all(shrinkPromises);
  });

export const claimHotspot = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Missing auth");
  }

  const { lobbyId } = data as { lobbyId: string };
  if (!lobbyId) {
    throw new functions.https.HttpsError("invalid-argument", "lobbyId required");
  }

  const lobbySnap = await getDb().ref(`lobbies/${lobbyId}`).once("value");
  const lobby = lobbySnap.val() as Lobby | null;
  if (!lobby) {
    throw new functions.https.HttpsError("not-found", "Lobby not found");
  }
  if (lobby.state !== "RUNNING") {
    throw new functions.https.HttpsError("failed-precondition", "Game is not running");
  }

  const playerSnap = await getDb().ref(`lobbies/${lobbyId}/players/${uid}`).once("value");
  const player = playerSnap.val() as LobbyPlayer | null;
  if (!player || !player.alive) {
    throw new functions.https.HttpsError("failed-precondition", "Player not active");
  }

  const hotspotsSnap = await getDb().ref(`lobbies/${lobbyId}/hotspots`).once("value");
  const hotspots = (hotspotsSnap.val() ?? {}) as Record<string, Hotspot>;
  const now = Date.now();
  const progress = resolveHotspotProgressForPlayer(
    player,
    hotspots,
    lobby.config?.hotspotDwellSeconds ?? HOTSPOT_DWELL_SECONDS,
    lobby.config?.hotspotPersonalCooldownSeconds ?? HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
    now
  );

  if (Object.keys(progress.updates).length > 0) {
    await getDb().ref(`lobbies/${lobbyId}/players/${uid}`).update(progress.updates);
  }

  const enteredAt =
    typeof progress.updates.hotspotEnteredAt === "number"
      ? progress.updates.hotspotEnteredAt
      : player.hotspotEnteredAt;
  const currentHotspotId =
    typeof progress.updates.currentHotspotId === "string"
      ? progress.updates.currentHotspotId
      : progress.currentHotspotId;

  return {
    success: true,
    currentHotspotId,
    grantedPowerup: progress.grantedPowerup,
    storedPowerupType:
      (progress.updates.storedPowerupType as StoredPowerupType | undefined) ??
      normalizeStoredPowerup(player.storedPowerupType),
    hotspotRewardGrantedForCurrentStay:
      (progress.updates.hotspotRewardGrantedForCurrentStay as boolean | undefined) ??
      player.hotspotRewardGrantedForCurrentStay,
    nextPowerupEligibleAt:
      (progress.updates.nextPowerupEligibleAt as number | undefined) ?? player.nextPowerupEligibleAt,
    hotspotEnteredAt: enteredAt ?? null,
    dwellSecondsRemaining:
      currentHotspotId && !progress.grantedPowerup && enteredAt !== null
        ? Math.max(
            0,
            Math.ceil(
              (enteredAt +
                (lobby.config?.hotspotDwellSeconds ?? HOTSPOT_DWELL_SECONDS) * 1000 -
                now) /
                1000
            )
          )
        : 0,
  };
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

  const lobbySnap = await getDb().ref(`lobbies/${lobbyId}`).once("value");
  const lobby = lobbySnap.val() as Lobby | null;
  if (!lobby) {
    throw new functions.https.HttpsError("not-found", "Lobby not found");
  }
  if (lobby.state !== "RUNNING") {
    throw new functions.https.HttpsError("failed-precondition", "Game is not running");
  }

  const playerSnap = await getDb().ref(`lobbies/${lobbyId}/players/${uid}`).once("value");
  const player = playerSnap.val() as LobbyPlayer | null;
  if (!player || !player.alive) {
    throw new functions.https.HttpsError("failed-precondition", "Player not active");
  }

  const result = resolvePowerupActivation(lobby, player, Date.now());
  await getDb().ref(`lobbies/${lobbyId}/players/${uid}`).update({
    storedPowerupType: "NONE",
    activePowerup: result.powerup,
    powerupEndsAt: result.powerupEndsAt,
  });

  return {
    success: true,
    powerup: result.powerup,
    powerupEndsAt: result.powerupEndsAt,
    durationSeconds: result.durationSeconds,
  };
});

export const onPlayerReady = functions.database
  .ref("/lobbies/{lobbyId}/players/{playerId}/ready")
  .onWrite(async (_, context) => {
    const { lobbyId } = context.params;

    const lobbySnap = await getDb().ref(`lobbies/${lobbyId}`).once("value");
    const lobby = lobbySnap.val() as Lobby | null;
    if (!lobby || lobby.state !== "WAITING") return;

    const playersSnap = await getDb().ref(`lobbies/${lobbyId}/players`).once("value");
    const players = (playersSnap.val() ?? {}) as Record<string, LobbyPlayer>;
    const playerList = Object.values(players);

    const allReady = playerList.length >= 2 && playerList.every((p) => p.ready);
    await getDb().ref(`lobbies/${lobbyId}`).update({ allReady });
  });

export const refreshCatchEligibility = functions.database
  .ref("/lobbies/{lobbyId}/players/{playerId}/lastLocationAt")
  .onWrite(async (_, context) => {
    const { lobbyId } = context.params;
    const playerId = context.params.playerId as string;

    const lobbySnap = await getDb().ref(`lobbies/${lobbyId}`).once("value");
    const lobby = lobbySnap.val() as Lobby | null;
    if (!lobby) return;

    const playersSnap = await getDb().ref(`lobbies/${lobbyId}/players`).once("value");
    const players = (playersSnap.val() ?? {}) as Record<string, LobbyPlayer>;
    const now = Date.now();

    // WAITING: host disconnect kills the lobby
    if (lobby.state === "WAITING") {
      const host = players[lobby.hostId];
      if (host && host.lastLocationAt > 0 && now - host.lastLocationAt > DISCONNECT_STALE_MS) {
        await getDb().ref(`lobbies/${lobbyId}`).update({ state: "ENDED" });
      }
      return;
    }

    if (lobby.state !== "RUNNING") return;

    // Disconnect elimination: eliminate players whose location is stale
    const disconnectedIds = Object.entries(players)
      .filter(([, p]) => p.alive && p.lastLocationAt > 0 && now - p.lastLocationAt > DISCONNECT_STALE_MS)
      .map(([id]) => id);

    if (disconnectedIds.length > 0) {
      const disconnectUpdates: Record<string, unknown> = {};
      for (const pid of disconnectedIds) {
        disconnectUpdates[`${pid}/alive`] = false;
        disconnectUpdates[`${pid}/catchEligible`] = false;
        disconnectUpdates[`${pid}/currentHotspotId`] = null;
        disconnectUpdates[`${pid}/hotspotEnteredAt`] = null;
        disconnectUpdates[`${pid}/hotspotRewardGrantedForCurrentStay`] = false;
        disconnectUpdates[`${pid}/eliminatedAt`] = now;
        players[pid] = { ...players[pid], alive: false, catchEligible: false, eliminatedAt: now };
      }
      await getDb().ref(`lobbies/${lobbyId}/players`).update(disconnectUpdates);
      const ended = await checkAndWriteWin(lobbyId, players);
      if (ended) return;
    }

    // Hotspot progress for the player who just updated their location
    const currentPlayer = players[playerId];
    if (currentPlayer?.alive && now - currentPlayer.lastLocationAt <= STALE_LOCATION_MS) {
      const hotspotsSnap = await getDb().ref(`lobbies/${lobbyId}/hotspots`).once("value");
      const hotspots = (hotspotsSnap.val() ?? {}) as Record<string, Hotspot>;
      const hotspotProgress = resolveHotspotProgressForPlayer(
        currentPlayer,
        hotspots,
        lobby.config?.hotspotDwellSeconds ?? HOTSPOT_DWELL_SECONDS,
        lobby.config?.hotspotPersonalCooldownSeconds ?? HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
        now
      );
      if (Object.keys(hotspotProgress.updates).length > 0) {
        await getDb().ref(`lobbies/${lobbyId}/players/${playerId}`).update(hotspotProgress.updates);
        players[playerId] = { ...players[playerId], ...hotspotProgress.updates } as LobbyPlayer;
      }
    }

    // Expire powerups across all players
    const expiredUpdates = clearExpiredPowerups(players, now);
    if (Object.keys(expiredUpdates).length > 0) {
      await getDb().ref(`lobbies/${lobbyId}/players`).update(expiredUpdates);
      for (const [path, value] of Object.entries(expiredUpdates)) {
        const [pid, field] = path.split("/");
        players[pid] = { ...players[pid], [field]: value } as LobbyPlayer;
      }
    }

    // Catch eligibility based on seeker proximity
    const seeker = Object.values(players).find((p) => p.role === "SEEKER" && p.alive);
    if (!seeker) return;

    const catchRadius =
      lobby.config?.catchEligibilityRadiusMeters ??
      catchEligibilityRadiusMeters(lobby.currentRadiusMeters);

    const catchUpdates: Record<string, boolean> = {};
    for (const [pid, player] of Object.entries(players)) {
      if (player.role !== "HIDER" || !player.alive) {
        catchUpdates[`${pid}/catchEligible`] = false;
        continue;
      }
      catchUpdates[`${pid}/catchEligible`] = isWithinSquareBounds(
        seeker.lat,
        seeker.lng,
        player.lat,
        player.lng,
        catchRadius
      );
    }
    await getDb().ref(`lobbies/${lobbyId}/players`).update(catchUpdates);
  });
