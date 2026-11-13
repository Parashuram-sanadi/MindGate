package com.mindgate.parent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ParentApprovalListenerService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
