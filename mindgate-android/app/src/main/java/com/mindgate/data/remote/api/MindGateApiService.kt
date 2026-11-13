package com.mindgate.data.remote.api

import com.mindgate.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface MindGateApiService {
    @FormUrlEncoded
    @POST("token")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") pass: String
    ): Response<AuthResponse>

    @POST("api/auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): Response<com.google.gson.JsonObject>

    @POST("api/auth/google/complete")
    suspend fun googleAuthComplete(@Body request: GoogleAuthCompleteRequest): Response<AuthResponse>

    @GET("api/user/firebase-token")
    suspend fun getFirebaseToken(): Response<Map<String, String>>

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("predict")
    suspend fun predict(@Body request: PredictionRequest): Response<PredictionResponse>

    @POST("api/user/select-role")
    suspend fun selectRole(@Body request: SelectRoleRequest): Response<Unit>

    @GET("health")
    suspend fun health(): Response<Map<String, Any>>

    @GET("ping")
    suspend fun ping(): Response<Map<String, Any>>

    @Multipart
    @POST("predict/batch/file")
    suspend fun predictBatchFile(
        @Part file: MultipartBody.Part
    ): Response<BatchFileResponse>

    @POST("extensions/grant")
    suspend fun grantExtension(@Body request: ExtensionGrantRequest): Response<ExtensionLedgerDto>

    @GET("extensions/today/{studentId}")
    suspend fun getTodayExtensions(@Path("studentId") studentId: Long): Response<TodayExtensionsResponse>

    @POST("events/protection-disabled")
    suspend fun reportProtectionDisabled(@Body payload: Map<String, Long>): Response<Unit>

    @POST("events/limit-extended")
    suspend fun reportLimitExtended(@Body payload: Map<String, Any>): Response<Unit>

    @POST("extensions/request-approval")
    suspend fun requestExtensionApproval(@Body request: Map<String, Any>): Response<PendingExtensionDto>

    @GET("extensions/pending/{id}/status")
    suspend fun getPendingExtensionStatus(@Path("id") requestId: Long): Response<PendingExtensionDto>

    @PATCH("extensions/pending/{id}/approve")
    suspend fun approvePendingExtension(@Path("id") requestId: Long): Response<Unit>

    @PATCH("extensions/pending/{id}/deny")
    suspend fun denyPendingExtension(@Path("id") requestId: Long): Response<Unit>

    @POST("study/session")
    suspend fun reportStudySession(@Body session: Map<String, Any>): Response<Unit>

    @GET("extensions/eligibility/{studentId}")
    suspend fun getExtensionEligibility(@Path("studentId") studentId: Long): Response<Map<String, Any>>
}
