package com.example.bitcamp26.core.model;

public class HotspotState {
    public String id;
    public double lat;
    public double lng;
    public double radiusMeters;
    public boolean active;
    public String claimedBy;   // userId or null
    public Long claimedAt;     // timestamp or null
    public String powerupType; // "HIDER_VISION_REDUCTION" | "SEEKER_MINIMAP_BOOST"

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public double getRadiusMeters() { return radiusMeters; }
    public void setRadiusMeters(double radiusMeters) { this.radiusMeters = radiusMeters; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getClaimedBy() { return claimedBy; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }

    public Long getClaimedAt() { return claimedAt; }
    public void setClaimedAt(Long claimedAt) { this.claimedAt = claimedAt; }

    public String getPowerupTypeString() { return powerupType; }
    public void setPowerupTypeString(String powerupType) { this.powerupType = powerupType; }

    public void setPowerupType(PowerupType type) {
        if (type == null) {
            this.powerupType = null;
        } else {
            this.powerupType = type.name();
        }
    }

    // Required by Firebase
    public HotspotState() {}

    public HotspotState(String id, double lat, double lng, double radiusMeters, String powerupType) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.radiusMeters = radiusMeters;
        this.active = true;
        this.claimedBy = null;
        this.claimedAt = null;
        this.powerupType = powerupType;
    }

    public PowerupType getPowerupType() {
        if (powerupType == null || powerupType.trim().isEmpty()) {
            return null;
        }

        try {
            return PowerupType.valueOf(powerupType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
