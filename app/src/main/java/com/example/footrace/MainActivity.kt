package com.example.footrace

import android.os.Bundle
import android.view.RoundedCorner
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
                    Header(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding)
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
fun Header(modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues()) {
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

    if(isOnboarded) {
        var selectedElement by remember { mutableStateOf(TimeElement.TODAY) }
        var friends by remember { mutableStateOf<List<User>>(emptyList()) }
        var pendingFriends by remember { mutableStateOf<List<User>>(emptyList()) }
        var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
        val currentUid = remember { NetworkData.getUid() }
        var isLoading by remember { mutableStateOf(true) }

        fun refreshLeaderboard() {
            scope.launch {
                val container = NetworkData.getFriends() ?: NetworkData.FriendContainer()
                val allUids = (container.friends).filter { it.isNotBlank() }.distinct()
                val pendUids = (container.pendingFriends).filter { it.isNotBlank() }.distinct()
                friends = NetworkData.friendToUData(allUids)
                allUsers = NetworkData.friendToUData((allUids + currentUid))
                pendingFriends = NetworkData.friendToUData(pendUids)
            }
        }

        suspend fun sync() {
            isLoading = true
            if (health.isPermitted()) {
                stepCount = health.getSteps()
                NetworkData.sendSteps(stepCount)
                refreshLeaderboard()
                isLoading = false
            } else {
                launcher.launch(health.permissions)
            }
        }

        LaunchedEffect (isOnboarded) {
            if(isOnboarded) { sync() }
        }

        var scrollState = rememberScrollState()

        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = {scope.launch{sync()}},
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = contentPadding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomStart = 32.dp,
                            bottomEnd = 32.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .padding(top = contentPadding.calculateTopPadding() + 8.dp)
                                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Today's Sprint",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "%,d".format(stepCount.daySteps),
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Black
                            )

                            Text(
                                text = "steps",
                                fontSize = 14.sp,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp)
                    ) {
                        LeaderboardTimeFilter(
                            selectedElement = selectedElement,
                            onWindowSelected = { selectedElement = it },
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Leaderboard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${allUsers.size} Racers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LeaderboardSection(
                            users = allUsers,
                            selectedElement = selectedElement,
                            currentUid = currentUid
                        )
                    }
                }
            }

            // Add a view Fragment
            ExtendedFloatingActionButton(
                onClick = { showSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 75.dp)
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "Open sheet")
                Text(text = "Friends")
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

                    Text(
                        text = "Add Friend",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Invite more competitors to your race!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )


                    OutlinedTextField(
                        value = friendSearchName,
                        onValueChange = { friendSearchName = it },
                        label = { Text("Friend Name") },
                        placeholder = { Text("Big Stepper") },
                        modifier = Modifier
                            .fillMaxWidth(),
                        maxLines = 1
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                NetworkData.sendFriendByName(friendSearchName)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Open sheet")
                        Text(text = "Send Friend Request")
                    }


                        Spacer(modifier = Modifier.height(16.dp))

                        // pending friends

                        if(pendingFriends.isNotEmpty()) {
                            Text(
                                text = "Pending Friends (${pendingFriends.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(pendingFriends, key = { it.uid }) { friend ->
                                        PendingFriendItemRow(
                                            friend = friend,
                                            onAddClick = {
                                                scope.launch {
                                                    NetworkData.approveFriend(friend.uid)
                                                }
                                            }
                                        )
                                    }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // current friends

                        Text(
                            text = "Friends (${friends.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if(friends.isEmpty()) {
                            Text(
                                text = "No friends. Add some!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(friends, key = { it.uid }) { friend ->
                                    FriendItemRow(
                                        friend = friend,
                                        onRemoveClick = {
                                            scope.launch {
                                                NetworkData.removeFriend(friend.uid)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


enum class TimeElement(val label: String){
    YESTERDAY("Yesterday"),
    TODAY("Today"),
    MONTH("Month")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardTimeFilter(
    selectedElement: TimeElement,
    onWindowSelected: (TimeElement) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        TimeElement.entries.forEachIndexed { index, element -> SegmentedButton(
            selected = selectedElement ==  element,
            onClick = { onWindowSelected(element) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeElement.entries.size)
        ) { Text(element.label) } }
    }
}

@Composable
fun LeaderboardSection(
    users: List<User>,
    selectedElement: TimeElement,
    currentUid: String,
    modifier: Modifier = Modifier
) {
    val sortedUsers = remember(users, selectedElement) {
        users.sortedByDescending { user ->
            when(selectedElement) {
                TimeElement.TODAY -> user.daySteps
                TimeElement.YESTERDAY -> user.yesterdaySteps
                TimeElement.MONTH -> user.monthSteps
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        itemsIndexed(sortedUsers, key = { _, user -> user.uid }) { index, user ->
            val isCurrentUser = user.uid == currentUid

            val stepCount = when (selectedElement) {
                TimeElement.TODAY -> user.daySteps
                TimeElement.YESTERDAY -> user.yesterdaySteps
                TimeElement.MONTH -> user.monthSteps
            }

            Card(
                modifier = Modifier.fillMaxWidth().animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)),
                colors = CardDefaults.cardColors(
                    containerColor = if(index == 0){
                        MaterialTheme.colorScheme.primaryContainer
                    }else{
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if(index == 0) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            if (index == 0) {
                                Text(
                                    text = "In the Lead",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = if (isCurrentUser) "${user.name} (You)" else user.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    Text(
                        text = "${"%,d".format(stepCount)} steps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun FriendItemRow(
    friend: User,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${"%,d".format(friend.daySteps)} steps today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove friend",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PendingFriendItemRow(
    friend: User,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add friend")
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