# MindGate System Comprehensive Summary

MindGate is an advanced digital-wellbeing platform designed to mitigate gadget addiction through real-time monitoring, risk-adaptive restriction, and a multi-role collaborative ecosystem (Student, Parent, Teacher).

---

## 1. System Architecture

### **Core Stack**
-   **Android App**: Jetpack Compose, Hilt (DI), Room (Local DB), DataStore (Session), Retrofit (Networking), Firebase SDK.
-   **Backend**: FastAPI (Python), SQLAlchemy (SQL Lite/Postgres), Pydantic.
-   **Firebase**: Authentication (Email/Google), Cloud Firestore (Real-time sync), Cloud Messaging (FCM).

### **Authentication & Identity Flow**
1.  **Backend Entry**: Users log in via FastAPI to receive a standard JWT and a Firebase Custom Token.
2.  **Unified UID**: The system enforces `Firebase UID == Backend User ID (String)`.
3.  **Session Restoration**: The app performs a clean `signOut()` followed by `signInWithCustomToken()` on every login to ensure session alignment across SQL and NoSQL layers.

---

## 2. Key Functional Modules

### **A. Digital Wellness Engine (Local-First)**
-   **Precise Tracking**: Uses `UsageStatsManager` query events to pair foreground/background transitions for second-accurate data.
-   **Accessibility Enforcement**: A dedicated `AccessibilityService` detects app launches and enforces blocks even without internet.
-   **Hard-Block Overlay**: A custom full-screen activity that traps interaction once limits are reached.

### **B. Collaborative Extension System**
-   **Student Path**: Taps "Ask Parent" → Writes to Firestore `extension_requests` (`status: "pending"`).
-   **Parent Path**: A foreground "Watchdog" service on the parent device detects the write → Shows high-priority notification with "Allow/Deny" actions.
-   **Unlocking**: Approval updates Firestore status → Student device detects change → Grants time in local Room DB → App unblocks.
-   **Replay Protection**: Documents move from `approved` to `consumed` after use.

### **C. Quiz Extension (Cognitive Regulation)**
-   Students can earn a one-time 15-minute extension by passing a cognitive puzzle (Math or Trivia).
-   Limits are capped at 3 times per day to prevent abuse.
-   Successes are logged to Firestore so parents can track self-regulation achievements.

### **D. Teacher Oversight**
-   Batch classification of student datasets (CSV/XLSX).
-   SHAP Explainability providing top 3 factors behind each student's addiction risk.

---

## 3. Folder Structure Map

```text
mindgate-android/
├── app/
│   ├── src/main/java/com/mindgate/
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── dao/          (AppLimitDao, ExtensionGrantDao, ParentDao)
│   │   │   │   ├── database/     (MindGateDatabase - Room implementation)
│   │   │   │   ├── entities/     (UserEntity, AppLimitEntity, ExtensionGrant)
│   │   │   │   └── SessionManager.kt (DataStore session persistence)
│   │   │   ├── remote/
│   │   │   │   ├── api/          (MindGateApiService, ParentApiService)
│   │   │   │   └── dto/          (AuthDto, ParentDto, UsageDto)
│   │   │   └── repository/
│   │   │       ├── UserRepository.kt (Auth logic & Firebase restoration)
│   │   │       ├── FirestoreRepository.kt (Real-time coordination logic)
│   │   │       ├── UsageRepository.kt (Local limits orchestrator)
│   │   │       └── ParentRepository.kt (Linking & monitoring logic)
│   │   ├── domain/
│   │   │   ├── manager/          (ExtensionManager, PolicyEngine)
│   │   │   └── model/            (UserModel, ExtensionRequest, Challenge)
│   │   ├── parent/               (New Watchdog Service Layer)
│   │   │   ├── ParentApprovalListenerService.kt (Foreground watchdog)
│   │   │   ├── ApprovalReceiver.kt (One-tap notification actions)
│   │   │   └── manager/          (ParentServiceManager)
│   │   ├── presentation/
│   │   │   ├── navigation/       (NavGraph, Screen, RoleGuard)
│   │   │   ├── ui/
│   │   │   │   ├── auth/         (Unified Login/Register)
│   │   │   │   ├── blocked/      (BlockedOverlayActivity, ExtensionGranted)
│   │   │   │   ├── home/         (Student Dashboard & Usage Charts)
│   │   │   │   ├── parent/       (ParentDashboard, ChildUsageDetail, Linking)
│   │   │   │   ├── profile/      (Account ID Visibility & Linking Entry)
│   │   │   │   └── quiz/         (Wellness Challenge Engine)
│   │   │   └── viewmodel/        (Role-specific UI logic controllers)
│   │   └── util/                 (UsageStatsHelper, AccessibilityService, Log)
│   └── build.gradle.kts          (Dependency management & Build config)
├── project/ (FastAPI Backend)
│   ├── api/
│   │   ├── main.py               (Endpoint definitions)
│   │   ├── models.py             (SQLAlchemy Schemas)
│   │   ├── firebase_utils.py     (Admin SDK wrapper)
│   │   └── database.py           (SQLite connection setup)
│   └── mindgate_backend.db       (Production local database)
└── firestore.rules               (Secure role-based access control)
```

---

## 4. Current Deployment Status
-   **Security Rules**: Published & Aligned (Strict studentId/parentId matching).
-   **Extension Loop**: 100% Functional via Firestore Watchdog.
-   **Installation**: Standard APK install on Student/Parent devices.
-   **UID Clarity**: Account ID visible in Profile UI for easy linking.

**PHASE 4 CONCLUDED — SYSTEM READY FOR FINAL DEMONSTRATION.**
