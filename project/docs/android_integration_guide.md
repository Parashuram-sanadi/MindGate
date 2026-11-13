# Android Integration Guide
## Electronic Gadget Addiction Prediction API

---

## 1. Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- minSdkVersion 24 (Android 7.0)
- Kotlin 1.9+
- Gradle 8.x

---

## 2. Gradle Dependencies

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
}
```

Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 3. Data Models (Kotlin)

### Request Model

```kotlin
// data/model/PredictionRequest.kt
package com.yourapp.addiction.data.model

import com.google.gson.annotations.SerializedName

data class PredictionRequest(
    @SerializedName("screen_time")
    val screenTime: String,          // "<2","2-3","3-5","5-7","7-9","9+"

    @SerializedName("sleep_hours")
    val sleepHours: Float,           // 0.0 - 24.0

    @SerializedName("stress_level")
    val stressLevel: Int,            // 1 - 5

    @SerializedName("phone_checks")
    val phoneChecks: String,         // "1-2","3-5","6-10","10+"

    // Optional enriching fields
    @SerializedName("late_night_usage")
    val lateNightUsage: String? = null,

    @SerializedName("anxious_without_gadget")
    val anxiousWithoutGadget: String? = null,

    @SerializedName("emotional_coping")
    val emotionalCoping: String? = null,

    @SerializedName("usage_out_of_control")
    val usageOutOfControl: String? = null,

    @SerializedName("year_of_study")
    val yearOfStudy: String? = null,

    @SerializedName("gender")
    val gender: String? = null,

    // Gate overrides (set by backend logic only)
    @SerializedName("gate1_score_valid")
    val gate1ScoreValid: Boolean? = null,

    @SerializedName("gate2_danger_met")
    val gate2DangerMet: Boolean? = null,

    @SerializedName("gate3_loss_control")
    val gate3LossControl: Boolean? = null
)
```

### Response Model

```kotlin
// data/model/PredictionResponse.kt
package com.yourapp.addiction.data.model

import com.google.gson.annotations.SerializedName

data class PredictionResponse(
    @SerializedName("risk_level")
    val riskLevel: String,           // "Low" | "Moderate" | "High" | "Severe"

    @SerializedName("risk_score")
    val riskScore: Int,              // 0 - 100

    @SerializedName("confidence")
    val confidence: Float,           // 0.0 - 1.0

    @SerializedName("recommendation")
    val recommendation: String,

    @SerializedName("recommendations")
    val recommendations: List<String>,

    @SerializedName("intervention_level")
    val interventionLevel: String,

    @SerializedName("timestamp")
    val timestamp: String,

    @SerializedName("raw_prediction")
    val rawPrediction: String? = null,

    @SerializedName("downgraded")
    val downgraded: Boolean? = null,

    @SerializedName("model_version")
    val modelVersion: String = "1.0.0"
)

data class ApiError(
    val detail: String
)
```

---

## 4. Retrofit API Interface

```kotlin
// data/api/AddictionApiService.kt
package com.yourapp.addiction.data.api

import com.yourapp.addiction.data.model.PredictionRequest
import com.yourapp.addiction.data.model.PredictionResponse
import retrofit2.Response
import retrofit2.http.*

interface AddictionApiService {

    @POST("predict")
    suspend fun predict(
        @Body request: PredictionRequest
    ): Response<PredictionResponse>

    @GET("health")
    suspend fun health(): Response<Map<String, Any>>

    @GET("recommendations/{risk_level}")
    suspend fun getRecommendations(
        @Path("risk_level") riskLevel: String
    ): Response<Map<String, Any>>
}
```

---

## 5. Retrofit Client (Singleton)

```kotlin
// data/api/RetrofitClient.kt
package com.yourapp.addiction.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ⚠️ Change this to your production server URL
    private const val BASE_URL = "http://10.0.2.2:8000/"  // Android emulator → localhost

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: AddictionApiService by lazy {
        retrofit.create(AddictionApiService::class.java)
    }
}
```

---

## 6. Repository Layer

```kotlin
// data/repository/PredictionRepository.kt
package com.yourapp.addiction.data.repository

import com.yourapp.addiction.data.api.RetrofitClient
import com.yourapp.addiction.data.model.PredictionRequest
import com.yourapp.addiction.data.model.PredictionResponse

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class PredictionRepository {

    private val api = RetrofitClient.apiService

    suspend fun predict(request: PredictionRequest): Result<PredictionResponse> {
        return try {
            val response = api.predict(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response from server")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Result.Error(errorMsg, response.code())
            }
        } catch (e: java.net.UnknownHostException) {
            Result.Error("No internet connection. Please check your network.")
        } catch (e: java.net.SocketTimeoutException) {
            Result.Error("Request timed out. Please try again.")
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.localizedMessage}")
        }
    }
}
```

---

## 7. ViewModel

```kotlin
// ui/viewmodel/PredictionViewModel.kt
package com.yourapp.addiction.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.addiction.data.model.PredictionRequest
import com.yourapp.addiction.data.model.PredictionResponse
import com.yourapp.addiction.data.repository.PredictionRepository
import com.yourapp.addiction.data.repository.Result
import kotlinx.coroutines.launch

class PredictionViewModel : ViewModel() {

    private val repository = PredictionRepository()

    private val _predictionState = MutableLiveData<Result<PredictionResponse>>()
    val predictionState: LiveData<Result<PredictionResponse>> = _predictionState

    fun predict(
        screenTime: String,
        sleepHours: Float,
        stressLevel: Int,
        phoneChecks: String,
        lateNightUsage: String? = null,
        yearOfStudy: String? = null,
        gender: String? = null
    ) {
        _predictionState.value = Result.Loading

        val request = PredictionRequest(
            screenTime     = screenTime,
            sleepHours     = sleepHours,
            stressLevel    = stressLevel,
            phoneChecks    = phoneChecks,
            lateNightUsage = lateNightUsage,
            yearOfStudy    = yearOfStudy,
            gender         = gender
        )

        viewModelScope.launch {
            val result = repository.predict(request)
            _predictionState.postValue(result)
        }
    }
}
```

---

## 8. UI Fragment / Activity Example

```kotlin
// ui/fragment/PredictionFragment.kt
package com.yourapp.addiction.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.yourapp.addiction.R
import com.yourapp.addiction.data.repository.Result
import com.yourapp.addiction.ui.viewmodel.PredictionViewModel

class PredictionFragment : Fragment(R.layout.fragment_prediction) {

    private val viewModel: PredictionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinnerScreenTime    = view.findViewById<Spinner>(R.id.spinnerScreenTime)
        val spinnerPhoneChecks   = view.findViewById<Spinner>(R.id.spinnerPhoneChecks)
        val seekBarSleep         = view.findViewById<SeekBar>(R.id.seekBarSleep)
        val seekBarStress        = view.findViewById<SeekBar>(R.id.seekBarStress)
        val btnPredict           = view.findViewById<Button>(R.id.btnPredict)
        val progressBar          = view.findViewById<ProgressBar>(R.id.progressBar)
        val tvRiskLevel          = view.findViewById<TextView>(R.id.tvRiskLevel)
        val tvConfidence         = view.findViewById<TextView>(R.id.tvConfidence)
        val tvRecommendation     = view.findViewById<TextView>(R.id.tvRecommendation)
        val tvRiskScore          = view.findViewById<TextView>(R.id.tvRiskScore)

        // Spinners
        val screenTimeOptions = arrayOf("<2","2-3","3-5","5-7","7-9","9+")
        val phoneCheckOptions = arrayOf("1-2","3-5","6-10","10+")
        spinnerScreenTime.adapter  = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, screenTimeOptions)
        spinnerPhoneChecks.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, phoneCheckOptions)

        btnPredict.setOnClickListener {
            val screenTime  = spinnerScreenTime.selectedItem.toString()
            val phoneChecks = spinnerPhoneChecks.selectedItem.toString()
            val sleepHours  = (seekBarSleep.progress + 3).toFloat()   // 3–12 hrs
            val stressLevel = seekBarStress.progress + 1              // 1–5

            viewModel.predict(
                screenTime  = screenTime,
                sleepHours  = sleepHours,
                stressLevel = stressLevel,
                phoneChecks = phoneChecks
            )
        }

        // Observe state
        viewModel.predictionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Result.Loading -> {
                    progressBar.visibility  = View.VISIBLE
                    btnPredict.isEnabled    = false
                }
                is Result.Success -> {
                    progressBar.visibility  = View.GONE
                    btnPredict.isEnabled    = true
                    val response = state.data

                    tvRiskLevel.text     = "Risk Level: ${response.riskLevel}"
                    tvConfidence.text    = "Confidence: ${(response.confidence * 100).toInt()}%"
                    tvRiskScore.text     = "Score: ${response.riskScore}"
                    tvRecommendation.text = response.recommendation

                    // Color code the risk level
                    val color = when (response.riskLevel) {
                        "Low"      -> 0xFF2ECC71.toInt()
                        "Moderate" -> 0xFFF39C12.toInt()
                        "High"     -> 0xFFE74C3C.toInt()
                        "Severe"   -> 0xFF8E44AD.toInt()
                        else       -> 0xFF2ECC71.toInt()
                    }
                    tvRiskLevel.setTextColor(color)
                }
                is Result.Error -> {
                    progressBar.visibility = View.GONE
                    btnPredict.isEnabled   = true
                    Toast.makeText(
                        requireContext(),
                        "Error: ${state.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {}
            }
        }
    }
}
```

---

## 9. Network Security Config (Production)

Create `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">your-api-domain.com</domain>
    </domain-config>
    <!-- For development only -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="system"/>
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

Add to AndroidManifest.xml `<application>` tag:
```xml
android:networkSecurityConfig="@xml/network_security_config"
```

---

## 10. Error Handling Reference

| HTTP Code | Meaning | UI Action |
|-----------|---------|-----------|
| 200 | Success | Display results |
| 422 | Validation error | Show field-level error |
| 500 | Server error | Show retry button |
| Timeout | Network timeout | Show retry with delay |
| UnknownHost | No internet | Show network error dialog |

---

## 11. API Endpoint Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/predict` | Single prediction |
| POST | `/predict/batch` | Batch predictions (max 100) |
| GET | `/health` | Health check |
| GET | `/risk-levels` | Risk band info |
| GET | `/recommendations/{level}` | Recommendations for level |

---

## 12. Production Deployment URLs

Replace `10.0.2.2:8000` with:
- **Local device testing:** Your machine's LAN IP (e.g., `192.168.1.100:8000`)
- **Production:** Your HTTPS domain (e.g., `https://api.yourapp.com`)
- **Cloud:** AWS/GCP/Azure instance public IP with HTTPS

---

## 13. Sample cURL Test

```bash
curl -X POST "http://localhost:8000/predict" \
     -H "Content-Type: application/json" \
     -d '{
       "screen_time": "7-9",
       "sleep_hours": 5.0,
       "stress_level": 4,
       "phone_checks": "10+",
       "late_night_usage": "Always"
     }'
```

Expected response:
```json
{
  "risk_level": "High",
  "risk_score": 60,
  "confidence": 0.87,
  "recommendation": "Schedule specific gadget-free hours each day (at least 3 hours).",
  "recommendations": ["⚠️ Your gadget usage is significantly impacting your wellbeing.", "..."],
  "intervention_level": "Intervention",
  "timestamp": "2024-01-15T10:30:00Z",
  "model_version": "1.0.0"
}
```
