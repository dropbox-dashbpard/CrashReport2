package org.tecrash.crashreport2.db;

import com.raizlabs.android.dbflow.annotation.Database;

/**
 * Created by xiaocong on 15/11/14.
 */
@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION, generatedClassSeparator = "_")
public class AppDatabase {
    final static String NAME = "appdb";
    final static int VERSION = 1;
}
