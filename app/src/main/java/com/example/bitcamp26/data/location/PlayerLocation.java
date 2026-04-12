package com.example.bitcamp26.data.location;

/**
 * Simple data model for a player's geographical location.
 */
public class PlayerLocation {
    private final String userId;
    private final double latitude;
    private final double longitude;
    private final long timestamp;

    public PlayerLocation(String userId, double latitude, double longitude, long timestamp) {
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getTimestamp() { return timestamp; }
}
