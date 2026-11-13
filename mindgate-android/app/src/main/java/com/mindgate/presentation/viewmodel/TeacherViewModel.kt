package com.mindgate.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindgate.data.local.SessionManager
import com.mindgate.data.remote.dto.BatchFileResponse
import com.mindgate.data.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject

@HiltViewModel
class TeacherViewModel @Inject constructor(
    private val repository: TeacherRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    val requiredColumns = listOf(
        "Q1_AgeGroup", "Q2_Gender", "Q3_YearOfStudy", "Q4_Department", "Q5_Residence",
        "Q6_PrimaryGadget", "Q7_ScreenTime", "Q8_GadgetUses", "Q9_SocialPlatforms",
        "Q11_PhoneChecks_PerHr", "Q15_GadgetFreeDay", "sleep_hours", "Q23_SleepDuration",
        "Q24_GadgetBefore30minSleep", "Q25_WakesForPhone", "Q26_SleepQuality",
        "Q27_ExercisePerWeek", "Q28_PhysicalSymptoms", "Q29_SkipsMeals", "Q16_CGPA",
        "Q17_ClassesMissed", "Q18_GadgetDistractsStudy", "Q19_SelfStudyHrs",
        "Q20_ProductivityRating", "Q21_GadgetHelpsAcademic", "Q22_AssignmentsOnTime",
        "Q13_AnxiousWithoutGadget", "Q30_StressLevel", "Q12_PhantomCheckUrge",
        "Q33_EmotionalCoping", "Q34_UsageOutOfControl", "Q37_SelfLabel",
        "Q38_SatisfactionRating", "Q36_TriedToReduce", "Q10_LateNightUsage",
        "Q14_GadgetAtMeals", "Q31_SocialInteractionQuality", "Q32_PrefersOnline",
        "Q35_FeltNeedToReduce"
    )

    private val _uiState = MutableStateFlow<TeacherUiState>(TeacherUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _results = MutableStateFlow<BatchFileResponse?>(null)
    val results = _results.asStateFlow()

    fun uploadFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = TeacherUiState.Loading
            try {
                val file = uriToFile(context, uri)
                val result = repository.uploadBatchFile(file)
                result.onSuccess {
                    _results.value = it
                    _uiState.value = TeacherUiState.Success
                }.onFailure {
                    _uiState.value = TeacherUiState.Error(it.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _uiState.value = TeacherUiState.Error(e.message ?: "File error")
            }
        }
    }

    fun downloadTemplate(context: Context): File? {
        return try {
            val file = File(context.cacheDir, "mindgate_template.csv")
            val writer = OutputStreamWriter(FileOutputStream(file))
            writer.write(requiredColumns.joinToString(","))
            writer.write("\n")
            // Sample row with realistic values
            writer.write("19-21,Male,2nd Year,CSE,Hostel,Smartphone,5-7,\"Social Media,Gaming\",4-5,6-10,Sometimes,6.5,6-7,Often,Rarely,3,1-3,\"Eye strain,Headaches\",Sometimes,7-8,0-2,3,2-3,3,3,Usually,3,3,Sometimes,3,3,No,3,Tried and failed,Often,Sometimes,3,Sometimes,Yes sometimes")
            writer.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File {
        val extension = when (context.contentResolver.getType(uri)) {
            "text/csv" -> "csv"
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
            "application/vnd.ms-excel" -> "xls"
            else -> "csv"
        }
        
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$extension")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }
}

sealed class TeacherUiState {
    data object Idle : TeacherUiState()
    data object Loading : TeacherUiState()
    data object Success : TeacherUiState()
    data class Error(val message: String) : TeacherUiState()
}
