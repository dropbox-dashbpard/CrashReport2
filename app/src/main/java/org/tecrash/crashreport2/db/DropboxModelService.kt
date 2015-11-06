package org.tecrash.crashreport2.db

import android.os.SystemClock
import com.raizlabs.android.dbflow.sql.builder.Condition
import com.raizlabs.android.dbflow.sql.language.Select
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

    public fun create(tag: String, timestamp: Long, log: String = "") =
        DropboxModel(tag, timestamp, log, SystemClock.elapsedRealtime())
        .toSingletonObservable().map {
            it.save()
            it
        }

    public fun createAsync(tag: String, timestamp: Long, log: String = "") = observable<DropboxModel> { subscriber->
        thread {
            create(tag, timestamp, log).subscribe(subscriber)
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
        Select().from(DropboxModel::class.java)
        .where(Condition.column(DropboxModel_Table.ID).eq(id))
        .querySingle()
        .toSingletonObservable().forEach { item ->
            val file = File(item.log)
            if (file.exists())
                file.deleteRecursively()
            item.delete()
        }
    }

    public fun deleteAsync(id: Long) = thread {
        delete(id)
    }
}