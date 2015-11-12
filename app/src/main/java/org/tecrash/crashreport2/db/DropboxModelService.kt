package org.tecrash.crashreport2.db

import android.os.SystemClock
import com.raizlabs.android.dbflow.sql.builder.Condition
import com.raizlabs.android.dbflow.sql.language.Select
import rx.Observable
import rx.lang.kotlin.observable
import rx.lang.kotlin.toObservable
import rx.lang.kotlin.toSingletonObservable
import java.io.File
import kotlin.concurrent.thread

/**
 * DropboxRepository
 * Created by xiaocong on 15/10/3.
 */

public class DropboxModelService() {

    public fun createOrIncOccurs(tag: String, appName: String, incremental: String, timestamp: Long, log: String = ""): Observable<DropboxModel> {
        var item = Select().from(DropboxModel::class.java)
                .where(Condition.column(DropboxModel_Table.TAG).eq(tag))
                .and(Condition.column(DropboxModel_Table.APP).eq(appName))
                .and(Condition.column(DropboxModel_Table.INCREMENTAL).eq(incremental))
                .and(Condition.column(DropboxModel_Table.SERVERID).isNull).querySingle()
        //TODO transaction
        try {
            if (item == null) {
                item = DropboxModel(tag, appName, incremental, timestamp, log, SystemClock.elapsedRealtime())
            } else {
                item.occurs = item.occurs + 1
                removeFile(log)
            }
            item.save()
        } catch (e: Exception) {
            removeFile(log)
            return Observable.empty()
        }

        return item.toSingletonObservable()
    }

    private fun removeFile(log: String) {
        val file = File(log)
        if (file.exists())
            file.delete()
    }

    public fun createOrIncOccursAsync(tag: String, appName: String, incremental: String, timestamp: Long, log: String = "") = observable<DropboxModel> { subscriber->
        thread {
            createOrIncOccurs(tag, appName, incremental, timestamp, log).subscribe(subscriber)
        }
    }

    public fun get(id: Long) = Select().from(DropboxModel::class.java)
        .where(Condition.column(DropboxModel_Table.ID).eq(id))
        .querySingle().toSingletonObservable()

    public fun getAsync(id: Long) = observable<DropboxModel> { subscriber ->
        thread { get(id).subscribe(subscriber) }
    }


    public fun list(reported: Boolean = false, order: String = DropboxModel_Table.ID): rx.Observable<DropboxModel> {
        val select = Select().from(DropboxModel::class.java)
        val where = if (reported)
            select.where(Condition.column(DropboxModel_Table.SERVERID).isNotNull())
        else
            select.where(Condition.column(DropboxModel_Table.SERVERID).isNull())

        return where.orderBy(true, order).queryList().toObservable()
    }

    public fun listAsync(includeReported: Boolean = false, order: String = DropboxModel_Table.TIMESTAMP) = observable<DropboxModel> { subscriber->
        thread {
            list(includeReported, order).subscribe(subscriber)
        }
    }

    public fun delete(id: Long) {
        val item = Select().from(DropboxModel::class.java)
            .where(Condition.column(DropboxModel_Table.ID).eq(id))
            .querySingle()
        removeFile(item.log)
        item.delete()
    }

    public fun deleteAsync(id: Long) = thread {
        delete(id)
    }
}