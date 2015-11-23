package org.tecrash.crashreport2

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.os.Build
import android.os.DropBoxManager
import android.os.Handler
import android.os.SystemClock
import org.tecrash.crashreport2.app.App
import org.tecrash.crashreport2.db.DropboxModelService
import org.tecrash.crashreport2.job.SendJobService
import org.tecrash.crashreport2.util.ConfigService
import org.tecrash.crashreport2.util.Log
import rx.android.schedulers.HandlerScheduler
import rx.lang.kotlin.observable
import rx.subscriptions.CompositeSubscription
import java.io.File
import java.util.*
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
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
    @Inject lateinit var sharedPreferences: SharedPreferences

    private val userBuild: Boolean by lazy {
        Build.TYPE == "user"
    }

    val tags = arrayOf("SYSTEM_RESTART",
            "SYSTEM_TOMBSTONE",
            "system_server_watchdog",
            "system_app_crash",
            "system_app_anr",
            "data_app_crash",
            "data_app_anr")

    lateinit var handler: Handler
    lateinit var compositeSubscription: CompositeSubscription

    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as App).component.inject(this)

        if (!config.enabled()) {
            Log.i("Report service is disabled!")
            return
        }

        handler = Handler((app as App).handlerThread.looper)
        compositeSubscription = CompositeSubscription()

        when(intent.action) {
            DropBoxManager.ACTION_DROPBOX_ENTRY_ADDED -> {
                val timestamp = intent.extras.getLong(DropBoxManager.EXTRA_TIME, -1L)
                val tag = intent.extras.getString(DropBoxManager.EXTRA_TAG, null)
                if (timestamp != -1L && tag != null)
                    addDropboxDatabase(dropboxObserver(tag, timestamp))
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                addDropboxDatabase(dropboxObserver(System.currentTimeMillis()))
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
                return match.groups[1]!!.value
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
     * Get the last reported timestamp.
     */
    private fun getLastTimestamp(tag: String) = sharedPreferences.getLong("ts_$tag", System.currentTimeMillis() - SystemClock.elapsedRealtime())

    /**
     * Set the last reported timestamp.
     */
    private fun setLastTimestamp(tag: String, timestamp: Long) = sharedPreferences.edit().putLong("ts_$tag", timestamp).commit()

    /**
     * Get the logFileName per the dropbox entry, may be empty if no need.
     */
    private fun logFileName(tag: String, appName: String, timestamp: Long): String {
        val tags = if (userBuild)
            arrayOf("SYSTEM_RESTART", "system_server_watchdog")
        else
            arrayOf("SYSTEM_RESTART", "system_server_watchdog", "system_app_crash", "system_app_anr", "SYSTEM_TOMBSTONE")

        return if (tag in tags && app.filesDir.list().size < 100)
            saveLogcat("${app.filesDir.absolutePath}/db.$timestamp.log.gz")
        else
            ""
    }

    /**
     * Create observable for all deopbox entry before current timestamp.
     * **Befoe BOOT_COMPLETED, we won't received any ACTION_DROPBOX_ENTRY_ADDED broadcast intent.**
     */
    private fun dropboxObserver(timestamp: Long) = observable<DropBoxManager.Entry> { sub ->
        Log.d("Emit all DropBoxManager Entries, thread ID: ${Thread.currentThread().id}")
        tags.forEach { tag ->
            var last = getLastTimestamp(tag)
            var entry = dropBoxManager.getNextEntry(tag, last)
            while (entry != null && entry.timeMillis < timestamp) {
                Log.v(">>>>>> Emit dropbox item, tag = $tag, at ${Date(entry.timeMillis)} >>>>>>")
                sub.onNext(entry)

                last = entry.timeMillis
                entry = dropBoxManager.getNextEntry(tag, last)
            }
            setLastTimestamp(tag, timestamp)
        }
        sub.onCompleted()
    }.map {
        val appName = appName(it)
        val logFile = if (appName == "system_server")  // we only save log for system_server
            logFileName(it.tag, appName, it.timeMillis)
        else
            ""
        arrayOf(it.tag, appName, it.timeMillis.toString(), logFile)
    }

    /**
     * Create single observable per specified tag/timestamp.
     */
    private fun dropboxObserver(tag: String, timestamp: Long) = observable<Array<String>> { subscriber ->
        Log.d("Emit one DropBoxManager Entry, thread ID: ${Thread.currentThread().id}")
        Log.v(">>> Emit dropbox item, tag = $tag, at ${Date(timestamp)} >>>")
        setLastTimestamp(tag, timestamp)
        val entry = dropBoxManager.getNextEntry(tag, timestamp - 1)
        if (entry != null) {
            val appName = appName(entry)
            if ((entry.flags and DropBoxManager.IS_TEXT != 0 && entry.tag in tags) ||
                    (config.development && entry.tag == "system_app_strictmode"))
                subscriber.onNext(arrayOf(tag, appName, timestamp.toString(), logFileName(tag, appName, timestamp)))
        }
        subscriber.onCompleted()
    }

    private fun addDropboxDatabase(obv: rx.Observable<Array<String>>) {
        val sub = obv.flatMap {
            Log.d("Create database entry, thread ID: ${Thread.currentThread().id}")
            dbService.createOrIncOccurs(it[0], it[1], Build.VERSION.INCREMENTAL, it[2].toLong(), it[3])
        }.toList().subscribeOn(HandlerScheduler.from(handler)).subscribe {
            Log.d("Schedule sending job, thread ID: ${Thread.currentThread().id}")
            val job = JobInfo.Builder(SendJobService.JOB_SENDING_DROPBOX_ENTRY, jobComponentName)
                    .setMinimumLatency(if(config.development) 1000L else 60*1000L)
                    .setOverrideDeadline(if(config.development) 10*1000L else 60*60*1000L)
                    .setPersisted(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build()
            jobScheduler.schedule(job)
            compositeSubscription.unsubscribe()
        }
        compositeSubscription.add(sub)
    }
}
