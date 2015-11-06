package org.tecrash.crashreport2.api.data

/**
 * Report data structure
 * Created by xiaocong on 15/10/8.
 */
data class ReportData(val uptime: Long, val data: Array<ReportDataEntry>)

data class ReportDataEntry(val id: Long, val tag: String, val app: String, val occurred_at: Long, var count: Int = 1)

data class ReportResult<T>(val code: Long, val data: Array<T?>)

data class ReportResultEntry(val result: String, val dropbox_id: String)
