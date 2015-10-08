package org.tecrash.crashreport2

import java.io.File
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by xiaocong on 15/9/29.
 */

internal fun readProperty(key: String): String = try {
    val proc = Runtime.getRuntime().exec(arrayOf("/system/bin/getprop", key))
    InputStreamReader(proc.inputStream).readText().trim()
} catch (e: IOException) {
    e.printStackTrace()
    ""
}

internal fun readText(fileName: String): String = try {
    File(fileName).bufferedReader().readText()
} catch (e: Exception) {
    e.printStackTrace()
    ""
}

val userBuild: Boolean by lazy {
    android.os.Build.TYPE.equals("user")
}

val debugBuild: Boolean by lazy {
    !userBuild
}

