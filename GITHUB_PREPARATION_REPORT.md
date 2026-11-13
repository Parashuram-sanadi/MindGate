# MindGate — GitHub Upload Preparation & Security Audit Report

> **Project Name**: MindGate (Digital Wellness Platform)  
> **Date**: October 2026  
> **Target Repository**: GitHub  
> **Status**: Ready for Public/Private GitHub Upload  

---

## Executive Summary

A comprehensive security audit and file inspection was performed across both the **Android Application (`mindgate-android`)** and the **FastAPI Backend & ML Engine (`project`)**.

* **Git Status**: Git is currently not initialized. No secret credentials, database files, or private keys have been committed to version history.
* **Ignored Files**: Configured `.gitignore` files at the root level, `mindgate-android/.gitignore`, and `project/.gitignore`.
* **Build Integrity**: Rebuilding the Android APK (`app:assembleDebug`) and executing backend tests confirmed that all build-critical dependencies and code files remain 100% intact.

---

## 1. Files Safe to Upload

These files are essential for compiling, building, and running the MindGate platform and are safe for version control:

### 📱 Android Application (`mindgate-android`)
* All Kotlin source files (`app/src/main/java/com/mindgate/...`)
* Resource XML files (`app/src/main/res/` — drawables, values, layouts, themes)
* Manifest (`app/src/main/AndroidManifest.xml`)
* Gradle configuration files (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/wrapper/`)
* Firebase Configuration: `app/google-services.json` *(See Security Note below)*
* Firebase Security Rules: `firestore.rules`
* Documentation: `MINDGATE_PROJECT_GUIDE.md`, `README.md`

### 🐍 FastAPI Backend & ML Engine (`project`)
* All Python application source files (`main.py`, `train.py`, `evaluate.py`, `recommendation_engine.py`, `predict.py`)
* API module (`api/main.py`, `api/auth.py`, `api/database.py`, `api/models.py`, `api/schemas.py`, `api/model_service.py`, `api/firebase_utils.py`)
* ML pipeline modules (`src/*.py` — data loader, preprocessor, model trainer, model selector, explainability)
* Model configuration & hyperparameters (`configs/config.py`, `models/risk_mapping.json`, `models/best_parameters.json`, `models/default_inputs.json`)
* Model binaries for prediction: `models/best_model.pkl`, `models/feature_pipeline.pkl`, `models/label_encoder.pkl`
* Python dependencies specification: `requirements.txt`
* Training dataset: `data/augmented_dataset_final.xlsx`
* Evaluation reports & figures: `report_outputs/`, `plots/`, `reports/`

---

## 2. Sensitive Files Detected (Strictly Excluded)

The following files contain private credentials, user data, or machine-specific configurations and have been **strictly excluded** in `.gitignore`:

| Sensitive Item | File Path | Risk Description | Exclusion Action |
| :--- | :--- | :--- | :--- |
| **Firebase Admin Service Account** | `project/secrets/firebase-service-account.json` | Contains Firebase Admin SDK private key for full database read/write access. | Added to `.gitignore` |
| **Backend SQLite Database** | `project/mindgate_backend.db` & `mindgate-backend.db` | Contains real user accounts, hashed passwords, FCM tokens, and parent-child pairings. | Added to `.gitignore` |
| **Android Local Properties** | `mindgate-android/local.properties` | Machine-specific Android SDK paths and local developer properties. | Added to `.gitignore` |
| **Python Virtual Environment** | `project/venv/` & `.venv/` | Environment binaries, machine-specific Python packages. | Added to `.gitignore` |
| **Server & Startup Logs** | `project/*.log` (e.g. `server_out.log`, `server_err.log`) | Contains runtime execution traces and system IP logs. | Added to `.gitignore` |
| **Compiled Bytecode & Build Cache** | `mindgate-android/app/build/`, `.gradle/`, `__pycache__/` | Temporary compilation outputs and build caches. | Added to `.gitignore` |

---

## 3. Security Rationale: `google-services.json`

### Why `google-services.json` is RETAINED:
* **Functionality**: Android require `google-services.json` to compile and initialize Firebase Auth, Firestore, and FCM SDKs. Without this file, the Gradle build will fail.
* **Security Specifications**: `google-services.json` contains public client parameters (`project_id`, `api_key`, `app_id`, `client_id`). According to official Google/Firebase security documentation, the keys in `google-services.json` do not grant administrative backend access.
* **Protection Mechanism**: Database access security is enforced entirely by **Cloud Firestore Security Rules** (`firestore.rules`), which require custom token authentication (`request.auth != null`) matching user UIDs.
* **Recommendation**: Keep `google-services.json` committed so collaborators can build the APK immediately.

---

## 4. Exclusion Verification (`.gitignore` Configuration)

Three `.gitignore` files have been established:

1. **Top-Level Root (`.gitignore`)**:
   Covering both Android and Python modules for single-repository root commits.
2. **Android Sub-Module (`mindgate-android/.gitignore`)**:
   Covering Gradle build folders, `.idea/`, `local.properties`, keystores (`*.jks`, `*.keystore`).
3. **Python Sub-Module (`project/.gitignore`)**:
   Covering `venv/`, `__pycache__/`, `*.db`, `secrets/`, `firebase-service-account.json`, `*.log`.

---

## 5. Existing Git Tracking & Mitigation

* **Current Status**: Git is **not yet initialized** in `mindgate-android` or `project`. No sensitive credentials have been tracked or pushed to history.

### Untracking Command Reference (For Future Use):
If a sensitive file (e.g., `mindgate_backend.db` or `firebase-service-account.json`) is ever accidentally tracked in Git, run the following command to remove it from Git tracking without deleting the local file on your PC:

```bash
# Untrack file from Git repository while keeping local copy
git rm --cached project/mindgate_backend.db
git rm --cached project/secrets/firebase-service-account.json
git rm --cached mindgate-android/local.properties

# Commit the untracking change
git commit -m "security: untrack sensitive database and credentials"
```

---

## 6. Recommended Commands to Push to GitHub

Follow these steps to initialize Git and push your clean project to GitHub:

### Step 1: Open Terminal at Project Root
```powershell
cd D:\MOVIES\gadget_addiction_project
```

### Step 2: Initialize Git Repository
```bash
git init
```

### Step 3: Add Files to Staging (Respecting `.gitignore`)
```bash
git add .
```

### Step 4: Verify Ignored Files
Verify that `.db`, `venv/`, `build/`, `local.properties`, and `firebase-service-account.json` are **NOT** staged:
```bash
git status
```

### Step 5: Create Initial Commit
```bash
git commit -m "feat: initial commit for MindGate Digital Wellbeing platform"
```

### Step 6: Link Remote Repository & Push to GitHub
```bash
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/mindgate.git
git push -u origin main
```

---

### Verification Summary
- **Source Code Integrity**: 100% Intact
- **Build System**: Clean & Working
- **Security Exclusions**: Verified
