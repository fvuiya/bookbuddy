# 🚀 Firebase Realtime Database Setup Guide

## ✅ What's Configured

You now have Firebase Realtime Database integration instead of Firestore/Storage (which require Blaze Plan).

### Spark Plan Compatible Services:
- ✅ **Authentication** (Email/Password + Anonymous) - FREE
- ✅ **Realtime Database** - FREE (1GB storage, unlimited connections)
- ✅ **Analytics** - FREE

### Not Available in Spark Plan:
- ❌ Firestore (requires Blaze Plan)
- ❌ Storage (requires Blaze Plan)
- ❌ Cloud Functions (requires Blaze Plan)
- ❌ Crashlytics (requires Blaze Plan)

---

## 📋 Setup Steps

### Step 1: Enable Realtime Database in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Realtime Database** (left sidebar)
4. Click **Create Database**
5. Choose location closest to your users
6. Start in **test mode** (we'll add security rules below)
7. Click **Enable**

### Step 2: Set Up Security Rules

In Firebase Console → Realtime Database → Rules tab, replace with:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": true,
        ".write": "$uid === auth.uid"
      }
    },
    "usernames": {
      "$username": {
        ".read": true,
        ".write": "!data.exists() && newData.val() === auth.uid"
      }
    },
    "books": {
      "$bookId": {
        ".read": true,
        ".write": "auth != null && (!data.exists() || data.child('authorId').val() === auth.uid)"
      }
    },
    "posts": {
      "$postId": {
        ".read": true,
        ".write": "auth != null && (!data.exists() || data.child('userId').val() === auth.uid)"
      }
    },
    "comments": {
      "$postId": {
        "$commentId": {
          ".read": true,
          ".write": "auth != null"
        }
      }
    },
    "friendRequests": {
      "$userId": {
        ".read": "auth != null && auth.uid === $userId",
        "$requestId": {
          ".write": "auth != null && (newData.child('fromUserId').val() === auth.uid || newData.child('toUserId').val() === auth.uid)"
        }
      }
    },
    "friendships": {
      "$userId": {
        ".read": "auth != null",
        "$friendId": {
          ".write": "auth != null && auth.uid === $userId"
        }
      }
    },
    "notifications": {
      "$userId": {
        ".read": "auth != null && auth.uid === $userId",
        "$notificationId": {
          ".write": "auth != null"
        }
      }
    },
    "postLikes": {
      "$postId": {
        "$userId": {
          ".read": true,
          ".write": "auth != null && auth.uid === $userId"
        }
      }
    },
    "bookLikes": {
      "$bookId": {
        "$userId": {
          ".read": true,
          ".write": "auth != null && auth.uid === $userId"
        }
      }
    },
    "bookReviews": {
      "$bookId": {
        "$reviewId": {
          ".read": true,
          ".write": "auth != null"
        }
      }
    }
  }
}
```

Click **Publish** to save the rules.

---

## 📊 Database Structure

```
{
  "users": {
    "{uid}": {
      "uid": "string",
      "email": "string",
      "displayName": "string",
      "username": "string",
      "bio": "string",
      "profilePictureUrl": "string (Base64 or URL)",
      "createdAt": 123456789,
      "updatedAt": 123456789,
      "friendsCount": 0,
      "booksPublished": 0
    }
  },
  "usernames": {
    "{username}": "{uid}"  // Index for username lookup
  },
  "books": {
    "{bookId}": {
      "bookId": "string",
      "title": "string",
      "author": "string",
      "authorId": "string",
      "description": "string",
      "language": "string",
      "category": "string",
      "coverImageBase64": "string (small image)",
      "contentUrl": "string (external URL or Base64)",
      "isDraft": true,
      "createdAt": 123456789,
      "publishedAt": 123456789,
      "downloadsCount": 0,
      "likesCount": 0,
      "fileSize": 0,
      "pageCount": 0
    }
  },
  "posts": {
    "{postId}": {
      "postId": "string",
      "userId": "string",
      "username": "string",
      "content": "string",
      "bookId": "string (optional)",
      "createdAt": 123456789,
      "likesCount": 0,
      "commentsCount": 0,
      "sharesCount": 0
    }
  },
  "comments": {
    "{postId}": {
      "{commentId}": {
        "commentId": "string",
        "userId": "string",
        "username": "string",
        "content": "string",
        "createdAt": 123456789
      }
    }
  },
  "friendRequests": {
    "{toUserId}": {
      "{requestId}": {
        "requestId": "string",
        "fromUserId": "string",
        "fromUsername": "string",
        "toUserId": "string",
        "status": "pending|accepted|rejected",
        "createdAt": 123456789
      }
    }
  },
  "friendships": {
    "{userId}": {
      "{friendId}": {
        "friendshipId": "string",
        "friendId": "string",
        "friendUsername": "string",
        "createdAt": 123456789
      }
    }
  },
  "notifications": {
    "{userId}": {
      "{notificationId}": {
        "notificationId": "string",
        "type": "friend_request|like|comment|etc",
        "title": "string",
        "message": "string",
        "relatedId": "string",
        "isRead": false,
        "createdAt": 123456789
      }
    }
  },
  "postLikes": {
    "{postId}": {
      "{userId}": {
        "userId": "string",
        "createdAt": 123456789
      }
    }
  },
  "bookLikes": {
    "{bookId}": {
      "{userId}": {
        "userId": "string",
        "createdAt": 123456789
      }
    }
  }
}
```

---

## 💾 Book Storage Strategy (No Firebase Storage)

Since Firebase Storage requires Blaze Plan, we have these alternatives:

### Option 1: Base64 Encoding (Small Books)
- Store small text books (< 100KB) as Base64 in Realtime Database
- Good for: Short stories, articles, essays
- Limit: 1GB total database size

### Option 2: External Hosting
- Host book files on free services:
  - **GitHub Pages** (100GB free bandwidth/month)
  - **Cloudinary** (25GB free storage + CDN)
  - **ImgBB** (Free image hosting, works for Base64 text)
- Store URLs in Realtime Database

### Option 3: Peer-to-Peer Sharing
- Share book content directly between devices
- Use Realtime Database for metadata only

### Recommended Approach:
- **Cover images:** Store as small Base64 (< 50KB) in database
- **Small books:** Store as Base64 in database
- **Large books:** Host on GitHub Pages or Cloudinary, store URL

---

## 🧪 Testing

### Test Authentication

```bash
cd /Users/macbookair/AndroidStudioProjects/bookbuddy
./gradlew installDebug
```

1. Run the app
2. Click "Sign Up"
3. Create account with:
   - Email: test@example.com
   - Password: test123456
   - Display Name: Test User
   - Username: testuser
4. Check Firebase Console → Authentication → Users
5. Check Realtime Database → Data → users → {uid}

### Verify Database Structure

In Firebase Console → Realtime Database → Data:
- You should see `users/{uid}` node
- You should see `usernames/testuser` → uid

---

## 📊 Spark Plan Limits

### Realtime Database (FREE)
- ✅ **1 GB stored data**
- ✅ **10 GB/month downloaded**
- ✅ **Unlimited connections**
- ✅ **100 simultaneous connections**

### Authentication (FREE)
- ✅ **Unlimited users**
- ✅ **Unlimited sign-ins**

### What This Means:
- ~10,000 small books (100KB each) OR
- ~2,000 medium books (500KB each)
- Thousands of users
- Real-time sync for all features
- All completely FREE!

---

## 🔄 Switch from Mock to Real Service

Update `SocialActivity.kt`:

```kotlin
// Before:
val socialViewModel: SocialViewModel = viewModel { 
    SocialViewModel(MockSocialService()) 
}

// After:
val socialViewModel: SocialViewModel = viewModel { 
    SocialViewModel(FirebaseSocialService()) 
}
```

---

## 🎯 Next Steps

1. ✅ Complete Firebase Console setup (above)
2. ✅ Test authentication flow
3. ✅ Switch to FirebaseSocialService
4. ✅ Test creating posts
5. ✅ Implement book publishing
6. ✅ Add friend system
7. ✅ Enable notifications

---

## 🆘 Troubleshooting

### "Permission denied" errors
- **Solution:** Check security rules in Firebase Console
- Make sure rules are published
- Verify user is authenticated

### Data not syncing
- **Solution:** Check internet connection
- Verify database is enabled in Firebase Console
- Check for errors in Android Logcat

### "Database not found"
- **Solution:** Make sure you enabled Realtime Database in console
- Check that google-services.json is in app/ directory

---

## 📚 Resources

- [Realtime Database Docs](https://firebase.google.com/docs/database)
- [Security Rules Guide](https://firebase.google.com/docs/database/security)
- [Firebase Console](https://console.firebase.google.com/)

