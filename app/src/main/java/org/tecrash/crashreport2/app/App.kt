package org.tecrash.crashreport2.app

import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.Messenger
import org.tecrash.crashreport2.job.SendJobService
import org.tecrash.crashreport2.util.Log

/**
 * Application
 * Created by xiaocong on 15/10/2.
 */
class App : BaseApplication() {

    val handler = object: Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                SendJobService.MSG_SERVICE_OBJ -> {
                    jobService = msg.obj as SendJobService
                }
            }
        }
    }

    val handlerThread = HandlerThread("handlerThread")

    override fun onCreate() {
        super.onCreate()

        Log.d("Starting handler thread, thread ID: ${Thread.currentThread().id}")
        handlerThread.start()

        jobComponentName = ComponentName(this, SendJobService::class.java)
        val startServiceIntent = Intent(this, SendJobService::class.java).putExtra("messenger", Messenger(handler))
        startService(startServiceIntent)
    }

    override fun onTerminate() {
        Log.d("Quit handler thread, thread ID: ${Thread.currentThread().id}")
        handlerThread.quitSafely()
        super.onTerminate()
    }

    companion object {
        lateinit var jobService: SendJobService
        lateinit var jobComponentName: ComponentName
    }
}