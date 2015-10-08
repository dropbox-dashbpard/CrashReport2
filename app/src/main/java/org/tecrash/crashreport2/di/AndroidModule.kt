package org.tecrash.crashreport2.di

import android.app.Application
import android.app.job.JobScheduler
import android.content.Context
import dagger.Module
import dagger.Provides
import org.tecrash.crashreport2.app.AppLifecycleCallbacks
import org.tecrash.crashreport2.app.BaseApplication
import org.tecrash.crashreport2.app.ReleaseAppLifecycleCallbacks
import javax.inject.Singleton

/**
 * A module for Android-specific dependencies which require a [Context] or
 * [android.app.Application] to create.
 * Created by xiaocong on 15/10/3.
 */
@Module
class AndroidModule(private val application: BaseApplication) {

    /**
     * Allow the application context to be injected
     */
    @Provides @Singleton
    fun provideApplicationContext(): Application = application

    @Provides @Singleton
    fun provideJobScheduler(): JobScheduler = application.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

    @Provides @Singleton
    fun provideAppLifecycleCallbacks(): AppLifecycleCallbacks = ReleaseAppLifecycleCallbacks()
}
