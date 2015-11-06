package org.tecrash.crashreport2.job

import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Intent
import android.os.DropBoxManager
import android.os.Message
import android.os.Messenger
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.MultipartBuilder
import com.squareup.okhttp.RequestBody
import org.tecrash.crashreport2.api.DropboxApiServiceFactory
import org.tecrash.crashreport2.api.data.ReportData
import org.tecrash.crashreport2.api.data.ReportDataEntry
import org.tecrash.crashreport2.app.App
import org.tecrash.crashreport2.db.DropboxModel
import org.tecrash.crashreport2.db.DropboxModelService
import org.tecrash.crashreport2.util.ConfigService
import org.tecrash.crashreport2.util.Log
import rx.lang.kotlin.observable
import rx.lang.kotlin.toObservable
import rx.schedulers.Schedulers
import java.io.File
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
    @Inject lateinit var dropboxApiServiceFactory: DropboxApiServiceFactory
    @Inject lateinit var configService: ConfigService
    @Inject lateinit var jobScheduler: JobScheduler

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
        when (params?.jobId) {
            JOB_SENDING_DROPBOX_ENTRY -> doReport(params!!)
            JOB_SENDING_DROPBOX_CONTENT -> doUpload(params!!)
        }
        return true
    }

    private fun doUpload(params: JobParameters) {
        Log.d("Uploading...")
        // upload content & log
        dropboxDbService.list(true).map {
            it to dropBoxManager.getNextEntry(it.tag, it.timestamp - 1)
        }.filter {
            it.second?.timeMillis == it.first.timestamp
        }.toList().subscribeOn(Schedulers.newThread()).subscribe { list ->
            list.forEach {
                try {
                    if (it.first.contentUploadStatus == DropboxModel.SHOULD_BUT_NOT_UPLOADED) {
                        Log.d("Uploading dropbox entry content.")
                        dropboxApiServiceFactory.create(true, true).uploadContent(
                                auth="Bearer ${configService.key}",
                                dbId=it.first.serverId,
                                data= RequestBody.create(MediaType.parse("text/plain"), it.second.getText(1024*64))
                        ).execute()
                    }
                    if (it.first.logUploadStatus == DropboxModel.SHOULD_BUT_NOT_UPLOADED) {
                        Log.d("Uploading logs.")
                        val logFile = File(it.first.log)
                        val fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), logFile)
                        val multipartBuilder = MultipartBuilder("95416089-b2fd-4eab-9a14-166bb9c5788b")
                        multipartBuilder.addFormDataPart("attachment", logFile.name, fileBody)
                        dropboxApiServiceFactory.create(false, true).uploadFile(
                                auth="Bearer ${configService.key}",
                                dbId=it.first.serverId,
                                attachment=multipartBuilder.build()
                        ).execute()
                    }
                } catch (e: Exception) {
                    Log.e(e.toString())
                } finally {
                    dropboxDbService.delete(it.first.id)
                }
            }
            jobFinished(params, false)
        }
    }

    private fun doReport(params: JobParameters) {
        var reportData: ReportData? = null
        reportData().subscribeOn(Schedulers.newThread()).flatMap { data ->
            Log.d("Reporting crashes data...")
            reportData = data
            dropboxApiServiceFactory.create(true, true).report(auth="Bearer ${configService.key}", ua=configService.ua, data=data)
        }.flatMap { result ->
            result.data.zip(reportData!!.data).toObservable()
        }.filter {
            if (it.first == null) {
                // if the server rejects the item, it returns null
                dropboxDbService.deleteAsync(it.second.id)
                false
            } else
                true
        }.toList().subscribe { list ->
            Log.d("Report result: ${list}")
            list.forEach { zipped ->
                dropboxDbService.get(zipped.second.id).forEach { item ->
                    item.serverId = zipped.first!!.dropbox_id
                    item.contentUploadStatus = DropboxModel.SHOULD_BUT_NOT_UPLOADED
                    if (item.log.isNotEmpty() && File(item.log).exists())
                        item.logUploadStatus = DropboxModel.SHOULD_BUT_NOT_UPLOADED
                    item.save()
                }
            }
            jobFinished(params, false)
            // create a new job to upload content & log
            uploadContentJob()
        }
    }

    private fun uploadContentJob() {
        val job = JobInfo.Builder(SendJobService.JOB_SENDING_DROPBOX_CONTENT, jobComponentName)
                .setPersisted(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setMinimumLatency(1000*2)
                .setOverrideDeadline(1000*5)
//                .setRequiresCharging(true)
                .build()
        jobScheduler.schedule(job)
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
            "SYSTEM_BOOT", "SYSTEM_RECOVERY_LOG" -> "system"
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
        final val JOB_SENDING_DROPBOX_CONTENT = 2
        final val JOB_RETRIEVING_CONFIG = 101

        final val MSG_SERVICE_OBJ = 2
        final val procRegex = Regex("Process:\\s+([\\w\\-\\./:$#\\(\\)]+)")
        final val tbRegex = Regex("pid:\\s*\\d+,\\s*tid:\\s*\\d+,\\s*name:.+?>>>\\s+([\\w\\-\\./:$#\\(\\)]+)\\s+<<<")
    }
}
