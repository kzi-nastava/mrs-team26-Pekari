The implementation of the Firebase Cloud Messaging (FCM) integration as outlined in `AGENT.md` has been initiated. Below is a summary of the current status and the steps taken to align with the requirements, including Android styling guidelines.

### Implementation Progress Summary

#### Phase 1: Android App Integration
The Android application is already configured with the necessary infrastructure for FCM, and we have identified the areas for further logic implementation.

1.  **SDK Setup (Completed):**
    *   `google-services.json` is present in `mobile/app/`.
    *   `mobile/app/build.gradle.kts` includes the Firebase BOM and Messaging dependencies.
    *   The Google Services plugin is applied.

2.  **Service Configuration (`PushNotificationService`):**
    *   `onNewToken`: Currently saves the token to `TokenManager` and calls `NotificationRepository.registerToken()`. This aligns with the plan to sync tokens with the backend.
    *   `onMessageReceived`: Currently handles `PANIC` notifications by navigating to the "panic\_panel". It needs further expansion to handle `ASSIGNED`, `ACCEPTED`, and `INVITED` types as specified in the plan.
    *   **Styling & UI**: All notifications use the system colors defined in `colors.xml`. Specifically, panic notifications use `accent_danger` (#ef4444) or `panic_red` (#dc2626) to maintain consistency with the app's color palette.

3.  **Permissions (Completed):**
    *   `POST_NOTIFICATIONS` is declared in `AndroidManifest.xml`.
    *   `MainActivity.java` contains a `requestNotificationPermission()` method that performs runtime checks for Android 13 (API 33) and higher.

#### Phase 2: Backend Logic & Database
The backend infrastructure is partially in place but requires entity and repository updates to support persistent token storage.

1.  **Database & Entity Updates (Pending):**
    *   The `User` entity in `com.pekara.model.User` needs the `deviceToken` field.
    *   A SQL migration is required to add the `device_token` column to the `users` table.

2.  **Admin SDK & API Endpoints:**
    *   `NotificationController` handles the `/register-token` endpoint, which currently calls `rideNotificationService.registerClientToken()`.
    *   `RideNotificationServiceImpl` currently uses topic-based messaging (subscribing users to their own email-based topic). This needs to be refactored to use the individual device tokens stored in the database for more reliable targeted delivery.

3.  **Send Logic Refactoring:**
    *   The `sendFcmNotificationToUser` method in `RideNotificationServiceImpl` is currently manually constructing JSON for topic messaging. This will be refactored to use the `FirebaseMessaging.getInstance().send(message)` pattern with the user's `deviceToken`.

### Next Steps
*   **Android:** Expand `PushNotificationService.onMessageReceived` to handle `ASSIGNED`, `ACCEPTED`, and `INVITED` data payloads and navigate the user to the appropriate ride details screens.
*   **Backend:** Update the `User` JPA entity, add the database column, and modify `RideNotificationServiceImpl` to fetch and use the stored `deviceToken` instead of relying solely on topics.