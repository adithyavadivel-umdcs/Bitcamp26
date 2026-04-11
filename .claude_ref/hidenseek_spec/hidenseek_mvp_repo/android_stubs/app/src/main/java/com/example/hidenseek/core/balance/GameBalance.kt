package com.example.hidenseek.core.balance

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

object GameBalance {

    private fun clamp(value: Double, min: Double, max: Double): Double {
        return value.coerceIn(min, max)
    }

    private fun clampInt(value: Int, min: Int, max: Int): Int {
        return value.coerceIn(min, max)
    }

    data class Config(
        val mapRadiusMeters: Double,
        val hotspotRadiusMeters: Double,
        val catchEligibilityRadiusMeters: Double,
        val seekerFreezeRadiusMeters: Double,
        val startClusterRadiusMeters: Double,
        val headStartSeconds: Int,
        val shrinkIntervalSeconds: Int,
        val shrinkWarningSeconds: Int,
        val shrinkGraceSeconds: Int,
        val seekerFreezeViolationSeconds: Int,
        val disconnectTimeoutSeconds: Int,
        val hiderInvisibilityDurationSeconds: Int,
        val seekerRevealAllDurationSeconds: Int,
        val hotspotCount: Int,
        val shrinkStepMeters: Double,
        val minimumPlayableRadiusMeters: Double,
    )

    fun fromMapRadius(mapRadiusMeters: Double): Config {
        val scale = mapRadiusMeters / 100.0

        val hotspotRadiusMeters = clamp(8.0 * scale, 6.0, 20.0)
        val catchEligibilityRadiusMeters = clamp(15.0 * scale, 10.0, 35.0)
        val seekerFreezeRadiusMeters = clamp(12.0 * scale, 8.0, 20.0)
        val startClusterRadiusMeters = clamp(20.0 * scale, 10.0, 30.0)

        val headStartSeconds = clamp(20.0 * scale, 15.0, 60.0).roundToInt()
        val shrinkIntervalSeconds = clamp(60.0 * scale, 45.0, 120.0).roundToInt()
        val hiderInvisibilityDurationSeconds = clamp(12.0 * scale, 8.0, 25.0).roundToInt()
        val seekerRevealAllDurationSeconds = clamp(8.0 * scale, 6.0, 20.0).roundToInt()

        val hotspotCount = clampInt((mapRadiusMeters / 40.0).roundToInt(), 3, 8)
        val shrinkStepMeters = max(mapRadiusMeters * 0.15, 10.0)
        val minimumPlayableRadiusMeters = clamp(mapRadiusMeters * 0.25, 40.0, 120.0)

        return Config(
            mapRadiusMeters = mapRadiusMeters,
            hotspotRadiusMeters = hotspotRadiusMeters,
            catchEligibilityRadiusMeters = catchEligibilityRadiusMeters,
            seekerFreezeRadiusMeters = seekerFreezeRadiusMeters,
            startClusterRadiusMeters = startClusterRadiusMeters,
            headStartSeconds = headStartSeconds,
            shrinkIntervalSeconds = shrinkIntervalSeconds,
            shrinkWarningSeconds = 10,
            shrinkGraceSeconds = 3,
            seekerFreezeViolationSeconds = 2,
            disconnectTimeoutSeconds = 20,
            hiderInvisibilityDurationSeconds = hiderInvisibilityDurationSeconds,
            seekerRevealAllDurationSeconds = seekerRevealAllDurationSeconds,
            hotspotCount = hotspotCount,
            shrinkStepMeters = shrinkStepMeters,
            minimumPlayableRadiusMeters = minimumPlayableRadiusMeters,
        )
    }

    fun levelFromTotalSteps(totalSteps: Long): Int {
        return floor(totalSteps / 2000.0).toInt() + 1
    }
}
