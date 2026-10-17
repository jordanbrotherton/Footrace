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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.lifecycle.lifecycleScope
import com.example.footrace.ui.theme.AppTheme
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

// Jouvens Added these for textfield
import androidx.compose.material3.OutlinedTextField

// Adding Rows
import androidx.compose.foundation.layout.Row

// Adding width
import androidx.compose.foundation.layout.width

// Importing Circle Shape
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect

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
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Header(modifier = Modifier.padding(innerPadding))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val health = remember { HealthData(context.applicationContext) }
    var stepCount by remember { mutableStateOf(StepData()) }
    val scope = rememberCoroutineScope()

    var showNameDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var isOnboarded by remember { mutableStateOf(false) }

    // Variable to take friend Username
    var friendSearchName by remember { mutableStateOf("") }

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val launcher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(health.permissions)) {
            scope.launch {
                val name = NetworkData.getName()
                if(name.isBlank()) {
                    showNameDialog = true
                }else{
                    isOnboarded = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if(!health.isPermitted()){
            launcher.launch(health.permissions)
        }else{
            val name = NetworkData.getName()
            if(name.isBlank()) {
                showNameDialog = true
            }else{
                isOnboarded = true
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = {},
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Your Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()){
                            scope.launch {
                                NetworkData.sendName(inputName.trim())
                                showNameDialog = false
                                isOnboarded = true
                            }
                        }
                    }
                ) { Text("Join") }
            }
        )
    }

/*-------------------------------------Jouvens =Added----------------------------------------------*/
    if(isOnboarded) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${stepCount.daySteps}",
                    style = MaterialTheme.typography.displayLarge
                )
                Text(text = "Steps Today", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = {
                    scope.launch {
                        if (health.isPermitted()) {
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

            // Add a view Fragment
            FloatingActionButton(
                onClick = { showSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 75.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Open sheet")
            }


            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {

                            OutlinedTextField(
                                value = friendSearchName,
                                onValueChange = { friendSearchName = it },
                                label = { Text("Friend Name") },
                                placeholder = { Text("Big Stepper") },
                                modifier = Modifier
                                    .height(65.dp)
                                    .width(300.dp),
                                maxLines = 1
                            )

                            Button(
                                onClick = {
                                    scope.launch {
                                        NetworkData.sendFriendByName(friendSearchName)
                                    }
                                },
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(top = 13.dp, start = 16.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Open sheet")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        // Put your sheet content here

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppTheme {
        Greeting("Android")
    }
}