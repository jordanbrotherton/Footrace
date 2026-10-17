package com.example.footrace

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


data class StepData(
    val daySteps: Long = 0L,
    val yesterdaySteps: Long = 0L,
    val monthSteps: Long = 0L
)

class HealthData(context: Context) {
    val health: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    suspend fun isPermitted(): Boolean {
        val granted = health.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun getSteps(): StepData {
        val zoneID = ZoneId.systemDefault()
        val now = LocalDate.now(zoneID)
        val nowInstant = Instant.now()

        val dayStart = now.atStartOfDay(zoneID).toInstant()
        val yesterday = now.minusDays(1).atStartOfDay(zoneID).toInstant()
        val monthStart = now.withDayOfMonth(1).atStartOfDay(zoneID).toInstant()

        val responseDay = health.aggregate(AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(dayStart, nowInstant)))
        val responseYesterday = health.aggregate(AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(yesterday, dayStart)))
        val responseMonth = health.aggregate(AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(monthStart, nowInstant)))
        return StepData(
            responseDay[StepsRecord.COUNT_TOTAL] ?: 0L,
            responseYesterday[StepsRecord.COUNT_TOTAL] ?: 0L,
            responseMonth[StepsRecord.COUNT_TOTAL] ?: 0L
        )
    }
}
