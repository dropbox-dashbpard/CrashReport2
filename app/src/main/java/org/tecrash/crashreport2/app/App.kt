package org.tecrash.crashreport2.app

import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.os.Messenger
import org.tecrash.crashreport2.job.SendJobService

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

    override fun onCreate() {
        super.onCreate()

        jobComponentName = ComponentName(this, SendJobService::class.java)
        val startServiceIntent = Intent(this, SendJobService::class.java).putExtra("messenger", Messenger(handler))
        startService(startServiceIntent)
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    companion object {
        lateinit var jobService: SendJobService
        lateinit var jobComponentName: ComponentName
    }
}