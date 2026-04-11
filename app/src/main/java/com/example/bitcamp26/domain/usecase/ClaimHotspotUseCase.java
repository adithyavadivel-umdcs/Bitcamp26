package com.example.bitcamp26.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.HotspotState;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.util.GeoUtils;
import com.example.bitcamp26.core.util.TimeUitls;

import java.util.List;

/**
 * Handles the business rules for claiming a hotspot during a match.
 */
public class ClaimHotspotUseCase {

    /**
     * Attempts to claim a hotspot for a player.
     */
    public ClaimResult execute(@Nullable GameState gameState,
                               @Nullable HotspotState hotspot,
                               @Nullable String playerId,
                               @Nullable PlayerRole playerRole,
                               double playerLat,
                               double playerLng) {

        if (gameState == null) {
            return ClaimResult.failure("Game state is missing.");
        }

        if (hotspot == null) {
            return ClaimResult.failure("Hotspot is missing.");
        }

        if (playerId == null || playerId.trim().isEmpty()) {
            return ClaimResult.failure("Player ID is missing.");
        }

        if (playerRole == null) {
            return ClaimResult.failure("Player role is missing.");
        }

        if (!GeoUtils.isValidCoordinate(playerLat, playerLng)) {
            return ClaimResult.failure("Player location is invalid.");
        }

        if (!hotspot.isActive()) {
            return ClaimResult.failure("This hotspot is no longer active.");
        }

        if (hotspot.getClaimedBy() != null && !hotspot.getClaimedBy().trim().isEmpty()) {
            return ClaimResult.failure("This hotspot has already been claimed.");
        }

        if (!isPlayerAllowedToClaim(playerRole)) {
            return ClaimResult.failure("This player role cannot claim hotspots.");
        }

        if (!isWithinHotspotRadius(hotspot, playerLat, playerLng)) {
            return ClaimResult.failure("Player is not inside the hotspot radius.");
        }

        hotspot.setClaimedBy(playerId);
        hotspot.setClaimedAt(TimeUitls.nowMillis());
        hotspot.setActive(false);

        int updatedScore = calculateUpdatedScore(gameState, playerRole);
        return ClaimResult.success(hotspot, updatedScore);
    }

    /**
     * Returns true if this role is allowed to claim hotspots.
     * Adjust this rule later if your game design changes.
     */
    public boolean isPlayerAllowedToClaim(@Nullable PlayerRole playerRole) {
        return playerRole == PlayerRole.HIDER || playerRole == PlayerRole.SEEKER;
    }

    /**
     * Returns true if the player position lies within the hotspot radius.
     */
    public boolean isWithinHotspotRadius(@NonNull HotspotState hotspot,
                                         double playerLat,
                                         double playerLng) {
        return GeoUtils.isWithinRadius(
                hotspot.getLat(),
                hotspot.getLng(),
                playerLat,
                playerLng,
                hotspot.getRadiusMeters()
        );
    }

    /**
     * Returns the first claimable hotspot the player is currently inside.
     */
    @Nullable
    public HotspotState findClaimableHotspot(@Nullable List<HotspotState> hotspots,
                                             @Nullable PlayerRole playerRole,
                                             double playerLat,
                                             double playerLng) {
        if (hotspots == null || hotspots.isEmpty()) {
            return null;
        }

        if (!isPlayerAllowedToClaim(playerRole)) {
            return null;
        }

        for (HotspotState hotspot : hotspots) {
            if (hotspot == null) {
                continue;
            }

            boolean unclaimed = hotspot.getClaimedBy() == null || hotspot.getClaimedBy().trim().isEmpty();
            if (hotspot.isActive() && unclaimed && isWithinHotspotRadius(hotspot, playerLat, playerLng)) {
                return hotspot;
            }
        }

        return null;
    }

    /**
     * Simple score update rule. Replace with richer game logic later if needed.
     */
    public int calculateUpdatedScore(@NonNull GameState gameState,
                                     @NonNull PlayerRole playerRole) {
        int currentScore = gameState.getScore();

        switch (playerRole) {
            case HIDER:
                return currentScore + 10;
            case SEEKER:
                return currentScore + 5;
            default:
                return currentScore;
        }
    }

    public static final class ClaimResult {
        private final boolean success;
        private final String message;
        private final HotspotState updatedHotspot;
        private final int updatedScore;

        private ClaimResult(boolean success,
                            @NonNull String message,
                            @Nullable HotspotState updatedHotspot,
                            int updatedScore) {
            this.success = success;
            this.message = message;
            this.updatedHotspot = updatedHotspot;
            this.updatedScore = updatedScore;
        }

        public static ClaimResult success(@NonNull HotspotState hotspot, int updatedScore) {
            return new ClaimResult(true, "Hotspot claimed successfully.", hotspot, updatedScore);
        }

        public static ClaimResult failure(@NonNull String message) {
            return new ClaimResult(false, message, null, 0);
        }

        public boolean isSuccess() {
            return success;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        @Nullable
        public HotspotState getUpdatedHotspot() {
            return updatedHotspot;
        }

        public int getUpdatedScore() {
            return updatedScore;
        }
    }
}
