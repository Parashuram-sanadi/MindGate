package com.mindgate.domain.model

enum class ChallengeType { MATH, TRIVIA }

data class Challenge(
    val type: ChallengeType,
    val question: String,
    val correctAnswer: String,
    val options: List<String> = emptyList()    // MCQ options for TRIVIA
) {
    fun checkAnswer(input: String): Boolean =
        input.trim().equals(correctAnswer.trim(), ignoreCase = true)
}
