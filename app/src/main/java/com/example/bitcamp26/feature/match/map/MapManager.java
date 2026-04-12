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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to manage Google Map markers and circles.
 */
public class MapManager implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private Marker selfMarker;
    private final Map<String, Marker> playerMarkers = new HashMap<>();
    private final List<Circle> hotspotCircles = new ArrayList<>();

    private PlayerLocation pendingSelfLocation;
    private String pendingSelfLabel;
    private List<PlayerMarkersRenderer.RenderablePlayerMarker> pendingPlayerMarkers;
    private List<HotspotState> pendingHotspots;

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (pendingSelfLocation != null) {
            updateSelfLocation(pendingSelfLocation, pendingSelfLabel);
        }
        if (pendingPlayerMarkers != null) {
            updatePlayerMarkers(pendingPlayerMarkers);
        }
        if (pendingHotspots != null) {
            updateHotspots(pendingHotspots);
        }
    }

    public void updateSelfLocation(@NonNull PlayerLocation location, @Nullable String label) {
        if (googleMap == null) {
            this.pendingSelfLocation = location;
            this.pendingSelfLabel = label;
            return;
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (selfMarker == null) {
            selfMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(label != null ? label + " (You)" : "You")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
        } else {
            selfMarker.setPosition(latLng);
        }
    }

    public void updatePlayerMarkers(@NonNull List<PlayerMarkersRenderer.RenderablePlayerMarker> markers) {
        if (googleMap == null) {
            pendingPlayerMarkers = new ArrayList<>(markers);
            return;
        }

        List<String> activeIds = new ArrayList<>();

        for (PlayerMarkersRenderer.RenderablePlayerMarker pm : markers) {
            if (pm.isSelf() || pm.getPlayerId() == null) {
                continue;
            }
            activeIds.add(pm.getPlayerId());
            boolean isSeeker = pm.getMarkerStyle() == PlayerMarkersRenderer.MarkerStyle.SEEKER;
            Marker existing = playerMarkers.get(pm.getPlayerId());
            if (existing == null) {
                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(pm.getLatitude(), pm.getLongitude()))
                        .title(pm.getLabel())
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                isSeeker ? BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN)));
                if (marker != null) {
                    playerMarkers.put(pm.getPlayerId(), marker);
                }
            } else {
                existing.setPosition(new LatLng(pm.getLatitude(), pm.getLongitude()));
                existing.setTitle(pm.getLabel());
            }
        }

        List<String> staleIds = new ArrayList<>();
        for (Map.Entry<String, Marker> entry : playerMarkers.entrySet()) {
            if (!activeIds.contains(entry.getKey())) {
                entry.getValue().remove();
                staleIds.add(entry.getKey());
            }
        }
        for (String staleId : staleIds) {
            playerMarkers.remove(staleId);
        }
    }

    public void updateHotspots(@Nullable List<HotspotState> hotspots) {
        if (googleMap == null) {
            pendingHotspots = hotspots == null ? null : new ArrayList<>(hotspots);
            return;
        }

        for (Circle c : hotspotCircles) {
            c.remove();
        }
        hotspotCircles.clear();

        if (hotspots == null) return;

        for (HotspotState h : hotspots) {
            Circle c = googleMap.addCircle(new CircleOptions()
                    .center(new LatLng(h.getLat(), h.getLng()))
                    .radius(h.getRadiusMeters())
                    .strokeColor(Color.BLUE)
                    .fillColor(0x220000FF));
            hotspotCircles.add(c);
        }
    }

    public void clearAll() {
        if (selfMarker != null) {
            selfMarker.remove();
            selfMarker = null;
        }

        for (Marker marker : playerMarkers.values()) {
            marker.remove();
        }
        playerMarkers.clear();

        for (Circle circle : hotspotCircles) {
            circle.remove();
        }
        hotspotCircles.clear();

        pendingSelfLocation = null;
        pendingSelfLabel = null;
        pendingPlayerMarkers = null;
        pendingHotspots = null;
    }
}
