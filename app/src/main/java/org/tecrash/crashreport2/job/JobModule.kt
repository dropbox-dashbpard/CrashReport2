package org.tecrash.crashreport2.job

import android.app.Application
import android.content.ComponentName
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by xiaocong on 15/10/8.
 */
@Module
class JobModule {
    @Provides @Singleton
    fun provideJobComponentName(app: Application): ComponentName = ComponentName(app, SendJobService::class.java)
}