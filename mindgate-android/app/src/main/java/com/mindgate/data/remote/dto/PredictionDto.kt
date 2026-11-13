package com.mindgate.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PredictionRequest(
    @SerializedName("Q1_AgeGroup") val ageGroup: String,
    @SerializedName("Q2_Gender") val gender: String,
    @SerializedName("Q3_YearOfStudy") val yearOfStudy: String,
    @SerializedName("Q4_Department") val department: String,
    @SerializedName("Q5_Residence") val residence: String,
    @SerializedName("Q6_PrimaryGadget") val primaryGadget: String,
    @SerializedName("Q7_ScreenTime") val screenTime: String,
    @SerializedName("Q8_GadgetUses") val gadgetUses: String,
    @SerializedName("Q9_SocialPlatforms") val socialPlatforms: String,
    @SerializedName("Q11_PhoneChecks_PerHr") val phoneChecks: String,
    @SerializedName("Q15_GadgetFreeDay") val gadgetFreeDay: String,
    @SerializedName("sleep_hours") val sleepHours: Double,
    @SerializedName("Q23_SleepDuration") val sleepDuration: String,
    @SerializedName("Q24_GadgetBefore30minSleep") val gadgetBeforeSleep: String,
    @SerializedName("Q25_WakesForPhone") val wakesForPhone: String,
    @SerializedName("Q26_SleepQuality") val sleepQuality: String,
    @SerializedName("Q27_ExercisePerWeek") val exercisePerWeek: String,
    @SerializedName("Q28_PhysicalSymptoms") val physicalSymptoms: String,
    @SerializedName("Q29_SkipsMeals") val skipsMeals: String,
    @SerializedName("Q16_CGPA") val cgpa: String,
    @SerializedName("Q17_ClassesMissed") val classesMissed: String,
    @SerializedName("Q18_GadgetDistractsStudy") val distractStudy: Int,
    @SerializedName("Q19_SelfStudyHrs") val selfStudyHrs: String,
    @SerializedName("Q20_ProductivityRating") val productivity: Int,
    @SerializedName("Q21_GadgetHelpsAcademic") val helpsAcademic: Int,
    @SerializedName("Q22_AssignmentsOnTime") val assignmentsOnTime: String,
    @SerializedName("Q13_AnxiousWithoutGadget") val anxious: Int,
    @SerializedName("Q30_StressLevel") val stress: Int,
    @SerializedName("Q12_PhantomCheckUrge") val phantomUrge: String,
    @SerializedName("Q33_EmotionalCoping") val emotionalCoping: Int,
    @SerializedName("Q34_UsageOutOfControl") val usageOutOfControl: Int,
    @SerializedName("Q37_SelfLabel") val selfLabel: String,
    @SerializedName("Q38_SatisfactionRating") val satisfaction: Int,
    @SerializedName("Q36_TriedToReduce") val triedToReduce: String,
    @SerializedName("Q10_LateNightUsage") val lateNightUsage: String,
    @SerializedName("Q14_GadgetAtMeals") val gadgetAtMeals: String,
    @SerializedName("Q31_SocialInteractionQuality") val socialQuality: Int,
    @SerializedName("Q32_PrefersOnline") val prefersOnline: String,
    @SerializedName("Q35_FeltNeedToReduce") val feltNeedToReduce: String,
    @SerializedName("gate1_score_valid") val gate1ScoreValid: Boolean? = null,
    @SerializedName("gate2_danger_met") val gate2DangerMet: Boolean? = null,
    @SerializedName("gate3_loss_control") val gate3LossControl: Boolean? = null
)

data class GateResultsDto(
    @SerializedName("gate1_score_valid") val gate1ScoreValid: Boolean = false,
    @SerializedName("gate2_danger_met") val gate2DangerMet: Boolean = false,
    @SerializedName("gate3_loss_control") val gate3LossControl: Boolean = false,
    @SerializedName("all_gates_pass") val allGatesPass: Boolean = false
)

data class PredictionResponse(
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("risk_score") val riskScore: Double,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("recommendation") val recommendation: String,
    @SerializedName("recommendations") val recommendations: List<String>,
    @SerializedName("intervention_level") val interventionLevel: String,
    @SerializedName("top_factors") val topFactors: List<String>? = null,
    @SerializedName("downgraded") val downgraded: Boolean? = null,
    @SerializedName("gate_results") val gateResults: GateResultsDto? = null,
    @SerializedName("model_version") val modelVersion: String,
    @SerializedName("timestamp") val timestamp: String
)

data class BatchFileEntry(
    @SerializedName("student_info") val studentInfo: Map<String, String>,
    @SerializedName("prediction") val prediction: PredictionResponse
)

data class BatchFileResponse(
    @SerializedName("results") val results: List<BatchFileEntry>,
    @SerializedName("summary") val summary: Map<String, Any>
)
