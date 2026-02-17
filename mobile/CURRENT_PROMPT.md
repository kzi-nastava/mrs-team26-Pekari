# Admin Ride History Implementation Plan for Mobile

## Overview

Implement admin ride history functionality for the mobile app, allowing admins to view all completed rides in the system and see detailed ride information with route visualization on a map.

---

## Phase 1: API Layer

### 1.1 Create API Models
**Files to create in `data/api/model/`:**

#### AdminRideHistoryResponse.java
```java
// List item model for admin ride history
- id (Long)
- status (String)
- createdAt, scheduledAt, startedAt, completedAt (String - ISO datetime)
- pickupAddress, dropoffAddress (String)
- pickup, dropoff (LocationPoint)
- stops (List<LocationPoint>)
- cancelled (Boolean)
- cancelledBy, cancellationReason (String)
- cancelledAt (String)
- price (BigDecimal)
- distanceKm (Double)
- estimatedDurationMinutes (Integer)
- panicActivated (Boolean)
- panickedBy (String)
- vehicleType (String)
- babyTransport, petTransport (Boolean)
- driver (AdminDriverBasicInfo - inner class)
- passengers (List<AdminPassengerBasicInfo> - inner class)

Inner classes:
- AdminDriverBasicInfo: id, firstName, lastName, email, phoneNumber
- AdminPassengerBasicInfo: id, firstName, lastName, email
```

#### AdminRideDetailResponse.java
```java
// Detailed model for single ride view
All fields from AdminRideHistoryResponse plus:
- routeCoordinates (String - JSON for map)
- driver (AdminDriverDetailInfo - extended)
- passengers (List<AdminPassengerDetailInfo> - extended)
- ratings (List<AdminRideRatingInfo>)
- inconsistencyReports (List<AdminInconsistencyReportInfo>)

Inner classes:
- AdminDriverDetailInfo: id, firstName, lastName, email, phoneNumber, profilePicture, licenseNumber, vehicleModel, licensePlate, averageRating, totalRides
- AdminPassengerDetailInfo: id, firstName, lastName, email, phoneNumber, profilePicture, totalRides, averageRating
- AdminRideRatingInfo: id, passengerId, passengerName, vehicleRating, driverRating, comment, ratedAt
- AdminInconsistencyReportInfo: id, reportedByUserId, reportedByName, description, reportedAt
```

#### AdminRideHistoryFilter.java
```java
// Request body for filtering
- startDate (String - optional)
- endDate (String - optional)
```

### 1.2 Extend RideApiService
**Add to `data/api/service/RideApiService.java`:**
```java
@POST("rides/history/admin/all")
Call<PaginatedResponse<AdminRideHistoryResponse>> getAdminRideHistory(
    @Body AdminRideHistoryFilter filter,
    @Query("page") int page,
    @Query("size") int size
);

@GET("rides/admin/{id}")
Call<AdminRideDetailResponse> getAdminRideDetail(@Path("id") Long rideId);
```

### 1.3 Extend RideRepository
**Add to `data/repository/RideRepository.java`:**
```java
public void getAdminRideHistory(AdminRideHistoryFilter filter, int page, int size,
                                 RepoCallback<PaginatedResponse<AdminRideHistoryResponse>> callback)

public void getAdminRideDetail(Long rideId, RepoCallback<AdminRideDetailResponse> callback)
```

---

## Phase 2: Presentation Layer - ViewStates

**Files to create in `presentation/history/viewstate/`:**

### 2.1 AdminHistoryViewState.java
```java
public class AdminHistoryViewState {
    public boolean loading;
    public boolean error;
    public String errorMessage;
    public List<AdminRideUIModel> rides;
    public String sortField;      // createdAt, startedAt, completedAt, price, distanceKm, status, pickup, dropoff
    public boolean sortAscending; // default false (newest first)
}
```

### 2.2 AdminRideUIModel.java
```java
public class AdminRideUIModel {
    public Long id;
    public String status;
    public String createdAt;
    public String startedAt;
    public String completedAt;
    public String pickupAddress;
    public String dropoffAddress;
    public double price;
    public Double distanceKm;
    public String vehicleType;
    public boolean cancelled;
    public String cancelledBy;
    public boolean panicActivated;
    public String panickedBy;
    public String driverName;
    public String driverEmail;
    public int passengerCount;
}
```

### 2.3 AdminRideDetailViewState.java
```java
public class AdminRideDetailViewState {
    public boolean loading;
    public boolean error;
    public String errorMessage;
    public AdminRideDetailResponse rideDetail;
}
```

---

## Phase 3: Presentation Layer - ViewModels

**Files to create in `presentation/history/viewmodel/`:**

### 3.1 AdminHistoryViewModel.java
```java
- MutableLiveData<AdminHistoryViewState> state
- List<AdminRideUIModel> currentRides (for sorting)
- String currentSortField = "createdAt"
- boolean currentSortAscending = false

Methods:
- loadHistory(LocalDate from, LocalDate to) - fetch paginated history
- sortRides(String field, boolean ascending) - client-side sorting
- toggleDateSort() - for shake detection
- mapToUIModels(List<AdminRideHistoryResponse>) - convert API response to UI models
```

### 3.2 AdminRideDetailViewModel.java
```java
- MutableLiveData<AdminRideDetailViewState> state

Methods:
- loadRideDetail(Long rideId) - fetch detailed ride info from /rides/admin/{id}
```

---

## Phase 4: Presentation Layer - Views

### 4.1 AdminHistoryFragment.java
**Location:** `presentation/history/views/`

```java
Features:
- ViewBinding with FragmentAdminHistoryBinding
- Date range picker (MaterialDatePicker) for filtering
- RecyclerView with AdminHistoryAdapter
- Sort controls (Spinner for field + Button for direction)
- Shake detection for date sort toggle (using existing ShakeDetector)
- Click handler to open AdminRideDetailFragment dialog
- Loading/Error/Empty state handling

Lifecycle:
- onCreateView: setup binding, viewmodel, recycler, filter, sort, shake
- onResume: register shake detector
- onPause: unregister shake detector
- onDestroyView: cleanup binding
```

### 4.2 AdminHistoryAdapter.java
**Location:** `presentation/history/views/`

```java
- Extends ListAdapter<AdminRideUIModel, ViewHolder>
- DiffUtil.ItemCallback for efficient updates
- OnRideClickListener interface

ViewHolder displays:
- Route: pickupAddress → dropoffAddress
- Dates: created, started, completed (formatted)
- Status badge with color (COMPLETED=green, CANCELLED=red, IN_PROGRESS=blue, PANIC=red with icon)
- Price (formatted as "X RSD")
- Distance (formatted as "X.X km")
- Driver name
- Passenger count ("X passengers")
- Panic indicator (if panicActivated)
- Cancelled indicator (if cancelled)
```

### 4.3 AdminRideDetailFragment.java
**Location:** `presentation/history/views/`

```java
DialogFragment (fullscreen style) displaying:
- Header with "Ride Details #ID" and close button
- Map with route visualization (using existing MapHelper)
- Status badge with panic indicator

Sections (MaterialCardViews):
1. Route: pickup, stops, dropoff
2. Ride Information: created, scheduled, started, completed, price, distance, duration, vehicle type, options (baby/pet)
3. Cancellation Details (if cancelled): cancelledBy, cancelledAt, reason
4. Panic Alert (if panicActivated): panickedBy
5. Driver Details: name, email, phone, license, vehicle model, plate, rating, total rides
6. Passengers List: name, email, phone for each
7. Ratings: passenger name, driver rating (stars), vehicle rating (stars), comment, date
8. Inconsistency Reports: description, reportedBy, date

Map drawing (using MapHelper):
- Parse routeCoordinates JSON
- Add pickup marker (green)
- Add stop markers (blue)
- Add dropoff marker (red)
- Draw route polyline
- Fit bounds to show all points
```

---

## Phase 5: Layout Resources

**Files to create in `res/layout/`:**

### 5.1 fragment_admin_history.xml
```xml
ConstraintLayout:
├── LinearLayout (filterBar)
│   ├── Button (btnFilter) - "Filter by date"
│   ├── MaterialCardView (sort controls)
│   │   ├── TextView "Sort by"
│   │   ├── Spinner (spinnerSortField)
│   │   ├── MaterialButton (btnSortDirection) - "↓"
│   │   └── MaterialButton (btnResetSort) - "Reset"
│   └── TextView (shake hint)
├── View (divider)
└── FrameLayout
    ├── ProgressBar (progress) - loading state
    ├── LinearLayout (errorLayout) - error state
    │   └── TextView (txtError)
    ├── LinearLayout (layoutEmptyState) - empty state
    │   ├── TextView "No rides found"
    │   └── TextView "Try adjusting your date filter"
    └── RecyclerView (recyclerHistory)
```

### 5.2 item_admin_history.xml
```xml
MaterialCardView:
└── LinearLayout (vertical)
    ├── LinearLayout (horizontal - route row)
    │   └── TextView (txtRoute) - "Pickup → Dropoff"
    ├── LinearLayout (horizontal - dates row)
    │   ├── TextView (txtCreatedAt)
    │   ├── TextView (txtStartedAt)
    │   └── TextView (txtCompletedAt)
    ├── LinearLayout (horizontal - info row)
    │   ├── TextView (txtStatus) - status badge
    │   ├── TextView (txtPrice)
    │   └── TextView (txtDistance)
    ├── LinearLayout (horizontal - people row)
    │   ├── TextView (txtDriver)
    │   └── TextView (txtPassengerCount)
    ├── TextView (txtCancelled) - visibility conditional
    └── TextView (txtPanic) - visibility conditional
```

### 5.3 fragment_admin_ride_detail.xml
```xml
CoordinatorLayout:
└── MaterialCardView (bottom sheet style)
    └── ScrollView
        └── LinearLayout (vertical)
            ├── LinearLayout (header)
            │   ├── TextView "Ride Details #ID"
            │   └── Button (btnClose)
            ├── ProgressBar (progressDetail)
            ├── TextView (txtErrorDetail)
            └── LinearLayout (contentContainer)
                ├── MaterialCardView (map)
                │   └── MapView
                ├── TextView (txtDetailStatus)
                ├── MaterialCardView (route)
                │   ├── TextView (txtDetailPickup)
                │   ├── LinearLayout (containerStops)
                │   └── TextView (txtDetailDropoff)
                ├── MaterialCardView (ride info)
                │   ├── txtDetailCreated, txtDetailScheduled
                │   ├── txtDetailStarted, txtDetailCompleted
                │   ├── txtDetailPrice, txtDetailDistance
                │   ├── txtDetailDuration, txtDetailVehicle
                │   └── txtDetailOptions
                ├── MaterialCardView (cardCancellation) - conditional
                │   ├── txtCancelledBy, txtCancelledAt
                │   └── txtCancellationReason
                ├── MaterialCardView (cardPanic) - conditional
                │   └── txtPanickedBy
                ├── MaterialCardView (cardDriver)
                │   └── txtDetailDriver (multi-line)
                ├── MaterialCardView (cardPassengers)
                │   └── LinearLayout (containerPassengers)
                ├── MaterialCardView (cardRatings)
                │   └── txtDetailRatings
                └── MaterialCardView (cardInconsistencies)
                    └── txtDetailInconsistencies
```

---

## Phase 6: Navigation Integration

### 6.1 Update nav_graph.xml
```xml
<fragment
    android:id="@+id/adminHistoryFragment"
    android:name="com.example.blackcar.presentation.history.views.AdminHistoryFragment"
    android:label="@string/title_admin_history"
    tools:layout="@layout/fragment_admin_history" />

<!-- Add action from homeFragment -->
<action
    android:id="@+id/action_home_to_admin_history"
    app:destination="@id/adminHistoryFragment"
    app:enterAnim="@android:anim/slide_in_left"
    app:exitAnim="@android:anim/slide_out_right"
    app:popEnterAnim="@android:anim/slide_in_left"
    app:popExitAnim="@android:anim/slide_out_right" />
```

### 6.2 Update HomeFragment (optional)
- Check user role from SessionManager
- Show "All Rides" option for ADMIN role
- Navigate to AdminHistoryFragment on click

---

## Phase 7: String Resources

**Add to `res/values/strings.xml`:**
```xml
<string name="title_admin_history">All Rides</string>
<string name="admin_rides_count">%d rides found</string>
<string name="no_rides_found">No rides found</string>
<string name="try_adjusting_filter">Try adjusting your date filter</string>
<string name="ride_details_title">Ride Details #%d</string>
<string name="label_created">Created</string>
<string name="label_scheduled">Scheduled</string>
<string name="label_started">Started</string>
<string name="label_completed">Completed</string>
<string name="label_cancelled_by">Cancelled by</string>
<string name="label_cancelled_at">Cancelled at</string>
<string name="label_cancellation_reason">Reason</string>
<string name="label_panic_activated">Panic Activated</string>
<string name="label_panicked_by">Activated by</string>
<string name="passengers_count">%d passenger(s)</string>
```

---

## Implementation Order

| Phase | Description | Estimated Files |
|-------|-------------|-----------------|
| 1 | API Layer (models, service, repository) | 5 files |
| 2 | ViewStates | 3 files |
| 3 | ViewModels | 2 files |
| 5 | Layouts (XML) | 3 files |
| 4 | Views (Fragment, Adapter) | 3 files |
| 6 | Navigation | 1 file (edit) |
| 7 | Strings | 1 file (edit) |

**Total: ~18 files (15 new, 3 edits)**

---

## Reusable Components

From existing passenger history implementation:
- `ShakeDetector` - shake detection for sort toggle
- `MapHelper` - map markers and route drawing
- Spinner layouts (`spinner_item_white.xml`, `spinner_dropdown_item_white.xml`)
- Color resources and styles
- Date formatting patterns

---

## API Endpoints Summary

| Endpoint | Method | Request | Response |
|----------|--------|---------|----------|
| `/rides/history/admin/all?page=X&size=Y` | POST | AdminRideHistoryFilter | PaginatedResponse<AdminRideHistoryResponse> |
| `/rides/admin/{id}` | GET | - | AdminRideDetailResponse |
