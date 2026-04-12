package com.example.bitcamp26.data.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bitcamp26.core.util.GeoUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

/**
 * Repository for reading the user's current device location.
 *
 * Note: the caller is responsible for ensuring location permissions
 * have already been granted before calling these methods.
 */
public class LocationRepository {

    private final FusedLocationProviderClient fusedLocationClient;

    public LocationRepository(@NonNull Context context) {
        this(LocationServices.getFusedLocationProviderClient(context.getApplicationContext()));
    }

    public LocationRepository(@NonNull FusedLocationProviderClient fusedLocationClient) {
        this.fusedLocationClient = fusedLocationClient;
    }

    /**
     * Attempts to fetch the device's last known location.
     */
    @SuppressLint("MissingPermission")
    public void getLastKnownLocation(@NonNull final LocationCallback callback) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        callback.onError("Last known location is unavailable.");
                        return;
                    }

                    callback.onSuccess(location);
                })
                .addOnFailureListener(e -> callback.onError(getMessageOrDefault(e, "Failed to get last known location.")));
    }

    /**
     * Requests a fresh high-accuracy current location.
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocation(@NonNull final LocationCallback callback) {
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        Task<Location> task = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.getToken()
        );

        task.addOnSuccessListener(location -> {
                    if (location == null) {
                        callback.onError("Current location is unavailable.");
                        return;
                    }

                    callback.onSuccess(location);
                })
                .addOnFailureListener(e -> callback.onError(getMessageOrDefault(e, "Failed to get current location.")));
    }

    /**
     * Returns true if the given location lies within the provided radius of the target point.
     */
    public boolean isWithinRadius(@Nullable Location location,
                                  double targetLat,
                                  double targetLng,
                                  double radiusMeters) {
        if (location == null || radiusMeters < 0) {
            return false;
        }

        return GeoUtils.isWithinRadius(
                targetLat,
                targetLng,
                location.getLatitude(),
                location.getLongitude(),
                radiusMeters
        );
    }

    /**
     * Returns the distance in meters from the given location to a target point.
     */
    public double distanceTo(@Nullable Location location,
                             double targetLat,
                             double targetLng) {
        if (location == null) {
            return Double.MAX_VALUE;
        }

        return GeoUtils.distanceMeters(
                location.getLatitude(),
                location.getLongitude(),
                targetLat,
                targetLng
        );
    }

    @NonNull
    private String getMessageOrDefault(@Nullable Exception e, @NonNull String defaultMessage) {
        return e != null && e.getMessage() != null && !e.getMessage().trim().isEmpty()
                ? e.getMessage()
                : defaultMessage;
    }

    public interface LocationCallback {
        void onSuccess(@NonNull Location location);
        void onError(@NonNull String errorMessage);
    }
}
