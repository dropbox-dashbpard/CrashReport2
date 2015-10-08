package org.tecrash.crashreport2.job

import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.os.Message
import android.os.Messenger
import org.tecrash.crashreport2.util.Log

/**
 * Sending dropbox job
 * Created by xiaocong on 15/10/3.
 */
class SendJobService(): JobService() {
    override fun onCreate() {
        super.onCreate()
        Log.d("Job service created!")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Job service destroyed!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callback = intent?.getParcelableExtra<Messenger>("messenger")
        callback?.let {
            val m = Message.obtain()
            m.what = MSG_SERVICE_OBJ
            m.obj = this
            callback.send(m)
        }

        return Service.START_NOT_STICKY
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d("Job stopped!")
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("Job started!")
        if (params?.jobId == JOB_SENDING_DROPBOX_ENTRY) {
            //TODO upload dropbox entry to server
        }
        jobFinished(params, false)
        return false
    }

    companion object {
        final val JOB_SENDING_DROPBOX_ENTRY = 1
        final val JOB_RETRIEVING_CONFIG = 101

        final val MSG_SERVICE_OBJ = 2
    }
}
