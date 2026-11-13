package com.mindgate.data.local.content

/**
 * Curated "Explore & Recharge" content library.
 * Generated from arpita_data.xlsx
 *
 * All 35 entries have verified, specific YouTube video IDs -- no
 * search-query fallbacks remain. This is a static, bundled data
 * source: no Firebase/Firestore/Room/network involved. Do not add
 * any backend call to load this content.
 */

enum class ExploreCategory(val emoji: String, val label: String) {
    NATURE("🌿", "Nature"),
    TRAVEL("🌍", "Travel"),
    MINDFULNESS("🧘", "Mindfulness"),
    SELF_GROWTH("🧠", "Self-Growth"),
    MEANINGFUL("❤️", "Meaningful"),
    INSPIRATION("🎙️", "Inspiration"),
    DIGITAL_DETOX("📵", "Digital Detox"),
}

data class InspirationVideo(
    val id: String,
    val title: String,
    val purpose: String,
    val category: ExploreCategory,
    val videoId: String?,       // real YouTube video ID, or null
    val searchQuery: String?    // fallback search query, or null
)

object ExploreContentLibrary {
    val videos: List<InspirationVideo> = listOf(
        InspirationVideo(
            id = "ev_001",
            title = "Relaxing Nature Sounds - National Geographic",
            purpose = "Relaxation and grounding",
            category = ExploreCategory.NATURE,
            videoId = "ZVwENdOOjnM",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_002",
            title = "Around the World in 10 Minutes - National Geographic",
            purpose = "Connection with nature and exploration",
            category = ExploreCategory.NATURE,
            videoId = "RLfNgIDUnoE",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_003",
            title = "Nature and Mental Health",
            purpose = "Understanding the calming effect of nature",
            category = ExploreCategory.NATURE,
            videoId = "dylahsMIfh0",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_004",
            title = "The Healing Power of Nature",
            purpose = "Encourages time away from screens",
            category = ExploreCategory.NATURE,
            videoId = "xL6V142FmQo",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_005",
            title = "Nature, Healing and the Mind",
            purpose = "Reflection and mental reset",
            category = ExploreCategory.NATURE,
            videoId = "_vRMRBxvtZA",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_006",
            title = "Why We Travel - Pico Iyer",
            purpose = "Broadens perspective and curiosity",
            category = ExploreCategory.TRAVEL,
            videoId = "ylwZjlBIAaY",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_007",
            title = "The Transformative Power of Travel",
            purpose = "Encourages real-world experiences",
            category = ExploreCategory.TRAVEL,
            videoId = "WdEvs3KZFB4",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_008",
            title = "Travel as a Way of Learning",
            purpose = "Promotes learning beyond screens",
            category = ExploreCategory.TRAVEL,
            videoId = "S0TemlxiMdw",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_009",
            title = "Inspiring Travel Stories - National Geographic",
            purpose = "Inspires exploration",
            category = ExploreCategory.TRAVEL,
            videoId = "FWPZekeERi8",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_010",
            title = "Travel and Stepping Outside Your Comfort Zone",
            purpose = "Encourages trying new experiences",
            category = ExploreCategory.TRAVEL,
            videoId = "AZ1CBhgzTU0",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_011",
            title = "7-Minute Meditation - Yoga With Adriene",
            purpose = "Short guided relaxation",
            category = ExploreCategory.MINDFULNESS,
            videoId = "k0PSUDvLi8E",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_012",
            title = "The 6 Types of Meditation",
            purpose = "Introduces different mindfulness practices",
            category = ExploreCategory.MINDFULNESS,
            videoId = "Ht2Sa94qY7Y",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_013",
            title = "Mindfulness and Meditation",
            purpose = "Builds awareness and calm",
            category = ExploreCategory.MINDFULNESS,
            videoId = "5AqgMo1P05E",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_014",
            title = "How Mindfulness Changes the Brain",
            purpose = "Explains benefits of mindfulness",
            category = ExploreCategory.MINDFULNESS,
            videoId = "IeblJdB2-Vo",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_015",
            title = "Guided Breathing Meditation",
            purpose = "Helps students pause and reset",
            category = ExploreCategory.MINDFULNESS,
            videoId = "YFSc7Ck0Ao0",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_016",
            title = "Inside the Mind of a Master Procrastinator - Tim Urban",
            purpose = "Helps recognize the distraction loop",
            category = ExploreCategory.SELF_GROWTH,
            videoId = "arj7oStGLkU",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_017",
            title = "The Power of Believing You Can Improve - Carol Dweck",
            purpose = "Growth mindset",
            category = ExploreCategory.SELF_GROWTH,
            videoId = "_X0mgOOSpLU",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_018",
            title = "The Happiness Advantage - Shawn Achor",
            purpose = "Positive mindset and well-being",
            category = ExploreCategory.SELF_GROWTH,
            videoId = "fLJsdqxnZb0",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_019",
            title = "Your Body Language May Shape Who You Are - Amy Cuddy",
            purpose = "Confidence and self-development",
            category = ExploreCategory.SELF_GROWTH,
            videoId = "Ks-_Mh1QhMc",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_020",
            title = "How Great Leaders Inspire Action - Simon Sinek",
            purpose = "Purpose and motivation",
            category = ExploreCategory.SELF_GROWTH,
            videoId = "qp0HIF3SfI4",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_021",
            title = "The Meaning of Life - The School of Life",
            purpose = "Reflection on purpose and life",
            category = ExploreCategory.MEANINGFUL,
            videoId = "Ebt0X5ybm9Y",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_022",
            title = "The Meaning of Life in 60 Seconds",
            purpose = "Short reflective content",
            category = ExploreCategory.MEANINGFUL,
            videoId = "iUdhJ_S_z3w",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_023",
            title = "What Makes a Good Life? - Robert Waldinger",
            purpose = "Relationships and meaningful living",
            category = ExploreCategory.MEANINGFUL,
            videoId = "8KkKuTCFvzI",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_024",
            title = "What Really Matters in Life",
            purpose = "Encourages reflection on priorities",
            category = ExploreCategory.MEANINGFUL,
            videoId = "01GWGmg5jn8",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_025",
            title = "Gratitude - Louie Schwartzberg",
            purpose = "Gratitude and appreciation",
            category = ExploreCategory.MEANINGFUL,
            videoId = "gXDMoiEkyuQ",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_026",
            title = "The Last Lecture - Randy Pausch",
            purpose = "Purpose, resilience, and meaningful goals",
            category = ExploreCategory.INSPIRATION,
            videoId = "ji5_MqicxSo",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_027",
            title = "How Great Leaders Inspire Action - Simon Sinek",
            purpose = "Finding purpose and taking action",
            category = ExploreCategory.INSPIRATION,
            videoId = "qp0HIF3SfI4",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_028",
            title = "Grit: The Power of Passion and Perseverance - Angela Duckworth",
            purpose = "Persistence and long-term goals",
            category = ExploreCategory.INSPIRATION,
            videoId = "H14bBuluwB8",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_029",
            title = "Do Schools Kill Creativity? - Ken Robinson",
            purpose = "Encourages creativity and independent thinking",
            category = ExploreCategory.INSPIRATION,
            videoId = "iG9CE55wbtY",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_030",
            title = "Your Elusive Creative Genius - Elizabeth Gilbert",
            purpose = "Creative confidence and inspiration",
            category = ExploreCategory.INSPIRATION,
            videoId = "86x-u-tz0MA",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_031",
            title = "Break Your Phone Addiction - Matt D'Avella",
            purpose = "Directly addresses excessive phone use",
            category = ExploreCategory.DIGITAL_DETOX,
            videoId = "cV5GWt26qDo",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_032",
            title = "I Tried a 24 Hour Digital Detox",
            purpose = "Real-life digital detox experience",
            category = ExploreCategory.DIGITAL_DETOX,
            videoId = "1bNfZgU8yBQ",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_033",
            title = "Digital Detox and Mindset",
            purpose = "Encourages healthier digital habits",
            category = ExploreCategory.DIGITAL_DETOX,
            videoId = "o0xI45Vch0A",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_034",
            title = "Smartphone Addiction and Going Light",
            purpose = "Awareness of smartphone dependence",
            category = ExploreCategory.DIGITAL_DETOX,
            videoId = "9XlRu3GDzeA",
            searchQuery = null
        ),
        InspirationVideo(
            id = "ev_035",
            title = "Digital Minimalism - Reducing Phone Use",
            purpose = "Practical digital habit reduction",
            category = ExploreCategory.DIGITAL_DETOX,
            videoId = "5nemvG_EzqY",
            searchQuery = null
        ),
    )

    fun forCategory(category: ExploreCategory): List<InspirationVideo> =
        videos.filter { it.category == category }
}