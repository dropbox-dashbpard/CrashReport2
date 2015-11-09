package org.tecrash.crashreport2.di

import dagger.Component
import org.tecrash.crashreport2.DropboxMessageReceiver
import org.tecrash.crashreport2.app.BaseApplication
import org.tecrash.crashreport2.job.SendJobService
import javax.inject.Singleton

/**
 * Dagger di
 * Created by xiaocong on 15/10/3.
 */
@Singleton
@Component(modules = arrayOf(AndroidModule::class, DataModule::class))
interface AppComponent {
    fun inject(application: BaseApplication)
    fun inject(receiver: DropboxMessageReceiver)
    fun inject(jobService: SendJobService)

    object Initializer {
        fun init(app: BaseApplication): AppComponent =
            DaggerAppComponent.builder()
                .androidModule(AndroidModule(app))
                .dataModule(DataModule())
                .build()
    }
}