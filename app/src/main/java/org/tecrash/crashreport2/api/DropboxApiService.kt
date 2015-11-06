package org.tecrash.crashreport2.api

import com.squareup.okhttp.RequestBody
import org.tecrash.crashreport2.api.data.ReportData
import org.tecrash.crashreport2.api.data.ReportResult
import org.tecrash.crashreport2.api.data.ReportResultEntry
import retrofit.Call
import retrofit.http.*

/**
 * Dropbox API
 * Created by xiaocong on 15/10/6.
 */
interface DropboxApiService {
    @POST("/api/0/dropbox")
    fun report(@Header("Authorization") auth: String,
               @Header("X-Dropbox-UA") ua: String,
               @Body data: ReportData): Call<ReportResult<ReportResultEntry>>

    @POST("/api/0/dropbox/{dropboxId}/content")
    @Headers("Content-type: text/plain")
    fun uploadContent(@Header("Authorization") auth: String,
                      @Path("dropboxId") dbId: String,
                      @Body data: RequestBody): Call<Unit>

    // I hope to use @Multipart to upload file, but it doesn't work on retrofit 2.0beta
    // see http://stackoverflow.com/questions/32663281/aws-s3-rest-api-with-android-retrofit-v2-library-uploaded-image-is-damaged/32796626#32796626
    // TODO change it when retrofit resolves the issue.
    @POST("/api/0/dropbox/{dropboxId}/upload")
    @Headers("Content-Type: multipart/form-data;boundary=95416089-b2fd-4eab-9a14-166bb9c5788b")
    fun uploadFile(@Header("Authorization") auth: String,
                   @Path("dropboxId") dbId: String,
                   @Body attachment: RequestBody): Call<Unit>
}
