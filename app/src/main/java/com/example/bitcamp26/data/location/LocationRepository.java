package com.example.bitcamp26.data.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.util.GeoUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;

/**
 * Repository for reading and continuously tracking the user's device location.
 *
 * One-shot helpers: {@link #getLastKnownLocation} / {@link #getCurrentLocation}
 * Continuous updates: {@link #startLocationUpdates} / {@link #stopLocationUpdates}
 *
 * The caller is responsible for ensuring location permissions are granted
 * before calling any of these methods.
 */
public class LocationRepository {

    /** Update interval: target every 1.5 s, accept as fast as 1 s. */
    private static final long UPDATE_INTERVAL_MS = 1_500L;
    private static final long MIN_UPDATE_INTERVAL_MS = 1_000L;

    private final FusedLocationProviderClient fusedLocationClient;

    @Nullable
    private com.google.android.gms.location.LocationCallback activeLocationCallback;

    public LocationRepository(@NonNull Context context) {
        this(LocationServices.getFusedLocationProviderClient(context.getApplicationContext()));
    }

    public LocationRepository(@NonNull FusedLocationProviderClient fusedLocationClient) {
        this.fusedLocationClient = fusedLocationClient;
    }

    // -------------------------------------------------------------------------
    // Continuous real-time updates
    // -------------------------------------------------------------------------

    /**
     * Starts receiving location updates roughly every 1–2 seconds.
     *
     * Each update is delivered as a {@link PlayerLocation} to the callback.
     * Call {@link #stopLocationUpdates()} in onPause / onDestroyView.
     *
     * @param userId   The ID to stamp on each {@link PlayerLocation} snapshot.
     *                 Use the authenticated user's UID so teammates can key on it.
     * @param callback Receives updates on the main thread.
     */
    @SuppressLint("MissingPermission")
    public void startLocationUpdates(@NonNull String userId,
                                     @NonNull LocationUpdateCallback callback) {
        stopLocationUpdates(); // clean up any previous subscription

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                UPDATE_INTERVAL_MS
        )
                .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
                .build();

        activeLocationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location location = result.getLastLocation();
                if (location == null) return;

                callback.onLocationUpdate(new PlayerLocation(
                        userId,
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getTime()
                ));
            }
        };

        fusedLocationClient
                .requestLocationUpdates(request, activeLocationCallback, Looper.getMainLooper())
                .addOnFailureListener(e ->
                        callback.onError(getMessageOrDefault(e, "Failed to start location updates.")));
    }

    /**
     * Stops continuous location updates started by {@link #startLocationUpdates}.
     * Safe to call even if no updates are active.
     */
    public void stopLocationUpdates() {
        if (activeLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(activeLocationCallback);
            activeLocationCallback = null;
        }
    }

    // -------------------------------------------------------------------------
    // One-shot helpers (kept for backwards compatibility)
    // -------------------------------------------------------------------------

    /**
     * Attempts to fetch the device's last known location.
     */
    @SuppressLint("MissingPermission")
    public void getLastKnownLocation(@NonNull final OneTimeLocationCallback callback) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        callback.onError("Last known location is unavailable.");
                        return;
                    }
                    callback.onSuccess(location);
                })
                .addOnFailureListener(e ->
                        callback.onError(getMessageOrDefault(e, "Failed to get last known location.")));
    }

    /**
     * Requests a fresh high-accuracy current location (one-time).
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocation(@NonNull final OneTimeLocationCallback callback) {
        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        callback.onError("Current location is unavailable.");
                        return;
                    }
                    callback.onSuccess(location);
                })
                .addOnFailureListener(e ->
                        callback.onError(getMessageOrDefault(e, "Failed to get current location.")));
    }

    // -------------------------------------------------------------------------
    // Geo helpers
    // -------------------------------------------------------------------------

    /** Returns true if the given location lies within radiusMeters of the target. */
    public boolean isWithinRadius(@Nullable Location location,
                                  double targetLat,
                                  double targetLng,
                                  double radiusMeters) {
        if (location == null || radiusMeters < 0) return false;
        return GeoUtils.isWithinRadius(
                targetLat, targetLng,
                location.getLatitude(), location.getLongitude(),
                radiusMeters);
    }

    /** Returns the distance in metres from location to the target point. */
    public double distanceTo(@Nullable Location location,
                             double targetLat,
                             double targetLng) {
        if (location == null) return Double.MAX_VALUE;
        return GeoUtils.distanceMeters(
                location.getLatitude(), location.getLongitude(),
                targetLat, targetLng);
    }

    // -------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------

    /**
     * Callback for continuous real-time updates from {@link #startLocationUpdates}.
     * Delivered on the main thread.
     */
    public interface LocationUpdateCallback {
        void onLocationUpdate(@NonNull PlayerLocation location);
        void onError(@NonNull String errorMessage);
    }

    /**
     * Callback for one-time location requests.
     * @deprecated Prefer {@link LocationUpdateCallback} for live tracking.
     */
    public interface OneTimeLocationCallback {
        void onSuccess(@NonNull Location location);
        void onError(@NonNull String errorMessage);
    }

    /**
     * Backwards-compatible alias so callers using the old {@code RepositoryLocationCallback}
     * name still compile without changes.
     */
    public interface RepositoryLocationCallback extends OneTimeLocationCallback {}

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    @NonNull
    private String getMessageOrDefault(@Nullable Exception e, @NonNull String defaultMessage) {
        return e != null && e.getMessage() != null && !e.getMessage().trim().isEmpty()
                ? e.getMessage()
                : defaultMessage;
    }
}
