package com.example.bitcamp26.feature.match.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * A FrameLayout that prevents an ancestor ScrollView from intercepting touch events,
 * so that an embedded Google Map can receive all gestures (pan, zoom, etc.).
 *
 * Usage: replace the FrameLayout that wraps the SupportMapFragment container in XML.
 */
public class TouchableWrapper extends FrameLayout {

    public TouchableWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // Tell every ancestor (including ScrollView) not to steal this gesture.
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Gesture is over — restore normal scroll behaviour.
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
