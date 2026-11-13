package com.mindgate.data.repository

import com.mindgate.data.local.dao.ParentDao
import com.mindgate.data.local.entities.ParentChildRelationEntity
import com.mindgate.data.remote.api.ParentApiService
import com.mindgate.data.remote.dto.LinkAcceptRequest
import com.mindgate.data.remote.util.NetworkResult
import com.mindgate.data.remote.util.safeApiCall
import com.mindgate.util.MindGateLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentRepository @Inject constructor(
    private val api: ParentApiService,
    private val dao: ParentDao
) {
    val children: Flow<List<ParentChildRelationEntity>> = dao.observeChildren()

    suspend fun createLinkCode() = safeApiCall { api.createLinkCode() }

    suspend fun acceptLinkCode(code: String): NetworkResult<com.mindgate.data.remote.dto.LinkAcceptResponse> {
        val result = safeApiCall { api.acceptLinkCode(LinkAcceptRequest(code)) }
        if (result is NetworkResult.Success) {
            val body = result.data
            dao.upsertAll(listOf(ParentChildRelationEntity(body.studentId, body.studentName, "active", body.studentFirebaseUid)))
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val parentUid = auth.currentUser?.uid
            val studentUid = body.studentFirebaseUid
            
            if (parentUid != null && studentUid != null) {
                return try {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    
                    val linkData = hashMapOf(
                        "studentUid" to studentUid,
                        "parentUid" to parentUid,
                        "studentId" to studentUid, // Backward compatibility alias
                        "parentId" to parentUid, // Backward compatibility alias
                        "studentName" to body.studentName,
                        "linkedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    // ALIGNED: Use .add() as requested in Requirement 2
                    firestore.collection("parentChildLinks").add(linkData).await()
                    
                    MindGateLog.d("FIRESTORE", "Link registered in Firestore for student=$studentUid parent=$parentUid")
                    result // Return the original success result
                } catch (e: Exception) {
                    MindGateLog.e("FIRESTORE", "Linking write failed", e)
                    NetworkResult.Error("Failed to register link in Firestore. Please check your network or security rules.", 500)
                }
            } else {
                return NetworkResult.Error("Could not resolve student identity for Firestore link.", 400)
            }
        }
        return result
    }

    suspend fun syncChildren() = safeApiCall { 
        val result = api.listChildren()
        if (result.isSuccessful) {
            val remote = result.body()!!
            dao.clearAll()
            dao.upsertAll(remote.map { ParentChildRelationEntity(it.studentId, it.name, it.status, it.studentFirebaseUid) })
        }
        result
    }

    suspend fun getChildStatus(studentId: Long) = safeApiCall { api.getChildStatus(studentId) }

    suspend fun getChildUsage(studentId: Long) = safeApiCall { api.getChildUsage(studentId) }

    suspend fun revokeLink(studentId: Long): NetworkResult<Unit> {
        val result = safeApiCall { api.revokeLink(studentId) }
        if (result is NetworkResult.Success) {
            dao.delete(studentId)
        }
        return result
    }

    suspend fun checkConnectionStatus() = safeApiCall { api.getConnectionStatus() }

    suspend fun notifyParentOfRequest(appName: String) = safeApiCall {
        api.notifyParentOfRequest(mapOf("appName" to appName))
    }

    suspend fun syncFullUsage(usageMap: Map<String, Int>, appInfoHelper: com.mindgate.util.AppInfoHelper) = safeApiCall {
        val json = com.google.gson.JsonObject()
        json.addProperty("date", java.time.LocalDate.now().toString())
        val array = com.google.gson.JsonArray()
        usageMap.forEach { (pkg, minutes) ->
            val item = com.google.gson.JsonObject()
            item.addProperty("package", pkg)
            item.addProperty("label", appInfoHelper.getAppName(pkg))
            item.addProperty("seconds", minutes * 60)
            array.add(item)
        }
        json.add("usage", array)
        api.syncFullUsage(json)
    }
}
