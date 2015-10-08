package org.tecrash.crashreport2.app

import android.app.Application
import org.tecrash.crashreport2.di.AppComponent
import javax.inject.Inject

/**
 * Base application
 * Created by xiaocong on 15/10/3.
 */
abstract class BaseApplication : Application() {

    val component: AppComponent by lazy { AppComponent.Initializer.init(this) }

    @Inject lateinit var appLifecycleCallbacks: AppLifecycleCallbacks

    override fun onCreate() {
        super.onCreate()
        // Inject
        component.inject(this)

        // app lifecycle callback
        appLifecycleCallbacks.onCreate(this)
    }

    override fun onTerminate() {
        // app lifecycle callback
        appLifecycleCallbacks.onTerminate(this)

        super.onTerminate()
    }
}
