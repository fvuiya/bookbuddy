# Firebase Setup Guide for BookBuddy

## Current Status
- ✅ Social features implemented with MockSocialService
- ⏳ Firebase integration needed to make features live
- 💡 Using Firebase Spark Plan (free tier, no credit card required)

## Step-by-Step Setup Plan

### Step 1: Create Firebase Project (Web Console)
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name: `bookbuddy` (or your preference)
4. Disable Google Analytics (optional, can enable later)
5. Click "Create project"

### Step 2: Add Android App to Firebase Project
1. In Firebase Console, click the Android icon to add an Android app
2. Register app with package name: `com.vuiya.bookbuddy`
3. App nickname: `BookBuddy` (optional)
4. Debug signing certificate SHA-1: (optional for now, needed later for Google Sign-In)
5. Download `google-services.json`
6. Place it in `/Users/macbookair/AndroidStudioProjects/bookbuddy/app/` directory

### Step 3: Add Firebase SDK to Project (Done Below)
This is automated - see the gradle file changes below.

### Step 4: Enable Firebase Services in Console

#### Authentication
1. Go to Firebase Console → Authentication → Get Started
2. Enable sign-in methods:
   - ✅ Email/Password (enable)
   - ✅ Google (enable - requires SHA-1 certificate)
   - ✅ Anonymous (enable for guest users)

#### Firestore Database
1. Go to Firebase Console → Firestore Database → Create Database
2. Choose "Start in **test mode**" (we'll add security rules later)
3. Select Cloud Firestore location: Choose closest to your users
4. Click "Enable"

#### Firebase Storage
1. Go to Firebase Console → Storage → Get Started
2. Start in **test mode** (we'll add security rules later)
3. Click "Done"

### Step 5: Configure Security Rules (Important!)

#### Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User profiles - users can read all, write own
    match /users/{userId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Books - anyone can read, authenticated users can write
    match /books/{bookId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && 
                               request.auth.uid == resource.data.authorId;
    }
    
    // Posts - anyone can read, authenticated users can write
    match /posts/{postId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && 
                               request.auth.uid == resource.data.userId;
    }
    
    // Comments - anyone can read, authenticated users can write
    match /comments/{commentId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && 
                               request.auth.uid == resource.data.userId;
    }
    
    // Friend requests
    match /friendRequests/{requestId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && 
        (request.auth.uid == resource.data.fromUserId || 
         request.auth.uid == resource.data.toUserId);
    }
    
    // Notifications
    match /notifications/{notificationId} {
      allow read: if request.auth != null && 
                     request.auth.uid == resource.data.userId;
      allow create: if request.auth != null;
    }
  }
}
```

#### Storage Security Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Book files - anyone can read, authenticated users can upload
    match /books/{bookId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // User profile pictures
    match /profiles/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Book covers
    match /covers/{bookId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

### Step 6: Firestore Data Structure

```
users/
  {userId}/
    - uid: string
    - email: string
    - displayName: string
    - username: string (unique)
    - bio: string
    - profilePictureUrl: string
    - createdAt: timestamp
    - updatedAt: timestamp
    - friendsCount: number
    - booksPublished: number

books/
  {bookId}/
    - bookId: string
    - title: string
    - author: string
    - authorId: string (user uid)
    - description: string
    - language: string
    - category: string
    - coverUrl: string
    - fileUrl: string (Firebase Storage path)
    - isDraft: boolean
    - createdAt: timestamp
    - updatedAt: timestamp
    - publishedAt: timestamp
    - downloadsCount: number
    - likesCount: number
    - reviewsCount: number
    - averageRating: number

posts/
  {postId}/
    - postId: string
    - userId: string
    - username: string
    - content: string
    - bookId: string (optional)
    - bookTitle: string (optional)
    - createdAt: timestamp
    - likesCount: number
    - commentsCount: number
    - sharesCount: number

comments/
  {commentId}/
    - commentId: string
    - postId: string
    - userId: string
    - username: string
    - content: string
    - createdAt: timestamp

friendRequests/
  {requestId}/
    - requestId: string
    - fromUserId: string
    - toUserId: string
    - status: string (pending/accepted/rejected)
    - createdAt: timestamp

notifications/
  {notificationId}/
    - notificationId: string
    - userId: string (recipient)
    - type: string (friend_request/new_book/like/comment/share)
    - title: string
    - message: string
    - relatedId: string (bookId, postId, etc.)
    - isRead: boolean
    - createdAt: timestamp
```

## Implementation Checklist

### Firebase Integration (Priority 1)
- [ ] Create Firebase project in console
- [ ] Download and add google-services.json
- [ ] Add Firebase dependencies to build.gradle
- [ ] Sync and verify Firebase SDK integration

### Authentication UI (Priority 2)
- [ ] Create LoginActivity in Compose
- [ ] Create SignupActivity in Compose
- [ ] Implement Email/Password authentication
- [ ] Implement Google Sign-In
- [ ] Add "Continue as Guest" (Anonymous auth)
- [ ] Create user profile setup flow
- [ ] Add logout functionality

### Replace MockSocialService (Priority 3)
- [ ] Create FirebaseSocialService implementing SocialService interface
- [ ] Implement user profile CRUD operations
- [ ] Implement friend request system with Firestore
- [ ] Implement post creation and retrieval
- [ ] Implement comments and likes
- [ ] Implement notifications with FCM
- [ ] Switch SocialActivity to use FirebaseSocialService

### Book Publishing (Priority 4)
- [ ] Upload book files to Firebase Storage
- [ ] Save book metadata to Firestore
- [ ] Implement book discovery/browsing
- [ ] Add book download functionality
- [ ] Implement search and filtering

### Offline Support (Priority 5)
- [ ] Enable Firestore offline persistence
- [ ] Implement local caching strategy
- [ ] Handle sync conflicts
- [ ] Show online/offline status

## Next Steps
1. Complete Step 1-2 in Firebase Console (manual)
2. Run the gradle changes (automated below)
3. Place google-services.json in app/ directory (manual)
4. Start implementing LoginActivity

