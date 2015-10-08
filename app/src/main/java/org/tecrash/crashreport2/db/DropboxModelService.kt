package org.tecrash.crashreport2.db

import android.os.SystemClock
import com.raizlabs.android.dbflow.sql.builder.Condition
import com.raizlabs.android.dbflow.sql.language.Delete
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

    public fun create(tag: String, timestamp: Long, log: String = "") = observable<DropboxModel> { subscriber->
        DropboxModel(tag, timestamp, log, SystemClock.elapsedRealtime()).toSingletonObservable()
        .map {
            it.save()
            it
        }.subscribe(subscriber)
    }

    public fun createAsync(tag: String, timestamp: Long, log: String = "") = observable<DropboxModel> { subscriber->
        thread {
            create(tag, timestamp, log).subscribe(subscriber)
        }
    }

    public fun list(includeReported: Boolean = false, order: String = DropboxModel_Table.TIMESTAMP) = observable<DropboxModel> { subscriber->
        val select = Select().from(DropboxModel::class.java)
        val where = if (includeReported)
            select.where()
        else
            select.where(Condition.column(DropboxModel_Table.SERVERID).isNull())

        where.orderBy(true, order).queryList().toObservable().subscribe(subscriber)
    }

    public fun listAsync(includeReported: Boolean = false, order: String = DropboxModel_Table.TIMESTAMP) = observable<DropboxModel> { subscriber->
        thread {
            list(includeReported, order).subscribe(subscriber)
        }
    }

    public fun delete(id: Long) {
        val item = Select().from(DropboxModel::class.java).where(Condition.column(DropboxModel_Table.ID).eq(id)).querySingle()
        item.log?.let {
            val file = File(item.log)
            if (file.exists())
                file.deleteRecursively()
        }
        Delete().from(DropboxModel::class.java).where(Condition.column(DropboxModel_Table.ID).eq(id)).queryClose()
    }

    public fun deleteAsync(id: Long) = thread {
        delete(id)
    }
}