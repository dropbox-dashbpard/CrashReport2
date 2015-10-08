package org.tecrash.crashreport2.app

/**
 * Release Impl
 * Created by xiaocong on 15/10/6.
 */

import android.app.Application
import com.raizlabs.android.dbflow.config.FlowManager

public class ReleaseAppLifecycleCallbacks(): AppLifecycleCallbacks {

    override fun onCreate(application: Application) {
        FlowManager.init(application);
    }

    override fun onTerminate(application: Application) {
        FlowManager.destroy()
    }
}
