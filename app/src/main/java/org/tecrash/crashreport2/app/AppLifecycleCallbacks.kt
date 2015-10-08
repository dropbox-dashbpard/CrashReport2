package org.tecrash.crashreport2.app

/**
 * App life cycle
 * Created by xiaocong on 15/10/5.
 */

import android.app.Application

public interface AppLifecycleCallbacks {

    public fun onCreate(application: Application)

    public fun onTerminate(application: Application)
}
