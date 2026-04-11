package com.example.bitcamp26.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Mirrors lobbies/{lobbyId} in Firebase Realtime Database.
 */
public class Lobby {
    public String hostId;
    public String inviteCode;
    public String state;           // matches GameState enum names
    public long createdAt;
    public double mapCenterLat;
    public double mapCenterLng;
    public double initialRadiusMeters;
    public double currentRadiusMeters;
    public double rallyPointLat;
    public double rallyPointLng;
    public String seekerId;
    public Map<String, Object> config;
    public Map<String, Object> timing;
    public Map<String, Object> results;

    // Required by Firebase
    public Lobby() {}

    public Lobby(String hostId, String inviteCode, double mapCenterLat, double mapCenterLng, double initialRadiusMeters) {
        this.hostId = hostId;
        this.inviteCode = inviteCode;
        this.state = GameState.LOBBY.name();
        this.createdAt = System.currentTimeMillis();
        this.mapCenterLat = mapCenterLat;
        this.mapCenterLng = mapCenterLng;
        this.initialRadiusMeters = initialRadiusMeters;
        this.currentRadiusMeters = initialRadiusMeters;
        // Rally point: offset slightly from center (server can refine this)
        this.rallyPointLat = mapCenterLat + 0.002;
        this.rallyPointLng = mapCenterLng;
        this.seekerId = null;
        this.config = GameBalance.defaultConfig(initialRadiusMeters);
        this.timing = new HashMap<>();
        this.results = new HashMap<>();
    }

    public GameState getGameState() {
        if (state == null) return GameState.LOBBY;
        try {
            return GameState.valueOf(state);
        } catch (IllegalArgumentException e) {
            return GameState.LOBBY;
        }
    }
}
