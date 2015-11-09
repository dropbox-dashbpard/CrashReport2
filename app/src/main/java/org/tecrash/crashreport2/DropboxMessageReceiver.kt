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
import kotlin.text.Regex

/**
* Created by xiaocong on 15/9/29.
*/
public class DropboxMessageReceiver : BroadcastReceiver() {

    @Inject lateinit var dbService: DropboxModelService
    @Inject lateinit var app: Application
    @Inject lateinit var config: ConfigService
    @Inject lateinit var jobScheduler: JobScheduler
    @Inject lateinit var jobComponentName: ComponentName
    @Inject lateinit var dropBoxManager: DropBoxManager

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

    /**
     * Get the process name of the dropbox entry.
     */
    private fun appName(item: DropBoxManager.Entry): String = when(item.tag) {
        "SYSTEM_RESTART", "system_server_lowmem", "system_server_watchdog", "system_server_wtf" -> {
            "system_server"
        }
        "BATTERY_DISCHARGE_INFO" -> "battery"
        "SYSTEM_BOOT", "SYSTEM_RECOVERY_LOG" -> "system"
        "APANIC_CONSOLE", "KERNEL_PANIC", "KERNEL_PANIC", "SYSTEM_AUDIT", "SYSTEM_LAST_KMSG" -> {
            "kernel"
        }
        "SYSTEM_TOMBSTONE" -> processName(item.getText(4 * 1024), SendJobService.tbRegex)
        "system_app_crash", "data_app_crash", "system_app_anr", "data_app_anr", "system_app_wtf" -> {
            processName(item.getText(2 * 1024), SendJobService.procRegex)
        }
        else -> processName(item.getText(2 * 1024), SendJobService.procRegex)
    }

    /**
     * Get the process name per the regex of the tag.
     */
    private fun processName(content: String, regex: Regex): String {
        content.lines().forEach { line ->
            val match = regex.find(line)
            match?.let {
                return match.groups.get(1)!!.value
            }
        }
        return "unknown"
    }

    /**
     * Save logcat to specified file.
     */
    private fun saveLogcat(fileName: String): String {
        try {
            val inputStream = Runtime.getRuntime().exec(arrayOf("/system/bin/logcat", "-d")).inputStream
            val outputStream = GZIPOutputStream(File(fileName).outputStream(), 8192)

            val buffer = ByteArray(1024 * 8)
            var bytesRead = inputStream.read(buffer)
            while (bytesRead > -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }
            Log.d("Log file $fileName saved.")

            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            return ""
        }

        return fileName
    }

    /**
     * Get the logFileName per the dropbox entry, may be empty if no need.
     */
    private fun logFileName(tag: String, appName: String, timestamp: Long): String {
        val tags = arrayOf("SYSTEM_RESTART", "SYSTEM_TOMBSTONE", "system_server_watchdog", "system_app_anr", "system_app_crash")

        return if (tag in tags && app.filesDir.list().size < 20)
            saveLogcat("${app.filesDir.absolutePath}/db.$timestamp.log.gz")
        else
            ""
    }

    private fun dropboxObserver(tag: String, timestamp: Long) = observable<Array<String>> { subscriber ->
        thread {
            Log.v(">>> Emit dropbox item, tag = $tag, at ${Date(timestamp)} >>>")
            val entry = dropBoxManager.getNextEntry(tag, timestamp - 1)
            val appName = appName(entry)

            entry.toSingletonObservable().filter {
                //TODO filter all tag/app we are interested in.
                val tags = arrayOf("SYSTEM_RESTART",
                        "SYSTEM_TOMBSTONE",
                        "system_server_watchdog",
                        "system_app_crash",
                        "system_app_anr",
                        "data_app_crash",
                        "data_app_anr")
                if (it.flags and DropBoxManager.IS_TEXT == 0)
                    false
                else if (it.tag in tags)
                    true // we should report
                else if (config.development)
                    it.tag == "system_app_strictmode"  // we should report strictmode in case of development
                else
                    false  // we should not report
            }.map {
                arrayOf(tag, appName, timestamp.toString(), logFileName(tag, appName, timestamp))
            }.subscribe(subscriber)
        }
    }

    private fun receiveDropboxAdded(tag: String, timestamp: Long) = dropboxObserver(tag, timestamp)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.newThread())
        //.delay(5L, TimeUnit.SECONDS)
        .flatMap {
            dbService.createOrIncOccursAsync(it.get(0), it.get(1), it.get(2).toLong(), it.get(3))
        }.subscribe { item ->
            Log.d("DB entry saved: ${item.toString()}")
            val job = JobInfo.Builder(SendJobService.JOB_SENDING_DROPBOX_ENTRY, jobComponentName)
                .setMinimumLatency(if(config.development) 1000L else 60*1000L)
                .setOverrideDeadline(if(config.development) 10*1000L else 60*60*1000L)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
            jobScheduler.schedule(job)
        }
}
