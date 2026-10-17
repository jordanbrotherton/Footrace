package com.example.footrace

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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
     * @param name The user's name to save online.
     * @return A [Boolean] indicating whether the save was successful.
     */
    suspend fun sendSteps(steps: StepData, name: String = ""): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val userUpdate = hashMapOf(
            "uid" to uid,
            "name" to name,
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
}