package com.vuiya.bookbuddy.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.vuiya.bookbuddy.models.*
import com.vuiya.bookbuddy.models.firebase.*
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Firebase Realtime Database implementation of SocialService
 * Spark Plan Compatible - uses only free tier features
 */
class FirebaseSocialService : SocialService {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    // Convert Realtime models to app models
    private fun RealtimeUser.toUserProfile() = UserProfile(
        userId = uid,
        username = username,
        displayName = displayName,
        email = email,
        bio = bio,
        profilePictureUrl = profilePictureUrl,
        joinDate = createdAt,
        totalBooks = booksPublished,
        followersCount = 0, // TODO: track this
        followingCount = 0  // TODO: track this
    )

    private fun RealtimePost.toPost() = Post(
        postId = postId,
        userId = userId,
        username = username,
        userProfilePic = userProfilePictureUrl,
        content = content,
        bookId = bookId,
        bookTitle = bookTitle,
        timestamp = createdAt,
        likesCount = likesCount,
        commentsCount = commentsCount,
        sharesCount = sharesCount
    )

    private fun RealtimeBook.toSocialBook() = SocialBook(
        bookId = bookId,
        title = title,
        author = author,
        description = description,
        coverImageUrl = coverImageBase64,
        downloadUrl = contentUrl,
        uploadDate = publishedAt,
        uploaderId = authorId,
        uploaderUsername = author,
        language = language,
        fileSize = fileSize,
        downloadsCount = downloadsCount,
        likesCount = likesCount,
        commentsCount = 0, // TODO: track this
        isPublic = !isDraft
    )

    private fun RealtimeNotification.toNotification() = Notification(
        notificationId = notificationId,
        userId = userId,
        type = when (type) {
            "friend_request" -> NotificationType.FRIEND_REQUEST
            "friend_accepted" -> NotificationType.FRIEND_ACCEPTED
            "new_book" -> NotificationType.BOOK_SHARED
            "like" -> NotificationType.BOOK_LIKED
            "comment" -> NotificationType.BOOK_COMMENTED
            else -> NotificationType.GENERIC
        },
        title = title,
        message = message,
        relatedUserId = relatedUsername,
        relatedBookId = relatedId,
        timestamp = createdAt,
        isRead = isRead
    )

    private fun RealtimeFriendRequest.toFriendRequest() = FriendRequest(
        requestId = requestId,
        senderId = fromUserId,
        receiverId = toUserId,
        status = when (status) {
            "accepted" -> RequestStatus.ACCEPTED
            "rejected" -> RequestStatus.REJECTED
            else -> RequestStatus.PENDING
        },
        timestamp = createdAt
    )

    // User Profile Methods
    override suspend fun getCurrentUserProfile(): Result<UserProfile> = runCatching {
        if (currentUserId.isEmpty()) throw Exception("Not authenticated")
        val user = getUser(currentUserId) ?: throw Exception("User not found")
        user.toUserProfile()
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<Boolean> = runCatching {
        if (currentUserId.isEmpty()) throw Exception("Not authenticated")
        database.child("users").child(currentUserId).updateChildren(
            mapOf(
                "displayName" to profile.displayName,
                "bio" to profile.bio,
                "profilePictureUrl" to profile.profilePictureUrl,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
        true
    }

    override suspend fun getUserProfile(userId: String): Result<UserProfile> = runCatching {
        val user = getUser(userId) ?: throw Exception("User not found")
        user.toUserProfile()
    }

    // Friend/Follow Methods
    override suspend fun sendFriendRequest(receiverId: String): Result<Boolean> = runCatching {
        if (currentUserId.isEmpty()) throw Exception("Not authenticated")

        val currentUser = getUser(currentUserId) ?: throw Exception("User not found")
        val targetUser = getUser(receiverId) ?: throw Exception("Target user not found")

        val requestId = database.child("friendRequests").child(receiverId).push().key
            ?: throw Exception("Failed to generate request ID")

        val request = RealtimeFriendRequest(
            requestId = requestId,
            fromUserId = currentUserId,
            fromUsername = currentUser.username,
            fromDisplayName = currentUser.displayName,
            toUserId = receiverId,
            toUsername = targetUser.username,
            status = "pending",
            createdAt = System.currentTimeMillis()
        )

        database.child("friendRequests").child(receiverId).child(requestId).setValue(request).await()

        // Create notification
        val notificationId = database.child("notifications").child(receiverId).push().key
            ?: throw Exception("Failed to generate notification ID")
        val notification = RealtimeNotification(
            notificationId = notificationId,
            userId = receiverId,
            type = "friend_request",
            title = "New Friend Request",
            message = "${currentUser.displayName} sent you a friend request",
            relatedId = currentUserId,
            relatedUsername = currentUser.username,
            isRead = false,
            createdAt = System.currentTimeMillis()
        )

        database.child("notifications").child(receiverId).child(notificationId).setValue(notification).await()
        true
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Boolean> = runCatching {
        // TODO: Implement
        true
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Boolean> = runCatching {
        // TODO: Implement
        true
    }

    override suspend fun cancelFriendRequest(requestId: String): Result<Boolean> = runCatching {
        // TODO: Implement
        true
    }

    override suspend fun unfriend(userId: String): Result<Boolean> = runCatching {
        // TODO: Implement
        true
    }

    override suspend fun getPendingFriendRequests(): Result<List<FriendRequest>> = runCatching {
        if (currentUserId.isEmpty()) throw Exception("Not authenticated")

        suspendCoroutine { continuation ->
            database.child("friendRequests").child(currentUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val requests = mutableListOf<FriendRequest>()
                        snapshot.children.forEach { child ->
                            child.getValue(RealtimeFriendRequest::class.java)?.let {
                                if (it.status == "pending") {
                                    requests.add(it.toFriendRequest())
                                }
                            }
                        }
                        continuation.resume(requests)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        throw error.toException()
                    }
                })
        }
    }

    override suspend fun getFriendsList(): Result<List<UserProfile>> = runCatching {
        // TODO: Implement properly
        emptyList()
    }

    override suspend fun getFollowers(): Result<List<UserProfile>> = runCatching {
        // TODO: Implement
        emptyList()
    }

    override suspend fun getFollowing(): Result<List<UserProfile>> = runCatching {
        // TODO: Implement
        emptyList()
    }

    override suspend fun followUser(userId: String): Result<Boolean> = runCatching {
        // TODO: Implement
        true
    }

    override suspend fun unfollowUser(userId: String): Result<Boolean> = runCatching {
        // TODO: Implement
        true
    }

    // Book Sharing Methods
    override suspend fun uploadBook(book: SocialBook, localFilePath: String): Result<String> = runCatching {
        // TODO: Implement book upload (Base64 or external hosting)
        "book_id_placeholder"
    }

    override suspend fun getPublicBooks(limit: Int, offset: Int): Result<List<SocialBook>> = runCatching {
        suspendCoroutine { continuation ->
            database.child("books")
                .orderByChild("publishedAt")
                .limitToLast(limit)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val books = mutableListOf<SocialBook>()
                        snapshot.children.forEach { child ->
                            child.getValue(RealtimeBook::class.java)?.let {
                                if (!it.isDraft) {
                                    books.add(it.toSocialBook())
                                }
                            }
                        }
                        continuation.resume(books.reversed())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Return empty list instead of throwing exception
                        android.util.Log.w("FirebaseSocialService",
                            "Failed to load books: ${error.message}. " +
                            "Make sure Realtime Database is enabled and security rules are set.")
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    override suspend fun getFriendBooks(limit: Int, offset: Int): Result<List<SocialBook>> = runCatching {
        // TODO: Filter by friends
        getPublicBooks(limit, offset).getOrThrow()
    }

    override suspend fun searchBooks(query: String, limit: Int, offset: Int): Result<List<SocialBook>> = runCatching {
        // TODO: Implement search
        emptyList()
    }

    override suspend fun downloadBook(bookId: String, destinationPath: String): Result<Boolean> = runCatching {
        // TODO: Implement book download
        true
    }

    override suspend fun deleteUploadedBook(bookId: String): Result<Boolean> = runCatching {
        // TODO: Implement
        true
    }

    override suspend fun getBookDetails(bookId: String): Result<SocialBook> = runCatching {
        suspendCoroutine { continuation ->
            database.child("books").child(bookId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val book = snapshot.getValue(RealtimeBook::class.java)
                        if (book != null) {
                            continuation.resume(book.toSocialBook())
                        } else {
                            throw Exception("Book not found")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        throw error.toException()
                    }
                })
        }
    }

    // Feed/Post Methods
    override suspend fun createPost(content: String, bookId: String?): Result<String> = runCatching {
        if (currentUserId.isEmpty()) throw Exception("Not authenticated")

        val user = getUser(currentUserId) ?: throw Exception("User not found")
        val postId = database.child("posts").push().key ?: throw Exception("Failed to generate post ID")

        val post = RealtimePost(
            postId = postId,
            userId = currentUserId,
            username = user.username,
            userProfilePictureUrl = user.profilePictureUrl,
            content = content,
            bookId = bookId ?: "",
            bookTitle = "", // TODO: Fetch if bookId provided
            createdAt = System.currentTimeMillis(),
            likesCount = 0,
            commentsCount = 0,
            sharesCount = 0
        )

        database.child("posts").child(postId).setValue(post).await()
        postId
    }

    override suspend fun getFeedPosts(limit: Int, offset: Int): Result<List<Post>> = runCatching {
        suspendCoroutine { continuation ->
            database.child("posts")
                .orderByChild("createdAt")
                .limitToLast(limit)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val posts = mutableListOf<Post>()
                        snapshot.children.forEach { child ->
                            child.getValue(RealtimePost::class.java)?.let {
                                posts.add(it.toPost())
                            }
                        }
                        continuation.resume(posts.reversed())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Return empty list instead of throwing exception
                        // This prevents crashes when database is not configured
                        android.util.Log.w("FirebaseSocialService",
                            "Failed to load posts: ${error.message}. " +
                            "Make sure Realtime Database is enabled and security rules are set.")
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    override suspend fun getFriendActivity(limit: Int, offset: Int): Result<List<Post>> = runCatching {
        // TODO: Filter by friends
        getFeedPosts(limit, offset).getOrThrow()
    }

    override suspend fun getBookPosts(bookId: String, limit: Int, offset: Int): Result<List<Post>> = runCatching {
        // TODO: Filter by bookId
        emptyList()
    }

    override suspend fun deletePost(postId: String): Result<Boolean> = runCatching {
        // TODO: Implement
        true
    }

    // Interaction Methods
    override suspend fun likePost(postId: String): Result<Boolean> = runCatching {
        if (currentUserId.isEmpty()) throw Exception("Not authenticated")

        val likeRef = database.child("postLikes").child(postId).child(currentUserId)
        val postRef = database.child("posts").child(postId)

        suspendCoroutine { continuation ->
            likeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        val like = PostLike(currentUserId, postId, System.currentTimeMillis())
                        likeRef.setValue(like)
                        postRef.child("likesCount").setValue(ServerValue.increment(1))
                        continuation.resume(true)
                    } else {
                        continuation.resume(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    throw error.toException()
                }
            })
        }
    }

    override suspend fun unlikePost(postId: String): Result<Boolean> = runCatching {
        if (currentUserId.isEmpty()) throw Exception("Not authenticated")

        val likeRef = database.child("postLikes").child(postId).child(currentUserId)
        val postRef = database.child("posts").child(postId)

        likeRef.removeValue().await()
        postRef.child("likesCount").setValue(ServerValue.increment(-1)).await()
        true
    }

    override suspend fun addComment(postId: String, content: String): Result<String> = runCatching {
        if (currentUserId.isEmpty()) throw Exception("Not authenticated")

        val user = getUser(currentUserId) ?: throw Exception("User not found")
        val commentId = database.child("comments").child(postId).push().key
            ?: throw Exception("Failed to generate comment ID")

        val comment = RealtimeComment(
            commentId = commentId,
            postId = postId,
            userId = currentUserId,
            username = user.username,
            userProfilePictureUrl = user.profilePictureUrl,
            content = content,
            createdAt = System.currentTimeMillis()
        )

        database.child("comments").child(postId).child(commentId).setValue(comment).await()
        database.child("posts").child(postId).child("commentsCount")
            .setValue(ServerValue.increment(1)).await()
        commentId
    }

    override suspend fun deleteComment(commentId: String): Result<Boolean> = runCatching {
        // TODO: Implement
        true
    }

    override suspend fun getCommentsForPost(postId: String, limit: Int, offset: Int): Result<List<Comment>> = runCatching {
        suspendCoroutine { continuation ->
            database.child("comments").child(postId)
                .limitToLast(limit)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val comments = mutableListOf<Comment>()
                        snapshot.children.forEach { child ->
                            child.getValue(RealtimeComment::class.java)?.let {
                                comments.add(Comment(
                                    commentId = it.commentId,
                                    postId = it.postId,
                                    userId = it.userId,
                                    username = it.username,
                                    userProfilePic = it.userProfilePictureUrl,
                                    content = it.content,
                                    timestamp = it.createdAt
                                ))
                            }
                        }
                        continuation.resume(comments)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        throw error.toException()
                    }
                })
        }
    }

    // Notification Methods
    override suspend fun getNotifications(limit: Int, offset: Int): Result<List<Notification>> = runCatching {
        if (currentUserId.isEmpty()) return@runCatching emptyList()

        suspendCoroutine { continuation ->
            database.child("notifications").child(currentUserId)
                .orderByChild("createdAt")
                .limitToLast(limit)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val notifications = mutableListOf<Notification>()
                        snapshot.children.forEach { child ->
                            child.getValue(RealtimeNotification::class.java)?.let {
                                notifications.add(it.toNotification())
                            }
                        }
                        continuation.resume(notifications.reversed())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Return empty list instead of throwing exception
                        android.util.Log.w("FirebaseSocialService",
                            "Failed to load notifications: ${error.message}")
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    override suspend fun markNotificationAsRead(notificationId: String): Result<Boolean> = runCatching {
        if (currentUserId.isEmpty()) throw Exception("Not authenticated")
        database.child("notifications").child(currentUserId).child(notificationId)
            .child("isRead").setValue(true).await()
        true
    }

    override suspend fun markAllNotificationsAsRead(): Result<Boolean> = runCatching {
        // TODO: Implement batch update
        true
    }

    // Helper methods
    private suspend fun getUser(userId: String): RealtimeUser? = suspendCoroutine { continuation ->
        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    continuation.resume(snapshot.getValue(RealtimeUser::class.java))
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(null)
                }
            })
    }
}

