/**
 * Server-side game balance constants.
 * Source of truth: FORMULAS.md in the spec package.
 * If FORMULAS.md changes, update this file AND GameBalance.java to match.
 *
 * All radius/time values are derived from mapRadiusMeters so that
 * small and large maps feel proportional.
 */

export const DEFAULT_MAP_RADIUS_METERS = 150;
export const HOTSPOT_SIDE_LENGTH_FEET = 15;
export const HOTSPOT_SIDE_LENGTH_METERS = HOTSPOT_SIDE_LENGTH_FEET * 0.3048;
export const HOTSPOT_HALF_WIDTH_METERS = HOTSPOT_SIDE_LENGTH_METERS / 2;
export const HOTSPOT_MAX_COVERAGE_FRACTION = 0.10;
export const HOTSPOT_DWELL_SECONDS = 15;
export const HOTSPOT_PERSONAL_COOLDOWN_SECONDS = 120;

// ── Internal helpers ─────────────────────────────────────────────────────────

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function scale(mapRadiusMeters: number): number {
  return mapRadiusMeters / 100.0;
}

// ── Radius formulas ──────────────────────────────────────────────────────────

/** Stored hotspot half-width in meters for a fixed 15ft by 15ft square hotspot. */
export function hotspotRadiusMeters(mapRadiusMeters: number): number {
  return HOTSPOT_HALF_WIDTH_METERS;
}

/** Seeker must be within this distance of a hider to submit a catch code. */
export function catchEligibilityRadiusMeters(mapRadiusMeters: number): number {
  return clamp(15.0 * scale(mapRadiusMeters), 10.0, 35.0);
}

/** Seeker freeze zone radius during head start. */
export function seekerFreezeRadiusMeters(mapRadiusMeters: number): number {
  return clamp(12.0 * scale(mapRadiusMeters), 8.0, 20.0);
}

/** All players must cluster within this radius of the host before game start. */
export function startClusterRadiusMeters(mapRadiusMeters: number): number {
  return clamp(20.0 * scale(mapRadiusMeters), 10.0, 30.0);
}

// ── Time formulas ────────────────────────────────────────────────────────────

/** Seconds hiders get to run before seeker can move. */
export function headStartSeconds(mapRadiusMeters: number): number {
  return clamp(20.0 * scale(mapRadiusMeters), 15.0, 60.0);
}

/** Seconds between shrink events. */
export function shrinkIntervalSeconds(_mapRadiusMeters?: number): number {
  return 60;
}

/** Warning shown to clients before next shrink fires. */
export const SHRINK_WARNING_SECONDS = 10;

/**
 * Extra distance buffer (meters) added to the new radius when evaluating
 * OOB eliminations after a shrink. Accounts for GPS inaccuracy.
 * BUG_RISK_RULES: "Use generous radii" and "Respect accuracyMeters".
 */
export const SHRINK_GRACE_METERS = 15;

/**
 * Location samples older than this are treated as stale during shrink
 * evaluation. Stale locations skip elimination (benefit of the doubt).
 * BUG_RISK_RULES: "Do not eliminate based on stale location".
 */
export const STALE_LOCATION_MS = 30_000;

/**
 * If a player's lastLocationAt is older than this, they are treated as
 * disconnected and eliminated. 5 location cycles × 3s max interval.
 */
export const DISCONNECT_STALE_MS = 15_000;

/** Powerup duration for hider-specific seeker vision reduction (seconds). */
export function hiderVisionReductionDurationSeconds(mapRadiusMeters: number): number {
  return clamp(12.0 * scale(mapRadiusMeters), 8.0, 25.0);
}

/** Powerup duration for seeker minimap boost (seconds). */
export function seekerMinimapBoostDurationSeconds(mapRadiusMeters: number): number {
  return clamp(8.0 * scale(mapRadiusMeters), 6.0, 20.0);
}

/** While active, seeker minimap width and height are multiplied by this value. */
export const SEEKER_MINIMAP_BOOST_MULTIPLIER = 2.0;

/** While active on a hider, seeker-specific visibility for that hider is scaled by this value. */
export const HIDER_VISION_REDUCTION_MULTIPLIER = 0.6;

// ── Hotspot formulas ─────────────────────────────────────────────────────────

/** Number of hotspots placed on the map. */
export function hotspotCount(mapRadiusMeters: number): number {
  const mapArea = Math.pow(mapRadiusMeters * 2, 2);
  const hotspotArea = Math.pow(HOTSPOT_SIDE_LENGTH_METERS, 2);
  const rawLimit = (mapArea * HOTSPOT_MAX_COVERAGE_FRACTION) / hotspotArea;
  if (rawLimit <= 0) {
    return 0;
  }
  const maxCount = Number.isInteger(rawLimit) ? rawLimit - 1 : Math.floor(rawLimit);
  return Math.max(0, maxCount);
}

// ── Shrink formula ───────────────────────────────────────────────────────────

/** Meters to subtract from current radius on each shrink event. Flat 20ft. */
export function shrinkStepMeters(_initialRadiusMeters?: number): number {
  return 20 * 0.3048; // 6.096 m
}

/** Half-width at which the map stops shrinking and hiders win. Flat 25ft (half of 50ft floor). */
export function minimumPlayableRadiusMeters(_initialRadiusMeters?: number): number {
  return 25 * 0.3048; // 7.62 m
}

// ── Config block ─────────────────────────────────────────────────────────────

/** Full config block written to Firebase on lobby creation. */
export function defaultConfig(mapRadiusMeters: number): Record<string, number> {
  return {
    headStartSeconds: headStartSeconds(mapRadiusMeters),
    shrinkIntervalSeconds: shrinkIntervalSeconds(mapRadiusMeters),
    shrinkWarningSeconds: SHRINK_WARNING_SECONDS,
    shrinkGraceMeters: SHRINK_GRACE_METERS,
    catchEligibilityRadiusMeters: catchEligibilityRadiusMeters(mapRadiusMeters),
    hotspotRadiusMeters: hotspotRadiusMeters(mapRadiusMeters),
    hotspotSideLengthMeters: HOTSPOT_SIDE_LENGTH_METERS,
    hotspotMaxCoverageFraction: HOTSPOT_MAX_COVERAGE_FRACTION,
    hotspotDwellSeconds: HOTSPOT_DWELL_SECONDS,
    hotspotPersonalCooldownSeconds: HOTSPOT_PERSONAL_COOLDOWN_SECONDS,
    seekerFreezeRadiusMeters: seekerFreezeRadiusMeters(mapRadiusMeters),
    startClusterRadiusMeters: startClusterRadiusMeters(mapRadiusMeters),
    hiderVisionReductionDurationSeconds: hiderVisionReductionDurationSeconds(mapRadiusMeters),
    seekerMinimapBoostDurationSeconds: seekerMinimapBoostDurationSeconds(mapRadiusMeters),
    hiderVisionReductionMultiplier: HIDER_VISION_REDUCTION_MULTIPLIER,
    seekerMinimapBoostMultiplier: SEEKER_MINIMAP_BOOST_MULTIPLIER,
  };
}
