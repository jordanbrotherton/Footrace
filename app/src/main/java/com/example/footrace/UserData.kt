package com.example.footrace

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant

/**
 * Contains all the information of the user, fetched/saved to Firestore.
 */
data class User(
    @DocumentId
    val uid: String = "",
    val name: String = "",
    val daySteps: Long = 0L,
    val yesterdaySteps: Long = 0L,
    val monthSteps: Long = 0L,
    val friends: List<String> = emptyList(),
    val pendingFriends: List<String> = emptyList(),
    val lastActive: Timestamp = Timestamp.now(),
    @ServerTimestamp
    val updated: Timestamp? = null
)