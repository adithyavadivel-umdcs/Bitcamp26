
package com.example.bitcamp26.feature.match.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.HotspotState;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight container that reserves space for a future map implementation
 * and layers a HotspotOverlayView on top.
 *
 * For now, this class shows a placeholder "map" surface and can render
 * hotspots using simple normalized screen coordinates.
 */
public class MapViewContainer extends FrameLayout {

    private final View mapPlaceholderView;
    private final TextView placeholderLabel;
    private final HotspotOverlayView hotspotOverlayView;

    private final List<HotspotState> hotspotStates = new ArrayList<>();

    private boolean mapReady;
    private float defaultRadiusPx;

    public MapViewContainer(@NonNull Context context) {
        super(context);
        mapPlaceholderView = new View(context);
        placeholderLabel = new TextView(context);
        hotspotOverlayView = new HotspotOverlayView(context);
        init();
    }

    public MapViewContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mapPlaceholderView = new View(context);
        placeholderLabel = new TextView(context);
        hotspotOverlayView = new HotspotOverlayView(context);
        init();
    }

    public MapViewContainer(@NonNull Context context,
                            @Nullable AttributeSet attrs,
                            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mapPlaceholderView = new View(context);
        placeholderLabel = new TextView(context);
        hotspotOverlayView = new HotspotOverlayView(context);
        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
        defaultRadiusPx = dpToPx(36f);

        LayoutParams matchParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );

        mapPlaceholderView.setLayoutParams(matchParams);
        mapPlaceholderView.setBackgroundColor(0xFFECEFF1);
        addView(mapPlaceholderView);

        placeholderLabel.setText("Map will render here");
        placeholderLabel.setTextSize(16f);
        placeholderLabel.setGravity(Gravity.CENTER);
        LayoutParams labelParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        labelParams.gravity = Gravity.CENTER;
        addView(placeholderLabel, labelParams);

        hotspotOverlayView.setLayoutParams(matchParams);
        hotspotOverlayView.setClickable(false);
        hotspotOverlayView.setFocusable(false);
        addView(hotspotOverlayView);
    }

    /**
     * Returns the hotspot overlay layered above the placeholder map surface.
     */
    @NonNull
    public HotspotOverlayView getHotspotOverlayView() {
        return hotspotOverlayView;
    }

    /**
     * Replaces the current hotspot state list and redraws the overlay.
     */
    public void setHotspots(@Nullable List<HotspotState> hotspots) {
        hotspotStates.clear();
        if (hotspots != null) {
            hotspotStates.addAll(hotspots);
        }
        redrawHotspots();
    }

    /**
     * Adds a single hotspot state and redraws the overlay.
     */
    public void addHotspot(@NonNull HotspotState hotspot) {
        hotspotStates.add(hotspot);
        redrawHotspots();
    }

    /**
     * Clears all hotspot state and overlay content.
     */
    public void clearHotspots() {
        hotspotStates.clear();
        hotspotOverlayView.clearHotspots();
    }

    /**
     * Shows or hides the placeholder label.
     */
    public void setPlaceholderTextVisible(boolean visible) {
        placeholderLabel.setVisibility(visible ? VISIBLE : GONE);
    }

    /**
     * Sets the placeholder label text.
     */
    public void setPlaceholderText(@NonNull String text) {
        placeholderLabel.setText(text);
    }

    /**
     * Marks whether a real map implementation has been attached.
     * This currently affects only the placeholder label visibility.
     */
    public void setMapReady(boolean mapReady) {
        this.mapReady = mapReady;
        placeholderLabel.setVisibility(mapReady ? GONE : VISIBLE);
    }

    public boolean isMapReady() {
        return mapReady;
    }

    /**
     * Sets the default radius used when drawing projected hotspots.
     */
    public void setDefaultRadiusPx(float defaultRadiusPx) {
        this.defaultRadiusPx = Math.max(0f, defaultRadiusPx);
        redrawHotspots();
    }

    public float getDefaultRadiusPx() {
        return defaultRadiusPx;
    }

    /**
     * Redraws the overlay by projecting hotspot lat/lng values into this view's bounds.
     *
     * Since a real map projection is not yet attached, this uses a simple normalized
     * transformation from latitude/longitude to x/y positions for placeholder rendering.
     */
    public void redrawHotspots() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            post(this::redrawHotspots);
            return;
        }

        List<HotspotOverlayView.RenderableHotspot> renderables = new ArrayList<>();
        for (HotspotState hotspot : hotspotStates) {
            if (hotspot == null) {
                continue;
            }

            float x = projectLongitudeToX(hotspot.getLng());
            float y = projectLatitudeToY(hotspot.getLat());

            renderables.add(
                    HotspotOverlayView.RenderableHotspot.fromState(
                            hotspot,
                            x,
                            y,
                            defaultRadiusPx
                    )
            );
        }

        hotspotOverlayView.setHotspots(renderables);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        redrawHotspots();
    }

    /**
     * Converts longitude in the range [-180, 180] into an x coordinate.
     */
    private float projectLongitudeToX(double longitude) {
        double normalized = (longitude + 180.0) / 360.0;
        normalized = clamp01(normalized);
        return (float) (normalized * getWidth());
    }

    /**
     * Converts latitude in the range [-90, 90] into a y coordinate.
     */
    private float projectLatitudeToY(double latitude) {
        double normalized = (90.0 - latitude) / 180.0;
        normalized = clamp01(normalized);
        return (float) (normalized * getHeight());
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
