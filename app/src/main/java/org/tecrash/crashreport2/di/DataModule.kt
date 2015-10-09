package org.tecrash.crashreport2.di

import dagger.Module
import org.tecrash.crashreport2.api.ApiModule
import org.tecrash.crashreport2.db.DbModule
import org.tecrash.crashreport2.job.JobModule
import org.tecrash.crashreport2.util.ConfigModule

/**
 * Data module
 * Created by xiaocong on 15/10/6.
 */
@Module(includes = arrayOf( ConfigModule::class, DbModule::class, JobModule::class, ApiModule::class ))
class DataModule
