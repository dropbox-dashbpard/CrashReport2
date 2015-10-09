package org.tecrash.crashreport2.job

import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.Intent
import android.os.DropBoxManager
import android.os.Message
import android.os.Messenger
import org.tecrash.crashreport2.api.DropboxApiService
import org.tecrash.crashreport2.api.data.ReportData
import org.tecrash.crashreport2.api.data.ReportDataEntry
import org.tecrash.crashreport2.app.App
import org.tecrash.crashreport2.db.DropboxModel
import org.tecrash.crashreport2.db.DropboxModelService
import org.tecrash.crashreport2.util.ConfigService
import org.tecrash.crashreport2.util.Log
import rx.lang.kotlin.observable
import rx.schedulers.Schedulers
import javax.inject.Inject
import kotlin.concurrent.thread
import kotlin.text.Regex

/**
 * Sending dropbox job
 * Created by xiaocong on 15/10/3.
 */
class SendJobService(): JobService() {

    @Inject lateinit var jobComponentName: ComponentName
    @Inject lateinit var dropBoxManager: DropBoxManager
    @Inject lateinit var dropboxDbService: DropboxModelService
    @Inject lateinit var dropboxApiService: DropboxApiService
    @Inject lateinit var configService: ConfigService

    override fun onCreate() {
        super.onCreate()
        (application as App).component.inject(this)

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
            reportData().subscribeOn(Schedulers.newThread()).subscribe { data ->
                Log.d(data.toString())
                try {
                    val result = dropboxApiService.report(auth="Bearer ${configService.key}", ua=configService.ua, data=data).execute()
                    if (result.isSuccess) {
                        result.body().data.zip(data.data).forEach { zipped ->
                            if (zipped.first != null && (zipped.first.uploadContent || zipped.first.uploadLog)) {
                                Log.d(zipped.first.toString())
                                dropboxDbService.get(zipped.second.id).forEach { item ->
                                    item.serverId = zipped.first.dropbox_id
                                    if (zipped.first.uploadContent)
                                        item.contentUploadStatus = DropboxModel.SHOULD_BUT_NOT_UPLOADED
                                    if (zipped.first.uploadLog)
                                        item.logUploadStatus = DropboxModel.SHOULD_BUT_NOT_UPLOADED
                                    item.save()
                                    // TODO upload content and log
                                }
                            } else {
                                dropboxDbService.delete(zipped.second.id)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(e.getMessage() ?: "Error during sending dropbox data.")
                }
                jobFinished(params, false)
            }
        }
        return true
    }

    private fun reportData() = observable<ReportData> { subscriber ->
        thread {
            dropBoxItems().reduce(arrayOf<ReportDataEntry>()) { list, entry ->
                // remove duplicated (tag, app) in one reporting session, to avoid a burst number of
                // crashes data in a short time.
                val dupEntry = list.filter {
                    entry.app == it.app && entry.tag == it.tag
                }

                if (dupEntry.size() > 0) {
                    dupEntry.get(0).count += entry.count
                    dropboxDbService.delete(entry.id)

                    list
                } else {
                    arrayOf(*list, entry)
                }
            }.map {
                ReportData(System.currentTimeMillis(), it)
            }.subscribe(subscriber)
        }
    }

    private fun dropBoxItems() = dropboxDbService.list(false).map {
        it to dropBoxManager.getNextEntry(it.tag, it.timestamp - 1)
    }.filter {
        if ((it.second.flags and DropBoxManager.IS_TEXT) > 0 && it.first.timestamp == it.second.timeMillis)
            true
        else {
            dropboxDbService.deleteAsync(it.first.id)
            false
        }
    }.map {
        ReportDataEntry(it.first.id, it.second.tag, appName(it.second), it.second.timeMillis)
    }

    private fun appName(item: DropBoxManager.Entry): String = when(item.tag) {
            "SYSTEM_RESTART", "system_server_lowmem", "system_server_watchdog", "system_server_wtf" -> {
                "system_server"
            }
            "BATTERY_DISCHARGE_INFO" -> "battery"
            "SYSTEM_BOOT", "SYSTEM_RECOVERY_LOG" -> {
                "system"
            }
            "APANIC_CONSOLE", "KERNEL_PANIC", "KERNEL_PANIC", "SYSTEM_AUDIT", "SYSTEM_LAST_KMSG" -> {
                "kernel"
            }
            "SYSTEM_TOMBSTONE" -> processName(item.getText(4 * 1024), tbRegex)
            "system_app_crash", "data_app_crash", "system_app_anr", "data_app_anr", "system_app_wtf" -> {
                processName(item.getText(2 * 1024), procRegex)
            }
            else -> processName(item.getText(2 * 1024), procRegex)
        }

    private fun processName(content: String, regex: Regex): String {
        content.lines().forEach { line ->
            val match = regex.match(line)
            match?.let {
                return match.groups.get(1)!!.value
            }
        }
        return "Unknown"
    }

    companion object {
        final val JOB_SENDING_DROPBOX_ENTRY = 1
        final val JOB_RETRIEVING_CONFIG = 101

        final val MSG_SERVICE_OBJ = 2
        final val procRegex = Regex("Process:\\s+([\\w\\-\\./:$#\\(\\)]+)")
        final val tbRegex = Regex("pid:\\s*\\d+,\\s*tid:\\s*\\d+,\\s*name:.+?>>>\\s+([\\w\\-\\./:$#\\(\\)]+)\\s+<<<")
    }
}
