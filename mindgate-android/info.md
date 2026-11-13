# MindGate System Information

## 1. System Overview
MindGate is a comprehensive digital wellness platform consisting of an **Android Application**, a **FastAPI Backend**, and **Firebase Integration** (Authentication + Firestore). It supports three primary roles: **Student**, **Parent**, and **Teacher**.

---

## 2. Core Functionality

### **Authentication & Roles**
*   **Unified Auth Flow**: Users register/login via FastAPI. The backend returns a standard JWT for API calls and a **Firebase Custom Token**.
*   **Firebase Sync**: The Android app uses the Custom Token to sign into Firebase. 
*   **UID Identity**: The system enforces **Firebase UID == Backend Numeric ID** (as a string). This ensures that Firestore security rules can reliably identify the user from the SQL database.
*   **Role Enforcement**: Users are strictly routed to their respective dashboards (`Student`, `Parent`, or `Teacher`) upon login.

### **Usage Tracking (Precise)**
*   **Source of Truth**: Uses `UsageStatsManager` with the `queryEvents` API.
*   **Precision**: Manually pairs `MOVE_TO_FOREGROUND` and `MOVE_TO_BACKGROUND` events to calculate second-accurate usage.
*   **Real-time monitoring**: `UsageMonitorService` runs as a Foreground Service (API 34 compliant) to track usage every 15 seconds.

### **Restriction Engine**
*   **Hard Blocking**: When a daily limit is reached, `MindGateAccessibilityService` detects the app launch and immediately triggers the `BlockedOverlayActivity`.
*   **80% & 90% Nudges**: Students receive system notifications when approaching their limit (75% and 90% thresholds).

---

## 3. Extension & Coordination

### **Unlocking Mechanisms**
1.  **Quiz Path (Student-Earned)**: Students solve a challenge (Math/Trivia) to earn 15 mins. Capped at 3 times/day.
2.  **Parent Path (Coordination)**: Students tap "Ask Parent," creating a real-time Firestore request.

### **Firestore Lifecycle: `extension_requests`**
| Status | Triggered By | Action |
| :--- | :--- | :--- |
| `PENDING` | Student | Request created with `packageName` and `requestedMinutes`. |
| `APPROVED` | Parent | Parent sets `approvedMinutes` and `respondedAt`. |
| `DENIED` | Parent | Parent rejects request; student remains blocked. |
| `CONSUMED` | Student | Student device detects approval, unblocks app, and marks doc as spent. |

---

## 4. Blocked Screen Analysis (Screenshot Diagnosis)
The attached image shows the **BlockedOverlayActivity**. Here is the breakdown of the information shown:

### **Cause for the Screen**
*   **Trigger**: The `MindGateAccessibilityService` detected that you opened "WhatsApp" and checked the local Room database (`app_limits`).
*   **Condition**: Total usage for today (tracked by `UsageMonitorService`) has reached or exceeded the configured daily limit, and there is no active extension.

### **Identified Issues in Screenshot**
1.  **"0 minutes allowed" Bug**: The overlay failed to receive the correct limit value from the background service via Intent Extras. (Fix: Logic updated to redundantly load the limit from Room if the Intent extra is missing).
2.  **"Permission denied" Error**: The student attempted to "Ask Parent," but the request failed.
    *   **Root Cause**: The student's device had not yet synced the Parent's Firebase UID from the backend. Without a valid `parentUid`, the Firestore Security Rules reject the document creation.
    *   **Fix**: Implemented a mandatory background link-sync that fetches the Parent UID as soon as a link is established, even if the student is not on the linking screen.

---

## 5. Firebase Setup Requirements
To ensure the system works, the following must be configured in your Firebase Project:

### **1. Authentication**
*   Enable **Google Sign-In** (for the unified auth flow).
*   The Backend uses the **Firebase Admin SDK** to generate Custom Tokens.

### **2. Firestore Rules**
Paste these into the **Rules** tab of your Firebase Console:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /extension_requests/{requestId} {
      // 1. Student can create their own request with a valid Parent UID
      allow create: if request.auth != null &&
                      request.resource.data.studentUid == request.auth.uid &&
                      request.resource.data.parentUid is string &&
                      request.resource.data.parentUid != "";

      // 2. Read access for involved parties
      allow read: if request.auth != null &&
                    (resource.data.studentUid == request.auth.uid ||
                     resource.data.parentUid == request.auth.uid);

      // 3. Updates (Approval/Denial/Consumption)
      allow update: if request.auth != null && (
                      (resource.data.parentUid == request.auth.uid && resource.data.status == 'PENDING') ||
                      (resource.data.studentUid == request.auth.uid && resource.data.status == 'APPROVED')
                    );
    }
  }
}
```

---

## 6. Project File Structure

### **Android (`app/src/main/java/com/mindgate`)**
*   📂 `data`:
    *   📂 `local`: Room DB (`MindGateDatabase`), DataStore (`SessionManager`), Prefs.
    *   📂 `remote`: Retrofit API interfaces and DTOs (`AuthDto`, `ParentDto`).
    *   📂 `repository`: Data logic orchestrators (`UserRepository`, `UsageRepository`, `FirestoreRepository`).
*   📂 `domain`:
    *   📂 `manager`: `ExtensionManager` (Handles the logic of time-grants).
    *   📂 `model`: Data classes used across the UI (`UserModel`, `ExtensionRequest`).
*   📂 `presentation`:
    *   📂 `navigation`: `NavGraph.kt` (Central routing) and `RoleGuard.kt`.
    *   📂 `ui`: Screen Composables and ViewModels (Auth, Dashboard, Quiz, Blocked).
*   📂 `service`:
    *   `UsageMonitorService.kt`: Persistent tracking loop.
    *   `MindGateAccessibilityService.kt`: Real-time block engine.

### **Backend (`project/api`)**
*   `main.py`: Endpoint definitions (`/token`, `/predict`, `/api/parent/*`).
*   `models.py`: SQLAlchemy SQL schema.
*   `schemas.py`: Pydantic validation models.
*   `firebase_utils.py`: Admin SDK wrapper (Custom tokens, Push notifications).

---

## 7. Verification Checklist
1.  **Auth**: `UID_CHECK` Logcat tag proves Firebase UID == DB ID.
2.  **Tracking**: `USAGE` Logcat tag proves event-based accuracy vs. Digital Wellbeing.
3.  **Linking**: `SESSION` Logcat tag proves background sync of Parent UID.
4.  **Blocking**: Overlay shows correct "X minutes used" from intent extras.
5.  **Requests**: Firestore console shows state transition `PENDING` -> `APPROVED` -> `CONSUMED`.
