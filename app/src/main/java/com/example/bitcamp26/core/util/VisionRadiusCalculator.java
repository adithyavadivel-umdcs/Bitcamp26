package com.example.bitcamp26.core.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.balance.GameBalance;
import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;

/**
 * Computes the effective vision radius for a player based on their role
 * and active powerup state.
 *
 * Vision radius = baseRadius * multiplier
 *
 * Rules:
 *   Seeker, no powerup active anywhere:    1.0x  (SEEKER_VISION_MULTIPLIER)
 *   Hider, any state:                      1.3x  (HIDER_VISION_MULTIPLIER)
 *   Seeker + SEEKER_REVEAL_ALL active:     2.0x  (SEEKER_BUFF_VISION_MULTIPLIER)
 *   Seeker + any hider has HIDER_INVISIBILITY: 0.7x (SEEKER_NERF_VISION_MULTIPLIER)
 *
 * Note: seeker nerf takes priority over seeker buff if both conditions somehow
 * apply simultaneously (defensive — in practice only one powerup is active at a time).
 *
 * Usage:
 *   double vision = VisionRadiusCalculator.getVisionRadius(player, allPlayers, baseRadius);
 */
public class VisionRadiusCalculator {

    private VisionRadiusCalculator() {}

    /**
     * Returns the effective vision radius in meters for the given player.
     *
     * @param player       the player whose vision radius to compute
     * @param allPlayers   all players in the match (needed to check hider invisibility)
     * @param baseRadius   the current play zone radius in meters
     * @return vision radius in meters
     */
    public static double getVisionRadius(@Nullable Player player,
                                         @Nullable java.util.List<Player> allPlayers,
                                         double baseRadius) {
        if (player == null || player.getRole() == null) {
            return baseRadius * GameBalance.SEEKER_VISION_MULTIPLIER;
        }

        PlayerRole role = player.getRole();

        if (role == PlayerRole.HIDER) {
            return baseRadius * GameBalance.HIDER_VISION_MULTIPLIER;
        }

        if (role == PlayerRole.SEEKER) {
            // Check for seeker nerf first — any hider has HIDER_INVISIBILITY active
            if (anyHiderHasInvisibility(allPlayers)) {
                return baseRadius * GameBalance.SEEKER_NERF_VISION_MULTIPLIER;
            }

            // Check for seeker buff — seeker themselves has SEEKER_REVEAL_ALL active
            if (player.getActivePowerup() == PowerupType.SEEKER_REVEAL_ALL) {
                return baseRadius * GameBalance.SEEKER_BUFF_VISION_MULTIPLIER;
            }

            // Standard seeker
            return baseRadius * GameBalance.SEEKER_VISION_MULTIPLIER;
        }

        return baseRadius * GameBalance.SEEKER_VISION_MULTIPLIER;
    }

    /**
     * Returns just the vision multiplier (not the absolute radius) for the given player.
     * Useful for logging or UI labels.
     */
    public static double getVisionMultiplier(@Nullable Player player,
                                              @Nullable java.util.List<Player> allPlayers) {
        if (player == null || player.getRole() == null) {
            return GameBalance.SEEKER_VISION_MULTIPLIER;
        }

        PlayerRole role = player.getRole();

        if (role == PlayerRole.HIDER) {
            return GameBalance.HIDER_VISION_MULTIPLIER;
        }

        if (role == PlayerRole.SEEKER) {
            if (anyHiderHasInvisibility(allPlayers)) {
                return GameBalance.SEEKER_NERF_VISION_MULTIPLIER;
            }
            if (player.getActivePowerup() == PowerupType.SEEKER_REVEAL_ALL) {
                return GameBalance.SEEKER_BUFF_VISION_MULTIPLIER;
            }
            return GameBalance.SEEKER_VISION_MULTIPLIER;
        }

        return GameBalance.SEEKER_VISION_MULTIPLIER;
    }

    /**
     * Returns a human-readable label for the current vision state.
     * Useful for debug overlays or the match HUD.
     */
    @NonNull
    public static String getVisionLabel(@Nullable Player player,
                                         @Nullable java.util.List<Player> allPlayers) {
        double multiplier = getVisionMultiplier(player, allPlayers);

        if (multiplier == GameBalance.SEEKER_BUFF_VISION_MULTIPLIER) {
            return "Vision: 2x 👁";
        } else if (multiplier == GameBalance.HIDER_VISION_MULTIPLIER) {
            return "Vision: 1.3x";
        } else if (multiplier == GameBalance.SEEKER_NERF_VISION_MULTIPLIER) {
            return "Vision: 0.7x ⚡";
        } else {
            return "Vision: 1x";
        }
    }

    /**
     * Returns true if any hider in the list has HIDER_INVISIBILITY active.
     * This triggers the seeker nerf.
     */
    public static boolean anyHiderHasInvisibility(@Nullable java.util.List<Player> players) {
        if (players == null) return false;
        for (Player p : players) {
            if (p == null) continue;
            if (p.getRole() == PlayerRole.HIDER
                    && p.getActivePowerup() == PowerupType.HIDER_INVISIBILITY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the seeker currently has reveal-all active.
     */
    public static boolean seekerHasRevealAll(@Nullable Player seeker) {
        return seeker != null
                && seeker.getRole() == PlayerRole.SEEKER
                && seeker.getActivePowerup() == PowerupType.SEEKER_REVEAL_ALL;
    }
}
