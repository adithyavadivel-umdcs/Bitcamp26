
package com.example.bitcamp26.feature.match.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.model.HotspotState;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple custom overlay view that draws hotspot square bounds and labels.
 *
 * This version is screen-coordinate based. The caller is expected to convert
 * map coordinates into on-screen x/y positions before passing them in.
 */
public class HotspotOverlayView extends View {

    private final Paint activeFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inactiveFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inactiveStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private final List<RenderableHotspot> hotspots = new ArrayList<>();

    private boolean showLabels = true;
    private float minimumRadiusPx = 24f;

    public HotspotOverlayView(@NonNull Context context) {
        super(context);
        init();
    }

    public HotspotOverlayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HotspotOverlayView(@NonNull Context context,
                              @Nullable AttributeSet attrs,
                              int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        activeFillPaint.setStyle(Paint.Style.FILL);
        activeFillPaint.setColor(Color.argb(70, 76, 175, 80));

        inactiveFillPaint.setStyle(Paint.Style.FILL);
        inactiveFillPaint.setColor(Color.argb(50, 158, 158, 158));

        activeStrokePaint.setStyle(Paint.Style.STROKE);
        activeStrokePaint.setStrokeWidth(dpToPx(2));
        activeStrokePaint.setColor(Color.rgb(56, 142, 60));

        inactiveStrokePaint.setStyle(Paint.Style.STROKE);
        inactiveStrokePaint.setStrokeWidth(dpToPx(2));
        inactiveStrokePaint.setColor(Color.rgb(97, 97, 97));

        labelPaint.setColor(Color.BLACK);
        labelPaint.setTextSize(spToPx(12));
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Replaces the current set of hotspots to draw.
     */
    public void setHotspots(@Nullable List<RenderableHotspot> hotspots) {
        this.hotspots.clear();
        if (hotspots != null) {
            this.hotspots.addAll(hotspots);
        }
        invalidate();
    }

    /**
     * Adds one hotspot to the overlay.
     */
    public void addHotspot(@NonNull RenderableHotspot hotspot) {
        hotspots.add(hotspot);
        invalidate();
    }

    /**
     * Clears all hotspots from the overlay.
     */
    public void clearHotspots() {
        hotspots.clear();
        invalidate();
    }

    /**
     * Controls whether labels are shown beneath each hotspot.
     */
    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
        invalidate();
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    /**
     * Sets the minimum hotspot half-size in pixels.
     */
    public void setMinimumRadiusPx(float minimumRadiusPx) {
        this.minimumRadiusPx = Math.max(0f, minimumRadiusPx);
        invalidate();
    }

    public float getMinimumRadiusPx() {
        return minimumRadiusPx;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        for (RenderableHotspot hotspot : hotspots) {
            if (hotspot == null) {
                continue;
            }

            float halfSize = Math.max(minimumRadiusPx, hotspot.radiusPx);
            boolean active = hotspot.active;

            canvas.drawRect(
                    hotspot.centerXPx - halfSize,
                    hotspot.centerYPx - halfSize,
                    hotspot.centerXPx + halfSize,
                    hotspot.centerYPx + halfSize,
                    active ? activeFillPaint : inactiveFillPaint
            );

            canvas.drawRect(
                    hotspot.centerXPx - halfSize,
                    hotspot.centerYPx - halfSize,
                    hotspot.centerXPx + halfSize,
                    hotspot.centerYPx + halfSize,
                    active ? activeStrokePaint : inactiveStrokePaint
            );

            if (showLabels && hotspot.label != null && !hotspot.label.trim().isEmpty()) {
                canvas.drawText(
                        hotspot.label,
                        hotspot.centerXPx,
                        hotspot.centerYPx + halfSize + dpToPx(16),
                        labelPaint
                );
            }
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    /**
     * Lightweight drawing model for a hotspot projected onto the screen.
     */
    public static class RenderableHotspot {
        public final String id;
        public final String label;
        public final float centerXPx;
        public final float centerYPx;
        public final float radiusPx;
        public final boolean active;

        public RenderableHotspot(@Nullable String id,
                                 @Nullable String label,
                                 float centerXPx,
                                 float centerYPx,
                                 float radiusPx,
                                 boolean active) {
            this.id = id;
            this.label = label;
            this.centerXPx = centerXPx;
            this.centerYPx = centerYPx;
            this.radiusPx = radiusPx;
            this.active = active;
        }

        @NonNull
        public static RenderableHotspot fromState(@NonNull HotspotState hotspotState,
                                                  float centerXPx,
                                                  float centerYPx,
                                                  float radiusPx) {
            String label = hotspotState.getId() != null
                    ? hotspotState.getId()
                    : "Hotspot";

            return new RenderableHotspot(
                    hotspotState.getId(),
                    label,
                    centerXPx,
                    centerYPx,
                    radiusPx,
                    hotspotState.isActive()
            );
        }
    }
}
