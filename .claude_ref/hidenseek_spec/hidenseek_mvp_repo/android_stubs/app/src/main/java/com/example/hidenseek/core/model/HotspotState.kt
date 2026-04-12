package com.example.hidenseek.core.model

data class HotspotState(
    val id: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Double,
    val powerupType: PowerupType,
    val active: Boolean = true,
    val claimedBy: String? = null,
    val claimedAt: Long? = null,
)
