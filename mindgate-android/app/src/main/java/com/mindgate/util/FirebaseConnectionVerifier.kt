package com.mindgate.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FirebaseConnectionVerifier {
    private const val TAG = "FirebaseVerifier"

    suspend fun runTest() {
        Log.i(TAG, "Starting Firebase verification")

        // 1. Firebase Auth Initialization
        try {
            val auth = FirebaseAuth.getInstance()
            Log.i(TAG, "Authentication initialization SUCCESS (User UID: ${auth.currentUser?.uid ?: "Anonymous"})")
        } catch (e: Exception) {
            Log.e(TAG, "Authentication initialization FAILED: ${e.message}")
        }

        // 2. Cloud Firestore Test
        try {
            val db = FirebaseFirestore.getInstance()
            val testData = hashMapOf(
                "message" to "Firebase connection successful",
                "timestamp" to FieldValue.serverTimestamp(),
                "platform" to "Android"
            )
            
            db.collection("connection_test")
                .document("android_test")
                .set(testData)
                .await()
            
            Log.i(TAG, "Firestore test SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore test FAILED: ${e.message}")
        }

        // 3. FCM Test
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.i(TAG, "FCM token retrieval SUCCESS")
            Log.d(TAG, "Token: $token")
        } catch (e: Exception) {
            Log.e(TAG, "FCM token retrieval FAILED: ${e.message}")
        }

        Log.i(TAG, "Firebase verification completed")
    }
}
