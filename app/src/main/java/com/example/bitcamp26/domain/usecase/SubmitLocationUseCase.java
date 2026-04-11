package com.example.bitcamp26.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.util.GeoUtils;
import com.example.bitcamp26.core.util.TimeUitls;

import java.util.List;

/**
 * Handles the business rules for submitting a player's live location during a match.
 */
public class SubmitLocationUseCase {

    /**
     * Validates and applies a location update for a player in the current game.
     */
    public SubmitLocationResult execute(@Nullable GameState gameState,
                                        @Nullable String playerId,
                                        double latitude,
                                        double longitude) {
        if (gameState == null) {
            return SubmitLocationResult.failure("Game state is missing.");
        }

        if (!gameState.isStarted()) {
            return SubmitLocationResult.failure("The game has not started yet.");
        }

        if (gameState.isFinished()) {
            return SubmitLocationResult.failure("The game is already finished.");
        }

        if (playerId == null || playerId.trim().isEmpty()) {
            return SubmitLocationResult.failure("Player ID is missing.");
        }

        if (!GeoUtils.isValidCoordinate(latitude, longitude)) {
            return SubmitLocationResult.failure("Submitted coordinates are invalid.");
        }

        Player player = findPlayerById(gameState.getPlayers(), playerId);
        if (player == null) {
            return SubmitLocationResult.failure("Player was not found in the current game.");
        }

        if (player.isCaught()) {
            return SubmitLocationResult.failure("Caught players cannot submit location updates.");
        }

        if (!canRoleSubmitLocation(player.getRole())) {
            return SubmitLocationResult.failure("This player role cannot submit location updates.");
        }

        player.setLatitude(latitude);
        player.setLongitude(longitude);
        player.setLastLocationUpdatedAt(TimeUitls.nowMillis());

        boolean insideHotspot = isPlayerInsideAnyHotspot(gameState, latitude, longitude);

        return SubmitLocationResult.success(player, insideHotspot);
    }

    /**
     * Returns the player with the given ID, or null if not found.
     */
    @Nullable
    public Player findPlayerById(@Nullable List<Player> players, @Nullable String playerId) {
        if (players == null || players.isEmpty() || playerId == null || playerId.trim().isEmpty()) {
            return null;
        }

        for (Player player : players) {
            if (player == null || player.getId() == null) {
                continue;
            }

            if (playerId.equals(player.getId())) {
                return player;
            }
        }

        return null;
    }

    /**
     * Returns true if the given role may submit live location updates.
     */
    public boolean canRoleSubmitLocation(@Nullable PlayerRole role) {
        return role == PlayerRole.HIDER || role == PlayerRole.SEEKER;
    }

    /**
     * Returns true if the given location lies inside at least one active hotspot.
     */
    public boolean isPlayerInsideAnyHotspot(@NonNull GameState gameState,
                                            double latitude,
                                            double longitude) {
        if (gameState.getHotspots() == null || gameState.getHotspots().isEmpty()) {
            return false;
        }

        for (com.example.bitcamp26.core.model.HotspotState hotspot : gameState.getHotspots()) {
            if (hotspot == null || !hotspot.isActive()) {
                continue;
            }

            if (GeoUtils.isWithinRadius(
                    hotspot.getLat(),
                    hotspot.getLng(),
                    latitude,
                    longitude,
                    hotspot.getRadiusMeters())) {
                return true;
            }
        }

        return false;
    }

    public static final class SubmitLocationResult {
        private final boolean success;
        private final String message;
        private final Player updatedPlayer;
        private final boolean insideHotspot;

        private SubmitLocationResult(boolean success,
                                     @NonNull String message,
                                     @Nullable Player updatedPlayer,
                                     boolean insideHotspot) {
            this.success = success;
            this.message = message;
            this.updatedPlayer = updatedPlayer;
            this.insideHotspot = insideHotspot;
        }

        public static SubmitLocationResult success(@NonNull Player updatedPlayer,
                                                   boolean insideHotspot) {
            return new SubmitLocationResult(
                    true,
                    "Location submitted successfully.",
                    updatedPlayer,
                    insideHotspot
            );
        }

        public static SubmitLocationResult failure(@NonNull String message) {
            return new SubmitLocationResult(false, message, null, false);
        }

        public boolean isSuccess() {
            return success;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        @Nullable
        public Player getUpdatedPlayer() {
            return updatedPlayer;
        }

        public boolean isInsideHotspot() {
            return insideHotspot;
        }
    }
}
