package com.example.bitcamp26.data.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Repository for handling Android GPS location updates.
 */
public class LocationRepository {

    private final FusedLocationProviderClient client;
    private LocationCallback locationCallback;

    public LocationRepository(@NonNull Context context) {
        this.client = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates(@NonNull String userId, @NonNull LocationUpdateCallback callback) {
        stopLocationUpdates();

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(1000L)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    callback.onLocationUpdate(new PlayerLocation(
                            userId,
                            locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude(),
                            System.currentTimeMillis()
                    ));
                }
            }
        };

        client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                .addOnFailureListener(e -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown GPS error"));
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            client.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    public interface LocationUpdateCallback {
        void onLocationUpdate(@NonNull PlayerLocation location);
        void onError(@NonNull String errorMessage);
    }
}
