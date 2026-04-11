package com.example.bitcamp26.data.location;

/**
 * Lightweight snapshot of a player's location at a specific moment in time.
 *
 * This is the output type of real-time GPS updates. It intentionally has
 * no game logic fields — it only carries what the device measured.
 *
 * Later: pass this to Firebase via SubmitLocationUseCase or a Firebase repo.
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

    public String getUserId() {
        return userId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /** Unix epoch ms — from Location.getTime(). */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PlayerLocation{"
                + "userId='" + userId + '\''
                + ", lat=" + latitude
                + ", lng=" + longitude
                + ", ts=" + timestamp
                + '}';
    }
}
