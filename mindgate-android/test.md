# MindGate System Architecture & Status Summary

## 1. System Overview
MindGate is a digital-wellbeing platform designed to manage gadget addiction through a multi-device role architecture (Student, Parent, and Teacher). It combines real-time usage monitoring, risk-adaptive blocking, and a collaborative approval system.

### Core Authentication Flow
- **Backend**: FastAPI manages users and generates JWTs.
- **Firebase Sync**: Backend generates a Firebase Custom Token where `Firebase UID == Backend User ID`.
- **Android**: `FirebaseAuth.signInWithCustomToken()` ensures Firestore and Authentication use a consistent identity.

---

## 2. Key Features & Workflows

### A. Extension System (Parent-Student Loop)
1.  **Student Request**: When a limit is reached, the student taps "Ask Parent".
2.  **Firestore Write**: An `extension_requests` document is created (`status: "PENDING"`).
3.  **Parent Approval**: Parent receives a real-time notification/update and taps "Allow".
4.  **Approval Logic**: Status becomes `APPROVED`. `respondedAt` (parent's timestamp) + `approvedMinutes` determines the new expiry.
5.  **Application**: The student's device detects the approval, updates the local Room database (`app_limits`), and unblocks the app via the `AccessibilityService`.
6.  **Replay Protection**: The request is marked `CONSUMED` after use.

### B. Quiz Extension (Instant Grant)
1.  **Solve Quiz**: Student passes a focus/calming puzzle.
2.  **Local Grant**: `ExtensionManager` grants 15 minutes immediately.
3.  **Activity Log**: An informational record (`status: "EARNED"`) is written to Firestore so parents can see the achievement in their "Recent Activity" feed.

### C. Usage Monitoring & Blocking
- **UsageMonitorService**: Periodically checks `UsageStatsManager` and updates local DB.
- **MindGateAccessibilityService**: The "Enforcer". It checks if `usedMinutes >= limit` AND `currentTime > extensionUntil`. If both are true, it triggers the `BlockedOverlayActivity`.

---

## 3. Project File Structure (Core Components)

```text
mindgate-android/app/src/main/java/com/mindgate/
├── data/
│   ├── local/
│   │   ├── dao/ (AppLimitDao, ExtensionGrantDao, UserDao)
│   │   ├── entities/ (UserEntity, ParentChildRelationEntity)
│   │   └── SessionManager.kt (JWT & User ID storage)
│   ├── remote/
│   │   ├── api/ (MindGateApiService, ParentApiService)
│   │   └── dto/ (AuthResponse, ExtensionRequest)
│   └── repository/
│       ├── UserRepository.kt (Auth logic & Firebase Sync)
│       ├── FirestoreRepository.kt (Real-time requests/activity)
│       ├── ParentRepository.kt (Linking & Monitoring)
│       └── ExtensionRepository.kt (Local/Remote extension sync)
├── domain/
│   ├── manager/
│   │   ├── ExtensionManager.kt (Decision engine for grants)
│   │   └── PolicyEngine.kt (Risk-based minute calculation)
│   └── model/ (ExtensionRequest, UserModel)
├── presentation/
│   ├── navigation/ (MindGateNavGraph - Role-based routing)
│   ├── ui/
│   │   ├── auth/ (Login/Register)
│   │   ├── blocked/ (BlockedOverlayActivity - The lock screen)
│   │   ├── parent/ (ParentDashboardScreen - Approval & Activity UI)
│   │   ├── quiz/ (QuizPuzzleActivity - Focus challenges)
│   │   └── screens/ (HomeScreen, Splash - Routing logic)
│   └── viewmodel/ (ParentDashboardViewModel, BlockedOverlayViewModel)
└── util/
    └── MindGateAccessibilityService.kt (The blocking engine)
```

---

## 4. Firestore Security Rules
Located at: `mindgate-android/firestore.rules`

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    match /extension_requests/{requestId} {
      // 1. CREATE: Students can create PENDING requests or EARNED logs for themselves.
      allow create: if request.auth != null &&
                      request.resource.data.studentUid == request.auth.uid &&
                      (request.resource.data.status == 'PENDING' || request.resource.data.status == 'EARNED');

      // 2. READ: Only the involved student and the linked parent can see the document.
      allow read: if request.auth != null &&
                    (resource.data.studentUid == request.auth.uid ||
                     resource.data.parentUid == request.auth.uid);

      // 3. UPDATE:
      allow update: if request.auth != null && (
                      // Parents can approve/deny a PENDING request
                      (resource.data.parentUid == request.auth.uid &&
                       resource.data.status == 'PENDING' &&
                       (request.resource.data.status == 'APPROVED' ||
                        request.resource.data.status == 'DENIED'))
                      ||
                      // Students can mark an APPROVED request as CONSUMED
                      (resource.data.studentUid == request.auth.uid &&
                       resource.data.status == 'APPROVED' &&
                       request.resource.data.status == 'CONSUMED')
                    ) &&
                    // Safety: studentUid and parentUid cannot be tampered with.
                    request.resource.data.studentUid == resource.data.studentUid &&
                    request.resource.data.parentUid == resource.data.parentUid;
    }
    
    match /connection_test/{docId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 5. Current Implementation Status
- **Authentication**: Unified (JWT + Firebase Custom Token).
- **Linking**: Functional (Code-based MG-XXXXXXXX).
- **Extension Request**: **FIXED** (UID Mismatch resolved).
- **Parent Approval**: Functional (Real-time listener).
- **Quiz Earned Activity**: Functional (Informational logs).
- **Blocking**: Functional (Accessibility-based).
- **Backend**: FastAPI running at `http://192.168.0.102:8000/`.
