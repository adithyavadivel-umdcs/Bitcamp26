package com.example.bitcamp26.feature.match.components;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.Player;
import com.example.bitcamp26.core.model.PlayerRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that converts player state into lightweight marker models
 * that a map, overlay, or custom view can render.
 *
 * This class does not draw directly to the screen. Instead, it prepares
 * renderable marker data such as label text, visibility, position, and style.
 * That makes it easy to reuse with a Google Map, a custom overlay, or any
 * future renderer.
 */
public class PlayerMarkersRenderer {

    /**
     * Default marker size in pixels for render models.
     */
    private static final float DEFAULT_MARKER_SIZE_PX = 28f;

    /**
     * Builds renderable marker data for a list of players.
     *
     * The caller provides the current viewer's role so visibility rules can be applied.
     * For example, hidden hiders can be excluded for seekers unless reveal logic is enabled.
     */
    @NonNull
    public List<RenderablePlayerMarker> buildMarkers(@Nullable List<Player> players,
                                                     @Nullable String currentViewerPlayerId,
                                                     @Nullable PlayerRole currentViewerRole,
                                                     boolean revealAllHiders,
                                                     boolean showCaughtPlayers) {
        List<RenderablePlayerMarker> markers = new ArrayList<>();

        if (players == null || players.isEmpty()) {
            return markers;
        }

        for (Player player : players) {
            if (player == null) {
                continue;
            }

            if (!hasRenderableLocation(player)) {
                continue;
            }

            if (!shouldRenderPlayer(player,
                    currentViewerPlayerId,
                    currentViewerRole,
                    revealAllHiders,
                    showCaughtPlayers)) {
                continue;
            }

            markers.add(toRenderableMarker(player, currentViewerPlayerId));
        }

        return markers;
    }

    /**
     * Returns true if the given player should appear on the map for the viewer.
     */
    public boolean shouldRenderPlayer(@Nullable Player player,
                                      @Nullable String currentViewerPlayerId,
                                      @Nullable PlayerRole currentViewerRole,
                                      boolean revealAllHiders,
                                      boolean showCaughtPlayers) {
        if (player == null) {
            return false;
        }

        if (!hasRenderableLocation(player)) {
            return false;
        }

        if (player.isCaught() && !showCaughtPlayers) {
            return false;
        }

        boolean isSelf = isSamePlayer(player.getId(), currentViewerPlayerId);
        if (isSelf) {
            return true;
        }

        PlayerRole targetRole = player.getRole();
        if (targetRole == null) {
            return false;
        }

        if (currentViewerRole == null) {
            return true;
        }

        if (currentViewerRole == PlayerRole.HIDER) {
            return targetRole == PlayerRole.HIDER || targetRole == PlayerRole.SEEKER;
        }

        if (currentViewerRole == PlayerRole.SEEKER) {
            if (targetRole == PlayerRole.SEEKER) {
                return true;
            }

            if (targetRole == PlayerRole.HIDER) {
                if (revealAllHiders) {
                    return true;
                }

                if (player.getActivePowerup() != null && player.getActivePowerup().name().equals("HIDER_INVISIBILITY")) {
                    return false;
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Converts a single player into a lightweight marker model.
     */
    @NonNull
    public RenderablePlayerMarker toRenderableMarker(@NonNull Player player,
                                                     @Nullable String currentViewerPlayerId) {
        String id = player.getId();
        String displayName = safeDisplayName(player);
        boolean isSelf = isSamePlayer(id, currentViewerPlayerId);
        boolean isCaught = player.isCaught();
        boolean hasPowerup = player.getHeldPowerup() != null || player.getActivePowerup() != null;

        MarkerStyle style = resolveMarkerStyle(player, isSelf, isCaught, hasPowerup);
        String label = buildMarkerLabel(player, isSelf);
        String subtitle = buildMarkerSubtitle(player, isSelf);

        return new RenderablePlayerMarker(
                id,
                label,
                subtitle,
                player.getLatitude(),
                player.getLongitude(),
                style,
                DEFAULT_MARKER_SIZE_PX,
                isSelf,
                isCaught,
                hasPowerup
        );
    }

    /**
     * Returns true if a player has coordinates that can be rendered.
     */
    public boolean hasRenderableLocation(@Nullable Player player) {
        if (player == null) {
            return false;
        }

        return !Double.isNaN(player.getLatitude())
                && !Double.isNaN(player.getLongitude())
                && player.getLatitude() >= -90.0
                && player.getLatitude() <= 90.0
                && player.getLongitude() >= -180.0
                && player.getLongitude() <= 180.0;
    }

    /**
     * Chooses a marker style based on role and player state.
     */
    @NonNull
    public MarkerStyle resolveMarkerStyle(@NonNull Player player,
                                          boolean isSelf,
                                          boolean isCaught,
                                          boolean hasPowerup) {
        if (isCaught) {
            return MarkerStyle.CAUGHT;
        }

        if (isSelf) {
            return MarkerStyle.SELF;
        }

        if (hasPowerup) {
            return MarkerStyle.POWERUP;
        }

        PlayerRole role = player.getRole();
        if (role == PlayerRole.SEEKER) {
            return MarkerStyle.SEEKER;
        }

        if (role == PlayerRole.HIDER) {
            return MarkerStyle.HIDER;
        }

        return MarkerStyle.DEFAULT;
    }

    /**
     * Builds the main label shown on or near the marker.
     */
    @NonNull
    public String buildMarkerLabel(@NonNull Player player,
                                   boolean isSelf) {
        String base = safeDisplayName(player);
        return isSelf ? base + " (You)" : base;
    }

    /**
     * Builds a subtitle string that can be shown in an info window or details panel.
     */
    @NonNull
    public String buildMarkerSubtitle(@NonNull Player player,
                                      boolean isSelf) {
        StringBuilder builder = new StringBuilder();

        if (player.getRole() != null) {
            builder.append(player.getRole().name());
        } else {
            builder.append("UNKNOWN_ROLE");
        }

        if (isSelf) {
            builder.append(" • SELF");
        }

        if (player.isCaught()) {
            builder.append(" • CAUGHT");
        }

        if (player.getHeldPowerup() != null) {
            builder.append(" • HELD:").append(player.getHeldPowerup().name());
        }

        if (player.getActivePowerup() != null) {
            builder.append(" • ACTIVE:").append(player.getActivePowerup().name());
        }

        return builder.toString();
    }

    @NonNull
    private String safeDisplayName(@NonNull Player player) {
        String displayName = player.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            return "Player";
        }
        return displayName.trim();
    }

    private boolean isSamePlayer(@Nullable String a, @Nullable String b) {
        return a != null && b != null && a.equals(b);
    }

    /**
     * Lightweight marker representation for UI rendering.
     */
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
                                      double latitude,
                                      double longitude,
                                      @NonNull MarkerStyle markerStyle,
                                      float markerSizePx,
                                      boolean self,
                                      boolean caught,
                                      boolean hasPowerup) {
            this.playerId = playerId;
            this.label = label;
            this.subtitle = subtitle;
            this.latitude = latitude;
            this.longitude = longitude;
            this.markerStyle = markerStyle;
            this.markerSizePx = markerSizePx;
            this.self = self;
            this.caught = caught;
            this.hasPowerup = hasPowerup;
        }

        @Nullable
        public String getPlayerId() {
            return playerId;
        }

        @NonNull
        public String getLabel() {
            return label;
        }

        @NonNull
        public String getSubtitle() {
            return subtitle;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @NonNull
        public MarkerStyle getMarkerStyle() {
            return markerStyle;
        }

        public float getMarkerSizePx() {
            return markerSizePx;
        }

        public boolean isSelf() {
            return self;
        }

        public boolean isCaught() {
            return caught;
        }

        public boolean hasPowerup() {
            return hasPowerup;
        }
    }

    /**
     * Semantic marker styles that UI code can map to icons, colors, or badges.
     */
    public enum MarkerStyle {
        DEFAULT,
        SELF,
        SEEKER,
        HIDER,
        POWERUP,
        CAUGHT
    }
}
