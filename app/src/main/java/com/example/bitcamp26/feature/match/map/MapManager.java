package com.example.bitcamp26.feature.match.map;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.HotspotState;
import com.example.bitcamp26.data.location.PlayerLocation;
import com.example.bitcamp26.feature.match.components.PlayerMarkersRenderer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the Google Map: self-location marker, other-player markers, and hotspot circles.
 *
 * Usage in a Fragment:
 * <pre>
 *   // 1. Create once
 *   MapManager mapManager = new MapManager();
 *
 *   // 2. Attach to a SupportMapFragment container
 *   SupportMapFragment frag = SupportMapFragment.newInstance();
 *   getChildFragmentManager().beginTransaction()
 *       .add(R.id.mapContainer, frag).commit();
 *   frag.getMapAsync(mapManager);   // ← passes this as OnMapReadyCallback
 *
 *   // 3. Feed it data
 *   mapManager.updateSelfLocation(playerLocation);
 *   mapManager.updatePlayerMarkers(renderableMarkers);
 *   mapManager.updateHotspots(hotspotList);
 * </pre>
 *
 * Threading: all public methods must be called on the main thread.
 */
public class MapManager implements OnMapReadyCallback {

    private static final float DEFAULT_ZOOM = 17f;

    // Hotspot circle style — translucent blue fill, solid blue stroke
    private static final int HOTSPOT_FILL   = Color.argb(50,  0, 140, 255);
    private static final int HOTSPOT_STROKE = Color.argb(180, 0, 100, 220);
    private static final float HOTSPOT_STROKE_WIDTH = 3f;

    @Nullable
    private GoogleMap googleMap;

    /** Marker representing the local device's GPS position. */
    @Nullable
    private Marker selfMarker;
    
    // Buffer for when data is pushed BEFORE onMapReady
    @Nullable
    private PlayerLocation lastKnownSelfLocation;
    @Nullable
    private String lastKnownSelfLabel;
    @Nullable
    private List<PlayerMarkersRenderer.RenderablePlayerMarker> lastKnownPlayerMarkers;
    @Nullable
    private List<HotspotState> lastKnownHotspots;

    /** Markers for all other visible players, keyed by playerId. */
    private final Map<String, Marker> playerMarkers = new HashMap<>();

    /** Hotspot radius circles, keyed by hotspot id. */
    private final Map<String, Circle> hotspotCircles = new HashMap<>();

    /** Tracks whether we've centered the camera on the user yet. */
    private boolean hasCenteredOnUser = false;

    // -------------------------------------------------------------------------
    // OnMapReadyCallback
    // -------------------------------------------------------------------------

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Apply buffered data now that we are ready
        if (lastKnownSelfLocation != null) {
            updateSelfLocation(lastKnownSelfLocation, lastKnownSelfLabel);
        }
        if (lastKnownPlayerMarkers != null) {
            updatePlayerMarkers(lastKnownPlayerMarkers);
        }
        if (lastKnownHotspots != null) {
            updateHotspots(lastKnownHotspots);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns true once {@link #onMapReady} has fired. */
    public boolean isReady() {
        return googleMap != null;
    }

    /**
     * Moves (or creates) the self-location marker to the GPS position.
     * On the first call, the camera flies to the user's location at {@link #DEFAULT_ZOOM}.
     */
    public void updateSelfLocation(@NonNull PlayerLocation location, @Nullable String label) {
        lastKnownSelfLocation = location;
        lastKnownSelfLabel = label;
        if (googleMap == null) return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        String title = (label != null && !label.trim().isEmpty()) ? label : "You";

        if (selfMarker == null) {
            selfMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .zIndex(2f));
        } else {
            selfMarker.setPosition(latLng);
            selfMarker.setTitle(title);
        }

        // Lock camera to player position and maintain default zoom level on every update
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
        hasCenteredOnUser = true;
    }

    /**
     * Overload for updateSelfLocation without a label.
     */
    public void updateSelfLocation(@NonNull PlayerLocation location) {
        updateSelfLocation(location, null);
    }

    /**
     * Syncs the player markers on the map with the given renderable list.
     *
     * Self markers (isSelf == true) are intentionally skipped — the GPS position
     * from {@link #updateSelfLocation} is authoritative for the local player.
     *
     * Existing markers are reused (moved in place) to avoid flickering.
     * Stale markers whose player IDs are no longer in the list are removed.
     */
    public void updatePlayerMarkers(
            @NonNull List<PlayerMarkersRenderer.RenderablePlayerMarker> markers) {
        lastKnownPlayerMarkers = markers;
        if (googleMap == null) return;

        Map<String, Boolean> activeIds = new HashMap<>();

        for (PlayerMarkersRenderer.RenderablePlayerMarker m : markers) {
            if (m.getPlayerId() == null || m.isSelf()) continue;
            String id = m.getPlayerId();
            activeIds.put(id, true);

            LatLng pos = new LatLng(m.getLatitude(), m.getLongitude());

            Marker existing = playerMarkers.get(id);
            if (existing == null) {
                Marker newMarker = googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(m.getLabel())
                        .snippet(m.getSubtitle())
                        .icon(BitmapDescriptorFactory.defaultMarker(hueFor(m))));
                playerMarkers.put(id, newMarker);
            } else {
                existing.setPosition(pos);
                existing.setTitle(m.getLabel());
            }
        }

        // Remove markers for players who are no longer in the list
        playerMarkers.entrySet().removeIf(entry -> {
            if (!activeIds.containsKey(entry.getKey())) {
                entry.getValue().remove();
                return true;
            }
            return false;
        });
    }

    /**
     * Draws a translucent radius circle for each active hotspot.
     * Existing circles are reused; stale ones are removed.
     *
     * Pass null or an empty list to clear all hotspot circles.
     */
    public void updateHotspots(@Nullable List<HotspotState> hotspots) {
        lastKnownHotspots = hotspots;
        if (googleMap == null) return;

        Map<String, Boolean> activeIds = new HashMap<>();

        if (hotspots != null) {
            for (HotspotState h : hotspots) {
                if (h == null || h.getId() == null) continue;
                activeIds.put(h.getId(), true);

                if (!hotspotCircles.containsKey(h.getId())) {
                    Circle circle = googleMap.addCircle(new CircleOptions()
                            .center(new LatLng(h.getLat(), h.getLng()))
                            .radius(h.getRadiusMeters())
                            .fillColor(HOTSPOT_FILL)
                            .strokeColor(HOTSPOT_STROKE)
                            .strokeWidth(HOTSPOT_STROKE_WIDTH));
                    hotspotCircles.put(h.getId(), circle);
                }
            }
        }

        hotspotCircles.entrySet().removeIf(entry -> {
            if (!activeIds.containsKey(entry.getKey())) {
                entry.getValue().remove();
                return true;
            }
            return false;
        });
    }

    /**
     * Removes all markers and circles from the map and resets state.
     * Call this when the match ends or the fragment is destroyed.
     */
    public void clearAll() {
        if (googleMap == null) return;

        for (Marker m : playerMarkers.values()) m.remove();
        playerMarkers.clear();

        for (Circle c : hotspotCircles.values()) c.remove();
        hotspotCircles.clear();

        if (selfMarker != null) {
            selfMarker.remove();
            selfMarker = null;
        }

        hasCenteredOnUser = false;
    }

    // -------------------------------------------------------------------------
    // Debug helper
    // -------------------------------------------------------------------------

    /**
     * Manually set the self-location (useful for emulator testing).
     * Equivalent to calling updateSelfLocation with a hand-crafted PlayerLocation.
     */
    public void debugSetLocation(double latitude, double longitude) {
        updateSelfLocation(new PlayerLocation(
                "debug_player", latitude, longitude, System.currentTimeMillis()));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private float hueFor(@NonNull PlayerMarkersRenderer.RenderablePlayerMarker m) {
        switch (m.getMarkerStyle()) {
            case SEEKER:  return BitmapDescriptorFactory.HUE_RED;
            case HIDER:   return BitmapDescriptorFactory.HUE_GREEN;
            case CAUGHT:  return BitmapDescriptorFactory.HUE_ORANGE;
            case POWERUP: return BitmapDescriptorFactory.HUE_YELLOW;
            default:      return BitmapDescriptorFactory.HUE_VIOLET;
        }
    }
}
