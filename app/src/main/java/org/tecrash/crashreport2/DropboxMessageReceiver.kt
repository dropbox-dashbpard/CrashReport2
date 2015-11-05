package org.tecrash.crashreport2

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.DropBoxManager
import org.tecrash.crashreport2.app.App
import org.tecrash.crashreport2.db.DropboxModelService
import org.tecrash.crashreport2.job.SendJobService
import org.tecrash.crashreport2.util.ConfigService
import org.tecrash.crashreport2.util.Log
import rx.android.schedulers.AndroidSchedulers
import rx.lang.kotlin.observable
import rx.lang.kotlin.toSingletonObservable
import rx.schedulers.Schedulers
import java.io.File
import java.util.*
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import kotlin.concurrent.thread

/**
* Created by xiaocong on 15/9/29.
*/
public class DropboxMessageReceiver : BroadcastReceiver() {

    @Inject lateinit var dbService: DropboxModelService
    @Inject lateinit var app: Application
    @Inject lateinit var config: ConfigService
    @Inject lateinit var jobScheduler: JobScheduler
    @Inject lateinit var jobComponentName: ComponentName

    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as App).component.inject(this)

        if (!config.enabled()) {
            Log.i("Report service is disabled!")
            return
        }

        when(intent.action) {
            DropBoxManager.ACTION_DROPBOX_ENTRY_ADDED -> {
                val timestamp = intent.extras.getLong(DropBoxManager.EXTRA_TIME, -1L)
                val tag = intent.extras.getString(DropBoxManager.EXTRA_TAG, null)
                if (timestamp != -1L && tag != null)
                    receiveDropboxAdded(tag, timestamp)
            }
        }
    }

    private fun saveLogcat(fileName: String): String {
        val inputStream = Runtime.getRuntime().exec(arrayOf("/system/bin/logcat", "-d")).inputStream
        val outputStream = GZIPOutputStream(File(fileName).outputStream(), 8192)

        val buffer = ByteArray(1024*8)
        var bytesRead = inputStream.read(buffer)
        while (bytesRead > -1) {
            outputStream.write(buffer, 0, bytesRead)
            bytesRead = inputStream.read(buffer)
        }

        inputStream.close()
        outputStream.close()

        return fileName
    }

    private fun dropboxObserver(tag: String, timestamp: Long) = observable<Array<String>> { subscriber ->
        thread {
            Log.v(">>> Emit dropbox item, tag = $tag, at ${Date(timestamp)} >>>")
            Pair(tag, timestamp).toSingletonObservable()
            .filter {
                arrayOf("SYSTEM_RESTART",
                        "SYSTEM_TOMBSTONE",
                        "system_server_watchdog",
                        "system_app_crash",
                        "system_app_anr",
                        "data_app_crash",
                        "data_app_anr",
                        "system_app_strictmode"
                ).contains(it.first)
            }.filter {
                if (config.development)
                    true
                else
                    it.first !in arrayOf("system_app_strictmode")
            }.map {
                val (tag, timestamp) = it
                val fileName = if (arrayOf("SYSTEM_RESTART",
                        "SYSTEM_TOMBSTONE",
                        "system_server_watchdog",
                        "system_app_anr").contains(tag)) {
                    val fileName = "${app.filesDir}/db.$timestamp.log.gz"
                    Log.d("Log file $fileName saved for dropbox entry $tag")

                    // save logcat
                    saveLogcat(fileName)
                } else {
                    ""
                }

                // return map result
                arrayOf(tag, timestamp.toString(), fileName)
            }.subscribe(subscriber)
        }
    }

    private fun receiveDropboxAdded(tag: String, timestamp: Long) = dropboxObserver(tag, timestamp)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.newThread())
        //.delay(5L, TimeUnit.SECONDS)
        .flatMap {
            dbService.createAsync(it.get(0), it.get(1).toLong(), it.get(2))
        }.subscribe { item ->
            Log.d("DB entry saved: ${item.toString()}")
            val job = JobInfo.Builder(SendJobService.JOB_SENDING_DROPBOX_ENTRY, jobComponentName)
                .setMinimumLatency(if(config.development) 1000L else 60*1000L)
                .setOverrideDeadline(if(config.development) 10*1000L else 10*60*1000L)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .build()
            jobScheduler.schedule(job)
        }
}
