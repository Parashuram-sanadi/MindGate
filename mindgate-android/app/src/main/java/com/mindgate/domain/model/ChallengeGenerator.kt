package com.mindgate.domain.model

object ChallengeGenerator {

    private val triviaPool = listOf(
        Challenge(ChallengeType.TRIVIA,
            "Experts recommend how many hours of sleep for students?",
            "8", options = listOf("5","6","8","10")),
        Challenge(ChallengeType.TRIVIA,
            "The 20-20-20 rule: look away every 20 minutes at something how many feet away?",
            "20", options = listOf("10","20","30","50")),
        Challenge(ChallengeType.TRIVIA,
            "Which hormone does blue screen light suppress before sleep?",
            "Melatonin", options = listOf("Cortisol","Melatonin","Dopamine","Serotonin")),
        Challenge(ChallengeType.TRIVIA,
            "Which color of light from screens disrupts sleep the most?",
            "Blue", options = listOf("Red","Green","Blue","Yellow")),
        Challenge(ChallengeType.TRIVIA,
            "FOMO stands for?",
            "Fear Of Missing Out",
            options = listOf("Fear Of Missing Out","Feeling Of More Options",
                             "Focus On Mobile Only","Fear Of More Obligations"))
    )

    fun generate(type: ChallengeType): Challenge = when (type) {
        ChallengeType.MATH -> {
            val a = (100..500).random()
            val b = (100..500).random()
            when (listOf("+", "-", "×").random()) {
                "+" -> Challenge(ChallengeType.MATH, "$a + $b = ?", (a + b).toString())
                "-" -> {
                    val big = maxOf(a, b); val small = minOf(a, b)
                    Challenge(ChallengeType.MATH, "$big - $small = ?", (big - small).toString())
                }
                else -> {
                    val m = (2..9).random()
                    Challenge(ChallengeType.MATH, "$a × $m = ?", (a * m).toString())
                }
            }
        }
        ChallengeType.TRIVIA -> triviaPool.random()
    }
}
