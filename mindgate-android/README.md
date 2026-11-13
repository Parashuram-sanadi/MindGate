# MindGate Android App

Digital wellness Android client for the **Gadget Addiction Prediction** FastAPI backend.

## Open in Android Studio

1. Open Android Studio → **File → Open** → select `mindgate-android/`
2. Let Gradle sync complete
3. Start the Python API: `cd project && python main.py serve`
4. Run the app on an emulator (API base URL defaults to `http://10.0.2.2:8000/`)

## Features

### 👨‍🎓 Student Portal
- **Onboarding & Local Auth**: Secure local login and profile setup (Room DB).
- **34-Question Assessment**: Detailed survey matching the ML model's feature set.
- **Real-time Prediction**: Retrofit integration with the `/predict` endpoint for instant risk assessment.
- **Personalized Recommendations**: Digital wellness tips based on specific risk factors.
- **App Limits & Blocking**: UsageStats monitoring with Accessibility service for active app blocking.
- **Puzzle Extension**: Solve quick math/quiz puzzles for 15-minute usage extensions.
- **Background Sync**: WorkManager integration for usage data persistence and daily resets.

### 👨‍🏫 Teacher Dashboard
- **Teacher Login**: Secure dashboard access for academic counselors and teachers.
- **Batch Classification**: Upload CSV or Excel datasets (up to 2,000 rows) for group-wide analysis.
- **Class Analysis**: Overview of risk distribution across a whole class or department.
- **Individual Student Risks**: Detailed breakdown for each student in the uploaded file, including top contributing factors (SHAP values).
- **Template Download**: In-app CSV template generation for easy data collection.

## Permissions

After activating limits, grant:

1. **Usage access** (Settings → Special app access)
2. **Accessibility** (MindGate service)
3. **Display over other apps** (for block overlay)

## Project structure

```
com.mindgate/
  data/          Room + Retrofit + repositories
  domain/        models + use cases
  presentation/  Compose UI + navigation + ViewModels
  service/       Accessibility, WorkManager, receivers
  di/            Hilt modules for dependency injection
```

## Backend coupling

The Android client is tightly coupled with the **FastAPI ML Backend**.
- Prediction request/response fields match `project/api/schemas.py`. 
- Ensure the model is trained (`python main.py train`) before starting the server.
- The app uses `BuildConfig.API_BASE_URL` for network calls. Change this in `build.gradle.kts` if testing on a physical device.

---
*MindGate - Empowerment through Digital Balance.*
