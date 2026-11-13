package com.mindgate.parent

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ParentApprovalListenerService : Service() {

    private var listener: ListenerRegistration? = null
    private val CHANNEL_ID = "parent_watchdog_channel"
    private val PERSISTENT_NOTIF_ID = 1001
    private val REQUEST_NOTIF_ID_BASE = 2000

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(PERSISTENT_NOTIF_ID, buildPersistentNotification())
        attachListener()
        return START_STICKY
    }

    private fun attachListener() {
        // Use AuthStateListener to handle session re-alignment (anonymous -> aligned)
        val auth = FirebaseAuth.getInstance()
        val authListener = object : FirebaseAuth.AuthStateListener {
            override fun onAuthStateChanged(auth: FirebaseAuth) {
                val parentUid = auth.currentUser?.uid
                if (parentUid != null) {
                    startFirestoreQuery(parentUid)
                } else {
                    android.util.Log.e("ParentWatchdog", "ABORT: User not logged in")
                    listener?.remove()
                }
            }
        }
        auth.addAuthStateListener(authListener)
    }

    private fun startFirestoreQuery(parentUid: String) {
        android.util.Log.d("ParentWatchdog", "STARTING_LISTENER: parentUid='$parentUid'")
        
        listener?.remove()
        // ALIGNED: EXACT COLLECTION AND FIELDS PER RULES
        listener = FirebaseFirestore.getInstance()
            .collection("extension_requests")
            .whereEqualTo("parentUid", parentUid)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ParentWatchdog", "LISTENER_ERROR: ${error.message}")
                    return@addSnapshotListener
                }
                
                android.util.Log.i("ParentWatchdog", "LISTENER_EVENT: received ${snapshot?.documentChanges?.size ?: 0} changes")
                
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        android.util.Log.i("ParentWatchdog", "NEW_PENDING_DOC: docId=${change.document.id}")
                        showApprovalNotification(change.document)
                    }
                }
            }
    }

    private fun showApprovalNotification(doc: DocumentSnapshot) {
        val requestId = doc.id
        val studentName = doc.getString("studentName") ?: "Your child"
        val appPackage = doc.getString("appPackage") ?: "an app"

        val approveIntent = Intent(this, ApprovalReceiver::class.java).apply {
            putExtra("requestId", requestId)
            putExtra("decision", "approved")
        }
        val denyIntent = Intent(this, ApprovalReceiver::class.java).apply {
            putExtra("requestId", requestId)
            putExtra("decision", "denied")
        }
        
        val approvePending = PendingIntent.getBroadcast(
            this, requestId.hashCode(), approveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val denyPending = PendingIntent.getBroadcast(
            this, requestId.hashCode() + 1, denyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Extension request")
            .setContentText("$studentName wants more time on $appPackage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, "Approve", approvePending)
            .addAction(0, "Deny", denyPending)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(REQUEST_NOTIF_ID_BASE + requestId.hashCode(), notification)
    }

    private fun buildPersistentNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MindGate is watching for requests")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Parent Approval Watchdog",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
