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

    suspend fun getSteps(): Long {
        val dayStart = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val response = health.aggregate(AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(dayStart, Instant.now())))
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }
}