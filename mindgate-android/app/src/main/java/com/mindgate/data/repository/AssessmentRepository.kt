package com.mindgate.data.repository

import com.google.gson.Gson
import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.local.SessionManager
import com.mindgate.data.local.dao.AssessmentDao
import com.mindgate.data.local.dao.UserDao
import com.mindgate.data.local.entities.AssessmentResultEntity
import com.mindgate.data.remote.api.MindGateApiService
import com.mindgate.data.remote.dto.PredictionRequest
import com.mindgate.domain.model.AssessmentAnswers
import com.mindgate.domain.model.RiskResult
import com.mindgate.util.AssessmentMapper
import com.mindgate.util.MindGateLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssessmentRepository @Inject constructor(
    private val api: MindGateApiService,
    private val assessmentDao: AssessmentDao,
    private val userDao: UserDao,
    private val prefs: PreferencesManager,
    private val session: SessionManager,
    private val gson: Gson
) {
    fun observeLatestResult(userId: Long): Flow<RiskResult?> =
        assessmentDao.observeLatest(userId).map { it?.toModel() }

    fun observeHistory(userId: Long): Flow<List<RiskResult>> =
        assessmentDao.observeHistory(userId).map { list -> list.map { it.toModel() } }

    suspend fun getLatestResult(userId: Long): RiskResult? =
        assessmentDao.getLatest(userId)?.toModel()

    suspend fun getLatestResultById(resultId: Long): RiskResult? =
        assessmentDao.getById(resultId)?.toModel()

    suspend fun saveProgress(step: Int, answers: AssessmentAnswers) {
        prefs.saveAssessmentProgress(step, gson.toJson(answers.answers))
    }

    suspend fun getSavedProgress(): Pair<Int, AssessmentAnswers> {
        val step = prefs.assessmentStep.first()
        val json = prefs.assessmentAnswersJson.first()
        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson(json, Map::class.java) as? Map<String, String> ?: emptyMap()
        return step to AssessmentAnswers(map)
    }

    suspend fun clearProgress() = prefs.clearAssessmentProgress()

    suspend fun resolveUserId(explicitUserId: Long): Long {
        // 1. Try explicit ID if valid and exists in DB
        if (explicitUserId > 0L) {
            if (userDao.getById(explicitUserId) != null) return explicitUserId
            MindGateLog.d("ASSESS", "Explicit userId=$explicitUserId not in DB, trying session...")
        }

        // 2. Try session ID if valid and exists in DB
        val fromSession = session.getUserId()
        if (fromSession > 0L) {
            if (userDao.getById(fromSession) != null) return fromSession
            MindGateLog.d("ASSESS", "Session userId=$fromSession not in DB, trying first user...")
        }

        // 3. Fallback to first user in DB if any
        val firstUser = userDao.getFirstUser()
        if (firstUser != null) {
            MindGateLog.d("ASSESS", "Using fallback firstUser id=${firstUser.id}")
            return firstUser.id
        }

        MindGateLog.e("ASSESS", "No user found in database at all.")
        return -1L
    }

    suspend fun submitAssessment(userId: Long, answers: AssessmentAnswers): Result<RiskResult> {
        val resolvedUserId = resolveUserId(userId)
        MindGateLog.d("ASSESS", "Building request with resolvedUserId=$resolvedUserId (input=$userId)")

        if (resolvedUserId <= 0L) {
            return Result.failure(
                Exception("No user profile found. Please register or log in first.")
            )
        }

        val user = userDao.getById(resolvedUserId)
        if (user == null) {
            return Result.failure(
                Exception("User (id=$resolvedUserId) could not be retrieved. Try restarting the app.")
            )
        }

        val request = try {
            AssessmentMapper.buildAndValidateRequest(answers, user)
        } catch (e: IllegalArgumentException) {
            return Result.failure(Exception("Invalid assessment data: ${e.message}"))
        }

        MindGateLog.d("ASSESS", "PredictionRequest=$request")

        return try {
            MindGateLog.d("API", "Calling /predict ...")
            val response = api.predict(request)
            val errorBody = response.errorBody()?.string()
            MindGateLog.d("API", "Response code: ${response.code()}")
            MindGateLog.d("API", "Response body: ${response.body()}")
            MindGateLog.d("API", "Error body: $errorBody")

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return fallbackOrFail(resolvedUserId, answers, "API ${response.code()}: empty response body")
                val result = RiskResult(
                    riskLevel = body.riskLevel,
                    riskScore = body.riskScore,
                    confidence = body.confidence,
                    primaryRecommendation = body.recommendation,
                    recommendations = body.recommendations,
                    interventionLevel = body.interventionLevel,
                    topFactors = body.topFactors ?: emptyList(),
                    downgraded = body.downgraded ?: false
                )
                val resultId = saveResult(resolvedUserId, result, gson.toJson(answers.answers))
                userDao.setAssessmentCompleted(resolvedUserId, true)
                prefs.clearAssessmentProgress()
                Result.success(result.copy(id = resultId))
            } else {
                fallbackOrFail(
                    resolvedUserId,
                    answers,
                    "API ${response.code()}: ${errorBody ?: "Unknown error"}"
                )
            }
        } catch (e: IOException) {
            MindGateLog.e("API", "Network error: ${e.message}", e)
            fallbackOrFail(resolvedUserId, answers, "No internet / server unreachable: ${e.message}")
        } catch (e: Exception) {
            MindGateLog.e("API", "Unexpected error: ${e.message}", e)
            fallbackOrFail(resolvedUserId, answers, "Unexpected: ${e.localizedMessage}")
        }
    }

    private suspend fun fallbackOrFail(
        userId: Long,
        answers: AssessmentAnswers,
        error: String
    ): Result<RiskResult> {
        MindGateLog.d("ASSESS", "Fallback triggered: $error")
        val cached = assessmentDao.getLatest(userId)?.toModel()
        return if (cached != null) {
            Result.success(cached.copy(fromCache = true, apiWarning = error))
        } else {
            val local = AssessmentMapper.localFallback(answers)
            val resultId = saveResult(userId, local, gson.toJson(answers.answers))
            userDao.setAssessmentCompleted(userId, true)
            prefs.clearAssessmentProgress()
            Result.success(
                local.copy(fromCache = true, apiWarning = error, id = resultId)
            )
        }
    }

    private suspend fun saveResult(userId: Long, result: RiskResult, answersJson: String): Long {
        assessmentDao.clearLatestFlag(userId)
        return assessmentDao.insert(
            AssessmentResultEntity(
                userId = userId,
                riskLevel = result.riskLevel,
                riskScore = result.riskScore,
                confidence = result.confidence,
                recommendationsJson = gson.toJson(result.recommendations),
                primaryRecommendation = result.primaryRecommendation,
                interventionLevel = result.interventionLevel,
                topFactorsJson = gson.toJson(result.topFactors),
                answersJson = answersJson,
                downgraded = result.downgraded,
                isLatest = true
            )
        )
    }

    private fun AssessmentResultEntity.toModel() = RiskResult(
        id = id,
        riskLevel = riskLevel,
        riskScore = riskScore,
        confidence = confidence,
        primaryRecommendation = primaryRecommendation,
        recommendations = gson.fromJson(recommendationsJson, Array<String>::class.java).toList(),
        interventionLevel = interventionLevel,
        topFactors = if (topFactorsJson != null) gson.fromJson(topFactorsJson, Array<String>::class.java).toList() else emptyList(),
        downgraded = downgraded,
        timestamp = timestamp
    )
}
