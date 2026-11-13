package com.mindgate.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExtensionGrantRequest(
    @SerializedName("studentId") val studentId: Long,
    @SerializedName("appPackage") val appPackage: String,
    @SerializedName("minutes") val minutes: Int,
    @SerializedName("method") val method: String,
    @SerializedName("tier") val tier: Int = 1
)

data class ExtensionLedgerDto(
    @SerializedName("id") val id: Long,
    @SerializedName("studentId") val studentId: Long,
    @SerializedName("appPackage") val appPackage: String,
    @SerializedName("extensionMinutes") val extensionMinutes: Int,
    @SerializedName("grantedAt") val grantedAt: Long,
    @SerializedName("dateKey") val dateKey: String,
    @SerializedName("source") val source: String,
    @SerializedName("tier") val tier: Int
)

data class TodayExtensionsResponse(
    @SerializedName("dateKey") val dateKey: String,
    @SerializedName("extensions") val extensions: List<ExtensionLedgerDto>
)

data class PendingExtensionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("studentId") val studentId: Long,
    @SerializedName("appPackage") val appPackage: String,
    @SerializedName("requestedAt") val requestedAt: Long,
    @SerializedName("status") val status: String // pending/approved/denied/timed_out
)
