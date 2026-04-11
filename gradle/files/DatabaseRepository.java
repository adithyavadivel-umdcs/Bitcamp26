package com.example.bitcamp26.core.repository;

import androidx.annotation.NonNull;

import com.example.bitcamp26.core.model.Hotspot;
import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Single access point for all Firebase Realtime Database operations.
 * All writes go through here. Screens subscribe via listeners returned from here.
 */
public class DatabaseRepository {

    private static DatabaseRepository instance;
    private final DatabaseReference root;

    private DatabaseRepository() {
        root = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized DatabaseRepository getInstance() {
        if (instance == null) instance = new DatabaseRepository();
        return instance;
    }

    // ── References ────────────────────────────────────────────────────────────

    public DatabaseReference lobbyRef(String lobbyId) {
        return root.child("lobbies").child(lobbyId);
    }

    public DatabaseReference playerRef(String lobbyId, String userId) {
        return root.child("lobbies").child(lobbyId).child("players").child(userId);
    }

    public DatabaseReference playersRef(String lobbyId) {
        return root.child("lobbies").child(lobbyId).child("players");
    }

    public DatabaseReference hotspotRef(String lobbyId, String hotspotId) {
        return root.child("lobbies").child(lobbyId).child("hotspots").child(hotspotId);
    }

    public DatabaseReference hotspotsRef(String lobbyId) {
        return root.child("lobbies").child(lobbyId).child("hotspots");
    }

    public DatabaseReference eventsRef(String lobbyId) {
        return root.child("lobbies").child(lobbyId).child("events");
    }

    public DatabaseReference userRef(String userId) {
        return root.child("users").child(userId);
    }

    // ── Lobby operations ─────────────────────────────────────────────────────

    /** Write a new lobby. Returns the lobby ID. */
    public String createLobby(Lobby lobby) {
        DatabaseReference ref = root.child("lobbies").push();
        String lobbyId = ref.getKey();
        ref.setValue(lobby);
        return lobbyId;
    }

    /** Subscribe to the full lobby object. */
    public ValueEventListener subscribeLobby(String lobbyId, ValueEventListener listener) {
        lobbyRef(lobbyId).addValueEventListener(listener);
        return listener;
    }

    public void unsubscribeLobby(String lobbyId, ValueEventListener listener) {
        lobbyRef(lobbyId).removeEventListener(listener);
    }

    /** Update just the game state field (server cloud function also does this). */
    public void setLobbyState(String lobbyId, String state) {
        lobbyRef(lobbyId).child("state").setValue(state);
    }

    public void setCurrentRadius(String lobbyId, double radiusMeters) {
        lobbyRef(lobbyId).child("currentRadiusMeters").setValue(radiusMeters);
    }

    // ── Player operations ─────────────────────────────────────────────────────

    /** Add or update a player in the lobby. */
    public void setPlayer(String lobbyId, Player player) {
        playerRef(lobbyId, player.userId).setValue(player);
    }

    /** Update player location fields only (called every 2.5 seconds). */
    public void updatePlayerLocation(String lobbyId, String userId, double lat, double lng, double accuracy) {
        Map<String, Object> update = new HashMap<>();
        update.put("lat", lat);
        update.put("lng", lng);
        update.put("accuracyMeters", accuracy);
        update.put("lastLocationAt", System.currentTimeMillis());
        playerRef(lobbyId, userId).updateChildren(update);
    }

    /** Set player ready status. */
    public void setPlayerReady(String lobbyId, String userId, boolean ready) {
        playerRef(lobbyId, userId).child("ready").setValue(ready);
    }

    /** Set player alive status (server also does this on elimination). */
    public void setPlayerAlive(String lobbyId, String userId, boolean alive) {
        playerRef(lobbyId, userId).child("alive").setValue(alive);
    }

    /** Update step count for this match. */
    public void updateSteps(String lobbyId, String userId, long steps) {
        playerRef(lobbyId, userId).child("currentMatchSteps").setValue(steps);
    }

    /** Subscribe to all players in a lobby. */
    public ValueEventListener subscribePlayers(String lobbyId, ValueEventListener listener) {
        playersRef(lobbyId).addValueEventListener(listener);
        return listener;
    }

    public void unsubscribePlayers(String lobbyId, ValueEventListener listener) {
        playersRef(lobbyId).removeEventListener(listener);
    }

    // ── Hotspot operations ────────────────────────────────────────────────────

    public void setHotspot(String lobbyId, Hotspot hotspot) {
        hotspotRef(lobbyId, hotspot.id).setValue(hotspot);
    }

    public ValueEventListener subscribeHotspots(String lobbyId, ValueEventListener listener) {
        hotspotsRef(lobbyId).addValueEventListener(listener);
        return listener;
    }

    public void unsubscribeHotspots(String lobbyId, ValueEventListener listener) {
        hotspotsRef(lobbyId).removeEventListener(listener);
    }

    // ── Event log ─────────────────────────────────────────────────────────────

    /** Push a game event into the events log (for debugging / future use). */
    public void pushEvent(String lobbyId, String type, Map<String, Object> payload) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("createdAt", System.currentTimeMillis());
        event.put("payload", payload);
        eventsRef(lobbyId).push().setValue(event);
    }

    // ── User profile ──────────────────────────────────────────────────────────

    public void createOrUpdateUser(String userId, String displayName) {
        Map<String, Object> user = new HashMap<>();
        user.put("displayName", displayName);
        user.put("createdAt", System.currentTimeMillis());
        userRef(userId).updateChildren(user);
    }

    public void addStepsToUser(String userId, long stepsToAdd) {
        userRef(userId).child("totalSteps").get().addOnSuccessListener(snapshot -> {
            long current = snapshot.exists() ? snapshot.getValue(Long.class) : 0L;
            userRef(userId).child("totalSteps").setValue(current + stepsToAdd);
        });
    }
}
