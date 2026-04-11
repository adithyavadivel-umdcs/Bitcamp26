import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.database();

// ── Helpers ──────────────────────────────────────────────────────────────────

function generateInviteCode(): string {
  return Math.random().toString(36).substring(2, 8).toUpperCase();
}

function generateCatchCode(): string {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

function distanceMeters(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function gameBalance(mapRadiusMeters: number) {
  return {
    headStartSeconds: Math.max(30, Math.floor(mapRadiusMeters / 5)),
    shrinkIntervalSeconds: Math.max(60, Math.floor(mapRadiusMeters * 0.8)),
    shrinkWarningSeconds: 15,
    catchEligibilityRadiusMeters: Math.max(15, mapRadiusMeters * 0.1),
    hotspotRadiusMeters: Math.max(10, mapRadiusMeters * 0.07),
    hotspotDwellSeconds: 5,
    hiderInvisibilityDurationSeconds: Math.max(20, Math.floor(mapRadiusMeters / 8)),
    seekerRevealAllDurationSeconds: Math.max(15, Math.floor(mapRadiusMeters / 10)),
    startClusterRadiusMeters: 30,
    disconnectTimeoutSeconds: 60,
    matchTimeLimitSeconds: Math.max(300, Math.floor(mapRadiusMeters * 3)),
  };
}

// ── createLobby ───────────────────────────────────────────────────────────────
// Called by: host when creating a new game
// Input: { displayName, mapCenterLat, mapCenterLng, initialRadiusMeters }
// Returns: { lobbyId, inviteCode }

export const createLobby = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

  const { displayName, mapCenterLat, mapCenterLng, initialRadiusMeters } = data;
  const hostId = context.auth.uid;
  const inviteCode = generateInviteCode();
  const balance = gameBalance(initialRadiusMeters);

  const lobbyRef = db.ref("lobbies").push();
  const lobbyId = lobbyRef.key!;

  const lobby = {
    hostId,
    inviteCode,
    state: "LOBBY",
    createdAt: Date.now(),
    mapCenterLat,
    mapCenterLng,
    initialRadiusMeters,
    currentRadiusMeters: initialRadiusMeters,
    rallyPointLat: mapCenterLat + 0.002,
    rallyPointLng: mapCenterLng,
    seekerId: null,
    config: balance,
    timing: {},
    results: {},
  };

  await lobbyRef.set(lobby);

  // Add host as first player
  await db.ref(`lobbies/${lobbyId}/players/${hostId}`).set({
    userId: hostId,
    displayName,
    role: "UNASSIGNED",
    alive: true,
    connected: true,
    ready: false,
    lat: mapCenterLat,
    lng: mapCenterLng,
    accuracyMeters: 0,
    lastLocationAt: Date.now(),
    stepsAtGameStart: 0,
    currentMatchSteps: 0,
    activePowerup: "NONE",
    powerupEndsAt: null,
    catchCode: null,
  });

  return { lobbyId, inviteCode };
});

// ── joinLobby ─────────────────────────────────────────────────────────────────
// Called by: player entering an invite code
// Input: { inviteCode, displayName }
// Returns: { lobbyId }

export const joinLobby = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

  const { inviteCode, displayName } = data;
  const userId = context.auth.uid;

  // Find lobby by invite code
  const snapshot = await db
    .ref("lobbies")
    .orderByChild("inviteCode")
    .equalTo(inviteCode.toUpperCase())
    .limitToFirst(1)
    .once("value");

  if (!snapshot.exists()) {
    throw new functions.https.HttpsError("not-found", "Invalid invite code");
  }

  const lobbyId = Object.keys(snapshot.val())[0];
  const lobby = snapshot.val()[lobbyId];

  if (lobby.state !== "LOBBY") {
    throw new functions.https.HttpsError("failed-precondition", "Game already started");
  }

  await db.ref(`lobbies/${lobbyId}/players/${userId}`).set({
    userId,
    displayName,
    role: "UNASSIGNED",
    alive: true,
    connected: true,
    ready: false,
    lat: lobby.mapCenterLat,
    lng: lobby.mapCenterLng,
    accuracyMeters: 0,
    lastLocationAt: Date.now(),
    stepsAtGameStart: 0,
    currentMatchSteps: 0,
    activePowerup: "NONE",
    powerupEndsAt: null,
    catchCode: null,
  });

  return { lobbyId };
});

// ── startGame ─────────────────────────────────────────────────────────────────
// Called by: host pressing Start
// Input: { lobbyId }
// Validates: all players ready and within startClusterRadiusMeters
// Assigns roles, generates catch codes, transitions state to HEAD_START

export const startGame = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

  const { lobbyId } = data;
  const lobbyRef = db.ref(`lobbies/${lobbyId}`);
  const snapshot = await lobbyRef.once("value");
  const lobby = snapshot.val();

  if (!lobby) throw new functions.https.HttpsError("not-found", "Lobby not found");
  if (lobby.hostId !== context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "Only host can start");
  }
  if (lobby.state !== "LOBBY") {
    throw new functions.https.HttpsError("failed-precondition", "Not in lobby state");
  }

  const playersSnap = await db.ref(`lobbies/${lobbyId}/players`).once("value");
  const players: Record<string, any> = playersSnap.val() || {};
  const playerIds = Object.keys(players);

  if (playerIds.length < 2) {
    throw new functions.https.HttpsError("failed-precondition", "Need at least 2 players");
  }

  // Check all players are ready
  const notReady = playerIds.filter((id) => !players[id].ready);
  if (notReady.length > 0) {
    throw new functions.https.HttpsError("failed-precondition", "Not all players are ready");
  }

  // Check proximity
  const balance = lobby.config;
  const notInRange = playerIds.filter((id) => {
    const d = distanceMeters(
      players[id].lat,
      players[id].lng,
      lobby.mapCenterLat,
      lobby.mapCenterLng
    );
    return d > balance.startClusterRadiusMeters;
  });
  if (notInRange.length > 0) {
    throw new functions.https.HttpsError("failed-precondition", "Not all players are near the start point");
  }

  // Assign roles: host is seeker by default for MVP
  const seekerId = lobby.hostId;
  const updates: Record<string, any> = {};

  for (const id of playerIds) {
    const role = id === seekerId ? "SEEKER" : "HIDER";
    updates[`players/${id}/role`] = role;
    updates[`players/${id}/catchCode`] = role === "HIDER" ? generateCatchCode() : null;
    updates[`players/${id}/stepsAtGameStart`] = players[id].currentMatchSteps || 0;
  }

  // Generate hotspots
  const hotspotCount = balance.hotspotCount || 4;
  for (let i = 0; i < hotspotCount; i++) {
    const angle = (i / hotspotCount) * 2 * Math.PI;
    const dist = lobby.initialRadiusMeters * 0.5;
    const hotspotLat = lobby.mapCenterLat + (dist / 111320) * Math.cos(angle);
    const hotspotLng =
      lobby.mapCenterLng +
      (dist / (111320 * Math.cos((lobby.mapCenterLat * Math.PI) / 180))) * Math.sin(angle);
    const hotspotId = `hotspot_${i}`;
    const powerupType = i % 2 === 0 ? "HIDER_INVISIBILITY" : "SEEKER_REVEAL_ALL";

    updates[`hotspots/${hotspotId}`] = {
      id: hotspotId,
      lat: hotspotLat,
      lng: hotspotLng,
      radiusMeters: balance.hotspotRadiusMeters,
      active: true,
      claimedBy: null,
      claimedAt: null,
      powerupType,
    };
  }

  const gameStartAt = Date.now();
  updates["state"] = "HEAD_START";
  updates["seekerId"] = seekerId;
  updates["timing/gameStartAt"] = gameStartAt;
  updates["timing/headStartEndsAt"] = gameStartAt + balance.headStartSeconds * 1000;
  updates["timing/nextShrinkAt"] = gameStartAt + balance.headStartSeconds * 1000 + balance.shrinkIntervalSeconds * 1000;

  await lobbyRef.update(updates);
  return { success: true };
});

// ── transitionToActive ────────────────────────────────────────────────────────
// Called by: client after head start countdown ends (or scheduled function)
// Input: { lobbyId }

export const transitionToActive = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

  const { lobbyId } = data;
  const lobbyRef = db.ref(`lobbies/${lobbyId}`);
  const snapshot = await lobbyRef.once("value");
  const lobby = snapshot.val();

  if (!lobby) throw new functions.https.HttpsError("not-found", "Lobby not found");
  if (lobby.state !== "HEAD_START") return { success: true }; // idempotent

  await lobbyRef.update({ state: "ACTIVE" });
  return { success: true };
});

// ── shrinkCircle ──────────────────────────────────────────────────────────────
// Called by: client on shrink timer tick
// Input: { lobbyId }
// Shrinks the radius, eliminates out-of-bounds players

export const shrinkCircle = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

  const { lobbyId } = data;
  const lobbyRef = db.ref(`lobbies/${lobbyId}`);
  const snapshot = await lobbyRef.once("value");
  const lobby = snapshot.val();

  if (!lobby || lobby.state !== "ACTIVE") return { success: false, reason: "not active" };

  const balance = lobby.config;
  const minRadius = 20;
  const newRadius = Math.max(minRadius, lobby.currentRadiusMeters * 0.80);

  const playersSnap = await db.ref(`lobbies/${lobbyId}/players`).once("value");
  const players: Record<string, any> = playersSnap.val() || {};

  const updates: Record<string, any> = {};
  updates["currentRadiusMeters"] = newRadius;
  updates[`timing/nextShrinkAt`] = Date.now() + balance.shrinkIntervalSeconds * 1000;

  // Eliminate players outside new radius
  for (const [id, player] of Object.entries(players)) {
    if (!player.alive) continue;
    const d = distanceMeters(player.lat, player.lng, lobby.mapCenterLat, lobby.mapCenterLng);
    if (d > newRadius) {
      updates[`players/${id}/alive`] = false;
    }
  }

  await lobbyRef.update(updates);

  // Check win conditions after shrink
  await checkWinConditions(lobbyId, lobby.seekerId);
  return { success: true, newRadius };
});

// ── submitCatchCode ───────────────────────────────────────────────────────────
// Called by: seeker entering a hider's 6-digit code
// Input: { lobbyId, code }
// Validates proximity + code correctness, eliminates hider if valid

export const submitCatchCode = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

  const { lobbyId, code } = data;
  const seekerId = context.auth.uid;

  const lobbySnap = await db.ref(`lobbies/${lobbyId}`).once("value");
  const lobby = lobbySnap.val();

  if (!lobby || lobby.state !== "ACTIVE") {
    throw new functions.https.HttpsError("failed-precondition", "Game not active");
  }
  if (lobby.seekerId !== seekerId) {
    throw new functions.https.HttpsError("permission-denied", "Only seeker can submit codes");
  }

  const playersSnap = await db.ref(`lobbies/${lobbyId}/players`).once("value");
  const players: Record<string, any> = playersSnap.val() || {};
  const seeker = players[seekerId];

  if (!seeker) throw new functions.https.HttpsError("not-found", "Seeker not found");

  const balance = lobby.config;

  // Find a living hider with this code who is within catch range
  for (const [id, player] of Object.entries(players)) {
    if (id === seekerId) continue;
    if (!player.alive) continue;
    if (player.catchCode !== code) continue;

    const d = distanceMeters(seeker.lat, seeker.lng, player.lat, player.lng);
    if (d > balance.catchEligibilityRadiusMeters) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Hider is not within catch range"
      );
    }

    // Valid catch — eliminate hider
    await db.ref(`lobbies/${lobbyId}/players/${id}`).update({ alive: false });

    // Log event
    await db.ref(`lobbies/${lobbyId}/events`).push({
      type: "CATCH",
      createdAt: Date.now(),
      payload: { seekerId, caughtId: id, code },
    });

    await checkWinConditions(lobbyId, lobby.seekerId);
    return { success: true, caughtId: id, displayName: player.displayName };
  }

  // Code valid format but no matching eligible hider
  return { success: false, reason: "No eligible hider found with that code" };
});

// ── claimHotspot ──────────────────────────────────────────────────────────────
// Called by: player after dwelling in hotspot
// Input: { lobbyId, hotspotId }
// Validates proximity, grants powerup, deactivates hotspot

export const claimHotspot = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

  const { lobbyId, hotspotId } = data;
  const userId = context.auth.uid;

  const [lobbySnap, hotspotSnap, playerSnap] = await Promise.all([
    db.ref(`lobbies/${lobbyId}`).once("value"),
    db.ref(`lobbies/${lobbyId}/hotspots/${hotspotId}`).once("value"),
    db.ref(`lobbies/${lobbyId}/players/${userId}`).once("value"),
  ]);

  const lobby = lobbySnap.val();
  const hotspot = hotspotSnap.val();
  const player = playerSnap.val();

  if (!lobby || lobby.state !== "ACTIVE") {
    throw new functions.https.HttpsError("failed-precondition", "Game not active");
  }
  if (!hotspot || !hotspot.active) {
    throw new functions.https.HttpsError("failed-precondition", "Hotspot not available");
  }
  if (!player || !player.alive) {
    throw new functions.https.HttpsError("failed-precondition", "Player not alive");
  }

  // Verify player is actually in the hotspot
  const d = distanceMeters(player.lat, player.lng, hotspot.lat, hotspot.lng);
  if (d > hotspot.radiusMeters) {
    throw new functions.https.HttpsError("failed-precondition", "Not within hotspot radius");
  }

  const balance = lobby.config;
  const powerupType = hotspot.powerupType;
  const durationSeconds =
    powerupType === "HIDER_INVISIBILITY"
      ? balance.hiderInvisibilityDurationSeconds
      : balance.seekerRevealAllDurationSeconds;

  const powerupEndsAt = Date.now() + durationSeconds * 1000;

  await Promise.all([
    db.ref(`lobbies/${lobbyId}/hotspots/${hotspotId}`).update({
      active: false,
      claimedBy: userId,
      claimedAt: Date.now(),
    }),
    db.ref(`lobbies/${lobbyId}/players/${userId}`).update({
      activePowerup: powerupType,
      powerupEndsAt,
    }),
  ]);

  return { success: true, powerupType, powerupEndsAt };
});

// ── expirePowerup ─────────────────────────────────────────────────────────────
// Called by: client when powerup timer expires on device
// Input: { lobbyId }

export const expirePowerup = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

  const { lobbyId } = data;
  const userId = context.auth.uid;

  await db.ref(`lobbies/${lobbyId}/players/${userId}`).update({
    activePowerup: "NONE",
    powerupEndsAt: null,
  });

  return { success: true };
});

// ── checkWinConditions (internal helper) ──────────────────────────────────────

async function checkWinConditions(lobbyId: string, seekerId: string) {
  const playersSnap = await db.ref(`lobbies/${lobbyId}/players`).once("value");
  const players: Record<string, any> = playersSnap.val() || {};

  const seeker = players[seekerId];
  if (!seeker || !seeker.alive) {
    // Seeker eliminated — hiders win
    await db.ref(`lobbies/${lobbyId}`).update({
      state: "FINISHED",
      "results/winner": "HIDERS",
      "results/endedAt": Date.now(),
    });
    return;
  }

  const livingHiders = Object.entries(players).filter(
    ([id, p]) => id !== seekerId && p.alive
  );

  if (livingHiders.length === 0) {
    // All hiders eliminated — seeker wins
    await db.ref(`lobbies/${lobbyId}`).update({
      state: "FINISHED",
      "results/winner": "SEEKER",
      "results/endedAt": Date.now(),
    });
  }
}

// ── endGame ───────────────────────────────────────────────────────────────────
// Called by: host to force-end, or match time limit reached on client
// Input: { lobbyId, winner } where winner = "SEEKER" | "HIDERS"

export const endGame = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

  const { lobbyId, winner } = data;

  await db.ref(`lobbies/${lobbyId}`).update({
    state: "FINISHED",
    "results/winner": winner,
    "results/endedAt": Date.now(),
  });

  return { success: true };
});
