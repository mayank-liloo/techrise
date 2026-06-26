# SecureCRM — Smart Customer & Support Management System

SecureCRM is a secure, end-to-end customer relationship and support management system. It features a modern Android application built with Jetpack Compose for customers to submit and track support complaints, backed by a Node.js & Express server connected to Firebase Firestore and an administrative Web Portal for employees to manage tickets, publish announcements, and update homepage marketing banners in real time.

---

## 🚀 Key Features

* **Complaint Tracking**: Customers can log complaints with description and camera image uploads, tracking state updates (Pending, In Progress, Resolved) and providing ratings/comments.
* **Employee Management**: Token-based administrative authentication to register authorized support staff members without needing a registration secret key.
* **Banner Manager**: Dynamic sliding banner manager allowing administrative staff to upload marketing slide graphics directly from the web portal.
* **Announcement Board**: Live news feed bulletins published directly to the customer-facing mobile feeds.
* **Security Middleware**: Robust JWT token authorization, cross-origin resource sharing (CORS), and secure HTTP headers (Helmet.js) shielding backend operations.

---

## 🛠️ Technology Stack

### Mobile Frontend
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (MVVM Architecture)
- **Networking**: Retrofit & OkHttp
- **Dependency Injection**: Hilt

### Backend & Database
- **Runtime**: Node.js & Express
- **Database**: Google Firebase Firestore (Cloud persistence)
- **Deployment**: Automatic Subtree CI/CD on Render

---

## 📦 How to Run Backend Locally

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Set up environment variables in `.env` (Port, Firebase credentials).
4. Run in development mode:
   ```bash
   npm run dev
   ```
