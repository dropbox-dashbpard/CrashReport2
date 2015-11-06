package org.tecrash.crashreport2.api

import android.app.Application
import android.content.SharedPreferences
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.OkHttpClient
import dagger.Module
import dagger.Provides
import org.tecrash.crashreport2.R
import org.tecrash.crashreport2.util.ConfigService
import retrofit.GsonConverterFactory
import retrofit.Retrofit
import retrofit.RxJavaCallAdapterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Api Service Module
 * Created by xiaocong on 15/10/6.
 */
@Module
class ApiModule {

    @Provides @Singleton
    fun provideSSLContext(): SSLContext {
        val sslContext: SSLContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(object: X509TrustManager {
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<out X509Certificate>? = null

            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }
        }), SecureRandom())

        return sslContext
    }

    @Provides @Singleton
    fun provideGzipRequestInterceptor(): GzipRequestInterceptor = GzipRequestInterceptor()

    @Provides @Named("Api")
    fun provideUrl(app: Application, sharedPreferences: SharedPreferences, configService: ConfigService): HttpUrl {
        var urlKey = sharedPreferences.getString(app.getString(R.string.pref_key_url), "")
        if("".equals(urlKey)) {
            val urls = app.resources.getStringArray(R.array.pref_key_url_list_values)
            urlKey = if (configService.development) urls[2] else urls[1]
        }
        return HttpUrl.parse(configService.metaData(urlKey))
    }

    @Provides
    fun provideDropboxApiServiceFactory(@Named("Api") url: HttpUrl, sslContext: SSLContext, gzipInterceptor: GzipRequestInterceptor): DropboxApiServiceFactory = object: DropboxApiServiceFactory {
        override fun create(zip: Boolean, rxcall: Boolean): DropboxApiService {
            val client = OkHttpClient()
            // ignore cert verification
            client.setHostnameVerifier { s: String?, sslSession: SSLSession ->
                true
            }
            client.setSslSocketFactory(sslContext.getSocketFactory())
            // zip compression
            if (zip)
                client.interceptors().add(gzipInterceptor)

            val builder = Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())

            // rxCallAdapter
            val build = if (rxcall)
                builder.addCallAdapterFactory(RxJavaCallAdapterFactory.create()).build()
            else
                builder.build()

            return build.create(DropboxApiService::class.java)
        }
    }
}
