package org.tecrash.crashreport2.db

import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Database module
 * Created by xiaocong on 15/10/5.
 */

@Module
class DbModule() {
    @Provides @Singleton fun provideDropboxModelService(): DropboxModelService = DropboxModelService()
}