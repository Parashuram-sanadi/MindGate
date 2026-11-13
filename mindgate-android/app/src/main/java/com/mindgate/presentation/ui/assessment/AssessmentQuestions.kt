package com.mindgate.presentation.ui.assessment

data class AssessmentQuestion(
    val id: String,
    val text: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val step: Int = 1
)

enum class QuestionType {
    DROPDOWN, SLIDER, LIKERT, RADIO, TOGGLE, MULTI_SELECT, READONLY
}

object AssessmentQuestions {
    val all: List<AssessmentQuestion> = listOf(
        // Step 1: Profile & Basic Usage
        AssessmentQuestion("Q1_AgeGroup", "Your age group?", QuestionType.DROPDOWN,
            listOf("15-18", "19-21", "22-25", "25+"), 1),
        AssessmentQuestion("Q4_Department", "Your academic department?", QuestionType.DROPDOWN,
            listOf("CSE", "ECE", "ME", "Civil", "Other"), 1),
        AssessmentQuestion("Q5_Residence", "Current place of residence?", QuestionType.DROPDOWN,
            listOf("Home", "Hostel", "PG"), 1),
        AssessmentQuestion("Q6_PrimaryGadget", "Primary gadget you use?", QuestionType.RADIO,
            listOf("Smartphone", "Laptop", "Tablet", "Multiple"), 1),
        AssessmentQuestion("Q7_ScreenTime", "Daily total screen time?", QuestionType.DROPDOWN,
            listOf("<2", "2-3", "3-5", "5-7", "7-9", "9+"), 1),
        AssessmentQuestion("Q8_GadgetUses", "What do you primarily use gadgets for?", QuestionType.MULTI_SELECT,
            listOf("Social Media", "Gaming", "Streaming", "Academic", "Creative", "Other"), 1),
        AssessmentQuestion("Q11_PhoneChecks_PerHr", "How many times do you check your phone per hour?", QuestionType.DROPDOWN,
            listOf("1-2", "3-5", "6-10", "10+"), 1),
        AssessmentQuestion("Q15_GadgetFreeDay", "Do you ever have a gadget-free day?", QuestionType.RADIO,
            listOf("Never", "Rarely", "Sometimes", "Often", "Always"), 1),

        // Step 2: Sleep & Physical
        AssessmentQuestion("sleep_hours", "Average sleep hours per night?", QuestionType.SLIDER, step = 2),
        AssessmentQuestion("Q23_SleepDuration", "Your sleep duration category?", QuestionType.DROPDOWN,
            listOf("<4", "4-5", "5-6", "6-7", "7-8", "8+"), 2),
        AssessmentQuestion("Q24_GadgetBefore30minSleep", "Use gadgets within 30 mins before sleep?", QuestionType.LIKERT, step = 2),
        AssessmentQuestion("Q25_WakesForPhone", "Wake up at night to check your phone?", QuestionType.LIKERT, step = 2),
        AssessmentQuestion("Q26_SleepQuality", "Rate your sleep quality (1-5)?", QuestionType.LIKERT, step = 2),
        AssessmentQuestion("Q27_ExercisePerWeek", "How many days do you exercise per week?", QuestionType.DROPDOWN,
            listOf("<1", "1-3", "3-5", "5+"), 2),
        AssessmentQuestion("Q28_PhysicalSymptoms", "Experience any physical symptoms?", QuestionType.MULTI_SELECT,
            listOf("Eye strain", "Headaches", "Back pain", "Neck pain", "Insomnia"), 2),
        AssessmentQuestion("Q29_SkipsMeals", "Skip meals due to gadget use?", QuestionType.LIKERT, step = 2),

        // Step 3: Academic
        AssessmentQuestion("Q16_CGPA", "Current CGPA band?", QuestionType.DROPDOWN,
            listOf("<5", "5-6", "6-7", "7-8", "8-9", "9-10"), 3),
        AssessmentQuestion("Q17_ClassesMissed", "Classes missed per month due to gadgets?", QuestionType.DROPDOWN,
            listOf("0-2", "3-5", "6-10", "10+"), 3),
        AssessmentQuestion("Q18_GadgetDistractsStudy", "Gadgets distract you from studying (1-5)?", QuestionType.LIKERT, step = 3),
        AssessmentQuestion("Q19_SelfStudyHrs", "Daily self-study hours?", QuestionType.DROPDOWN,
            listOf("<1", "1-2", "2-3", "3-4", "4+"), 3),
        AssessmentQuestion("Q20_ProductivityRating", "Rate your academic productivity (1-5)?", QuestionType.LIKERT, step = 3),
        AssessmentQuestion("Q21_GadgetHelpsAcademic", "Gadgets help your academic performance (1-5)?", QuestionType.LIKERT, step = 3),
        AssessmentQuestion("Q22_AssignmentsOnTime", "Submit assignments on time?", QuestionType.RADIO,
            listOf("Never", "Rarely", "Occasionally", "Sometimes", "Often", "Always"), 3),
        AssessmentQuestion("Q3_YearOfStudy", "Current year of study?", QuestionType.READONLY, step = 3),

        // Step 4: Psychological
        AssessmentQuestion("Q13_AnxiousWithoutGadget", "Feel anxious without your gadget (1-5)?", QuestionType.LIKERT, step = 4),
        AssessmentQuestion("Q30_StressLevel", "Your general stress level (1-5)?", QuestionType.LIKERT, step = 4),
        AssessmentQuestion("Q12_PhantomCheckUrge", "Urge to check phone even without notification?", QuestionType.LIKERT, step = 4),
        AssessmentQuestion("Q33_EmotionalCoping", "Use gadgets to cope with emotions (1-5)?", QuestionType.LIKERT, step = 4),
        AssessmentQuestion("Q34_UsageOutOfControl", "Feel your gadget use is out of control (1-5)?", QuestionType.LIKERT, step = 4),
        AssessmentQuestion("Q37_SelfLabel", "Do you consider yourself a gadget addict?", QuestionType.RADIO,
            listOf("Yes", "No"), 4),
        AssessmentQuestion("Q38_SatisfactionRating", "Satisfaction with current life balance (1-5)?", QuestionType.LIKERT, step = 4),
        AssessmentQuestion("Q36_TriedToReduce", "Have you tried to reduce usage?", QuestionType.RADIO,
            listOf("Never tried", "Tried and failed", "Tried and succeeded"), 4),

        // Step 5: Social & Wellness
        AssessmentQuestion("Q9_SocialPlatforms", "Number of social platforms active on?", QuestionType.DROPDOWN,
            listOf("0-1", "2-3", "4-5", "5+"), 5),
        AssessmentQuestion("Q10_LateNightUsage", "Frequency of late-night usage?", QuestionType.LIKERT, step = 5),
        AssessmentQuestion("Q14_GadgetAtMeals", "Use gadgets during meals?", QuestionType.LIKERT, step = 5),
        AssessmentQuestion("Q31_SocialInteractionQuality", "Quality of face-to-face interactions (1-5)?", QuestionType.LIKERT, step = 5),
        AssessmentQuestion("Q32_PrefersOnline", "Prefer online interaction over in-person?", QuestionType.LIKERT, step = 5),
        AssessmentQuestion("Q35_FeltNeedToReduce", "Felt a strong need to reduce usage?", QuestionType.RADIO,
            listOf("Not really", "Yes sometimes", "Yes often"), 5)
    )

    fun forStep(step: Int) = all.filter { it.step == step }
}
