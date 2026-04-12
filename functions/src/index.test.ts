import {
  LobbyPlayer,
  buildCreateLobbyState,
  buildLobbyPlayer,
  computeShrinkRadius,
  distanceMeters,
  evaluateCatchAttempt,
  findEligibleTarget,
  findOobPlayers,
  isWithinSquareBounds,
  prepareStartGame,
  resolveHotspotProgressForPlayer,
  resolvePowerupActivation,
  resolveWinner,
  shouldHidersWinByShrinkFloor,
} from "./index";
import {
  DEFAULT_MAP_RADIUS_METERS,
  HOTSPOT_HALF_WIDTH_METERS,
  HOTSPOT_DWELL_SECONDS,
  HOTSPOT_MAX_COVERAGE_FRACTION,
  HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
  HOTSPOT_SIDE_LENGTH_METERS,
  HIDER_VISION_REDUCTION_MULTIPLIER,
  SEEKER_MINIMAP_BOOST_MULTIPLIER,
  SHRINK_GRACE_METERS,
  SHRINK_WARNING_SECONDS,
  STALE_LOCATION_MS,
  catchEligibilityRadiusMeters,
  defaultConfig,
  headStartSeconds,
  hiderVisionReductionDurationSeconds,
  hotspotCount,
  hotspotRadiusMeters,
  minimumPlayableRadiusMeters,
  seekerFreezeRadiusMeters,
  seekerMinimapBoostDurationSeconds,
  shrinkIntervalSeconds,
  shrinkStepMeters,
  startClusterRadiusMeters,
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
    storedPowerupType: "NONE",
    nextPowerupEligibleAt: 0,
    currentHotspotId: null,
    hotspotEnteredAt: null,
    hotspotRewardGrantedForCurrentStay: false,
    activePowerup: "NONE",
    powerupEndsAt: null,
    catchCode: null,
    catchEligible: false,
    ...overrides,
  };
}

function latOffsetDegrees(meters: number): number {
  return meters / 111_111;
}

function lngOffsetDegrees(meters: number, latitude: number): number {
  return meters / (111_111 * Math.cos((latitude * Math.PI) / 180));
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

  test("uses default map radius when one is not provided", () => {
    const result = buildCreateLobbyState("host-1", "Host");

    expect(result.lobby.initialRadiusMeters).toBe(DEFAULT_MAP_RADIUS_METERS);
    expect(result.lobby.currentRadiusMeters).toBe(DEFAULT_MAP_RADIUS_METERS);
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

  test("assigns unique hider catch codes, hotspots, and timing", () => {
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
        powerupType: "HIDER_VISION_REDUCTION",
      },
    ]);

    expect(plan.gameStartAt).toBeGreaterThan(1000);
    expect(plan.nextShrinkAt).toBeGreaterThan(plan.gameStartAt);
    expect(plan.hotspots.hs_0.powerupType).toBe("HIDER_VISION_REDUCTION");
    expect(plan.playerUpdates["h1/catchCode"]).toHaveLength(6);
    expect(plan.playerUpdates["h2/catchCode"]).toHaveLength(6);
    expect(plan.playerUpdates["h1/catchCode"]).not.toBe(plan.playerUpdates["h2/catchCode"]);
    expect(plan.playerUpdates["host/catchCode"]).toBeNull();
  });

  test("generated hotspots stay below 10 percent map coverage and do not intersect", () => {
    const players = {
      host: makePlayer({ userId: "host", role: "SEEKER", ready: true }),
      h1: makePlayer({ userId: "h1", role: "HIDER", ready: true }),
    };

    const plan = prepareStartGame(lobby, players, 1000);
    const hotspots = Object.values(plan.hotspots);
    const mapArea = Math.pow(lobby.currentRadiusMeters * 2, 2);
    const hotspotArea = Math.pow(HOTSPOT_SIDE_LENGTH_METERS, 2);

    expect(hotspots.length).toBe(hotspotCount(lobby.currentRadiusMeters));
    expect(hotspots.every((hotspot) => hotspot.radiusMeters === HOTSPOT_HALF_WIDTH_METERS)).toBe(true);
    expect(hotspots.length * hotspotArea).toBeLessThan(mapArea * HOTSPOT_MAX_COVERAGE_FRACTION);

    for (let i = 0; i < hotspots.length; i++) {
      for (let j = i + 1; j < hotspots.length; j++) {
        const latMeters = Math.abs(hotspots[i].lat - hotspots[j].lat) * 111_111;
        const lngMeters =
          Math.abs(hotspots[i].lng - hotspots[j].lng) *
          111_111 *
          Math.cos((lobby.mapCenterLat * Math.PI) / 180);
        expect(latMeters >= HOTSPOT_SIDE_LENGTH_METERS || lngMeters >= HOTSPOT_SIDE_LENGTH_METERS)
          .toBe(true);
      }
    }
  });

  test("rejects start when not all players are ready", () => {
    const players = {
      host: makePlayer({ userId: "host", role: "SEEKER", ready: true }),
      h1: makePlayer({ userId: "h1", role: "HIDER", ready: false }),
    };

    expect(() => prepareStartGame(lobby, players, 1000)).toThrow(
      "All players must be ready before the game can start"
    );
  });

  test("rejects start when seeker or hider role is missing", () => {
    const players = {
      onlyHider: makePlayer({ userId: "h1", role: "HIDER", ready: true }),
      onlyHider2: makePlayer({ userId: "h2", role: "HIDER", ready: true }),
    };

    expect(() => prepareStartGame(lobby, players, 1000)).toThrow(
      "Need at least one SEEKER and one HIDER"
    );
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

  test("findEligibleTarget normalizes whitespace and casing", () => {
    expect(findEligibleTarget(players, " abc123 ")).toBe("h1");
  });
});

describe("shrinkCircle", () => {
  test("radius shrinks by 10 percent of the original half-width but never below the 20 percent floor", () => {
    const initial = 150;

    expect(computeShrinkRadius(initial, initial)).toBeCloseTo(
      initial - shrinkStepMeters(initial),
      1
    );
    expect(shrinkStepMeters(initial)).toBeCloseTo(15, 5);
    expect(minimumPlayableRadiusMeters(initial)).toBeCloseTo(30, 5);
    expect(computeShrinkRadius(31, initial)).toBe(minimumPlayableRadiusMeters(initial));
  });

  test("hiders win when the map reaches the 20 percent floor and at least one hider survives", () => {
    expect(
      shouldHidersWinByShrinkFloor(
        { currentRadiusMeters: 30, initialRadiusMeters: 150 },
        {
          seeker: makePlayer({ role: "SEEKER", alive: true }),
          h1: makePlayer({ role: "HIDER", alive: true }),
        }
      )
    ).toBe(true);

    expect(
      shouldHidersWinByShrinkFloor(
        { currentRadiusMeters: 30, initialRadiusMeters: 150 },
        {
          seeker: makePlayer({ role: "SEEKER", alive: true }),
        }
      )
    ).toBe(false);
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

  test("players inside the square corners survive shrink evaluation", () => {
    const centerLat = 38.9;
    const centerLng = -77.0;
    const players = {
      diagonal: makePlayer({
        userId: "diagonal",
        lat: centerLat + latOffsetDegrees(80),
        lng: centerLng + lngOffsetDegrees(80, centerLat),
        lastLocationAt: 999_500,
      }),
    };

    const eliminated = findOobPlayers(
      players,
      centerLat,
      centerLng,
      100,
      0,
      STALE_LOCATION_MS,
      1_000_000
    );

    expect(isWithinSquareBounds(
      centerLat,
      centerLng,
      players.diagonal.lat,
      players.diagonal.lng,
      100
    )).toBe(true);
    expect(distanceMeters(
      centerLat,
      centerLng,
      players.diagonal.lat,
      players.diagonal.lng
    )).toBeGreaterThan(100);
    expect(eliminated).toEqual([]);
  });
});

describe("hotspotDwellProgress", () => {
  const hotspotA = {
    id: "hs_0",
    lat: 38.9,
    lng: -77.0,
    radiusMeters: HOTSPOT_HALF_WIDTH_METERS,
    active: true,
    claimedBy: null,
    claimedAt: null,
    powerupType: "HIDER_VISION_REDUCTION" as const,
  };
  const hotspotB = {
    id: "hs_1",
    lat: 38.9002,
    lng: -77.0,
    radiusMeters: HOTSPOT_HALF_WIDTH_METERS,
    active: true,
    claimedBy: null,
    claimedAt: null,
    powerupType: "SEEKER_MINIMAP_BOOST" as const,
  };

  test("entering a hotspot starts a new stay", () => {
    const result = resolveHotspotProgressForPlayer(
      makePlayer({
        userId: "h1",
        role: "HIDER",
        lat: hotspotA.lat,
        lng: hotspotA.lng,
      }),
      { [hotspotA.id]: hotspotA },
      HOTSPOT_DWELL_SECONDS,
      HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
      2_000
    );

    expect(result.updates).toEqual({
      currentHotspotId: hotspotA.id,
      hotspotEnteredAt: 2_000,
      hotspotRewardGrantedForCurrentStay: false,
    });
    expect(result.grantedPowerup).toBeNull();
  });

  test("remaining inside the same hotspot grants exactly one reward after dwell", () => {
    const enteredAt = 2_000;
    const result = resolveHotspotProgressForPlayer(
      makePlayer({
        userId: "h1",
        role: "HIDER",
        lat: hotspotA.lat,
        lng: hotspotA.lng,
        currentHotspotId: hotspotA.id,
        hotspotEnteredAt: enteredAt,
      }),
      { [hotspotA.id]: hotspotA },
      HOTSPOT_DWELL_SECONDS,
      HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
      enteredAt + HOTSPOT_DWELL_SECONDS * 1000
    );

    expect(result.grantedPowerup).toBe("HIDER_VISION_REDUCTION");
    expect(result.updates.storedPowerupType).toBe("HIDER_VISION_REDUCTION");
    expect(result.updates.hotspotRewardGrantedForCurrentStay).toBe(true);
    expect(result.updates.nextPowerupEligibleAt).toBe(
      enteredAt + (HOTSPOT_DWELL_SECONDS + HOTSPOT_PERSONAL_COOLDOWN_SECONDS) * 1000
    );
  });

  test("leaving a hotspot resets stay progress immediately", () => {
    const result = resolveHotspotProgressForPlayer(
      makePlayer({
        userId: "h1",
        role: "HIDER",
        lat: 38.91,
        lng: -77.0,
        currentHotspotId: hotspotA.id,
        hotspotEnteredAt: 5_000,
        hotspotRewardGrantedForCurrentStay: true,
      }),
      { [hotspotA.id]: hotspotA },
      HOTSPOT_DWELL_SECONDS,
      HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
      7_000
    );

    expect(result.updates).toEqual({
      currentHotspotId: null,
      hotspotEnteredAt: null,
      hotspotRewardGrantedForCurrentStay: false,
    });
    expect(result.currentHotspotId).toBeNull();
  });

  test("cooldown and one-slot inventory prevent additional grants during the same stay", () => {
    const now = 20_000;
    const result = resolveHotspotProgressForPlayer(
      makePlayer({
        userId: "h1",
        role: "HIDER",
        lat: hotspotA.lat,
        lng: hotspotA.lng,
        currentHotspotId: hotspotA.id,
        hotspotEnteredAt: now - HOTSPOT_DWELL_SECONDS * 1000,
        nextPowerupEligibleAt: now + 60_000,
        storedPowerupType: "HIDER_VISION_REDUCTION",
      }),
      { [hotspotA.id]: hotspotA },
      HOTSPOT_DWELL_SECONDS,
      HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
      now
    );

    expect(result.updates).toEqual({});
    expect(result.grantedPowerup).toBeNull();
  });

  test("moving directly from one hotspot to another starts a fresh stay", () => {
    const result = resolveHotspotProgressForPlayer(
      makePlayer({
        userId: "host",
        role: "SEEKER",
        lat: hotspotB.lat,
        lng: hotspotB.lng,
        currentHotspotId: hotspotA.id,
        hotspotEnteredAt: 2_000,
        hotspotRewardGrantedForCurrentStay: true,
      }),
      { [hotspotA.id]: hotspotA, [hotspotB.id]: hotspotB },
      HOTSPOT_DWELL_SECONDS,
      HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
      10_000
    );

    expect(result.updates).toEqual({
      currentHotspotId: hotspotB.id,
      hotspotEnteredAt: 10_000,
      hotspotRewardGrantedForCurrentStay: false,
    });
    expect(result.currentHotspotId).toBe(hotspotB.id);
  });
});

describe("activatePowerup", () => {
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

  test("computes server-owned timing for an eligible hider", () => {
    const result = resolvePowerupActivation(
      lobby,
      makePlayer({
        userId: "h1",
        role: "HIDER",
        storedPowerupType: "HIDER_VISION_REDUCTION",
      }),
      5000
    );

    expect(result.powerup).toBe("HIDER_VISION_REDUCTION");
    expect(result.durationSeconds).toBe(hiderVisionReductionDurationSeconds(150));
    expect(result.powerupEndsAt).toBeGreaterThan(5000);
  });

  test("computes server-owned timing for an eligible seeker", () => {
    const result = resolvePowerupActivation(
      lobby,
      makePlayer({
        userId: "host",
        role: "SEEKER",
        storedPowerupType: "SEEKER_MINIMAP_BOOST",
      }),
      5000
    );

    expect(result.powerup).toBe("SEEKER_MINIMAP_BOOST");
    expect(result.durationSeconds).toBe(seekerMinimapBoostDurationSeconds(150));
  });

  test("activation rejects missing or ineligible powerups", () => {
    expect(() =>
      resolvePowerupActivation(lobby, makePlayer({ storedPowerupType: "NONE" }), 5000)
    ).toThrow("No powerup to activate");

    expect(() =>
      resolvePowerupActivation(
        lobby,
        makePlayer({
          role: "HIDER",
          storedPowerupType: "HIDER_VISION_REDUCTION",
          activePowerup: "HIDER_VISION_REDUCTION",
          powerupEndsAt: 9999,
        }),
        5000
      )
    ).toThrow("Powerup already activated");

    expect(() =>
      resolvePowerupActivation(
        lobby,
        makePlayer({ role: "HIDER", storedPowerupType: "SEEKER_MINIMAP_BOOST" }),
        5000
      )
    ).toThrow("Only the seeker can use SEEKER_MINIMAP_BOOST");
  });
});

describe("sanity helpers", () => {
  test("distanceMeters still measures roughly 100m", () => {
    const d = distanceMeters(0, 0, 0.0009, 0);

    expect(d).toBeGreaterThan(90);
    expect(d).toBeLessThan(110);
  });

  test("winner resolution favors hiders if seeker dies and seeker if no hiders remain", () => {
    expect(
      resolveWinner({
        seeker: makePlayer({ role: "SEEKER", alive: false }),
        h1: makePlayer({ role: "HIDER", alive: true }),
      })
    ).toBe("HIDERS");

    expect(
      resolveWinner({
        seeker: makePlayer({ role: "SEEKER", alive: true }),
      })
    ).toBe("SEEKER");

    expect(
      resolveWinner({
        seeker: makePlayer({ role: "SEEKER", alive: true }),
        h1: makePlayer({ role: "HIDER", alive: true }),
      })
    ).toBeNull();
  });
});

describe("gameBalance", () => {
  test("default config mirrors exported formulas", () => {
    const radius = 150;
    const config = defaultConfig(radius);

    expect(config.headStartSeconds).toBe(headStartSeconds(radius));
    expect(config.shrinkIntervalSeconds).toBe(120);
    expect(config.shrinkWarningSeconds).toBe(SHRINK_WARNING_SECONDS);
    expect(config.catchEligibilityRadiusMeters).toBe(catchEligibilityRadiusMeters(radius));
    expect(config.hotspotRadiusMeters).toBe(hotspotRadiusMeters(radius));
    expect(config.hotspotSideLengthMeters).toBe(HOTSPOT_SIDE_LENGTH_METERS);
    expect(config.hotspotMaxCoverageFraction).toBe(HOTSPOT_MAX_COVERAGE_FRACTION);
    expect(config.hotspotDwellSeconds).toBe(HOTSPOT_DWELL_SECONDS);
    expect(config.hotspotPersonalCooldownSeconds).toBe(HOTSPOT_PERSONAL_COOLDOWN_SECONDS);
    expect(config.seekerFreezeRadiusMeters).toBe(seekerFreezeRadiusMeters(radius));
    expect(config.startClusterRadiusMeters).toBe(startClusterRadiusMeters(radius));
    expect(config.hiderVisionReductionDurationSeconds).toBe(
      hiderVisionReductionDurationSeconds(radius)
    );
    expect(config.seekerMinimapBoostDurationSeconds).toBe(
      seekerMinimapBoostDurationSeconds(radius)
    );
    expect(config.hiderVisionReductionMultiplier).toBe(HIDER_VISION_REDUCTION_MULTIPLIER);
    expect(config.seekerMinimapBoostMultiplier).toBe(SEEKER_MINIMAP_BOOST_MULTIPLIER);
  });

  test("formula helpers clamp at documented lower and upper bounds", () => {
    expect(catchEligibilityRadiusMeters(10)).toBe(10);
    expect(catchEligibilityRadiusMeters(500)).toBe(35);
    expect(hotspotRadiusMeters(10)).toBe(HOTSPOT_HALF_WIDTH_METERS);
    expect(hotspotRadiusMeters(500)).toBe(HOTSPOT_HALF_WIDTH_METERS);
    expect(seekerFreezeRadiusMeters(10)).toBe(8);
    expect(seekerFreezeRadiusMeters(500)).toBe(20);
    expect(startClusterRadiusMeters(10)).toBe(10);
    expect(startClusterRadiusMeters(500)).toBe(30);
    expect(hiderVisionReductionDurationSeconds(10)).toBe(8);
    expect(hiderVisionReductionDurationSeconds(500)).toBe(25);
    expect(seekerMinimapBoostDurationSeconds(10)).toBe(6);
    expect(seekerMinimapBoostDurationSeconds(500)).toBe(20);
    expect(shrinkIntervalSeconds(10)).toBe(120);
    expect(shrinkIntervalSeconds(500)).toBe(120);
    expect(hotspotCount(10)).toBe(0);
    expect(hotspotCount(150)).toBe(430);
  });
});
