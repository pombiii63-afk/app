# 🚀 Campus Delivery (Collegiate Peer-to-Peer Gig Platform)
### Complete Startup-Grade Architecture, Database Schema, and Flutter Engineering Playbook

This document serves as the **Production Architecture Specification and Scalability Roadmap** for migrating the compiled **Campus Delivery** Material 3 Android engine to a cross-platform **Flutter and Firebase** cluster.

---

## 1. 📂 Flutter Code Structure & Directory Framework
To support a high-growth startup environment, we recommend a **Feature-First Architecture** combined with Domain-Driven Design (DDD) principles. This ensures that independent teams can work on different segments of the app (such as Driver Payouts vs. Customer Checkout) without source conflicts.

### Recommended Folder Structure
```text
lib/
├── main.dart                      # App entry point, Firebase pre-initialization block
├── app/
│   ├── theme/                     # Purple & White custom color pairings, custom styles
│   └── routes/                    # Type-safe routing mapping table and guards
├── core/                          # Cross-cutting concerns
│   ├── firebase/                  # Firestore endpoints, Firebase Auth wrapper API
│   ├── database/                  # local Hive or Isar database for caching offline states
│   └── utils/                     # Location converters, currency format widgets
└── features/                      # Autonomous business capabilities
    ├── auth/                      # Phone OTP validation, register sheets, student verify
    │   ├── data/                  # Auth state sources and repositories
    │   ├── domain/                # Profile models, student email validation policies
    │   └── presentation/          # LoginScreen, RegisterScreen, OtpInputWidget
    ├── order_feed/                # Live order maps, filter chips, category slides
    │   ├── data/
    │   ├── domain/                # Order status enum (Pending -> Accepted -> Delivered)
    │   └── presentation/          # FeedScreen, OrderCardComponent, SearchQueryBar
    ├── order_tracking/            # Real-time state progress timeline and maps
    │   ├── presentation/          # PathTrackerScreen, RatingReviewSheet, DisputeReportSheet
    ├── driver_dashboard/          # Weekly progress charts, earnings analytics
    │   ├── presentation/          # EarningsDashboard, ProgressionCanvasPlot, CarrierRunsList
    └── admin_moderation/          # User directories, account locks, infraction claims
        └── presentation/          # OperationCenterScreen, DisputeCard, RegisterAuditorTable
```

---

## 2. 🗄️ Firebase Database Schema (Firestore Collections)
Designed to scale to millions of orders across thousands of sub-university campuses. Includes structural optimization strategies including denormalization to keep query costs minimum.

### Collection `/users`
Stores student accounts, phone indexes, registration verification states and active indices.
```json
{
  "registrationNumber": "REG-2026-X11", // Document ID
  "name": "Sarah Chen",
  "phoneNumber": "+1555018844",
  "email": "schen@univ.edu",             // Enforced *.edu constraint
  "role": "PARTNER",                      // CUSTOMER or PARTNER or ADMIN
  "reliabilityScore": 100,               // Standard range: 0 to 100
  "strikes": 0,                          // Account lock trigger limit: 3
  "isSuspended": false,
  "completedDeliveriesCount": 24,
  "averageRating": 4.9,
  "createdAt": "2026-06-04T07:48:39Z"
}
```

### Collection `/orders`
Tracks the lifecycle of deliveries. Includes denormalized client details to avoid duplicate read queries on feed rendering.
```json
{
  "orderId": "ORD_78f192bc",             // Document ID (UUID)
  "itemName": "Bio-Chemistry Lab printed Manual",
  "pickupLocation": "Admin Xerox & Print Cafe",
  "dropLocation": "Science Annex, Lab Room 12",
  "deliveryFee": 5.00,
  "status": "ACCEPTED",                  // PENDING -> ACCEPTED -> PICKED_UP -> DELIVERED -> CANCELLED
  "notes": "Text when you reach the elevators",
  "customer": {
    "phone": "+1555018844",
    "name": "Sarah Chen",
    "email": "schen@univ.edu"
  },
  "deliveryPartner": {
    "registrationNumber": "DEV1001",
    "name": "Ethan Cole"
  },
  "rating": 0,                           // Set post-delivery 1.0 to 5.0
  "isReported": false,
  "reportReason": null,
  "timestamp": "2026-06-04T07:48:39Z"
}
```

### Collection `/earnings`
Tracks individual payouts. Sub-collection under `/users` or top-level partitioned database.
```json
{
  "earningId": "ERN_99a8d77c",           // Document ID
  "partnerId": "DEV1001",
  "orderId": "ORD_78f192bc",
  "amount": 5.00,
  "description": "Delivery: Biology Printed Manual",
  "timestamp": "2026-06-04T07:54:10Z"
}
```

### Collection `/reports`
Saves student complaints and automatic system flags to preserve trust.
```json
{
  "reportId": "REP_44d210ff",            // Document ID
  "orderId": "ORD_78f192bc",
  "reporterName": "Sarah Chen",
  "reportedRegNumber": "DEV1001",
  "reason": "Rider marked delivered but package never arrived.",
  "status": "PENDING",                   // PENDING -> RESOLVED -> DISMISSED
  "timestamp": "2026-06-04T07:55:00Z"
}
```

---

## 3. 🛡️ Firestore Security Rules
Enforce student safety constraints directly at the database layers. Prevents users from modifying other people's orders or changing their strike counts.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // User profile constraints
    match /users/{userId} {
      allow read: if request.auth != null;
      // Only users themselves or admin can write bio. Strikes can only be updated by Admin.
      allow write: if request.auth != null && (request.auth.uid == userId || resource.data.role == 'ADMIN');
    }
    
    // Delivery Order pipelines
    match /orders/{orderId} {
      allow read: if request.auth != null;
      // Creator can place. Riders can accept/modify status if they match partner credentials.
      allow create: if request.auth != null && request.resource.data.status == 'PENDING';
      allow update: if request.auth != null && (
        request.resource.data.customer.phone == resource.data.customer.phone || 
        request.resource.data.deliveryPartner.registrationNumber == request.auth.token.registrationNumber ||
        request.auth.token.role == 'ADMIN'
      );
    }
    
    // Earnings registers are append-only for riders, readable by earnings holder
    match /earnings/{earningId} {
      allow read: if request.auth != null && resource.data.partnerId == request.auth.token.registrationNumber;
      allow create: if request.auth != null && request.auth.token.role == 'PARTNER';
      allow update, delete: if false; // immutable history
    }
    
    // Staff disputes review
    match /reports/{reportId} {
      allow create: if request.auth != null;
      allow read, update, delete: if request.auth != null && request.auth.token.role == 'ADMIN';
    }
  }
}
```

---

## 4. ⚙️ Firebase Integration Architecture Plan

### Step A: Phone Authentication (OTP Flow)
1. **Trigger OTP Dispatch**: Submit subscriber phone to `FirebaseAuth.instance.verifyPhoneNumber`.
2. **Handle Handshakes**: Feed received auto-SMS token or verification digits to `PhoneAuthProvider.credential`.
3. **Link Registries**: On verification success, use the returned user UID to look up Firestore `/users` registry. If absent, navigate the user to the `.edu` email and registration number verification sheet.

### Step B: Storage Architecture for Assets
- **Location**: Use **Firebase Storage** (`gs://campus-delivery-bucket/`).
- **Profile Avatars**: Store photos in `/users/{registrationNumber}/avatar.png` utilizing Coil / cached image network in Flutter.
- **Delivery Proof Photos**: At droppoint, delivery partners must upload a proof photo to `/orders/{orderId}/drop_proof.png` before being permitted to mark the run status as "DELIVERED", reinforcing peer-to-peer accountability.

---

## 5. 🗺️ Startup Scalability & Trust Roadmap

```text
Phase 1: Campus Launch (Weeks 1 - 4)
  │  ├── Deploy local beta network with Compose mock sandbox (Completed)
  │  ├── Hook up Firebase Auth sandbox for campus QA
  │  └── Establish Trust Index framework (Strikes & Peer Ratings)
  ▼
Phase 2: Live Operation & Delivery Proof Proofing (Weeks 5 - 8)
  │  ├── Add mandatory "Package Drop Proof Photo" upload in Firebase Storage
  │  ├── Enable geolocation clustering so users only view orders from their specific campus
  │  └── Establish automatic dispute resolution for cancels under 5 minutes
  ▼
Phase 3: Automated Trust & Growth Hack (Weeks 9+)
  │  ├── Integrate real-time Route Tracking using OpenStreetMap API
  │  ├── Roll out automated Chat with preloaded safe-conversation prompts
  │  └── Launch Campus Leaderboard displaying top positive student runners with fee bonuses
```
