# MindGate System Architecture & Workflow

MindGate is a digital-wellbeing ecosystem designed to mitigate gadget addiction through AI-driven risk assessment, real-time usage monitoring, and a collaborative parent-student regulatory framework.

## 1. High-Level Architecture
The system follows a distributed client-server architecture consisting of three primary layers:

### A. Presentation Layer (Android App)
*   **Framework:** Jetpack Compose (Modern Declarative UI).
*   **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles.
*   **Roles:** 
    *   **Student:** Self-assessment, automated app blocking, wellness challenges.
    *   **Parent:** Real-time monitoring, usage analytics, remote extension approval.
    *   **Teacher:** Cohort analysis, bulk student risk assessment via AI.

### B. Logic & Intelligence Layer (FastAPI Backend)
*   **Engine:** FastAPI (Python) for high-performance asynchronous handling.
*   **AI Model:** CatBoost Gradient Boosting (highest accuracy on survey-based datasets).
*   **Risk Engine:** Processes 38 unique behavioral features (screen time, sleep hours, stress, etc.) to predict one of 4 risk levels: **Low, Moderate, High, Severe**.

### C. Data & Sync Layer
*   **Relational DB (PostgreSQL/SQLite):** Stores persistent user profiles, historical usage events, and account metadata.
*   **Real-time DB (Firebase Firestore):** Synchronizes time-critical extension requests and parent-child links between devices instantly.
*   **Auth:** Firebase Auth + Backend JWT for secure identity alignment.

---

## 2. Core Functional Workflows

### 📥 Registration & Identity Alignment
1.  User registers via the Android app (Email/Password or Google).
2.  Backend creates a record in the SQL database.
3.  Backend generates a **Firebase Custom Token** aligned with the internal SQL ID.
4.  App signs into Firebase using this token, ensuring that **Student ID 5 in SQL** is exactly **User 5 in Firestore**, allowing for seamless synchronization.

### 🧠 AI Assessment Flow
1.  Student completes a 5-step science-backed survey.
2.  Responses are sent to the `/predict` endpoint.
3.  The **CatBoost model** calculates a risk score (0-100) and identifies "Top 3 Risk Factors."
4.  The system generates a **Personalized Wellness Plan** with recommended daily limits.

### 🛑 Automated Blocking & Restoration
1.  **Watchdog Service:** A background Accessibility Service monitors active app packages.
2.  **Enforcement:** If a limit is hit, the `BlockedOverlayActivity` is launched, trapping user interaction.
3.  **Restoration (Self-Regulation):** Student can solve a "Wellness Challenge" (Math/Trivia) to earn 15 minutes.
4.  **Restoration (Collaboration):** Student sends a request to the parent. Firestore triggers a listener on the parent's device, showing a system notification.

### 👨‍👩‍👦 Parental Linking (The "Handshake")
1.  Student generates a temporary 6-digit link code (MG-XXXXXX).
2.  Parent enters this code in their app.
3.  Backend verifies the code, creates a permanent relation in SQL, and writes an entry to `parentChildLinks` in Firestore.
4.  This establishes a real-time data bridge for monitoring and approvals.

---

## 3. Technology Stack Summary
| Component | Technology |
| :--- | :--- |
| **Mobile UI** | Jetpack Compose, Material 3, Motion Layout |
| **Networking** | Retrofit, OkHttp, Coroutines |
| **Backend API** | FastAPI, Uvicorn, Pydantic v2 |
| **AI/ML** | CatBoost, Scikit-learn, SHAP (Explainability) |
| **Real-time** | Firebase Firestore, FCM (Cloud Messaging) |
| **Database** | SQLAlchemy, SQLite (Prototype) / PostgreSQL (Production) |

---

## 4. Key Performance Indicators (KPIs)
*   **AI Accuracy:** Cross-validation F1-Score of **0.9758**.
*   **Sync Latency:** Extension approval propagates in **< 500ms** via Firestore listeners.
*   **UI Feedback:** Standardized "Outline-to-Solid" button animations for high-quality user interaction.

---
*Prepared for the MindGate Digital Wellbeing Presentation.*
