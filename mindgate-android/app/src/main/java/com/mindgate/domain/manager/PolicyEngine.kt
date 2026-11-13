package com.mindgate.domain.manager

import com.mindgate.data.repository.PolicyRepository
import com.mindgate.util.UsageStatsHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyEngine @Inject constructor(
    private val policyRepository: PolicyRepository,
    private val usageStatsHelper: UsageStatsHelper
) {

    fun getTodayUsageSeconds(packageName: String): Long {
        return usageStatsHelper.getTodayUsageSecondsByPackage()[packageName] ?: 0L
    }

    suspend fun evaluateExtensionMinutes(
        factors: List<String>,
        circumventionCount: Int,
        baseMinutes: Int = 15
    ): Int {
        val config = policyRepository.getPolicyConfig()
        var minutes = baseMinutes
        
        // 4.5 Late night usage in top factors -> reduce extension minutes
        if (factors.any { it.contains("late", ignoreCase = true) }) {
            minutes = config.reducedExtensionMinutes
        }
        
        // 4.5 High circumvention -> even less minutes
        if (circumventionCount > config.circumventionThreshold) {
            minutes = config.severeCircumventionMinutes
        }
        
        return minutes
    }

    suspend fun isExtensionBlocked(factors: List<String>, localTimeHour: Int): Boolean {
        val config = policyRepository.getPolicyConfig()
        // 4.5 Late night factor + late hour -> no extensions allowed
        if (factors.any { it.contains("late", ignoreCase = true) } && (localTimeHour >= config.lateNightCutoffHour || localTimeHour < 6)) {
            return true
        }
        return false
    }
}
