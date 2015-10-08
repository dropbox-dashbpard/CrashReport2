package org.tecrash.crashreport2.util

import org.tecrash.crashreport2.debugBuild

/**
 * Log
 * Created by xiaocong on 15/10/6.
 */
object Log {
    const private val TAG = "CrashReport"

    fun v(msg: String) {
        if (debugBuild) android.util.Log.v(TAG, msg)
    }
    fun d(msg: String) {
        if (debugBuild) android.util.Log.d(TAG, msg)
    }
    fun i(msg: String) = android.util.Log.i(TAG, msg)
    fun w(msg: String) = android.util.Log.w(TAG, msg)
    fun e(msg: String) = android.util.Log.e(TAG, msg)
    fun wtf(msg: String) = android.util.Log.wtf(TAG, msg)
}
