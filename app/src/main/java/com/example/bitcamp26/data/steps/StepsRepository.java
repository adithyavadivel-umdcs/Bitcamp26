package com.example.bitcamp26.data.steps;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.time.TimeRangeFilter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository for reading step count data.
 *
 * This implementation uses Health Connect when available.
 * The caller is responsible for checking availability and requesting permissions.
 */
public class StepsRepository {

    private final HealthConnectClient healthConnectClient;
    private final Executor executor;

    public StepsRepository(@NonNull Context context) {
        this(HealthConnectClient.getOrCreate(context), Executors.newSingleThreadExecutor());
    }

    public StepsRepository(@NonNull HealthConnectClient healthConnectClient,
                           @NonNull Executor executor) {
        this.healthConnectClient = healthConnectClient;
        this.executor = executor;
    }

    /**
     * Returns the Health Connect permissions required to read step data.
     */
    @NonNull
    public static Set<String> getRequiredPermissions() {
        return Collections.singleton(HealthPermission.getReadPermission(StepsRecord.class));
    }

    /**
     * Reads the total number of steps for today.
     */
    public void getTodayStepCount(@NonNull final StepsCallback callback) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant();
        Instant now = Instant.now();
        getStepCountBetween(startOfDay, now, callback);
    }

    /**
     * Reads the total number of steps between two instants.
     */
    public void getStepCountBetween(@NonNull Instant start,
                                    @NonNull Instant end,
                                    @NonNull final StepsCallback callback) {
        executor.execute(() -> {
            try {
                ReadRecordsRequest<StepsRecord> request = new ReadRecordsRequest<>(
                        StepsRecord.class,
                        TimeRangeFilter.between(start, end)
                );

                List<StepsRecord> records = healthConnectClient.readRecords(request).getRecords();
                long totalSteps = 0L;

                for (StepsRecord record : records) {
                    totalSteps += record.getCount();
                }

                callback.onSuccess(totalSteps);
            } catch (Exception e) {
                String message = e.getMessage() != null && !e.getMessage().trim().isEmpty()
                        ? e.getMessage()
                        : "Failed to read step data.";
                callback.onError(message);
            }
        });
    }

    /**
     * Returns true if the user has met or exceeded the required step goal.
     */
    public boolean hasMetGoal(long currentSteps, long requiredSteps) {
        return currentSteps >= Math.max(requiredSteps, 0L);
    }

    /**
     * Returns the non-negative number of steps remaining to reach a goal.
     */
    public long stepsRemaining(long currentSteps, long requiredSteps) {
        return Math.max(0L, requiredSteps - currentSteps);
    }

    public interface StepsCallback {
        void onSuccess(long stepCount);
        void onError(@NonNull String errorMessage);
    }
}
