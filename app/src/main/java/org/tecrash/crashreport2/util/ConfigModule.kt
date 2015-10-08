package org.tecrash.crashreport2.util

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by xiaocong on 15/10/6.
 */
@Module
class ConfigModule() {
    @Provides @Singleton fun provideSharedPreferences(app: Application): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(app)

    @Provides @Singleton fun provideConfigService(app: Application, sharedPreferences: SharedPreferences): ConfigService =
            ConfigService(app, sharedPreferences)

}