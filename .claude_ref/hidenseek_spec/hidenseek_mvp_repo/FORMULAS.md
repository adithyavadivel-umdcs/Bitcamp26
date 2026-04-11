# Game Balance Formulas

All formulas are derived from mapRadiusMeters.

## Base scale
scale = mapRadiusMeters / 100.0

## Clamp helper
clamp(value, min, max)

## Radius formulas
hotspotRadiusMeters = clamp(8.0 * scale, 6.0, 20.0)
catchEligibilityRadiusMeters = clamp(15.0 * scale, 10.0, 35.0)
seekerFreezeRadiusMeters = clamp(12.0 * scale, 8.0, 20.0)
startClusterRadiusMeters = clamp(20.0 * scale, 10.0, 30.0)

## Time formulas
headStartSeconds = clamp(20.0 * scale, 15.0, 60.0)
shrinkIntervalSeconds = clamp(60.0 * scale, 45.0, 120.0)
shrinkWarningSeconds = 10
shrinkGraceSeconds = 3
seekerFreezeViolationSeconds = 2
disconnectTimeoutSeconds = 20
hiderInvisibilityDurationSeconds = clamp(12.0 * scale, 8.0, 25.0)
seekerRevealAllDurationSeconds = clamp(8.0 * scale, 6.0, 20.0)

## Hotspot formulas
hotspotCount = clampInt(round(mapRadiusMeters / 40.0), 3, 8)

## Shrink formula
initialRadiusMeters = mapRadiusMeters
shrinkStepMeters = max(initialRadiusMeters * 0.15, 10.0)
minimumPlayableRadiusMeters = clamp(initialRadiusMeters * 0.25, 40.0, 120.0)

## Level formula
level = floor(totalSteps / 2000) + 1
