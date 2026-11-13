package com.mindgate.data.remote.api

import com.mindgate.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ParentApiService {
    @POST("api/parent/link/create")
    suspend fun createLinkCode(): Response<LinkTokenResponse>

    @POST("api/parent/link/accept")
    suspend fun acceptLinkCode(@Body payload: LinkAcceptRequest): Response<LinkAcceptResponse>

    @GET("api/parent/children")
    suspend fun listChildren(): Response<List<ChildResponse>>

    @GET("api/parent/children/{student_id}/status")
    suspend fun getChildStatus(@Path("student_id") studentId: Long): Response<ChildStatusResponse>

    @GET("api/parent/children/{student_id}/usage")
    suspend fun getChildUsage(@Path("student_id") studentId: Long): Response<ChildUsageResponse>

    @DELETE("api/parent/children/{student_id}/link")
    suspend fun revokeLink(@Path("student_id") studentId: Long): Response<Unit>

    @GET("api/parent/alerts")
    suspend fun listAlerts(): Response<List<com.mindgate.data.remote.dto.ParentAlertResponse>>

    @GET("api/user/connection-status")
    suspend fun getConnectionStatus(): Response<com.mindgate.data.remote.dto.ConnectionStatusResponse>

    @POST("api/usage/limit-event")
    suspend fun reportLimitEvent(@Body payload: LimitEventRequest): Response<LimitEventResponse>

    @POST("api/devices/register")
    suspend fun registerDevice(@Body payload: DeviceRegistrationRequest): Response<Unit>

    @POST("api/parent/notify-request")
    suspend fun notifyParentOfRequest(@Body payload: Map<String, String>): Response<Unit>

    @POST("api/usage/sync-full")
    suspend fun syncFullUsage(@Body payload: com.google.gson.JsonObject): Response<Unit>
}
