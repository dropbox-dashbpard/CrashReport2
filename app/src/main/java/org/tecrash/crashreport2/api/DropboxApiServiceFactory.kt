package org.tecrash.crashreport2.api

/**
 * Created by xiaocong on 15/11/6.
 */

interface DropboxApiServiceFactory {
    fun create(zip: Boolean, rxcall: Boolean): DropboxApiService
}
