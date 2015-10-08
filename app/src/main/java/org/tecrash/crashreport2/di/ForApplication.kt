package org.tecrash.crashreport2.di

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Annotation for application
 * Created by xiaocong on 15/10/3.
 */
@Qualifier
@Retention(RUNTIME)
annotation public class ForApplication