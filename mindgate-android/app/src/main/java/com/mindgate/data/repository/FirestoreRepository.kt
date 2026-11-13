package com.mindgate.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindgate.domain.model.ExtensionRequest
import com.mindgate.util.MindGateLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // ALIGNED: EXACT COLLECTION NAME PER RULES
    private val requestsCollection = firestore.collection("extension_requests")
    private val linksCollection = firestore.collection("parentChildLinks")
    private val ledgerCollection = firestore.collection("extensionLedger")
    private val tokensCollection = firestore.collection("deviceTokens")

    /**
     * Requirement: Write new document to extension_requests with status "PENDING"
     * ALIGNED: Fields studentUid, parentUid, status (Uppercase)
     */
    suspend fun createExtensionRequest(
        studentUid: String,
        parentUid: String,
        appPackage: String,
        studentName: String,
        appName: String = "",
        requestedMinutes: Int = 15,
        status: String = "PENDING"
    ): Result<String> {
        return try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid ?: studentUid

            // LOGGING FOR DIAGNOSIS
            android.util.Log.e("MG_FIRESTORE", "--- SENDING REQUEST ---")
            android.util.Log.e("MG_FIRESTORE", "From Student: $uid")
            android.util.Log.e("MG_FIRESTORE", "To Parent: $parentUid")
            android.util.Log.e("MG_FIRESTORE", "App: $appName ($appPackage)")

            val requestMap = hashMapOf(
                "studentUid" to uid,
                "parentUid" to parentUid,
                "studentId" to uid, // Compatibility
                "parentId" to parentUid, // Compatibility
                "appPackage" to appPackage,
                "appName" to appName,
                "studentName" to studentName,
                "requestedMinutes" to requestedMinutes,
                "status" to "PENDING",
                "requestedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val docRef = requestsCollection.add(requestMap).await()
            android.util.Log.i("MG_FIRESTORE", "SUCCESS! Doc ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("MG_FIRESTORE", "CRITICAL FAILURE: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Attach a real-time listener for status updates
     */
    fun observeRequestStatus(requestId: String): Flow<ExtensionRequest?> = callbackFlow {
        val listener = requestsCollection.document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val request = snapshot.toObject(ExtensionRequest::class.java)
                    trySend(request)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getParentIdForStudent(studentUid: String): String? {
        return try {
            // Check studentId first (alias)
            var query = linksCollection.whereEqualTo("studentId", studentUid).limit(1).get().await()
            var doc = query.documents.firstOrNull()
            
            if (doc == null) {
                // Try studentUid field
                query = linksCollection.whereEqualTo("studentUid", studentUid).limit(1).get().await()
                doc = query.documents.firstOrNull()
            }

            doc?.getString("parentId") ?: doc?.getString("parentUid")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parent side: Observe pending requests
     * ALIGNED: status "PENDING"
     */
    fun observePendingRequestsForParent(parentUid: String): Flow<List<ExtensionRequest>> = callbackFlow {
        android.util.Log.d("FIRESTORE_DEBUG", "QUERY_PENDING: parentUid='$parentUid'")
        
        val query = requestsCollection
            .whereEqualTo("parentUid", parentUid)
            .whereEqualTo("status", "PENDING")

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FIRESTORE_DEBUG", "QUERY_PENDING_ERROR: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val requests = snapshot?.documents?.mapNotNull { it.toObject(ExtensionRequest::class.java) } ?: emptyList()
            android.util.Log.i("FIRESTORE_DEBUG", "QUERY_PENDING_SUCCESS: found ${requests.size} requests")
            trySend(requests)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Parent side: Observe recent activity
     */
    fun observeRecentActivityForParent(parentUid: String): Flow<List<ExtensionRequest>> = callbackFlow {
        val query = requestsCollection
            .whereEqualTo("parentUid", parentUid)
            .limit(20)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val activity = snapshot?.documents?.mapNotNull { it.toObject(ExtensionRequest::class.java) }
                ?.filter { it.status != "PENDING" } ?: emptyList()
            trySend(activity)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Parent side: Update status
     * ALIGNED: status "APPROVED" or "DENIED"
     */
    suspend fun updateRequestStatus(requestId: String, status: String, approvedMinutes: Int? = null): Result<Unit> {
        return try {
            val normalizedStatus = status.uppercase()
            val updates = mutableMapOf<String, Any>(
                "status" to normalizedStatus,
                "respondedAt" to com.google.firebase.Timestamp.now()
            )
            approvedMinutes?.let { updates["approvedMinutes"] = it }
            
            requestsCollection.document(requestId).update(updates).await()

            // Requirement: Write to ledger if approved
            if (normalizedStatus == "APPROVED") {
                val doc = requestsCollection.document(requestId).get().await()
                val sUid = doc.getString("studentUid") ?: doc.getString("studentId") ?: ""
                val pUid = doc.getString("parentUid") ?: doc.getString("parentId") ?: ""
                
                ledgerCollection.add(mapOf(
                    "requestId" to requestId,
                    "studentUid" to sUid,
                    "parentUid" to pUid,
                    "studentId" to sUid, // Compatibility alias
                    "parentId" to pUid, // Compatibility alias
                    "appPackage" to (doc.getString("appPackage") ?: ""),
                    "grantedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Student side: Mark as consumed
     */
    suspend fun markRequestConsumed(requestId: String): Result<Unit> {
        return try {
            requestsCollection.document(requestId).update("status", "CONSUMED").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Student side: Sync local grants from Firestore
     */
    suspend fun getApprovedRequestsForStudent(studentUid: String): List<ExtensionRequest> {
        return try {
            val query = requestsCollection
                .whereEqualTo("studentUid", studentUid)
                .whereEqualTo("status", "APPROVED")
            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { it.toObject(ExtensionRequest::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun observeEarnedExtensionsForStudent(studentUid: String): Flow<List<ExtensionRequest>> = callbackFlow {
        val query = requestsCollection
            .whereEqualTo("studentUid", studentUid)
            .whereEqualTo("status", "EARNED")

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val requests = snapshot?.documents?.mapNotNull { it.toObject(ExtensionRequest::class.java) } ?: emptyList()
            trySend(requests)
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateDeviceToken(userId: String, fcmToken: String) {
        try {
            tokensCollection.document(userId).set(
                mapOf(
                    "fcmToken" to fcmToken,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            MindGateLog.e("FIRESTORE", "Error updating device token", e)
        }
    }

    // Fallback sync helper
    suspend fun getPendingRequestsForParent(parentUid: String): List<ExtensionRequest> {
        return try {
            val query = requestsCollection
                .whereEqualTo("parentUid", parentUid)
                .whereEqualTo("status", "PENDING")
            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { it.toObject(ExtensionRequest::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
