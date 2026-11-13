package com.mindgate.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LinkTokenResponse(
    @SerializedName("code") val code: String,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("ttl_seconds") val ttlSeconds: Int? = null
)

data class LinkAcceptRequest(
    @SerializedName("code") val code: String
)

data class LinkAcceptResponse(
    @SerializedName("student_id") val studentId: Long,
    @SerializedName("student_name") val studentName: String,
    @SerializedName("linked_at") val linkedAt: String,
    @SerializedName("student_firebase_uid") val studentFirebaseUid: String? = null
)

data class ChildResponse(
    @SerializedName("student_id") val studentId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("student_firebase_uid") val studentFirebaseUid: String? = null
)

data class ChildStatusResponse(
    @SerializedName("today_usage_seconds") val todayUsageSeconds: Int,
    @SerializedName("daily_limit_seconds") val dailyLimitSeconds: Int,
    @SerializedName("exceeded") val exceeded: Boolean,
    @SerializedName("exceeded_by_seconds") val exceededBySeconds: Int?
)

data class AppUsage(
    @SerializedName("package") val packageName: String,
    @SerializedName("label") val label: String,
    @SerializedName("seconds") val seconds: Int
)

data class UsageEvent(
    @SerializedName("type") val type: String,
    @SerializedName("timestamp") val timestamp: String
)

data class ParentAlertResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("student_id") val studentId: Long,
    @SerializedName("student_name") val studentName: String,
    @SerializedName("type") val type: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("status") val status: String
)

data class ConnectionStatusResponse(
    @SerializedName("is_linked") val isLinked: Boolean,
    @SerializedName("role") val role: String,
    @SerializedName("linked_count") val linkedCount: Int? = null,
    @SerializedName("linked_parent_id") val linkedParentId: Long? = null,
    @SerializedName("linked_parent_firebase_uid") val linkedParentFirebaseUid: String? = null,
    @SerializedName("linked_student_ids") val linkedStudentIds: List<Long>? = null
)

data class ChildUsageResponse(
    @SerializedName("date") val date: String,
    @SerializedName("total_seconds") val totalSeconds: Int,
    @SerializedName("limit_seconds") val limitSeconds: Int,
    @SerializedName("by_app") val byApp: List<AppUsage>,
    @SerializedName("events") val events: List<UsageEvent>
)

data class LimitEventRequest(
    @SerializedName("app_package") val appPackage: String?,
    @SerializedName("limit_seconds") val limitSeconds: Int,
    @SerializedName("actual_usage_seconds") val actualUsageSeconds: Int,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("event_date") val eventDate: String
)

data class LimitEventResponse(
    @SerializedName("event_id") val eventId: Long,
    @SerializedName("notification") val notification: String
)

data class DeviceRegistrationRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("push_token") val pushToken: String,
    @SerializedName("platform") val platform: String = "android"
)

data class SelectRoleRequest(
    @SerializedName("role") val role: String
)
