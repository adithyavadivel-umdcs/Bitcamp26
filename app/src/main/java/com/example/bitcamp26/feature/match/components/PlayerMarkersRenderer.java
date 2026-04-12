package com.example.bitcamp26.feature.match.components;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;
import com.example.bitcamp26.core.util.PlayerPowerupTracker;
import com.example.bitcamp26.core.util.VisionRadiusCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts player state into lightweight marker models for map rendering.
 *
 * Updated to use VisionRadiusCalculator and PlayerPowerupTracker:
 * - Hiders with active HIDER_INVISIBILITY are hidden from the seeker
 *   UNLESS the seeker has SEEKER_REVEAL_ALL active (vision 2x)
 * - Seeker nerf (0.7x) applies when any hider has invisibility
 * - Vision multiplier label is included in marker subtitle for HUD display
 */
public class PlayerMarkersRenderer {

    private static final float DEFAULT_MARKER_SIZE_PX = 28f;

    /**
     * Builds renderable markers for all visible players from the viewer's perspective.
     *
     * @param players              all players in the match
     * @param currentViewerPlayerId the local player's ID
     * @param currentViewerRole    the local player's role
     * @param revealAllHiders      true if seeker has SEEKER_REVEAL_ALL active (overrides invisibility)
     * @param showCaughtPlayers    whether to include caught/eliminated players
     */
    @NonNull
    public List<RenderablePlayerMarker> buildMarkers(@Nullable List<Player> players,
                                                     @Nullable String currentViewerPlayerId,
                                                     @Nullable PlayerRole currentViewerRole,
                                                     boolean revealAllHiders,
                                                     boolean showCaughtPlayers) {
        List<RenderablePlayerMarker> markers = new ArrayList<>();
        if (players == null || players.isEmpty()) return markers;

        // Find the viewer's own Player object so we can pass it to VisionRadiusCalculator
        Player viewerPlayer = findPlayer(players, currentViewerPlayerId);

        for (Player player : players) {
            if (player == null) continue;
            if (!hasRenderableLocation(player)) continue;
            if (!shouldRenderPlayer(player, currentViewerPlayerId, currentViewerRole,
                    revealAllHiders, showCaughtPlayers, players)) continue;

            markers.add(toRenderableMarker(player, currentViewerPlayerId, viewerPlayer, players));
        }

        return markers;
    }

    /**
     * Returns true if this player should appear on the map for the viewer.
     *
     * Visibility rules:
     * - You always see yourself
     * - Seekers always see other seekers
     * - Hiders always see the seeker
     * - Hiders always see each other
     * - Seeker sees hiders ONLY IF:
     *     - revealAllHiders is true (SEEKER_REVEAL_ALL active), OR
     *     - the hider does NOT have HIDER_INVISIBILITY active
     */
    public boolean shouldRenderPlayer(@Nullable Player player,
                                      @Nullable String currentViewerPlayerId,
                                      @Nullable PlayerRole currentViewerRole,
                                      boolean revealAllHiders,
                                      boolean showCaughtPlayers,
                                      @Nullable List<Player> allPlayers) {
        if (player == null || !hasRenderableLocation(player)) return false;
        if (player.isCaught() && !showCaughtPlayers) return false;

        // Always show self
        if (isSamePlayer(player.getId(), currentViewerPlayerId)) return true;

        PlayerRole targetRole = player.getRole();
        if (targetRole == null || currentViewerRole == null) return true;

        if (currentViewerRole == PlayerRole.HIDER) {
            // Hiders see everyone
            return true;
        }

        if (currentViewerRole == PlayerRole.SEEKER) {
            if (targetRole == PlayerRole.SEEKER) return true;

            if (targetRole == PlayerRole.HIDER) {
                // If seeker has reveal-all, they see all hiders regardless
                if (revealAllHiders) return true;

                // If this specific hider has invisibility active, seeker can't see them
                if (PlayerPowerupTracker.isHiderInvisible(player)) return false;

                // Default: seeker cannot see hiders (they have to find them physically)
                return false;
            }
        }

        return true;
    }

    /** Legacy overload without allPlayers — kept for backward compatibility. */
    public boolean shouldRenderPlayer(@Nullable Player player,
                                      @Nullable String currentViewerPlayerId,
                                      @Nullable PlayerRole currentViewerRole,
                                      boolean revealAllHiders,
                                      boolean showCaughtPlayers) {
        return shouldRenderPlayer(player, currentViewerPlayerId, currentViewerRole,
                revealAllHiders, showCaughtPlayers, null);
    }

    /**
     * Converts a single player into a renderable marker.
     * Includes vision multiplier in the subtitle for HUD display.
     */
    @NonNull
    public RenderablePlayerMarker toRenderableMarker(@NonNull Player player,
                                                     @Nullable String currentViewerPlayerId,
                                                     @Nullable Player viewerPlayer,
                                                     @Nullable List<Player> allPlayers) {
        String id = player.getId();
        boolean isSelf    = isSamePlayer(id, currentViewerPlayerId);
        boolean isCaught  = player.isCaught();
        boolean hasPowerup = PlayerPowerupTracker.hasActivePowerup(player);

        MarkerStyle style   = resolveMarkerStyle(player, isSelf, isCaught, hasPowerup);
        String label        = buildMarkerLabel(player, isSelf);
        String subtitle     = buildMarkerSubtitle(player, isSelf, viewerPlayer, allPlayers);

        return new RenderablePlayerMarker(
                id, label, subtitle,
                player.getLatitude(), player.getLongitude(),
                style, DEFAULT_MARKER_SIZE_PX,
                isSelf, isCaught, hasPowerup);
    }

    /** Legacy overload without viewer context. */
    @NonNull
    public RenderablePlayerMarker toRenderableMarker(@NonNull Player player,
                                                     @Nullable String currentViewerPlayerId) {
        return toRenderableMarker(player, currentViewerPlayerId, null, null);
    }

    public boolean hasRenderableLocation(@Nullable Player player) {
        if (player == null) return false;
        return !Double.isNaN(player.getLatitude())
                && !Double.isNaN(player.getLongitude())
                && player.getLatitude()  >= -90.0  && player.getLatitude()  <= 90.0
                && player.getLongitude() >= -180.0 && player.getLongitude() <= 180.0;
    }

    @NonNull
    public MarkerStyle resolveMarkerStyle(@NonNull Player player,
                                          boolean isSelf,
                                          boolean isCaught,
                                          boolean hasPowerup) {
        if (isCaught)   return MarkerStyle.CAUGHT;
        if (isSelf)     return MarkerStyle.SELF;
        if (hasPowerup) return MarkerStyle.POWERUP;
        PlayerRole role = player.getRole();
        if (role == PlayerRole.SEEKER) return MarkerStyle.SEEKER;
        if (role == PlayerRole.HIDER)  return MarkerStyle.HIDER;
        return MarkerStyle.DEFAULT;
    }

    @NonNull
    public String buildMarkerLabel(@NonNull Player player, boolean isSelf) {
        String base = safeDisplayName(player);
        return isSelf ? base + " (You)" : base;
    }

    /**
     * Builds subtitle including powerup state and vision multiplier for the viewer.
     */
    @NonNull
    public String buildMarkerSubtitle(@NonNull Player player,
                                      boolean isSelf,
                                      @Nullable Player viewerPlayer,
                                      @Nullable List<Player> allPlayers) {
        StringBuilder sb = new StringBuilder();

        sb.append(player.getRole() != null ? player.getRole().name() : "UNKNOWN_ROLE");
        if (isSelf) sb.append(" • SELF");
        if (player.isCaught()) sb.append(" • CAUGHT");

        if (PlayerPowerupTracker.hasActivePowerup(player)) {
            sb.append(" • ACTIVE:").append(player.getActivePowerup().name());
            sb.append(" (").append(PlayerPowerupTracker.powerupRemainingLabel(player)).append(")");
        } else if (player.getHeldPowerup() != null) {
            sb.append(" • HELD:").append(player.getHeldPowerup().name());
        }

        // Add vision label when showing self
        if (isSelf && viewerPlayer != null) {
            sb.append(" • ").append(
                VisionRadiusCalculator.getVisionLabel(viewerPlayer, allPlayers));
        }

        return sb.toString();
    }

    @NonNull
    private String safeDisplayName(@NonNull Player player) {
        String name = player.getDisplayName();
        return (name == null || name.trim().isEmpty()) ? "Player" : name.trim();
    }

    private boolean isSamePlayer(@Nullable String a, @Nullable String b) {
        return a != null && b != null && a.equals(b);
    }

    @Nullable
    private Player findPlayer(@Nullable List<Player> players, @Nullable String id) {
        if (players == null || id == null) return null;
        for (Player p : players) {
            if (p != null && id.equals(p.getId())) return p;
        }
        return null;
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    public static class RenderablePlayerMarker {
        private final String playerId;
        private final String label;
        private final String subtitle;
        private final double latitude;
        private final double longitude;
        private final MarkerStyle markerStyle;
        private final float markerSizePx;
        private final boolean self;
        private final boolean caught;
        private final boolean hasPowerup;

        public RenderablePlayerMarker(@Nullable String playerId,
                                      @NonNull String label,
                                      @NonNull String subtitle,
                                      double latitude, double longitude,
                                      @NonNull MarkerStyle markerStyle,
                                      float markerSizePx,
                                      boolean self, boolean caught, boolean hasPowerup) {
            this.playerId    = playerId;
            this.label       = label;
            this.subtitle    = subtitle;
            this.latitude    = latitude;
            this.longitude   = longitude;
            this.markerStyle = markerStyle;
            this.markerSizePx = markerSizePx;
            this.self        = self;
            this.caught      = caught;
            this.hasPowerup  = hasPowerup;
        }

        @Nullable public String getPlayerId()          { return playerId; }
        @NonNull  public String getLabel()             { return label; }
        @NonNull  public String getSubtitle()          { return subtitle; }
        public double getLatitude()                    { return latitude; }
        public double getLongitude()                   { return longitude; }
        @NonNull  public MarkerStyle getMarkerStyle()  { return markerStyle; }
        public float getMarkerSizePx()                 { return markerSizePx; }
        public boolean isSelf()                        { return self; }
        public boolean isCaught()                      { return caught; }
        public boolean hasPowerup()                    { return hasPowerup; }
    }

    public enum MarkerStyle {
        DEFAULT, SELF, SEEKER, HIDER, POWERUP, CAUGHT
    }
}
