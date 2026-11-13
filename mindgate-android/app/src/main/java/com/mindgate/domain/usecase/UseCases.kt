package com.mindgate.domain.usecase

import com.mindgate.data.repository.AssessmentRepository
import com.mindgate.data.repository.UsageRepository
import com.mindgate.domain.model.AssessmentAnswers
import com.mindgate.domain.model.RiskResult
import com.mindgate.util.UsageStatsHelper
import javax.inject.Inject

class SubmitAssessmentUseCase @Inject constructor(
    private val repository: AssessmentRepository
) {
    suspend operator fun invoke(userId: Long, answers: AssessmentAnswers): Result<RiskResult> =
        repository.submitAssessment(userId, answers)
}

class GetUsageStatsUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val usageStatsHelper: UsageStatsHelper
) {
    suspend operator fun invoke(userId: Long) {
        if (usageStatsHelper.hasUsageStatsPermission()) {
            usageRepository.syncUsageFromSystem(userId)
        }
    }
}

class CheckLimitsUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    suspend operator fun invoke(userId: Long): List<String> =
        usageRepository.checkAndBlockExceeded(userId)
}
