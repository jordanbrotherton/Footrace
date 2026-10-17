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
import androidx.lifecycle.lifecycleScope
import com.example.footrace.ui.theme.FootraceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            try {
                val uid = NetworkData.getAuth()
                android.util.Log.d("FootraceAuth", "Authenticated runner UID: $uid")
            } catch (e: Exception) {
                android.util.Log.e("FootraceAuth", "Auth failed", e)
            }
        }

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
    val health = remember { HealthData(context.applicationContext) }
    var stepCount by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(health.permissions)) {
            scope.launch {
                stepCount = health.getSteps()
                NetworkData.sendSteps(stepCount)
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
                if(health.isPermitted()) {
                    stepCount = health.getSteps()
                    NetworkData.sendSteps(stepCount)
                } else {
                    launcher.launch(health.permissions)
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