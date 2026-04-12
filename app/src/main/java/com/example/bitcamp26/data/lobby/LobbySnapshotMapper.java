package com.example.bitcamp26.data.lobby;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.Lobby;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class LobbySnapshotMapper {

    private LobbySnapshotMapper() {
    }

    @NonNull
    static Lobby fromSnapshot(@NonNull DataSnapshot snapshot) {
        Lobby lobby = new Lobby();
        String inviteCode = getString(snapshot.child("inviteCode"));
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            inviteCode = snapshot.getKey();
        }

        lobby.setInviteCode(inviteCode);
        lobby.setState(getString(snapshot.child("state")));
        lobby.setHostId(getString(snapshot.child("hostId")));
        lobby.setSeekerId(getString(snapshot.child("seekerId")));
        lobby.setCreatedAt(getLong(snapshot.child("createdAt")));
        lobby.setAllReady(getBoolean(snapshot.child("allReady")));
        lobby.setMapCenterLat(getDouble(snapshot.child("mapCenterLat")));
        lobby.setMapCenterLng(getDouble(snapshot.child("mapCenterLng")));
        lobby.setInitialRadiusMeters(getDouble(snapshot.child("initialRadiusMeters")));
        lobby.setCurrentRadiusMeters(getDouble(snapshot.child("currentRadiusMeters")));
        lobby.setGameStartAt(getLong(snapshot.child("timing").child("gameStartAt")));
        lobby.setNextShrinkAt(getLong(snapshot.child("timing").child("nextShrinkAt")));

        List<Player> players = new ArrayList<>();
        for (DataSnapshot playerSnapshot : snapshot.child("players").getChildren()) {
            players.add(toPlayer(playerSnapshot));
        }

        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player first, Player second) {
                if (first == null && second == null) {
                    return 0;
                }
                if (first == null) {
                    return 1;
                }
                if (second == null) {
                    return -1;
                }
                boolean firstIsSeeker = first.getRole() == PlayerRole.SEEKER;
                boolean secondIsSeeker = second.getRole() == PlayerRole.SEEKER;
                if (firstIsSeeker != secondIsSeeker) {
                    return firstIsSeeker ? -1 : 1;
                }
                String firstName = first.getDisplayName() != null ? first.getDisplayName() : "";
                String secondName = second.getDisplayName() != null ? second.getDisplayName() : "";
                return firstName.compareToIgnoreCase(secondName);
            }
        });

        lobby.setPlayers(players);
        lobby.setPlayerCount(players.size());
        return lobby;
    }

    @NonNull
    private static Player toPlayer(@NonNull DataSnapshot snapshot) {
        Player player = new Player();
        String userId = getString(snapshot.child("userId"));
        if (userId == null || userId.trim().isEmpty()) {
            userId = snapshot.getKey();
        }

        player.setUserId(userId);
        player.setDisplayName(getString(snapshot.child("displayName")));
        player.setRole(parseRole(getString(snapshot.child("role"))));
        player.setAlive(getBoolean(snapshot.child("alive"), true));
        player.setConnected(getBoolean(snapshot.child("connected"), true));
        player.setReady(getBoolean(snapshot.child("ready")));
        player.setCatchCode(getString(snapshot.child("catchCode")));
        player.setCatchEligible(getBoolean(snapshot.child("catchEligible")));
        player.setLatitude(getDouble(snapshot.child("lat")));
        player.setLongitude(getDouble(snapshot.child("lng")));
        player.setAccuracyMeters(getDouble(snapshot.child("accuracyMeters")));
        player.setLastLocationUpdatedAt(getLong(snapshot.child("lastLocationAt")));
        player.setHeldPowerup(parsePowerup(getString(snapshot.child("storedPowerupType"))));
        player.setActivePowerup(parsePowerup(getString(snapshot.child("activePowerup"))));
        player.setPowerupExpiresAt(getLong(snapshot.child("powerupEndsAt")));
        player.setNextPowerupEligibleAt(getLong(snapshot.child("nextPowerupEligibleAt")));
        player.setCaughtAt(getLong(snapshot.child("eliminatedAt")));
        return player;
    }

    @NonNull
    private static PlayerRole parseRole(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return PlayerRole.UNASSIGNED;
        }
        try {
            return PlayerRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return PlayerRole.UNASSIGNED;
        }
    }

    @NonNull
    private static PowerupType parsePowerup(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return PowerupType.NONE;
        }
        try {
            return PowerupType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            if ("HIDER_INVISIBILITY".equalsIgnoreCase(value)) {
                return PowerupType.HIDER_INVISIBILITY;
            }
            if ("SEEKER_REVEAL_ALL".equalsIgnoreCase(value)) {
                return PowerupType.SEEKER_REVEAL_ALL;
            }
            return PowerupType.NONE;
        }
    }

    @Nullable
    private static String getString(@NonNull DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        return value instanceof String ? (String) value : null;
    }

    private static boolean getBoolean(@NonNull DataSnapshot snapshot) {
        return getBoolean(snapshot, false);
    }

    private static boolean getBoolean(@NonNull DataSnapshot snapshot, boolean defaultValue) {
        Object value = snapshot.getValue();
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private static long getLong(@NonNull DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static double getDouble(@NonNull DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        return value instanceof Number ? ((Number) value).doubleValue() : 0d;
    }
}
