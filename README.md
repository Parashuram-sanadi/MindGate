# MindGate: AI-Driven Digital Wellbeing and Gadget Addiction Regulation Ecosystem

![Android](https://img.shields.io/badge/Android-Kotlin-green?style=flat-square&logo=android)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose)
![Backend](https://img.shields.io/badge/Backend-FastAPI-009688?style=flat-square&logo=fastapi)
![ML Engine](https://img.shields.io/badge/ML-CatBoost-FF6F00?style=flat-square&logo=python)
![Database](https://img.shields.io/badge/Storage-Room%20%7C%20SQLite-003B57?style=flat-square&logo=sqlite)
![Firebase](https://img.shields.io/badge/Realtime-Firestore%20%7C%20FCM-FFCA28?style=flat-square&logo=firebase)

> MindGate is an intelligent, multi-role digital wellbeing ecosystem designed to predict, monitor, and regulate electronic gadget overuse among students using Machine Learning, real-time Android accessibility enforcement, and a collaborative parent-student regulatory framework.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Problem Statement](#2-problem-statement)
3. [Project Objectives](#3-project-objectives)
4. [Key Features](#4-key-features)
5. [System Architecture](#5-system-architecture)
6. [Overall Workflow](#6-overall-workflow)
7. [Machine Learning Methodology](#7-machine-learning-methodology)
8. [Dataset and Preprocessing](#8-dataset-and-preprocessing)
9. [Model Comparison and Evaluation](#9-model-comparison-and-evaluation)
10. [Explainable AI Integration](#10-explainable-ai-integration)
11. [Android Application Modules](#11-android-application-modules)
12. [Parent, Student, and Teacher Workflows](#12-parent-student-and-teacher-workflows)
13. [Backend Architecture](#13-backend-architecture)
14. [Firebase Integration](#14-firebase-integration)
15. [Technology Stack](#15-technology-stack)
16. [Project Directory Structure](#16-project-directory-structure)
17. [Application Screenshots](#17-application-screenshots)
18. [Installation and Setup Instructions](#18-installation-and-setup-instructions)
19. [Running the Android Application](#19-running-the-android-application)
20. [Running the Python Backend](#20-running-the-python-backend)
21. [API Documentation](#21-api-documentation)
22. [Security and Privacy](#22-security-and-privacy)
23. [Limitations](#23-limitations)
24. [Future Enhancements](#24-future-enhancements)
25. [Contributors](#25-contributors)
26. [Acknowledgements](#26-acknowledgements)
27. [License](#27-license)

---

## 1. Project Overview

Electronic gadget addiction and excessive screen time pose severe risks to students' physical health, academic performance, and emotional stability. Existing screen time management tools act primarily as passive counters or rigid blockers that users can easily bypass.

MindGate addresses this challenge by providing an end-to-end digital wellbeing ecosystem powered by Artificial Intelligence and Machine Learning. The system continuously evaluates behavioral survey indicators, calculates personalized risk profiles (**Low**, **Moderate**, **High**, **Severe**), enforces intelligent app restrictions using Android background services, and enables a collaborative parental approval loop for extended screen time.

---

## 2. Problem Statement

Unregulated smartphone usage among teenagers and university students leads to:

* **Sleep Disruption & Physical Strain**: Late-night screen exposure suppresses melatonin and causes eye strain, headaches, and physical posture issues.
* **Academic Decline**: Habitual phone checking during study hours and missed lectures lead to reduced cognitive focus and lower academic performance.
* **Loss of Self-Regulation**: Traditional app blockers lack adaptive mechanisms, leading students to disable enforcement tools entirely.
* **Lack of Parental Visibility**: Parents lack real-time visibility into their children's digital habits and face friction when granting earned time extensions.

---

## 3. Project Objectives

1. **AI-Based Addiction Risk Prediction**: Classify student addiction risk using a trained CatBoost Gradient Boosting classifier evaluated on behavioral indicators.
2. **Clinical Danger-Gate Validation**: Prevent false-alarm Severe classifications through a three-gate clinical validation safety check.
3. **Adaptive Real-Time Enforcement**: Enforce local app limits on Android devices using a background Accessibility Watchdog Service and focus lock overlay.
4. **Self-Regulation & Earned Extensions**: Empower students to earn extra screen time by completing cognitive Wellness Challenges (Math/Trivia puzzles).
5. **Real-Time Collaborative Oversight**: Connect students and parents via a secure, code-based handshake (`MG-XXXXXX`), enabling instant Firestore-based extension requests and FCM notifications.
6. **Cohort Analytics for Educators**: Provide teachers and academic counselors with a dedicated portal for bulk student dataset analysis (up to 2,000 records) and class-wide risk distribution.

---

## 4. Key Features

### Student Portal
* **Science-Backed Risk Assessment**: Interactive survey mapping physical symptoms, study impact, compulsiveness, and sleep disruption.
* **Dynamic Screen Time Ring**: Real-time daily usage visualization with proportional top-app consumption progress bars.
* **Intelligent App Restrictions**: Set daily time limits per package name with optional bedtime curfew locks.
* **Wellness Challenges**: Solve math or trivia puzzles to self-earn 15-minute time grants.
* **Collaborative Time Requests**: Request extra time directly from a linked parent with live status tracking (`PENDING`, `APPROVED`, `DENIED`, `CONSUMED`).

### Parent Portal
* **Cross-Platform Pairing Handshake**: Link child accounts using an 8-character pairing code (`MG-XXXXXX`) with real-time Firestore sync.
* **Child Usage Analytics**: Live monitoring of daily screen time, top restricted apps, and active extensions.
* **Instant Push Approvals**: Receive high-priority FCM notifications and approve or deny extension requests in under 500ms.
* **Remote Limit Adjustment**: Update child app curfews and daily caps remotely.

### Teacher & Counselor Portal
* **Bulk Dataset Upload**: Analyze group datasets (CSV/XLSX up to 2,000 records) for class-wide addiction risk analysis.
* **Class Risk Visualizer**: Stacked risk distribution bar charts (Low, Moderate, High, Severe) for cohort evaluation.
* **Student Risk Breakdown**: Detailed individual risk scores, sub-score bars (Compulsion, Physical, Academic, Control), and SHAP feature drivers.
* **CSV Template Generator**: Download standardized dataset collection templates directly from the app.

---

## 5. System Architecture

MindGate follows a 3-tier distributed client-server architecture:

```
+-------------------------------------------------------------------------+
|                         PRESENTATION LAYER                              |
|                       (Android Application)                             |
|  - Jetpack Compose (Material 3)    - Room DB (Local Offline Cache)      |
|  - Accessibility Watchdog Service   - Hilt Dependency Injection         |
+------------------------------------+------------------------------------+
                                     |
                         HTTP / REST | JSON (Retrofit 2)
                                     v
+-------------------------------------------------------------------------+
|                       INTELLIGENCE LAYER                                |
|                        (FastAPI Backend)                                |
|  - FastAPI (Async Python 3.12)      - CatBoost ML Classifier            |
|  - Risk Engine (Danger-Gate Logic) - SHAP Explainability Engine         |
+-------------------+--------------------------------+--------------------+
                    |                                |
        SQLAlchemy  |                                | Firebase Admin
        ORM         v                                v SDK
+-------------------+----------------+ +----------------------------------+
|          PERSISTENT DATA           | |       REAL-TIME SYNC LAYER       |
|    SQLite / PostgreSQL Database    | |   Firebase Auth & Cloud Firestore |
|  - User Credentials & Hashed Pass  | |  - Parent-Child Live Links       |
|  - Parent-Child Pairing Records    | |  - Real-time Extension Requests  |
|  - Usage & Assessment Records      | |  - FCM Push Notifications        |
+------------------------------------+ +----------------------------------+
```

---

## 6. Overall Workflow

### 1. Registration & Custom Token Identity Alignment
```
[User App] ---> (POST /api/auth/register) ---> [FastAPI Backend]
                                                       |
                                            Create SQL User Record
                                                       |
                                         Generate Firebase Custom Token
                                            (UID = Backend User ID)
                                                       v
[User App] <--- (Returns JWT + Custom Token) <--- [FastAPI Backend]
    |
Sign into Firebase Auth with Custom Token
(Guarantees SQL ID 108 == Firebase UID 108)
```

### 2. Parent-Student Pairing Handshake
```
[Student Device]                              [Parent Device]
       |                                             |
Generates 8-char code                                |
(POST /api/parent/link/create)                       |
       |                                             |
Shows "MG-7K4P9X" code --------------------> Enters "MG-7K4P9X" code
                                            (POST /api/parent/link/accept)
                                                     |
                                           Establishes Link in SQL &
                                           Writes 'parentChildLinks'
                                           Document in Firestore
                                                     v
[Student Device] <======= (Real-time Sync) ======> [Parent Device]
```

### 3. Real-Time App Blocking & Extension Lifecycle
1. **Foreground Monitoring**: The `MindGateAccessibilityService` detects active application packages.
2. **Limit Evaluation**: When usage exceeds the configured daily limit, `BlockedOverlayActivity` is launched as a focus lock overlay.
3. **Extension Choice**: The student chooses between a **Wellness Challenge** (Math/Trivia) or an **Ask Parent** request.
4. **Approval Propagation**: Parent receives an FCM push notification, approves the request in Firestore, and the student's device automatically unblocks in under 500ms.

---

## 7. Machine Learning Methodology

The machine learning pipeline predicts gadget addiction risk based on user survey data:

1. **Model Selection**: CatBoost Classifier was selected as the core production model due to its accuracy on categorical survey data.
2. **Cross-Validation**: 5-fold Stratified Cross-Validation was used to evaluate generalization performance.
3. **Class Balancing**: Synthetic Minority Over-sampling Technique (SMOTE) was applied inside cross-validation folds.
4. **Clinical Danger-Gate Validation**: Predicted Severe risk classifications undergo three mandatory checks:
   * `gate1_score_valid`: Computed score threshold met (`≥ 65`).
   * `gate2_danger_met`: Physical and behavioral danger markers present.
   * `gate3_loss_control`: Explicit loss-of-control marker confirmed.
   * *If any gate fails, the risk level is automatically downgraded to High to prevent false Severe alarms.*

---

## 8. Dataset and Preprocessing

* **Dataset Size**: 396 survey samples with 64 raw columns.
* **Behavioral Features**: 44 features selected after target-derived data leakage removal.
* **Target Classes**: `Low`, `Moderate`, `High`, `Severe`.
* **Preprocessing Pipeline**:
  * **Ordinal Mapping**: Converting usage bands to numerical scales (`<2` → 0, `2-3` → 1, ..., `9+` → 5).
  * **Frequency Encoding**: Mapping response choices (`Never` → 0, ..., `Always` → 5).
  * **Leakage Removal**: Excluding target-derived fields (`score_usage_compulsion`, `danger_marker_count`) during model training.

---

## 9. Model Comparison and Evaluation

Eleven machine learning classifiers were evaluated using 5-fold Stratified Cross-Validation:

| Rank | Model | 5-Fold CV Accuracy | 5-Fold CV F1 Score | Test Set Accuracy | Test Set F1 Score | Test ROC-AUC (OvR) |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: |
| **1** | **CatBoost** | **0.9759** | **0.9758** | **0.9250** | **0.8969** | **0.9916** |
| 2 | Extra Trees | 0.9650 | 0.9650 | 0.9250 | 0.8917 | 0.9872 |
| 3 | LightGBM | 0.9649 | 0.9649 | 0.9000 | 0.8465 | 0.9798 |
| 4 | Random Forest | 0.9650 | 0.9648 | 0.9125 | 0.8556 | 0.9875 |
| 5 | SVM (RBF) | 0.9627 | 0.9626 | 0.9625 | 0.9478 | 0.9890 |

CatBoost achieved the highest overall cross-validation F1-score (**0.9758**) and was deployed in production.

---

## 10. Explainable AI Integration

To provide transparency, MindGate integrates **SHAP (SHapley Additive exPlanations)** using `shap.TreeExplainer`.

Top contributing risk factors identified across the dataset:
1. `sleep_disturbances`
2. `late_night_usage`
3. `Q28_PhysicalSymptoms_count`
4. `daily_screen_time_hours`
5. `Q13_AnxiousWithoutGadget`

SHAP feature drivers are returned by the backend API and displayed to users and educators to explain individual risk scores.

---

## 11. Android Application Modules

The Android application (`com.mindgate`) is structured into clean modular packages:

```
com.mindgate/
├── data/
│   ├── local/          # Room DB (Entities, DAOs) & DataStore Preferences
│   ├── remote/         # Retrofit API Service, DTOs & Network Handlers
│   └── repository/     # Repository Implementations (User, Usage, Parent, Firestore)
├── domain/
│   ├── manager/        # PolicyEngine & ExtensionManager
│   └── model/          # Pure Domain Data Models
├── presentation/
│   ├── navigation/     # NavGraph & RoleGuard Authorization
│   ├── ui/             # Jetpack Compose Screens (Auth, Home, Parent, Teacher, Blocked, Quiz)
│   └── theme/          # Material 3 Styling Tokens (Colors, Typography, Motion)
├── parent/             # Accessibility Service & Watchdog Receivers
├── service/            # Foreground Usage Monitor & WorkManager Jobs
└── util/               # Assessment Mapper, UsageStatsHelper & Logging Utilities
```

---

## 12. Parent, Student, and Teacher Workflows

### Student Workflow
1. **Onboarding & Auth**: Register/Login and complete local profile setup.
2. **Assessment**: Complete the 5-step survey to receive risk classification and custom limits.
3. **Usage & Enforcement**: UsageStats tracks time. When limits are exceeded, Accessibility Service launches the block overlay.
4. **Time Extensions**: Solve a Math/Trivia puzzle or send an extension request to a linked parent.

### Parent Workflow
1. **Pairing**: Enter the 8-character pairing code (`MG-XXXXXX`) generated by the student.
2. **Monitoring**: View child's daily screen time ring, top app usage breakdown, and risk status.
3. **Approvals**: Receive real-time push notifications for extension requests and respond with one tap.

### Teacher Workflow
1. **Authentication**: Sign in using teacher credentials.
2. **Batch Upload**: Upload CSV or XLSX spreadsheets containing up to 2,000 student records.
3. **Analytics**: Review class-wide risk distribution charts and inspect individual student SHAP drivers.

---

## 13. Backend Architecture

The backend is built with **FastAPI** and **Uvicorn**, providing asynchronous RESTful endpoints:

* **API Routes (`api/main.py`)**: Endpoints for authentication, risk inference, parent linking, and notifications.
* **Authentication (`api/auth.py`)**: JWT generation and password hashing (`pbkdf2_sha256`).
* **Database Layer (`api/models.py`)**: SQLAlchemy ORM mapping for SQLite/PostgreSQL.
* **Model Service (`api/model_service.py`)**: Singleton wrapper for CatBoost inference and SHAP calculation.
* **Firebase Utilities (`api/firebase_utils.py`)**: Firebase Admin SDK integration for Custom Token generation and FCM push notifications.

---

## 14. Firebase Integration

MindGate uses **Firebase Custom Token Alignment** to synchronize relational database user IDs with Firebase UIDs:

```
1. User authenticates via FastAPI -> Receives SQL User ID (e.g. 108).
2. Backend generates a Firebase Custom Token signed with UID = "108".
3. Mobile app calls FirebaseAuth.getInstance().signInWithCustomToken(token).
4. Firebase Auth UID becomes "108", aligning Firestore document paths directly with SQL primary keys.
```

### Cloud Firestore Collections
* **`extension_requests/{requestId}`**: Real-time extension requests (`studentUid`, `parentUid`, `appPackage`, `status`, `requestedMinutes`).
* **`parentChildLinks/{linkId}`**: Parent-student pairing records.
* **`deviceTokens/{userId}`**: FCM registration tokens for targeted push notifications.
* **`extensionLedger/{ledgerId}`**: Audit trail of approved time grants.

---

## 15. Technology Stack

| Component | Technology |
| :--- | :--- |
| **Android Language** | Kotlin 1.9.22 |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM + Clean Architecture |
| **Dependency Injection** | Hilt 2.50 |
| **Local Database** | Room DB v14 |
| **Preferences** | DataStore Preferences |
| **Networking** | Retrofit 2.9.0 + OkHttp 4.12.0 |
| **Backend Framework** | FastAPI 1.0.0 (Python 3.12) |
| **ASGI Server** | Uvicorn |
| **Machine Learning** | CatBoost Classifier 1.2.x |
| **ML Tools** | scikit-learn, pandas, numpy, Optuna, SMOTE |
| **Explainable AI** | SHAP (TreeExplainer) |
| **Cloud Services** | Firebase Auth (Custom Tokens), Cloud Firestore, FCM V1 API |
| **Relational DB** | SQLAlchemy (SQLite / PostgreSQL) |

---

## 16. Project Directory Structure

```
gadget_addiction_project/
├── mindgate-android/                 # Android Jetpack Compose Application
│   ├── app/
│   │   ├── src/main/java/com/mindgate/
│   │   │   ├── data/                 # Room DB, DataStore, Retrofit APIs & Repositories
│   │   │   ├── domain/               # Domain logic (PolicyEngine, ExtensionManager)
│   │   │   ├── presentation/         # Compose UI, Screens & ViewModels
│   │   │   ├── parent/               # Accessibility Watchdog & Approval Receivers
│   │   │   ├── service/              # Foreground Usage Monitor & WorkManager Jobs
│   │   │   └── util/                 # Assessment Mappers & Helpers
│   │   ├── build.gradle.kts
│   │   ├── google-services.json
│   │   └── firestore.rules           # Cloud Firestore Security Rules
│   └── build.gradle.kts
│
├── project/                          # FastAPI Backend & ML Pipeline
│   ├── api/                          # FastAPI Endpoints, Auth & Schemas
│   ├── src/                          # Preprocessing & Model Training Scripts
│   ├── models/                       # Serialized Artifacts (.pkl & .json)
│   ├── data/                         # Survey Dataset
│   ├── reports/                      # ML Metrics & Evaluation Reports
│   ├── requirements.txt              # Python Dependencies
│   ├── main.py                       # CLI Entry Point (train / serve)
│   ├── risk_engine.py                # Clinical Danger-Gate Logic
│   └── recommendation_engine.py       # Wellness Suggestions Engine
│
├── docs/                             # Documentation & Reference Media
│   └── screenshots/                  # Screenshot Assets
├── arc.md                            # Architecture Specification
├── compl.md                          # Technical System Overview
├── MINDGATE_PROJECT_GUIDE.md         # Developer Architecture Guide
└── README.md                         # Project Root Documentation
```

---

## 17. Application Screenshots

### 1. System Architecture & Machine Learning Pipeline

| Model Comparison Output | Cloud Firestore Security Rules |
| :---: | :---: |
| ![Model Comparison Output](docs/screenshots/model-comparison.jpg) | ![Firestore Security Rules](docs/screenshots/firestore-security-rules.jpg) |
| *Figure 1: Candidate model metrics & CatBoost selection.* | *Figure 2: Security rules published in Firebase console.* |

---

### 2. Onboarding & Permissions Setup

| Onboarding & Role Selection | Permission Setup Screen |
| :---: | :---: |
| ![Onboarding Role Selection](docs/screenshots/onboarding-role-selection.jpg) | ![Permission Setup Screen](docs/screenshots/permission-setup.jpg) |
| *Figure 3: Student and Parent portal onboarding entry.* | *Figure 4: Usage, Accessibility, and Notification permissions granted.* |

---

### 3. Student Portal & AI Risk Assessment

| Self-Assessment Survey | AI Risk Analysis Result |
| :---: | :---: |
| ![Self Assessment Survey](docs/screenshots/self-assessment-survey.jpg) | ![Risk Analysis Result](docs/screenshots/risk-analysis-result.jpg) |
| *Figure 5: Science-backed survey on a 1-5 scale.* | *Figure 6: HIGH risk (64/100), SHAP drivers, & personalized actions.* |

| Student Usage Dashboard | Explore & Recharge | Growth Journey Screen |
| :---: | :---: | :---: |
| ![Student Usage Dashboard](docs/screenshots/student-usage-dashboard.jpg) | ![Explore and Recharge](docs/screenshots/explore-and-recharge.jpg) | ![Growth Journey Screen](docs/screenshots/growth-journey.jpg) |
| *Figure 7: Total screen time ring & top app usage.* | *Figure 8: Curated wellness videos by category.* | *Figure 9: Risk trend, time reclaimed, & streak.* |

---

### 4. Device Pairing & Parental Oversight

| Parent Link Code | Parent Monitoring Dashboard |
| :---: | :---: |
| ![Parent Link Code](docs/screenshots/parent-link-code.jpg) | ![Parent Monitoring Dashboard](docs/screenshots/parent-monitoring-dashboard.jpg) |
| *Figure 10: `MG-XXXXXX` pairing code with 3-min timer.* | *Figure 15: Pending request, approve/deny activity, & child card.* |

---

### 5. App Restrictions & Self-Regulation Challenges

| Focus Lock Overlay | Wellness Quiz Challenge |
| :---: | :---: |
| ![Usage Limit Overlay](docs/screenshots/usage-limit-overlay.jpg) | ![Wellness Quiz Challenge](docs/screenshots/wellness-quiz-challenge.jpg) |
| *Figure 11: Hard block with quiz & ask-parent options.* | *Figure 12: Timed quiz to earn 15-min extension.* |

| Extension Unlocked | Awaiting Parent Approval |
| :---: | :---: |
| ![Extension Unlocked Screen](docs/screenshots/extension-unlocked.jpg) | ![Awaiting Parent Approval](docs/screenshots/awaiting-parent-approval.jpg) |
| *Figure 13: Countdown after extension grant.* | *Figure 14: Blocked screen awaiting parent decision.* |

---

### 6. Educator Portal & Cohort Analytics

| Teacher Dataset Upload | Class Risk Summary |
| :---: | :---: |
| ![Teacher Dataset Upload](docs/screenshots/teacher-dataset-upload.jpg) | ![Class Risk Summary](docs/screenshots/class-risk-summary.jpg) |
| *Figure 16: Bulk spreadsheet upload & CSV format guide.* | *Figure 17: 396 students, risk distribution donut, & roster list.* |

---

## 18. Installation and Setup Instructions

### Prerequisites
* **JDK**: Java 17
* **Android Studio**: Jellyfish | 2023.3.1 or newer
* **Android SDK**: Target SDK 35 (Min SDK 26)
* **Python**: Python 3.10+ (Python 3.12 recommended)

---

## 19. Running the Android Application

1. Open Android Studio.
2. Select **Open** and navigate to `D:/MOVIES/gadget_addiction_project/mindgate-android`.
3. Allow Gradle sync to complete.
4. Ensure `google-services.json` is present in `app/google-services.json`.
5. Select the `app` run configuration and click **Run** on an emulator or physical device.
6. Grant required permissions after launch:
   * **Usage Access** (`Settings -> Special App Access -> Usage Access`)
   * **Accessibility Service** (`Settings -> Accessibility -> MindGate`)
   * **Display over other apps** (Overlay permission)

*Note for physical devices: Update `API_BASE_URL` via the in-app Connection Settings screen (gear icon) to match your PC's local IP address.*

---

## 20. Running the Python Backend

1. Navigate to the backend directory:
   ```bash
   cd project
   ```

2. Create and activate a Python virtual environment:
   ```bash
   python -m venv venv
   # Windows PowerShell
   .\venv\Scripts\Activate.ps1
   # Linux / macOS
   source venv/bin/activate
   ```

3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Train or verify ML model artifacts:
   ```bash
   python main.py train
   ```

5. Start the FastAPI server:
   ```bash
   python main.py serve
   ```
   *The backend starts on `http://0.0.0.0:8000`. Interactive API documentation is available at `http://localhost:8000/docs`.*

---

## 21. API Documentation

Key backend REST API endpoints:

| Method | Endpoint | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/health` | Public | Server health status and model load check. |
| `POST` | `/token` | Public | User authentication & JWT token generation. |
| `POST` | `/api/auth/register` | Public | User account registration & initial role assignment. |
| `POST` | `/api/auth/google` | Public | Google ID Token verification & sign-in. |
| `POST` | `/api/auth/google/complete` | Public | Complete role selection for Google accounts. |
| `POST` | `/predict` | Authenticated | Single student addiction risk prediction. |
| `POST` | `/predict/batch/file` | Teacher | Bulk CSV/XLSX dataset processing for educators. |
| `POST` | `/api/parent/link/create` | Student | Generate 8-character pairing code (`MG-XXXXXX`). |
| `POST` | `/api/parent/link/accept` | Parent | Accept student code & establish active relationship. |
| `GET` | `/api/user/connection-status` | Authenticated | Fetch active parent-child link status and Firebase UIDs. |
| `POST` | `/api/parent/notify-request` | Student | Trigger FCM push notification to parent devices. |

---

## 22. Security and Privacy

* **Secret Management**: Environment variables (`.env`) manage secrets (`JWT_SECRET`). `.env` and `firebase-service-account.json` are excluded from version control.
* **Database Isolation**: SQLite database instances (`*.db`) containing user data are excluded from version control.
* **Firestore Security Rules**: Rules enforce `request.auth != null` and restrict document reads/writes strictly to authorized `studentUid` or `parentUid` holders.
* **Privacy Guardrails**: Explicit data schemas ensure parents view aggregate usage statistics and extension requests without exposing raw assessment survey answers.

---

## 23. Limitations

* **Accessibility Service Dependency**: Android operating systems may stop background accessibility services if battery optimization exemption is not granted.
* **Local Network Setup**: Physical test devices require local IP configuration on the same Wi-Fi network unless deployed to a public cloud instance.
* **Batch Dataset Limit**: Educator bulk upload processing is optimized for up to 2,000 records per file.

---

## 24. Future Enhancements

* **On-Device ML Inference**: Convert the CatBoost model to TensorFlow Lite or ONNX for 100% offline risk classification.
* **Wearable Sensor Integration**: Incorporate heart rate variability (HRV) and sleep tracking from smartwatches for objective physical health inputs.
* **Cross-Platform Parent App**: Develop a Flutter or React Native version of the Parent Dashboard for iOS support.

---

## 25. Contributors

All members are equal contributors to the MindGate project:

* **Arpita Pradhane**
* **Mouneshwar Sutar**
* **Parasuram Sanadi**
* **Pratiksha Pawar**

---

## 26. Acknowledgements

* **FastAPI & Uvicorn** for high-performance asynchronous Python API infrastructure.
* **CatBoost & SHAP** for machine learning classification and model explainability.
* **Jetpack Compose Team** for modern declarative Android UI development tools.
* **Firebase Team** for real-time Firestore database and FCM notification services.

---

## 27. License

*Proprietary / Educational Project. Final Year Engineering Project.*
