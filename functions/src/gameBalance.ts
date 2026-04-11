/**
 * Server-side game balance constants.
 * Source of truth: FORMULAS.md in the spec package.
 * If FORMULAS.md changes, update this file AND GameBalance.java to match.
 *
 * All radius/time values are derived from mapRadiusMeters so that
 * small and large maps feel proportional.
 */

export const DEFAULT_MAP_RADIUS_METERS = 150;

// ── Internal helpers ─────────────────────────────────────────────────────────

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function scale(mapRadiusMeters: number): number {
  return mapRadiusMeters / 100.0;
}

// ── Radius formulas ──────────────────────────────────────────────────────────

/** Radius of each hotspot's claim circle (meters). */
export function hotspotRadiusMeters(mapRadiusMeters: number): number {
  return clamp(8.0 * scale(mapRadiusMeters), 6.0, 20.0);
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
export function shrinkIntervalSeconds(mapRadiusMeters: number): number {
  return clamp(60.0 * scale(mapRadiusMeters), 45.0, 120.0);
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

/** Powerup duration for hider invisibility (seconds). */
export function hiderInvisibilityDurationSeconds(mapRadiusMeters: number): number {
  return clamp(12.0 * scale(mapRadiusMeters), 8.0, 25.0);
}

/** Powerup duration for seeker reveal-all (seconds). */
export function seekerRevealAllDurationSeconds(mapRadiusMeters: number): number {
  return clamp(8.0 * scale(mapRadiusMeters), 6.0, 20.0);
}

// ── Hotspot formulas ─────────────────────────────────────────────────────────

/** Number of hotspots placed on the map. */
export function hotspotCount(mapRadiusMeters: number): number {
  return clamp(Math.round(mapRadiusMeters / 40.0), 3, 8);
}

// ── Shrink formula ───────────────────────────────────────────────────────────

/** Meters to subtract from current radius on each shrink event. */
export function shrinkStepMeters(initialRadiusMeters: number): number {
  return Math.max(initialRadiusMeters * 0.15, 10.0);
}

/** Radius at which the circle stops shrinking. */
export function minimumPlayableRadiusMeters(initialRadiusMeters: number): number {
  return clamp(initialRadiusMeters * 0.25, 40.0, 120.0);
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
    seekerFreezeRadiusMeters: seekerFreezeRadiusMeters(mapRadiusMeters),
    startClusterRadiusMeters: startClusterRadiusMeters(mapRadiusMeters),
    hiderInvisibilityDurationSeconds: hiderInvisibilityDurationSeconds(mapRadiusMeters),
    seekerRevealAllDurationSeconds: seekerRevealAllDurationSeconds(mapRadiusMeters),
  };
}
