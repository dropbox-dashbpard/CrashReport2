package org.tecrash.crashreport2.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import org.tecrash.crashreport2.R
import org.tecrash.crashreport2.readProperty
import org.tecrash.crashreport2.readText
import java.util.*

/**
 * Global configuration
 * Created by xiaocong on 15/10/6.
 */

class ConfigService(private val app: Application, private val sharedPreferences: SharedPreferences) {

    val development: Boolean by lazy {
        app.packageName?.endsWith("dev", ignoreCase = true) ?: false
    }

    val production: Boolean by lazy {
        !development
    }

    val wifiMacAddress: String by lazy {
        readText("/sys/class/net/wlan0/address").trim()
    }

    val serialNo: String by lazy {
        val keys = arrayOf("ro.serialno")
        var serialNo: String = ""
        for (key in keys) {
            val sn: String = readProperty(key)
            if (!sn.equals("")) {
                serialNo = sn
                break
            }
        }
        serialNo
    }

    val imei: String by lazy {
        val tm: TelephonyManager? = app.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        tm?.deviceId ?: "0"
    }

    val ua: String by lazy {
        val (versionCode, appName) = try {
            val info = app.packageManager!!.getPackageInfo(app.packageName, 0)
            Pair(info!!.versionCode.toString(), info!!.packageName)
        } catch (e: Exception) {
            Pair("Unknown", "Unknown")
        }

        val language = Locale.getDefault().language

        val props = metaData("DROPBOX_REPORT_PROPERTIES") ?: ""
        val extraProps = extraProps(props)

        arrayOf(
            "sdk_int" to "${Build.VERSION.SDK_INT}",
            "app_version" to versionCode,
            "app_name" to appName,
            "lang" to language,
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "brand" to Build.BRAND,
            "board" to Build.BOARD,
            "device" to Build.DEVICE,
            "product" to Build.PRODUCT,
            "build_id" to Build.ID,
            "incremental" to Build.VERSION.INCREMENTAL,
            "display" to Build.DISPLAY,
            "sn" to serialNo,
            "mac_address" to wifiMacAddress,
            "imei" to imei,
            *(extraProps.toTypedArray())
        ).map {
            "${it.first}=${it.second}"
        }.reduce { s, d ->
            "$s;$d"
        }
    }

    val key: String by lazy {
        if (development)
            metaData("DROPBOX_DEVKEY")
        else
            metaData("DROPBOX_APPKEY")
    }

    fun extraProps(props: String) = props.split(";").map {
        it.split("=")
    }.filter {
        it.size() == 2
    }.map {
        it.get(0) to readProperty(it.get(1))
    }

    fun enabled() =
            sharedPreferences.getBoolean(app.getString(R.string.pref_key_on), true)

    fun metaData(key: String) =
            app.packageManager
                    .getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
                    .metaData.getString(key)

}