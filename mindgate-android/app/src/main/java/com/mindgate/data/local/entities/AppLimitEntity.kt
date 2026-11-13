package com.mindgate.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_limits",
    indices = [Index(value = ["userId", "packageName"], unique = true)]
)
data class AppLimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val isEnabled: Boolean = true,
    val suggestedLimit: Int,
    val isBlocked: Boolean = false,
    val extensionUntil: Long = 0L,
    
    // Scheduling
    val isCustomSchedule: Boolean = false,
    val monLimit: Int? = null,
    val tueLimit: Int? = null,
    val wedLimit: Int? = null,
    val thuLimit: Int? = null,
    val friLimit: Int? = null,
    val satLimit: Int? = null,
    val sunLimit: Int? = null
) {
    fun getEffectiveLimitMinutes(): Int {
        if (!isCustomSchedule) return dailyLimitMinutes
        
        val dayOfWeek = java.time.LocalDate.now().dayOfWeek.value // 1 (Mon) to 7 (Sun)
        return when (dayOfWeek) {
            1 -> monLimit ?: dailyLimitMinutes
            2 -> tueLimit ?: dailyLimitMinutes
            3 -> wedLimit ?: dailyLimitMinutes
            4 -> thuLimit ?: dailyLimitMinutes
            5 -> friLimit ?: dailyLimitMinutes
            6 -> satLimit ?: dailyLimitMinutes
            7 -> sunLimit ?: dailyLimitMinutes
            else -> dailyLimitMinutes
        }
    }
}
