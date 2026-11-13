package com.mindgate.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class ExtensionRequest(
    @DocumentId val id: String = "",
    
    // PRIMARY FIELDS - ALIGNED WITH FIRESTORE.RULES
    @get:PropertyName("studentUid") @set:PropertyName("studentUid") var studentUid: String = "",
    @get:PropertyName("parentUid") @set:PropertyName("parentUid") var parentUid: String = "",
    @get:PropertyName("studentName") @set:PropertyName("studentName") var studentName: String = "",
    @get:PropertyName("appName") @set:PropertyName("appName") var appName: String = "",
    @get:PropertyName("appPackage") @set:PropertyName("appPackage") var appPackage: String = "",
    @get:PropertyName("requestedMinutes") @set:PropertyName("requestedMinutes") var requestedMinutes: Int = 15,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "PENDING",
    
    @ServerTimestamp 
    @get:PropertyName("requestedAt") @set:PropertyName("requestedAt") var requestedAt: Timestamp? = null,
    
    @get:PropertyName("approvedMinutes") @set:PropertyName("approvedMinutes") var approvedMinutes: Int? = null,
    @get:PropertyName("respondedAt") @set:PropertyName("respondedAt") var respondedAt: Timestamp? = null,
    @get:PropertyName("consumedAt") @set:PropertyName("consumedAt") var consumedAt: Timestamp? = null,

    // COMPATIBILITY ALIASES (Writing both for transition safety)
    @get:PropertyName("studentId") @set:PropertyName("studentId") var studentId: String = "",
    @get:PropertyName("parentId") @set:PropertyName("parentId") var parentId: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Timestamp? = null
)
