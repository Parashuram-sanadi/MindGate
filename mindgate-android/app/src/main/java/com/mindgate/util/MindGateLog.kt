package com.mindgate.util

import android.util.Log
import com.mindgate.BuildConfig

object MindGateLog {
    private const val TAG = "MINDGATE"

    fun d(subtag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d("${TAG}_$subtag", message)
    }

    fun e(subtag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) Log.e("${TAG}_$subtag", message, throwable)
            else Log.e("${TAG}_$subtag", message)
        }
    }
}
