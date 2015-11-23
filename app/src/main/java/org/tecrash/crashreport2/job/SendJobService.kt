package org.tecrash.crashreport2.job

import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Intent
import android.os.Build
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
import rx.lang.kotlin.toObservable
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.io.File
import javax.inject.Inject
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

    lateinit var compositeSubscription: CompositeSubscription

    override fun onCreate() {
        super.onCreate()
        (application as App).component.inject(this)
        compositeSubscription = CompositeSubscription()
        Log.d("Job service created!")
    }

    override fun onDestroy() {
        compositeSubscription.unsubscribe()
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
        when (params?.jobId) {
            JOB_SENDING_DROPBOX_ENTRY -> doReport(params!!)
            JOB_SENDING_DROPBOX_CONTENT -> doUpload(params!!)
        }
        return true
    }

    private fun doUpload(params: JobParameters) {
        Log.d("Uploading...")
        // upload content & log
        val sub = dropboxDbService.list(true).map {
            it to dropBoxManager.getNextEntry(it.tag, it.timestamp - 1)
        }.filter {
            it.second?.timeMillis == it.first.timestamp
        }.toList().finallyDo {
            Log.d("Uploading job finished!")
            jobFinished(params, false)
        }.subscribeOn(Schedulers.newThread()).subscribe { list ->
            list.forEach {
                try {
                    if (it.first.contentUploadStatus == DropboxModel.SHOULD_BUT_NOT_UPLOADED) {
                        Log.d("Uploading dropbox entry content.")
                        dropboxApiServiceFactory.create(true, true).uploadContent(
                                auth="Bearer ${configService.key}",
                                dbId=it.first.serverId,
                                data= RequestBody.create(MediaType.parse("text/plain"), it.second.getText(1024*128))
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
        }

        compositeSubscription.add(sub)
    }

    private fun doReport(params: JobParameters) {
        var reportData: ReportData? = null
        val sub = reportData().flatMap { data ->
            Log.d("Reporting crashes data...")
            reportData = data
            dropboxApiServiceFactory.create(true, true).report(
                    auth="Bearer ${configService.key}",
                    ua=configService.ua,
                    data=data)
        }.flatMap { result ->
            result.data.zip(reportData!!.data).toObservable()
        }.filter {
            if (it.first == null) {
                // if the server rejects the item, it returns null
                dropboxDbService.delete(it.second.id)
                false
            } else
                true
        }.toList().finallyDo {
            Log.d("Reporting job finished!")
            // notify jobService that it's done.
            jobFinished(params, false)
        }.subscribeOn(Schedulers.newThread()).subscribe({ list ->
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

            // create a new job to upload content & log
            Log.d("Create a new job to upload log/content.")
            uploadContentJob()
        }, { error ->
            Log.e(error.toString())
        })

        compositeSubscription.add(sub)
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

    private fun reportData() = dropBoxItems().toList().map {
        ReportData(System.currentTimeMillis(), it.toTypedArray())
    }

        private fun dropBoxItems() = dropboxDbService.list(false).filter {
            if (it.incremental == Build.VERSION.INCREMENTAL)
                true
            else {
                dropboxDbService.delete(it.id)
                false
            }
        }.map {
            ReportDataEntry(it.id, it.tag, it.app, it.timestamp)
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
