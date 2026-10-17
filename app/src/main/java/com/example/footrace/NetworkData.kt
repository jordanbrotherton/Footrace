package com.example.footrace

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
object NetworkData {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    /**
     * Obtains the Firebase authentication.
     *
     *
     * @return The user ID if authenticated. May throw an error if failed.
     */
    suspend fun getAuth(): String {
        val ourUser = auth.currentUser

        return if (ourUser != null){
            ourUser.uid
        }else{
            // swap with google sometime later
            val result = auth.signInAnonymously().await()
            result.user?.uid ?: error("No Auth?")
        }
    }

    /**
     * Saves the step and user data for the client's user.
     *
     *
     * @param steps The [StepData] to save online.
     * @return A [Boolean] indicating whether the save was successful.
     */
    suspend fun sendSteps(steps: StepData): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val userUpdate = hashMapOf(
            "uid" to uid,
            "daySteps" to steps.daySteps,
            "yesterdaySteps" to steps.yesterdaySteps,
            "monthSteps" to steps.monthSteps,
            "lastActive" to Timestamp.now()
        )

        return try {
            db.collection("users").document(uid).set(userUpdate, SetOptions.merge()).await()
            true
        }catch (e: Exception){
            false
        }
    }

    /**
     * Saves the name for the client's user.
     *
     *
     * @param name The user's name to save online.
     * @return A [Boolean] indicating whether the save was successful.
     */
    suspend fun sendName(name: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false

        return try {
            db.collection("users").document(uid).set(mapOf("name" to name), SetOptions.merge()).await()
            true
        }catch (e: Exception){
            false
        }
    }

    /**
     * Obtains the name for the client's user.
     *
     *
     * @return A [String] with the user's name.
     */
    suspend fun getName(): String {
        val uid = auth.currentUser?.uid ?: return ""

        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.getString("name") ?: ""
        }catch (e: Exception){
            ""
        }
    }

    /**
     * Obtains the step data of the specified user.
     *
     *
     * @param uid The userID of the user to look up.
     * @return The user's step data as a [StepData]. May be null if invalid.
     */
    suspend fun getSteps(uid: String): StepData? {
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            val steps = StepData(
                snapshot.getLong("daySteps") ?: 0L,
                snapshot.getLong("yesterdaySteps") ?: 0L,
                snapshot.getLong("monthSteps") ?: 0L
            )
            return steps
        }catch (e: Exception){
            null
        }
    }

    /**
     * Basic return container for friend data.
     */
    data class FriendContainer(
        val friends: List<String> = emptyList(),
        val pendingFriends: List<String> = emptyList()
    )

    /**
     * Sends a friend request to the specified user ID.
     *
     *
     * @param uid The userID of the user to look up.
     * @return A [Boolean] if the request is successful.
     */
    suspend fun sendFriend(uid: String): Boolean {
        return try {
            val ourUid = auth.currentUser?.uid ?: return false
            if(ourUid == uid) { return false }
            val snapshotExists: Boolean = db.collection("users").document(uid).get().await().exists()
            if (snapshotExists) {
                val userDocRef = db.collection("users").document(uid)
                userDocRef.set(mapOf("pendingFriends" to FieldValue.arrayUnion(ourUid)), SetOptions.merge()).await()
                true
            }else{
                false
            }
        }catch (e: Exception){
            false
        }
    }

    /**
     * Gets the friends of the client user.
     *
     *
     * @return A [FriendContainer] containing friends or null if an error occurs.
     */
    suspend fun getFriends(): FriendContainer? {
        return try {
            val uid = auth.currentUser?.uid ?: return null
            val snapshot = db.collection("users").document(uid).get().await()
            val friends = FriendContainer(
                @Suppress("UNCHECKED_CAST")
                snapshot.get("friends") as? List<String> ?: emptyList(),
                @Suppress("UNCHECKED_CAST")
                snapshot.get("pendingFriends") as? List<String> ?: emptyList()
            )
            return friends
        }catch (e: Exception){
            null
        }
    }

    /**
     * Approves a friend request from the specified user ID.
     *
     *
     * @param uid The userID of the user to take in.
     * @return A [Boolean] if the request is successful.
     */
    suspend fun approveFriend(uid: String): Boolean {
        return try {
            val ourUid = auth.currentUser?.uid ?: return false
            val theirDocRef = db.collection("users").document(uid)
            val ourDocRef = db.collection("users").document(ourUid)
            ourDocRef.set(mapOf("pendingFriends" to FieldValue.arrayRemove(uid)), SetOptions.merge()).await()
            ourDocRef.set(mapOf("friends" to FieldValue.arrayUnion(uid)), SetOptions.merge()).await()
            theirDocRef.set(mapOf("friends" to FieldValue.arrayUnion(ourUid)), SetOptions.merge()).await()
            true
        }catch (e: Exception){
            false
        }
    }

    /**
     * Removes a friend of the specified user ID.
     *
     *
     * @param uid The userID of the user to look up.
     * @return A [Boolean] if the request is successful.
     */
    suspend fun removeFriend(uid: String): Boolean {
        return try {
            val ourUid = auth.currentUser?.uid ?: return false
            val theirDocRef = db.collection("users").document(uid)
            val ourDocRef = db.collection("users").document(ourUid)
            ourDocRef.set(mapOf("friends" to FieldValue.arrayRemove(uid)), SetOptions.merge()).await()
            theirDocRef.set(mapOf("friends" to FieldValue.arrayRemove(ourUid)), SetOptions.merge()).await()
            true
        }catch (e: Exception){
            false
        }
    }

    /**
     * Converts a list of UIDs into User Data
     *
     *
     * @param friends A list of UIDs.
     * @return A [List] of [User]s if the request is successful.
     */
    suspend fun friendToUData(friends: List<String>): List<User> {
        if (friends.isEmpty()) { return emptyList() }
        return try {
            val snapshots = db.collection("users")
                .whereIn(FieldPath.documentId(), friends.take(30))
                .get()
                .await()
            snapshots.documents.mapNotNull { doc ->
                User(
                    uid = doc.id,
                    name = doc.getString("name") ?: "User",
                    daySteps = doc.getLong("daySteps") ?: 0L,
                    yesterdaySteps = doc.getLong("yesterdaySteps") ?: 0L,
                    monthSteps = doc.getLong("monthSteps") ?: 0L
                )
            }
        }catch (e: Exception){
            return emptyList()
        }
    }

    /**
     * Sends a friend request with a name lookup.
     *
     *
     * @param name The name of the user to send.
     * @return A [Boolean] if the request is successful.
     */
    suspend fun sendFriendByName(name: String): Boolean {
        val snap = db.collection("users").whereEqualTo("name", name.trim()).limit(1).get().await()
        val targetUid = snap.documents.firstOrNull()?.id ?: return false
        return sendFriend(targetUid)
    }
}