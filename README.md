# RuralHealth Sync 🏥

An **Offline-First** Android application for Health Extension Workers (HEWs) in rural areas of Ethiopia. Patient data is stored locally with **Room (SQLite)** when there is no internet connection, then automatically pushed to a central **PHP/MySQL** backend when connectivity is restored.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                  Android App (Kotlin)                │
│                                                     │
│  ┌───────────┐    ┌───────────────┐    ┌─────────┐  │
│  │    UI     │◄──►│  Repository   │◄──►│  Room   │  │
│  │(Activity) │    │  (PatientRepo)│    │(SQLite) │  │
│  └───────────┘    └───────┬───────┘    └─────────┘  │
│                           │                          │
│                    ┌──────▼──────┐                   │
│                    │  Retrofit   │  (when online)    │
│                    └──────┬──────┘                   │
│                           │                          │
│                  ┌────────▼────────┐                 │
│                  │  WorkManager    │                 │
│                  │  (SyncWorker)   │                 │
│                  └─────────────────┘                 │
└─────────────────────────────────────────────────────┘
                           │ HTTPS
          ┌────────────────▼────────────────┐
          │         PHP REST API            │
          │   (http://10.0.2.2/ruralhealth_api/) │
          └────────────────┬────────────────┘
                           │
                  ┌────────▼────────┐
                  │  MySQL Database │
                  └─────────────────┘
```

### Key Architectural Decisions

| Layer | Technology | Purpose |
|---|---|---|
| Local Storage | **Room (SQLite)** | Offline-first data persistence |
| Network | **Retrofit 2 + Gson** | Communication with PHP backend |
| Background Sync | **WorkManager** | Reliable periodic sync (15 min) |
| Async | **Kotlin Coroutines** | Non-blocking I/O |
| UI | **MVVM + LiveData** | Reactive UI, lifecycle-aware |

---

## Project Structure

```
app/src/main/java/com/example/ruralhealthsync/
├── data/
│   ├── local/
│   │   ├── PatientEntity.kt      # Room @Entity (SQLite table)
│   │   ├── PatientDao.kt         # Room DAO (insert, query)
│   │   └── AppDatabase.kt        # Room @Database singleton
│   ├── remote/
│   │   ├── ApiService.kt         # Retrofit interface (POST /sync_patients.php)
│   │   └── RetrofitClient.kt     # Retrofit singleton (base URL config)
│   └── repository/
│       └── PatientRepository.kt  # Coordinates Room ↔ Retrofit
├── worker/
│   └── SyncWorker.kt             # CoroutineWorker for background sync
├── ui/
│   └── MainActivity.kt           # Schedules WorkManager sync task
└── RuralHealthApplication.kt     # Application class, initialises Room
```

---

## How to Build

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17**
- **Android SDK** with API Level 24+ (Android 7.0+)
- **XAMPP** (or any Apache + PHP + MySQL stack) running on your development machine for the backend

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/ERMI2112/RuralHealthSync-.git
   cd RuralHealthSync-
   ```

2. **Open in Android Studio**
   - File → Open → select the `RuralHealthSync-` folder
   - Wait for Gradle to sync and download dependencies

3. **Configure the backend URL** *(optional for emulator)*
   - The default base URL `http://10.0.2.2/ruralhealth_api/` points to localhost on the Android emulator
   - For a real device on the same Wi-Fi, replace `10.0.2.2` with your machine's local IP in `RetrofitClient.kt`

4. **Set up the PHP backend**
   - Place your PHP scripts in `htdocs/ruralhealth_api/` inside your XAMPP folder
   - Import the MySQL schema (`database/schema.sql`) into phpMyAdmin
   - Ensure the `sync_patients.php` endpoint accepts a POST request with a JSON array of patient objects

5. **Build and run**
   ```bash
   # From the project root
   ./gradlew assembleDebug
   ```
   Or use the **Run** button (▶) in Android Studio with an emulator or connected device.

---

## Sync Flow

```
App starts
    │
    ▼
WorkManager schedules SyncWorker
(every 15 minutes, requires CONNECTED network)
    │
    ▼
SyncWorker.doWork()
    │
    ├─► PatientRepository.syncUnsyncedPatients()
    │       │
    │       ├─► PatientDao.getUnsyncedPatients()   ← Fetch from SQLite
    │       │
    │       ├─► ApiService.syncPatients(list)      ← POST to PHP API
    │       │
    │       └─► PatientDao.markAsSynced(ids)       ← Update SQLite flags
    │
    └─► Result.success() or Result.retry()
```

---

## License

This project is developed as part of the **Mobile Application Development (ITec3056)** course project at the **University of Gondar**, Academic Year 2026.