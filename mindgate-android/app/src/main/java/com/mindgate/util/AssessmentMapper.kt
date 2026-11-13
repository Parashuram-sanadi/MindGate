package com.mindgate.util

import com.mindgate.data.local.entities.UserEntity
import com.mindgate.data.remote.dto.PredictionRequest
import com.mindgate.domain.model.AssessmentAnswers
import com.mindgate.domain.model.RiskResult
import kotlin.math.roundToInt

object AssessmentMapper {

    private val yearMap = mapOf(1 to "1st Year", 2 to "2nd Year", 3 to "3rd Year", 4 to "4th Year")

    fun buildAndValidateRequest(answers: AssessmentAnswers, user: UserEntity): PredictionRequest {
        return PredictionRequest(
            ageGroup = answers.get("Q1_AgeGroup") ?: "19-21",
            gender = user.gender,
            yearOfStudy = yearMap[user.yearOfStudy] ?: "2nd Year",
            department = answers.get("Q4_Department") ?: "CSE",
            residence = answers.get("Q5_Residence") ?: "Hostel",
            primaryGadget = answers.get("Q6_PrimaryGadget") ?: "Smartphone",
            screenTime = answers.get("Q7_ScreenTime") ?: "3-5",
            gadgetUses = answers.get("Q8_GadgetUses") ?: "Social Media",
            socialPlatforms = answers.get("Q9_SocialPlatforms") ?: "2-3",
            phoneChecks = answers.get("Q11_PhoneChecks_PerHr") ?: "3-5",
            gadgetFreeDay = answers.get("Q15_GadgetFreeDay") ?: "Never",
            sleepHours = answers.get("sleep_hours")?.toDoubleOrNull() ?: 7.0,
            sleepDuration = answers.get("Q23_SleepDuration") ?: "6-7",
            gadgetBeforeSleep = mapLikertToFreq(answers.get("Q24_GadgetBefore30minSleep")),
            wakesForPhone = mapLikertToFreq(answers.get("Q25_WakesForPhone")),
            sleepQuality = answers.get("Q26_SleepQuality") ?: "3",
            exercisePerWeek = answers.get("Q27_ExercisePerWeek") ?: "1-3",
            physicalSymptoms = answers.get("Q28_PhysicalSymptoms") ?: "None",
            skipsMeals = mapLikertToFreq(answers.get("Q29_SkipsMeals")),
            cgpa = answers.get("Q16_CGPA") ?: "7-8",
            classesMissed = answers.get("Q17_ClassesMissed") ?: "0-2",
            distractStudy = answers.get("Q18_GadgetDistractsStudy")?.toIntOrNull() ?: 3,
            selfStudyHrs = answers.get("Q19_SelfStudyHrs") ?: "1-2",
            productivity = answers.get("Q20_ProductivityRating")?.toIntOrNull() ?: 3,
            helpsAcademic = answers.get("Q21_GadgetHelpsAcademic")?.toIntOrNull() ?: 3,
            assignmentsOnTime = answers.get("Q22_AssignmentsOnTime") ?: "Sometimes",
            anxious = answers.get("Q13_AnxiousWithoutGadget")?.toIntOrNull() ?: 3,
            stress = answers.get("Q30_StressLevel")?.toIntOrNull() ?: 3,
            phantomUrge = mapLikertToFreq(answers.get("Q12_PhantomCheckUrge")),
            emotionalCoping = answers.get("Q33_EmotionalCoping")?.toIntOrNull() ?: 3,
            usageOutOfControl = answers.get("Q34_UsageOutOfControl")?.toIntOrNull() ?: 3,
            selfLabel = answers.get("Q37_SelfLabel") ?: "No",
            satisfaction = answers.get("Q38_SatisfactionRating")?.toIntOrNull() ?: 3,
            triedToReduce = answers.get("Q36_TriedToReduce") ?: "Never tried",
            lateNightUsage = mapLikertToFreq(answers.get("Q10_LateNightUsage")),
            gadgetAtMeals = answers.get("Q14_GadgetAtMeals") ?: "3",
            socialQuality = answers.get("Q31_SocialInteractionQuality")?.toIntOrNull() ?: 3,
            prefersOnline = answers.get("Q32_PrefersOnline") ?: "3",
            feltNeedToReduce = answers.get("Q35_FeltNeedToReduce") ?: "Not really",
            gate1ScoreValid = null,
            gate2DangerMet = null,
            gate3LossControl = (answers.get("Q34_UsageOutOfControl")?.toIntOrNull() ?: 0) >= 4
        )
    }

    private fun mapLikertToFreq(raw: String?): String = when (raw?.toIntOrNull()) {
        1 -> "Never"
        2 -> "Rarely"
        3 -> "Sometimes"
        4 -> "Often"
        5 -> "Always"
        else -> "Sometimes"
    }

    fun localFallback(answers: AssessmentAnswers): RiskResult {
        // ... (rest of localFallback remains similar but using updated keys)
        val screen = answers.get("Q7_ScreenTime") ?: "3-5"
        val sleep = answers.get("sleep_hours")?.toDoubleOrNull() ?: 7.0
        val stress = answers.get("Q30_StressLevel")?.toIntOrNull() ?: 3
        var score = 30.0
        if (screen.contains("9") || screen.contains("8")) score += 25
        else if (screen.contains("6") || screen.contains("7")) score += 15
        if (sleep < 6) score += 15
        if (stress >= 4) score += 10
        val level = when {
            score >= 65 -> "Severe"
            score >= 55 -> "High"
            score >= 35 -> "Moderate"
            else -> "Low"
        }
        val fallbackRecs = when (level) {
            "Low" -> listOf(
                "Great job maintaining a healthy and balanced digital lifestyle!",
                "Continue setting daily screen-free hours during meals and before sleep.",
                "Sustain your healthy habits with regular offline hobbies and physical activity.",
                "Track your weekly usage patterns to stay mindful of your digital balance."
            )
            "Moderate" -> listOf(
                "Set daily app limits on your most frequently used applications.",
                "Establish a 30-minute screen-free routine before going to sleep.",
                "Clean up non-essential notifications to reduce frequent phone checks.",
                "Practice the 20-20-20 rule: every 20 minutes, look 20 feet away for 20 seconds.",
                "Schedule regular offline breaks and engage in physical or creative activities."
            )
            "High" -> listOf(
                "Configure strict daily usage limits on your main problem applications.",
                "Enable bedtime or quiet-hours lock to protect your sleep schedule.",
                "Create phone-free zones during meals and dedicated study blocks.",
                "Connect a parent or guardian account for shared digital wellness accountability.",
                "Replace evening scrolling time with scheduled physical or social activities.",
                "Track your progress weekly to build consistent self-regulation habits."
            )
            else -> listOf( // Severe
                "Set immediate strict daily limits and an automatic bedtime lock on your device.",
                "Involve a parent, guardian, or trusted mentor to support your wellness goals.",
                "Consider speaking with a school counselor or mental health professional for structured guidance.",
                "Establish complete phone-free zones in your bedroom and study area.",
                "Follow a gradual reduction plan, reducing screen time by 15–20% each week.",
                "Monitor key wellness indicators such as sleep quality, mood changes, and social connection.",
                "Support helpline for guidance: iCall – 9152987821."
            )
        }

        return RiskResult(
            riskLevel = level,
            riskScore = score,
            confidence = 0.6,
            primaryRecommendation = fallbackRecs[0],
            recommendations = fallbackRecs,
            interventionLevel = when (level) {
                "Severe" -> "Urgent Intervention"
                "High" -> "Intervention"
                "Moderate" -> "Awareness"
                else -> "Monitor"
            }
        )
    }
}
