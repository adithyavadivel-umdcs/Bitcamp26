import {
  DEFAULT_MAP_RADIUS_METERS,
  HIDER_VISION_REDUCTION_MULTIPLIER,
  HOTSPOT_DWELL_SECONDS,
  HOTSPOT_HALF_WIDTH_METERS,
  HOTSPOT_MAX_COVERAGE_FRACTION,
  HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
  HOTSPOT_SIDE_LENGTH_METERS,
  SEEKER_MINIMAP_BOOST_MULTIPLIER,
  SHRINK_GRACE_METERS,
  SHRINK_WARNING_SECONDS,
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

describe("gameBalance.ts", () => {
  test("exports current fixed hotspot geometry and coverage rules", () => {
    expect(DEFAULT_MAP_RADIUS_METERS).toBe(150);
    expect(HOTSPOT_SIDE_LENGTH_METERS).toBeCloseTo(15 * 0.3048, 5);
    expect(HOTSPOT_HALF_WIDTH_METERS).toBeCloseTo(HOTSPOT_SIDE_LENGTH_METERS / 2, 5);
    expect(HOTSPOT_MAX_COVERAGE_FRACTION).toBe(0.1);
    expect(hotspotRadiusMeters(50)).toBeCloseTo(HOTSPOT_HALF_WIDTH_METERS, 5);
  });

  test("exports current flat shrink rules", () => {
    expect(shrinkIntervalSeconds(10)).toBe(60);
    expect(SHRINK_WARNING_SECONDS).toBe(10);
    expect(SHRINK_GRACE_METERS).toBe(15);
    expect(shrinkStepMeters(150)).toBeCloseTo(20 * 0.3048, 5);
    expect(minimumPlayableRadiusMeters(150)).toBeCloseTo(25 * 0.3048, 5);
  });

  test("formula helpers clamp as currently implemented", () => {
    expect(catchEligibilityRadiusMeters(10)).toBe(10);
    expect(catchEligibilityRadiusMeters(500)).toBe(35);
    expect(seekerFreezeRadiusMeters(10)).toBe(8);
    expect(seekerFreezeRadiusMeters(500)).toBe(20);
    expect(startClusterRadiusMeters(10)).toBe(10);
    expect(startClusterRadiusMeters(500)).toBe(30);
    expect(headStartSeconds(10)).toBe(15);
    expect(headStartSeconds(500)).toBe(60);
    expect(hiderVisionReductionDurationSeconds(10)).toBe(8);
    expect(seekerMinimapBoostDurationSeconds(500)).toBe(20);
  });

  test("hotspot count remains below 10 percent total map coverage", () => {
    const radius = 150;
    const count = hotspotCount(radius);
    const mapArea = Math.pow(radius * 2, 2);
    const hotspotArea = Math.pow(HOTSPOT_SIDE_LENGTH_METERS, 2);

    expect(count * hotspotArea).toBeLessThan(mapArea * HOTSPOT_MAX_COVERAGE_FRACTION);
  });

  test("defaultConfig mirrors exported constants and formulas", () => {
    const radius = 150;
    const config = defaultConfig(radius);

    expect(config.hotspotDwellSeconds).toBe(HOTSPOT_DWELL_SECONDS);
    expect(config.hotspotPersonalCooldownSeconds).toBe(HOTSPOT_PERSONAL_COOLDOWN_SECONDS);
    expect(config.hiderVisionReductionMultiplier).toBe(HIDER_VISION_REDUCTION_MULTIPLIER);
    expect(config.seekerMinimapBoostMultiplier).toBe(SEEKER_MINIMAP_BOOST_MULTIPLIER);
  });
});
