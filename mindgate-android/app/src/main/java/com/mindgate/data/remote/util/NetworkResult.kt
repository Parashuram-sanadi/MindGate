package com.mindgate.data.remote.util

import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
    data class Failure(val exception: Throwable) : NetworkResult<Nothing>()

    val isSuccess get() = this is Success
    
    fun getOrNull(): T? = (this as? Success)?.data

    fun errorDisplayMessage(): String {
        return when (this) {
            is Success -> "Success"
            is Error -> "Server Error ($code): $message"
            is Failure -> {
                val msg = exception.message ?: "Unknown error"
                when {
                    msg.contains("timeout", ignoreCase = true) -> "Connection Timed Out. Check if the server is running."
                    msg.contains("refused", ignoreCase = true) -> "Connection Refused. Server is likely offline."
                    msg.contains("Network is unreachable", ignoreCase = true) -> "Network Unreachable. Check your WiFi."
                    else -> "Connection failed: $msg"
                }
            }
        }
    }
}

suspend fun <T> safeApiCall(call: suspend () -> Response<T>): NetworkResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) NetworkResult.Success(body)
            else NetworkResult.Error("Empty response body", response.code())
        } else {
            NetworkResult.Error(response.message(), response.code())
        }
    } catch (e: Exception) {
        NetworkResult.Failure(e)
    }
}
