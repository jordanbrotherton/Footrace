package com.example.footrace

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
object NetworkData {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

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
}