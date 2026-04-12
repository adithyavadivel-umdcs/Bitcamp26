import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

admin.initializeApp();
const db = admin.database();

type LobbyPlayer = {
  userId: string;
  role: "SEEKER" | "HIDER";
  alive: boolean;
  lat: number;
  lng: number;
  lastLocationAt: number;
  catchCode?: string | null;
  catchEligible?: boolean;
};

function distanceMeters(aLat: number, aLng: number, bLat: number, bLng: number): number {
  const R = 6371000;
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(bLat - aLat);
  const dLng = toRad(bLng - aLng);
  const lat1 = toRad(aLat);
  const lat2 = toRad(bLat);

  const x =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.sin(dLng / 2) * Math.sin(dLng / 2) * Math.cos(lat1) * Math.cos(lat2);
  const y = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
  return R * y;
}

export const claimHotspot = functions.https.onCall(async (data, context) => {
  const { lobbyId, hotspotId } = data;
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Missing auth");

  const lobbySnap = await db.ref(`lobbies/${lobbyId}`).once("value");
  const lobby = lobbySnap.val();
  if (!lobby) throw new functions.https.HttpsError("not-found", "Lobby not found");

  const playerSnap = await db.ref(`lobbies/${lobbyId}/players/${uid}`).once("value");
  const player = playerSnap.val() as LobbyPlayer | null;
  if (!player || !player.alive) {
    throw new functions.https.HttpsError("failed-precondition", "Player not active");
  }

  const hotspotRef = db.ref(`lobbies/${lobbyId}/hotspots/${hotspotId}`);
  const result = await hotspotRef.transaction((hotspot) => {
    if (!hotspot) return hotspot;
    if (!hotspot.active) return;
    const dist = distanceMeters(player.lat, player.lng, hotspot.lat, hotspot.lng);
    if (dist > hotspot.radiusMeters) return;
    hotspot.active = false;
    hotspot.claimedBy = uid;
    hotspot.claimedAt = Date.now();
    return hotspot;
  });

  if (!result.committed) {
    throw new functions.https.HttpsError("aborted", "Hotspot already claimed or not in range");
  }

  const grantedPowerup = result.snapshot.val()?.powerupType ?? "NONE";
  await db.ref(`lobbies/${lobbyId}/players/${uid}`).update({
    activePowerup: grantedPowerup,
  });

  return { success: true, powerupType: grantedPowerup };
});

export const submitCatchCode = functions.https.onCall(async (data, context) => {
  const { lobbyId, code } = data;
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError("unauthenticated", "Missing auth");

  const playersSnap = await db.ref(`lobbies/${lobbyId}/players`).once("value");
  const players = (playersSnap.val() ?? {}) as Record<string, LobbyPlayer>;

  const seeker = players[uid];
  if (!seeker || seeker.role !== "SEEKER" || !seeker.alive) {
    throw new functions.https.HttpsError("failed-precondition", "Only active seeker can submit codes");
  }

  const matchedEntry = Object.entries(players).find(([_, p]) => {
    return p.role === "HIDER" && p.alive && p.catchEligible && p.catchCode === code;
  });

  if (!matchedEntry) {
    return { success: false, reason: "NO_ELIGIBLE_MATCH" };
  }

  const [targetId] = matchedEntry;
  await db.ref(`lobbies/${lobbyId}/players/${targetId}`).update({
    alive: false,
    eliminatedAt: Date.now(),
  });

  return { success: true, eliminatedUserId: targetId };
});

export const refreshCatchEligibility = functions.database
  .ref("/lobbies/{lobbyId}/players/{playerId}/lastLocationAt")
  .onWrite(async (_, context) => {
    const { lobbyId } = context.params;
    const lobbySnap = await db.ref(`lobbies/${lobbyId}`).once("value");
    const lobby = lobbySnap.val();
    if (!lobby || lobby.state !== "RUNNING") return;

    const playersSnap = await db.ref(`lobbies/${lobbyId}/players`).once("value");
    const players = (playersSnap.val() ?? {}) as Record<string, LobbyPlayer>;
    const seeker = Object.values(players).find((p) => p.role === "SEEKER" && p.alive);
    if (!seeker) return;

    const updates: Record<string, boolean> = {};
    for (const [id, player] of Object.entries(players)) {
      if (player.role !== "HIDER" || !player.alive) {
        updates[`${id}/catchEligible`] = false;
        continue;
      }
      const dist = distanceMeters(seeker.lat, seeker.lng, player.lat, player.lng);
      updates[`${id}/catchEligible`] = dist <= lobby.config.catchEligibilityRadiusMeters;
    }

    await db.ref(`lobbies/${lobbyId}/players`).update(updates);
  });
