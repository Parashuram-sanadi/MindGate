package com.mindgate.parent

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ApprovalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getStringExtra("requestId") ?: return
        val decision = intent.getStringExtra("decision") ?: return // "approved" or "denied"
        val normalizedDecision = decision.uppercase()

        val requestRef = FirebaseFirestore.getInstance()
            .collection("extension_requests").document(requestId)

        requestRef.update(
            mapOf(
                "status" to normalizedDecision,
                "respondedAt" to FieldValue.serverTimestamp()
            )
        ).addOnSuccessListener {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(2000 + requestId.hashCode())

            if (normalizedDecision == "APPROVED") {
                requestRef.get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val sUid = doc.getString("studentUid") ?: doc.getString("studentId") ?: ""
                        val pUid = doc.getString("parentUid") ?: doc.getString("parentId") ?: ""

                        FirebaseFirestore.getInstance().collection("extensionLedger").add(
                            mapOf(
                                "requestId" to requestId,
                                "studentUid" to sUid,
                                "parentUid" to pUid,
                                "studentId" to sUid,
                                "parentId" to pUid,
                                "appPackage" to (doc.getString("appPackage") ?: ""),
                                "grantedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }
        }
    }
}
