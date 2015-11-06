package org.tecrash.crashreport2.api

import com.squareup.okhttp.*
import okio.Buffer
import okio.BufferedSink
import okio.GzipSink
import okio.Okio

/**
 * Gzip request interceptor of HttpClient
 * Created by xiaocong on 15/10/6.
 */

class GzipRequestInterceptor(): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()

        return if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
            chain.proceed(originalRequest)
        } else {
            val compressedRequest = originalRequest.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method(), requestBodyWithContentLength(gzip(originalRequest.body())))
                    .build();
            chain.proceed(compressedRequest);
        }
    }

    private fun gzip(body: RequestBody) = object: RequestBody() {
        override fun contentType(): MediaType = body.contentType()

        override fun contentLength() = -1L

        override fun writeTo(sink: BufferedSink) {
            val gzipSink = Okio.buffer(GzipSink(sink))
            body.writeTo(gzipSink)
            gzipSink.close()
        }
    }

    private fun requestBodyWithContentLength(requestBody: RequestBody): RequestBody {
        val buffer = Buffer()
        requestBody.writeTo(buffer);

        return object: RequestBody() {
            override fun contentType(): MediaType {
                return requestBody.contentType();
            }

            override fun contentLength() = buffer.size()

            override fun writeTo(sink: BufferedSink) {
                sink.write(buffer.snapshot())
            }
        }
    }
}