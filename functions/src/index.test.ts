import {
  LobbyPlayer,
  buildCreateLobbyState,
  buildLobbyPlayer,
  computeShrinkRadius,
  distanceMeters,
  evaluateCatchAttempt,
  findOobPlayers,
  prepareStartGame,
  resolveHotspotClaimAttempt,
  resolvePowerupActivation,
} from "./index";
import {
  SHRINK_GRACE_METERS,
  STALE_LOCATION_MS,
  defaultConfig,
  minimumPlayableRadiusMeters,
  shrinkStepMeters,
} from "./gameBalance";

function makePlayer(overrides: Partial<LobbyPlayer>): LobbyPlayer {
  return {
    userId: "u1",
    displayName: "Test",
    role: "HIDER",
    alive: true,
    connected: true,
    ready: true,
    lat: 0,
    lng: 0,
    accuracyMeters: 5,
    lastLocationAt: 1_000_000,
    stepsAtGameStart: 0,
    currentMatchSteps: 0,
    activePowerup: "NONE",
    powerupEndsAt: null,
    catchCode: null,
    catchEligible: false,
    ...overrides,
  };
}

describe("createLobby", () => {
  test("builds a waiting lobby with invite-code compatible host record", () => {
    const now = 123456;
    const result = buildCreateLobbyState("host-1", "Host", 38.9, -77.0, 150, now);

    expect(result.lobby.hostId).toBe("host-1");
    expect(result.lobby.state).toBe("WAITING");
    expect(result.lobby.currentRadiusMeters).toBe(150);
    expect(result.lobby.config).toEqual(defaultConfig(150));
    expect(result.hostPlayer.role).toBe("SEEKER");
    expect(result.hostPlayer.ready).toBe(false);
    expect(result.hostPlayer.lastLocationAt).toBe(now);
  });
});

describe("joinLobby", () => {
  test("builds a hider player row for the joining user", () => {
    const player = buildLobbyPlayer("h1", "Hider", "HIDER", 10);

    expect(player.userId).toBe("h1");
    expect(player.displayName).toBe("Hider");
    expect(player.role).toBe("HIDER");
    expect(player.ready).toBe(false);
    expect(player.catchCode).toBeNull();
  });
});

describe("startGame", () => {
  test("assigns unique hider catch codes, hotspots, and timing", () => {
    const lobby = {
      hostId: "host",
      inviteCode: "ROOM1",
      state: "WAITING" as const,
      createdAt: 1,
      mapCenterLat: 38.9,
      mapCenterLng: -77.0,
      initialRadiusMeters: 150,
      currentRadiusMeters: 150,
      seekerId: "host",
      config: defaultConfig(150),
    };
    const players = {
      host: makePlayer({ userId: "host", role: "SEEKER", ready: true }),
      h1: makePlayer({ userId: "h1", role: "HIDER", ready: true }),
      h2: makePlayer({ userId: "h2", role: "HIDER", ready: true }),
    };

    const plan = prepareStartGame(lobby, players, 1000, [
      {
        id: "hs_0",
        lat: 38.9,
        lng: -77.0,
        radiusMeters: 12,
        active: true,
        claimedBy: null,
        claimedAt: null,
        powerupType: "HIDER_INVISIBILITY",
      },
    ]);

    expect(plan.gameStartAt).toBeGreaterThan(1000);
    expect(plan.nextShrinkAt).toBeGreaterThan(plan.gameStartAt);
    expect(plan.hotspots.hs_0.powerupType).toBe("HIDER_INVISIBILITY");
    expect(plan.playerUpdates["h1/catchCode"]).toHaveLength(6);
    expect(plan.playerUpdates["h2/catchCode"]).toHaveLength(6);
    expect(plan.playerUpdates["h1/catchCode"]).not.toBe(plan.playerUpdates["h2/catchCode"]);
    expect(plan.playerUpdates["host/catchCode"]).toBeNull();
  });
});

describe("submitCatchCode", () => {
  const players = {
    seeker: makePlayer({ userId: "seeker", role: "SEEKER" }),
    h1: makePlayer({
      userId: "h1",
      role: "HIDER",
      catchCode: "ABC123",
      catchEligible: true,
    }),
  };

  test("valid code succeeds", () => {
    expect(evaluateCatchAttempt(players, "ABC123")).toEqual({
      success: true,
      targetId: "h1",
    });
  });

  test("wrong code returns NO_ELIGIBLE_MATCH", () => {
    expect(evaluateCatchAttempt(players, "ZZZZZZ")).toEqual({
      success: false,
      reason: "NO_ELIGIBLE_MATCH",
    });
  });

  test("valid code but out of range also returns NO_ELIGIBLE_MATCH", () => {
    expect(
      evaluateCatchAttempt(
        {
          ...players,
          h1: { ...players.h1, catchEligible: false },
        },
        "ABC123"
      )
    ).toEqual({
      success: false,
      reason: "NO_ELIGIBLE_MATCH",
    });
  });
});

describe("shrinkCircle", () => {
  test("radius shrinks by one step but never below floor", () => {
    const initial = 150;
    expect(computeShrinkRadius(initial, initial)).toBeCloseTo(
      initial - shrinkStepMeters(initial),
      1
    );
    expect(computeShrinkRadius(41, initial)).toBe(minimumPlayableRadiusMeters(initial));
  });

  test("recent out-of-bounds players are eliminated", () => {
    const now = 1_000_000;
    const centerLat = 38.9;
    const centerLng = -77.0;
    const farLat = centerLat + 200 / 111_111;
    const players = {
      safe: makePlayer({
        userId: "safe",
        lat: centerLat + 20 / 111_111,
        lng: centerLng,
        lastLocationAt: now - 1000,
      }),
      oob: makePlayer({
        userId: "oob",
        lat: farLat,
        lng: centerLng,
        lastLocationAt: now - 1000,
      }),
      stale: makePlayer({
        userId: "stale",
        lat: farLat,
        lng: centerLng,
        lastLocationAt: now - STALE_LOCATION_MS - 1,
      }),
    };

    const eliminated = findOobPlayers(
      players,
      centerLat,
      centerLng,
      100,
      SHRINK_GRACE_METERS,
      STALE_LOCATION_MS,
      now
    );

    expect(eliminated).toContain("oob");
    expect(eliminated).not.toContain("safe");
    expect(eliminated).not.toContain("stale");
  });
});

describe("claimHotspot", () => {
  test("first valid in-range claim wins and grants the hotspot powerup", () => {
    const player = makePlayer({
      userId: "h1",
      role: "HIDER",
      lat: 38.9,
      lng: -77.0,
    });
    const result = resolveHotspotClaimAttempt(
      player,
      {
        id: "hs_0",
        lat: 38.9,
        lng: -77.0,
        radiusMeters: 10,
        active: true,
        claimedBy: null,
        claimedAt: null,
        powerupType: "HIDER_INVISIBILITY",
      },
      2000
    );

    expect(result.hotspot.active).toBe(false);
    expect(result.hotspot.claimedBy).toBe("h1");
    expect(result.hotspot.claimedAt).toBe(2000);
    expect(result.grantedPowerup).toBe("HIDER_INVISIBILITY");
  });
});

describe("activatePowerup", () => {
  test("computes server-owned timing for an eligible player", () => {
    const lobby = {
      hostId: "host",
      inviteCode: "ROOM1",
      state: "RUNNING" as const,
      createdAt: 1,
      mapCenterLat: 38.9,
      mapCenterLng: -77.0,
      initialRadiusMeters: 150,
      currentRadiusMeters: 150,
      seekerId: "host",
      config: defaultConfig(150),
    };

    const result = resolvePowerupActivation(
      lobby,
      makePlayer({
        userId: "h1",
        role: "HIDER",
        activePowerup: "HIDER_INVISIBILITY",
      }),
      5000
    );

    expect(result.powerup).toBe("HIDER_INVISIBILITY");
    expect(result.durationSeconds).toBeGreaterThan(0);
    expect(result.powerupEndsAt).toBeGreaterThan(5000);
  });
});

describe("sanity helpers", () => {
  test("distanceMeters still measures roughly 100m", () => {
    const d = distanceMeters(0, 0, 0.0009, 0);
    expect(d).toBeGreaterThan(90);
    expect(d).toBeLessThan(110);
  });
});
