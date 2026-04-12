package com.example.bitcamp26.core.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.model.PowerupType;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for querying the powerup state of all players in a match.
 *
 * Answers questions like:
 * - Which players have active powerups?
 * - Is a specific player's powerup still valid (not expired)?
 * - Which hiders are currently invisible?
 * - Is the seeker reveal-all active?
 *
 * All methods are stateless — pass in the current player list each time.
 * This pairs directly with VisionRadiusCalculator and PlayerMarkersRenderer.
 *
 * Usage:
 *   boolean invisible = PlayerPowerupTracker.isHiderInvisible(player);
 *   List<Player> buffed = PlayerPowerupTracker.getPlayersWithActivePowerups(allPlayers);
 */
public class PlayerPowerupTracker {

    private PlayerPowerupTracker() {}

    // ── Active powerup checks ─────────────────────────────────────────────────

    /**
     * Returns true if the given player has an active powerup that has not expired.
     */
    public static boolean hasActivePowerup(@Nullable Player player) {
        if (player == null) return false;
        if (player.getActivePowerup() == null
                || player.getActivePowerup() == PowerupType.NONE) return false;

        // If powerupExpiresAt is 0 treat as never-expiring (fallback)
        if (player.getPowerupExpiresAt() <= 0) return true;

        return System.currentTimeMillis() < player.getPowerupExpiresAt();
    }

    /**
     * Returns true if this hider is currently invisible to the seeker.
     */
    public static boolean isHiderInvisible(@Nullable Player player) {
        if (player == null) return false;
        if (player.getRole() != PlayerRole.HIDER) return false;
        return hasActivePowerup(player)
                && player.getActivePowerup() == PowerupType.HIDER_INVISIBILITY;
    }

    /**
     * Returns true if the seeker's reveal-all powerup is active.
     */
    public static boolean isSeekerRevealing(@Nullable Player player) {
        if (player == null) return false;
        if (player.getRole() != PlayerRole.SEEKER) return false;
        return hasActivePowerup(player)
                && player.getActivePowerup() == PowerupType.SEEKER_REVEAL_ALL;
    }

    // ── List queries ──────────────────────────────────────────────────────────

    /**
     * Returns all players who currently have an active, non-expired powerup.
     */
    @NonNull
    public static List<Player> getPlayersWithActivePowerups(@Nullable List<Player> players) {
        List<Player> result = new ArrayList<>();
        if (players == null) return result;
        for (Player p : players) {
            if (hasActivePowerup(p)) result.add(p);
        }
        return result;
    }

    /**
     * Returns all hiders who are currently invisible.
     */
    @NonNull
    public static List<Player> getInvisibleHiders(@Nullable List<Player> players) {
        List<Player> result = new ArrayList<>();
        if (players == null) return result;
        for (Player p : players) {
            if (isHiderInvisible(p)) result.add(p);
        }
        return result;
    }

    /**
     * Returns the seeker player if one exists in the list, null otherwise.
     */
    @Nullable
    public static Player findSeeker(@Nullable List<Player> players) {
        if (players == null) return null;
        for (Player p : players) {
            if (p != null && p.getRole() == PlayerRole.SEEKER) return p;
        }
        return null;
    }

    /**
     * Returns true if any hider in the list is currently invisible.
     * Shorthand used by VisionRadiusCalculator.
     */
    public static boolean anyHiderIsInvisible(@Nullable List<Player> players) {
        return !getInvisibleHiders(players).isEmpty();
    }

    // ── Expiry helpers ────────────────────────────────────────────────────────

    /**
     * Returns how many milliseconds remain on a player's active powerup.
     * Returns 0 if no active powerup or already expired.
     */
    public static long powerupRemainingMs(@Nullable Player player) {
        if (!hasActivePowerup(player)) return 0;
        if (player.getPowerupExpiresAt() <= 0) return 0;
        return Math.max(0, player.getPowerupExpiresAt() - System.currentTimeMillis());
    }

    /**
     * Returns the remaining powerup duration as a display string: "12s", "0s".
     */
    @NonNull
    public static String powerupRemainingLabel(@Nullable Player player) {
        long ms = powerupRemainingMs(player);
        return (ms / 1000) + "s";
    }

    /**
     * Returns a summary string of all active powerups for debugging.
     * Example: "Alice: HIDER_INVISIBILITY (8s), Bob: SEEKER_REVEAL_ALL (3s)"
     */
    @NonNull
    public static String buildActivePowerupSummary(@Nullable List<Player> players) {
        List<Player> active = getPlayersWithActivePowerups(players);
        if (active.isEmpty()) return "No active powerups";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < active.size(); i++) {
            Player p = active.get(i);
            String name = p.getDisplayName() != null ? p.getDisplayName() : "Player";
            String powerup = p.getActivePowerup() != null ? p.getActivePowerup().name() : "?";
            String remaining = powerupRemainingLabel(p);
            sb.append(name).append(": ").append(powerup).append(" (").append(remaining).append(")");
            if (i < active.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }
}
