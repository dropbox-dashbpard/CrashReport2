package org.tecrash.crashreport2.api

import org.tecrash.crashreport2.api.data.ReportData
import org.tecrash.crashreport2.api.data.ReportResult
import retrofit.http.Body
import retrofit.http.POST

/**
 * Dropbox API
 * Created by xiaocong on 15/10/6.
 */
interface DropboxApiService {
    @POST("/dropbox")
    fun report(@Body data: ReportData): Array<ReportResult>
}
