package com.example.bitcamp26.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.GameState;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;
import com.example.bitcamp26.core.util.TimeUitls;

import java.util.List;

/**
 * Handles the business rules for using powerups during a match.
 */
public class UsePowerupUseCase {

    private static final long DEFAULT_EFFECT_DURATION_MILLIS = 30_000L;

    /**
     * Attempts to apply a powerup to the given player.
     */
    public UsePowerupResult execute(@Nullable GameState gameState,
                                    @Nullable String playerId,
                                    @Nullable PowerupType powerupType) {
        if (gameState == null) {
            return UsePowerupResult.failure("Game state is missing.");
        }

        if (!gameState.isStarted()) {
            return UsePowerupResult.failure("The game has not started yet.");
        }

        if (gameState.isFinished()) {
            return UsePowerupResult.failure("The game is already finished.");
        }

        if (playerId == null || playerId.trim().isEmpty()) {
            return UsePowerupResult.failure("Player ID is missing.");
        }

        if (powerupType == null) {
            return UsePowerupResult.failure("Powerup type is missing.");
        }

        Player player = findPlayerById(gameState.getPlayers(), playerId);
        if (player == null) {
            return UsePowerupResult.failure("Player was not found in the game.");
        }

        if (player.isCaught()) {
            return UsePowerupResult.failure("Caught players cannot use powerups.");
        }

        if (!isPowerupAllowedForRole(player.getRole(), powerupType)) {
            return UsePowerupResult.failure("This powerup is not allowed for the player's role.");
        }

        if (!hasMatchingInventoryPowerup(player, powerupType)) {
            return UsePowerupResult.failure("Player does not currently hold this powerup.");
        }

        long activatedAt = TimeUitls.nowMillis();
        long expiresAt = activatedAt + resolveEffectDurationMillis(powerupType);

        player.setActivePowerup(powerupType);
        player.setPowerupActivatedAt(activatedAt);
        player.setPowerupExpiresAt(expiresAt);
        player.setHeldPowerup(null);

        return UsePowerupResult.success(player, powerupType, expiresAt);
    }

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

    public boolean isPowerupAllowedForRole(@Nullable PlayerRole role,
                                           @Nullable PowerupType powerupType) {
        if (role == null || powerupType == null) {
            return false;
        }

        switch (powerupType) {
            case HIDER_VISION_REDUCTION:
            case HIDER_INVISIBILITY:
                return role == PlayerRole.HIDER;
            case SEEKER_MINIMAP_BOOST:
            case SEEKER_REVEAL_ALL:
                return role == PlayerRole.SEEKER;
            default:
                return false;
        }
    }

    public boolean hasMatchingInventoryPowerup(@Nullable Player player,
                                               @Nullable PowerupType powerupType) {
        return player != null
                && powerupType != null
                && player.getHeldPowerup() == powerupType;
    }

    public long resolveEffectDurationMillis(@NonNull PowerupType powerupType) {
        switch (powerupType) {
            case HIDER_VISION_REDUCTION:
            case HIDER_INVISIBILITY:
                return 20_000L;
            case SEEKER_MINIMAP_BOOST:
            case SEEKER_REVEAL_ALL:
                return 15_000L;
            default:
                return DEFAULT_EFFECT_DURATION_MILLIS;
        }
    }

    public static final class UsePowerupResult {
        private final boolean success;
        private final String message;
        private final Player updatedPlayer;
        private final PowerupType activatedPowerup;
        private final long powerupExpiresAt;

        private UsePowerupResult(boolean success,
                                 @NonNull String message,
                                 @Nullable Player updatedPlayer,
                                 @Nullable PowerupType activatedPowerup,
                                 long powerupExpiresAt) {
            this.success = success;
            this.message = message;
            this.updatedPlayer = updatedPlayer;
            this.activatedPowerup = activatedPowerup;
            this.powerupExpiresAt = powerupExpiresAt;
        }

        public static UsePowerupResult success(@NonNull Player updatedPlayer,
                                               @NonNull PowerupType activatedPowerup,
                                               long powerupExpiresAt) {
            return new UsePowerupResult(
                    true,
                    "Powerup used successfully.",
                    updatedPlayer,
                    activatedPowerup,
                    powerupExpiresAt
            );
        }

        public static UsePowerupResult failure(@NonNull String message) {
            return new UsePowerupResult(false, message, null, null, 0L);
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

        @Nullable
        public PowerupType getActivatedPowerup() {
            return activatedPowerup;
        }

        public long getPowerupExpiresAt() {
            return powerupExpiresAt;
        }
    }
}
