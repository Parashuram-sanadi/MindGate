package com.mindgate.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("role") val role: String,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("firebase_token") val firebaseToken: String?
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String
)

data class RegisterResponse(
    @SerializedName("message") val message: String,
    @SerializedName("id") val id: Long
)

data class GoogleAuthRequest(
    @SerializedName("id_token") val idToken: String
)

data class GoogleAuthCompleteRequest(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("role") val role: String,
    @SerializedName("name") val name: String? = null
)
