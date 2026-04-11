package com.example.bitcamp26.data.steps;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.response.ReadRecordsResponse;
import androidx.health.connect.client.time.TimeRangeFilter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// TODO: This class should be rewritten in Kotlin so it can call Health Connect suspend functions
//       natively with coroutines instead of using JvmClassMappingKt and BuildersKt.runBlocking.
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlinx.coroutines.BuildersKt;

/**
 * Repository for reading step count data via the Health Connect SDK.
 *
 * The caller is responsible for checking availability and requesting permissions before use.
 *
 * Note: Health Connect is a Kotlin-first SDK. The three Java interop adaptations below are
 * necessary until this class is ported to Kotlin:
 *   1. JvmClassMappingKt.getKotlinClass() — converts Java Class to KClass for SDK APIs
 *   2. ReadRecordsRequest built with KClass explicitly
 *   3. BuildersKt.runBlocking() — bridges suspend functions onto the background executor thread
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
        // TODO: KClass required because HealthPermission.getReadPermission() is Kotlin-first.
        KClass<StepsRecord> kClass = JvmClassMappingKt.getKotlinClass(StepsRecord.class);
        return Collections.singleton(HealthPermission.getReadPermission(kClass));
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
                // TODO: BuildersKt.runBlocking blocks this executor thread to bridge the
                //       Kotlin suspend API. Safe here because we are not on the main thread.
                //       Replace with a proper coroutine scope when porting to Kotlin.
                KClass<StepsRecord> kClass = JvmClassMappingKt.getKotlinClass(StepsRecord.class);
                // ReadRecordsRequest has no @JvmOverloads; all 6 params required from Java.
                // Remaining args are the Kotlin defaults: empty filter, ascending, 1000 page size, no token.
                ReadRecordsRequest<StepsRecord> request = new ReadRecordsRequest<StepsRecord>(
                        kClass,
                        TimeRangeFilter.between(start, end),
                        Collections.emptySet(),
                        true,
                        1000,
                        null
                );

                @SuppressWarnings("unchecked")
                ReadRecordsResponse<StepsRecord> response =
                        (ReadRecordsResponse<StepsRecord>) BuildersKt.runBlocking(
                                EmptyCoroutineContext.INSTANCE,
                                (scope, cont) -> healthConnectClient.readRecords(request, cont)
                        );

                List<StepsRecord> records = response.getRecords();
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
