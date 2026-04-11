package com.example.bitcamp26.feature.match.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Banner view used to show the current shrink-zone or match-pressure state.
 *
 * This view is intentionally self-contained and programmatic so it can be dropped
 * into a layout or fragment without requiring an XML file first. It can show:
 * - a main title
 * - a supporting message
 * - an optional countdown/progress indicator
 * - visual urgency states such as normal, warning, and danger
 *
 * Typical examples:
 * - "Zone shrinking in 00:20"
 * - "Move closer to the center"
 * - "Safe zone updated"
 */
public class ShrinkBannerView extends LinearLayout {

    /**
     * Visual state for the banner.
     */
    public enum BannerState {
        NORMAL,
        WARNING,
        DANGER,
        SUCCESS,
        DISABLED
    }

    private final TextView titleTextView;
    private final TextView messageTextView;
    private final TextView timerTextView;
    private final ProgressBar progressBar;

    private BannerState currentState = BannerState.NORMAL;
    private boolean timerVisible = true;
    private boolean progressVisible = true;

    public ShrinkBannerView(@NonNull Context context) {
        super(context);
        titleTextView = new TextView(context);
        messageTextView = new TextView(context);
        timerTextView = new TextView(context);
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        init();
    }

    public ShrinkBannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        titleTextView = new TextView(context);
        messageTextView = new TextView(context);
        timerTextView = new TextView(context);
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        init();
    }

    public ShrinkBannerView(@NonNull Context context,
                            @Nullable AttributeSet attrs,
                            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        titleTextView = new TextView(context);
        messageTextView = new TextView(context);
        timerTextView = new TextView(context);
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_VERTICAL);

        int horizontalPadding = dpToPx(16);
        int verticalPadding = dpToPx(12);
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        LayoutParams matchWrapParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );

        titleTextView.setTextSize(16f);
        titleTextView.setTypeface(Typeface.DEFAULT_BOLD);
        titleTextView.setText("Shrink Zone");
        titleTextView.setGravity(Gravity.START);
        addView(titleTextView, matchWrapParams);

        LayoutParams messageParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        messageParams.topMargin = dpToPx(4);
        messageTextView.setTextSize(14f);
        messageTextView.setText("Stay inside the safe area.");
        addView(messageTextView, messageParams);

        LayoutParams timerParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        timerParams.topMargin = dpToPx(6);
        timerTextView.setTextSize(13f);
        timerTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        timerTextView.setText("00:00");
        addView(timerTextView, timerParams);

        LayoutParams progressParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        progressParams.topMargin = dpToPx(8);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        addView(progressBar, progressParams);

        applyStateStyling();
        updateVisibility();
    }

    /**
     * Sets the main banner title.
     */
    public void setTitle(@Nullable String title) {
        titleTextView.setText(safeText(title, "Shrink Zone"));
    }

    /**
     * Returns the current title text.
     */
    @NonNull
    public String getTitle() {
        return titleTextView.getText() != null ? titleTextView.getText().toString() : "";
    }

    /**
     * Sets the supporting banner message.
     */
    public void setMessage(@Nullable String message) {
        messageTextView.setText(safeText(message, ""));
    }

    /**
     * Returns the current message text.
     */
    @NonNull
    public String getMessage() {
        return messageTextView.getText() != null ? messageTextView.getText().toString() : "";
    }

    /**
     * Sets the timer text such as "00:18".
     */
    public void setTimerText(@Nullable String timerText) {
        timerTextView.setText(safeText(timerText, "00:00"));
    }

    /**
     * Returns the current timer text.
     */
    @NonNull
    public String getTimerText() {
        return timerTextView.getText() != null ? timerTextView.getText().toString() : "";
    }

    /**
     * Sets the progress percentage from 0 to 100.
     */
    public void setProgressPercent(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        progressBar.setProgress(clamped);
    }

    /**
     * Returns the current progress percentage.
     */
    public int getProgressPercent() {
        return progressBar.getProgress();
    }

    /**
     * Sets the visual urgency state.
     */
    public void setBannerState(@NonNull BannerState state) {
        currentState = state;
        applyStateStyling();
    }

    /**
     * Returns the current visual state.
     */
    @NonNull
    public BannerState getBannerState() {
        return currentState;
    }

    /**
     * Shows or hides the timer text.
     */
    public void setTimerVisible(boolean visible) {
        timerVisible = visible;
        updateVisibility();
    }

    public boolean isTimerVisible() {
        return timerVisible;
    }

    /**
     * Shows or hides the progress bar.
     */
    public void setProgressVisible(boolean visible) {
        progressVisible = visible;
        updateVisibility();
    }

    public boolean isProgressVisible() {
        return progressVisible;
    }

    /**
     * Convenience method for updating all visible banner data at once.
     */
    public void bind(@Nullable String title,
                     @Nullable String message,
                     @Nullable String timerText,
                     int progressPercent,
                     @NonNull BannerState state) {
        setTitle(title);
        setMessage(message);
        setTimerText(timerText);
        setProgressPercent(progressPercent);
        setBannerState(state);
    }

    /**
     * Convenience method for a disabled/inactive banner.
     */
    public void showInactiveState() {
        setTitle("Shrink Zone Inactive");
        setMessage("No active shrink event right now.");
        setTimerText("--:--");
        setProgressPercent(0);
        setBannerState(BannerState.DISABLED);
    }

    /**
     * Convenience method for a warning banner.
     */
    public void showWarningState(@Nullable String timerText) {
        setTitle("Zone Shrinking Soon");
        setMessage("Move toward the safe area.");
        setTimerText(timerText);
        setBannerState(BannerState.WARNING);
    }

    /**
     * Convenience method for an urgent banner.
     */
    public void showDangerState(@Nullable String timerText) {
        setTitle("Zone Shrinking Now");
        setMessage("Leave the danger area immediately.");
        setTimerText(timerText);
        setBannerState(BannerState.DANGER);
    }

    /**
     * Convenience method for a success/update banner.
     */
    public void showSuccessState(@Nullable String message) {
        setTitle("Safe Zone Updated");
        setMessage(safeText(message, "You are inside the safe zone."));
        setBannerState(BannerState.SUCCESS);
    }

    private void updateVisibility() {
        timerTextView.setVisibility(timerVisible ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(progressVisible ? View.VISIBLE : View.GONE);
    }

    private void applyStateStyling() {
        switch (currentState) {
            case NORMAL:
                setBackgroundColor(Color.parseColor("#E3F2FD"));
                titleTextView.setTextColor(Color.parseColor("#0D47A1"));
                messageTextView.setTextColor(Color.parseColor("#1565C0"));
                timerTextView.setTextColor(Color.parseColor("#0D47A1"));
                setAlpha(1.0f);
                break;

            case WARNING:
                setBackgroundColor(Color.parseColor("#FFF8E1"));
                titleTextView.setTextColor(Color.parseColor("#E65100"));
                messageTextView.setTextColor(Color.parseColor("#EF6C00"));
                timerTextView.setTextColor(Color.parseColor("#E65100"));
                setAlpha(1.0f);
                break;

            case DANGER:
                setBackgroundColor(Color.parseColor("#FFEBEE"));
                titleTextView.setTextColor(Color.parseColor("#B71C1C"));
                messageTextView.setTextColor(Color.parseColor("#C62828"));
                timerTextView.setTextColor(Color.parseColor("#B71C1C"));
                setAlpha(1.0f);
                break;

            case SUCCESS:
                setBackgroundColor(Color.parseColor("#E8F5E9"));
                titleTextView.setTextColor(Color.parseColor("#1B5E20"));
                messageTextView.setTextColor(Color.parseColor("#2E7D32"));
                timerTextView.setTextColor(Color.parseColor("#1B5E20"));
                setAlpha(1.0f);
                break;

            case DISABLED:
                setBackgroundColor(Color.parseColor("#F5F5F5"));
                titleTextView.setTextColor(Color.parseColor("#616161"));
                messageTextView.setTextColor(Color.parseColor("#757575"));
                timerTextView.setTextColor(Color.parseColor("#616161"));
                setAlpha(0.85f);
                break;
        }
    }

    @NonNull
    private String safeText(@Nullable String text, @NonNull String fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }
        return text.trim();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
