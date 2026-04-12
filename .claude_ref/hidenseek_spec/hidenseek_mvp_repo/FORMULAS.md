# Game Balance Formulas

All formulas are derived from mapHalfWidthMeters.

## Base scale
scale = mapHalfWidthMeters / 100.0

## Clamp helper
clamp(value, min, max)

## Radius formulas
hotspotSideLengthFeet = 15
hotspotSideLengthMeters = 15 * 0.3048
hotspotHalfWidthMeters = hotspotSideLengthMeters / 2
catchEligibilityRadiusMeters = clamp(15.0 * scale, 10.0, 35.0)
seekerFreezeRadiusMeters = clamp(12.0 * scale, 8.0, 20.0)
startClusterRadiusMeters = clamp(20.0 * scale, 10.0, 30.0)

## Time formulas
headStartSeconds = clamp(20.0 * scale, 15.0, 60.0)
shrinkIntervalSeconds = 120
shrinkWarningSeconds = 10
shrinkGraceSeconds = 3
seekerFreezeViolationSeconds = 2
disconnectTimeoutSeconds = 20
hiderVisionReductionDurationSeconds = clamp(12.0 * scale, 8.0, 25.0)
seekerMinimapBoostDurationSeconds = clamp(8.0 * scale, 6.0, 20.0)
hotspotDwellSeconds = 15
hotspotPersonalCooldownSeconds = 120

## Hotspot formulas
mapAreaSquareMeters = (mapHalfWidthMeters * 2) ^ 2
hotspotAreaSquareMeters = hotspotSideLengthMeters ^ 2
hotspotMaxCoverageFraction = 0.10
hotspotCount = largest integer N such that N * hotspotAreaSquareMeters < mapAreaSquareMeters * hotspotMaxCoverageFraction

## Shrink formula
initialHalfWidthMeters = mapHalfWidthMeters
shrinkStepMeters = initialHalfWidthMeters * 0.10
minimumPlayableHalfWidthMeters = initialHalfWidthMeters * 0.20

## Visibility formulas
seekerMinimapBoostMultiplier = 2.0
hiderVisionReductionMultiplier = 0.6

## Level formula
level = floor(totalSteps / 2000) + 1
