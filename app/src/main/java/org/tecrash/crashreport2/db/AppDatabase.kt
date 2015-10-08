package org.tecrash.crashreport2.db

import com.raizlabs.android.dbflow.annotation.Database
/**
 * Application database object
 * Created by xiaocong on 15/10/3.
 */

@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION, generatedClassSeparator = "_")
public object AppDatabase {
    const val NAME: String = "appdb"
    const val VERSION: Int = 1
}