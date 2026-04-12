package com.example.bitcamp26.feature.match.map;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.bitcamp26.core.model.HotspotState;
import com.example.bitcamp26.feature.match.components.PlayerMarkersRenderer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the embedded SupportMapFragment, camera, markers, and hotspot circles.
 */
public class MapManager implements OnMapReadyCallback {

    private static final LatLng DEFAULT_LOCATION = new LatLng(38.9869, -76.9426); // UMD campus
    private static final float DEFAULT_ZOOM = 15f;

    @Nullable private GoogleMap googleMap;
    private final List<PlayerMarkersRenderer.RenderablePlayerMarker> currentMarkers = new ArrayList<>();
    private final List<HotspotState> currentHotspots = new ArrayList<>();

    public MapManager(@NonNull FragmentManager fragmentManager, int containerId) {
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        fragmentManager.beginTransaction()
                .replace(containerId, mapFragment)
                .commitNow();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
        googleMap.getUiSettings().setAllGesturesEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        redrawMap();
    }

    public void updateMarkers(@NonNull List<PlayerMarkersRenderer.RenderablePlayerMarker> markers) {
        currentMarkers.clear();
        currentMarkers.addAll(markers);
        redrawMap();
    }

    public void updateHotspots(@Nullable List<HotspotState> hotspots) {
        currentHotspots.clear();
        if (hotspots != null) currentHotspots.addAll(hotspots);
        redrawMap();
    }

    public void clearHotspots() {
        currentHotspots.clear();
        redrawMap();
    }

    public void moveCameraTo(double latitude, double longitude) {
        if (googleMap != null) {
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), DEFAULT_ZOOM));
        }
    }

    private void redrawMap() {
        if (googleMap == null) return;
        googleMap.clear();

        for (PlayerMarkersRenderer.RenderablePlayerMarker m : currentMarkers) {
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(m.getLatitude(), m.getLongitude()))
                    .title(m.getLabel())
                    .snippet(m.getSubtitle()));
        }

        for (HotspotState h : currentHotspots) {
            if (h == null) continue;
            googleMap.addCircle(new CircleOptions()
                    .center(new LatLng(h.getLat(), h.getLng()))
                    .radius(h.getRadiusMeters() > 0 ? h.getRadiusMeters() : 50.0)
                    .strokeColor(Color.BLUE)
                    .fillColor(0x220000FF)
                    .strokeWidth(3f));
        }
    }
}
