package org.tecrash.crashreport2.db

import com.raizlabs.android.dbflow.sql.builder.Condition
import com.raizlabs.android.dbflow.sql.language.Select
import rx.lang.kotlin.observable
import rx.lang.kotlin.toObservable
import rx.lang.kotlin.toSingletonObservable
import kotlin.concurrent.thread

/**
 * DropboxRepository
 * Created by xiaocong on 15/10/3.
 */

public class DropboxModelService() {

    public fun create(tag: String, timestamp: Long, log: String = "") = observable<DropboxModel> { subscriber->
        thread {
            DropboxModel(tag, timestamp, log).toSingletonObservable()
            .map {
                it.save()
                it
            }.subscribe(subscriber)
        }
    }

    public fun list(includeReported: Boolean = false, order: String = DropboxModel_Table.TIMESTAMP) = observable<DropboxModel> { subscriber->
        thread {
            val select = Select().from(DropboxModel::class.java)
            val where = if (includeReported)
                select.where()
            else
                select.where(Condition.column(DropboxModel_Table.SERVERID).isNull())

            where.orderBy(true, order).queryList().toObservable().subscribe(subscriber)
        }
    }
}