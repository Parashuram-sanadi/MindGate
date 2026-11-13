package com.mindgate.data.repository

import com.mindgate.data.remote.api.MindGateApiService
import com.mindgate.data.remote.dto.BatchFileResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeacherRepository @Inject constructor(
    private val apiService: MindGateApiService
) {
    suspend fun checkHealth(): Result<Unit> {
        return try {
            val response = apiService.ping()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server returned status: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadBatchFile(file: File): Result<BatchFileResponse> {
        return try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = apiService.predictBatchFile(body)
            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Upload failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
