package com.example.footrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.footrace.ui.theme.FootraceTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val availabilityStatus = HealthConnectClient.getSdkStatus(this)
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            finishAffinity()
        }

        setContent {
            FootraceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Header()
                }
            }
        }
    }
}

suspend fun getSteps(health: HealthConnectClient): Long {
    val dayStart = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant()

    val response = health.aggregate(AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL),
        timeRangeFilter = TimeRangeFilter.between(dayStart, Instant.now())))
    return response[StepsRecord.COUNT_TOTAL] ?: 0L
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun Header() {
    val context = LocalContext.current

    val healthConnectClient = remember {
        HealthConnectClient.getOrCreate(context)
    }

    var stepCount by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    val launcher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted -> if (granted.containsAll(permissions)) {
        scope.launch {
            stepCount = getSteps(healthConnectClient)
        }
    }
    }

    Column(
        modifier =  Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$stepCount",
            style = MaterialTheme.typography.displayLarge
        )
        Text(text = "Steps Today", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            scope.launch {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                if(granted.containsAll(permissions)) {
                    stepCount = getSteps(healthConnectClient)
                } else {
                    launcher.launch(permissions)
                }
            }
        }) {
            Text("Update Steps")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FootraceTheme {
        Greeting("Android")
    }
}