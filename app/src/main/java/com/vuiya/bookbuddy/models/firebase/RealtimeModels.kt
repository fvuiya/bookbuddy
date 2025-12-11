package com.vuiya.bookbuddy.models.firebase

/**
 * Realtime Database models - optimized for Firebase Realtime Database structure
 * Note: Realtime Database uses JSON structure, so we don't use @DocumentId annotations
 */

/**
 * User model for Realtime Database
 */
data class RealtimeUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "", // Unique username
    val bio: String = "",
    val profilePictureUrl: String = "", // Base64 encoded or external URL
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val friendsCount: Int = 0,
    val booksPublished: Int = 0
)

/**
 * Book model for Realtime Database
 * For Spark Plan: Store small books as Base64 in database, or use external hosting
 */
data class RealtimeBook(
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val authorId: String = "", // User UID
    val description: String = "",
    val language: String = "",
    val category: String = "",
    val coverImageBase64: String = "", // Small cover image as Base64
    val contentUrl: String = "", // External URL or Base64 for small books
    val isDraft: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val publishedAt: Long = 0L,
    val downloadsCount: Int = 0,
    val likesCount: Int = 0,
    val reviewsCount: Int = 0,
    val averageRating: Float = 0f,
    val fileSize: Long = 0L, // In bytes
    val pageCount: Int = 0
)

/**
 * Post model for Realtime Database
 */
data class RealtimePost(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfilePictureUrl: String = "",
    val content: String = "",
    val bookId: String = "", // Optional - if post is about a book
    val bookTitle: String = "", // Optional
    val createdAt: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0
)

/**
 * Comment model
 */
data class RealtimeComment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfilePictureUrl: String = "",
    val content: String = "",
    val createdAt: Long = 0L
)

/**
 * Friend Request model
 */
data class RealtimeFriendRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromDisplayName: String = "",
    val toUserId: String = "",
    val toUsername: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val createdAt: Long = 0L
)

/**
 * Notification model
 */
data class RealtimeNotification(
    val notificationId: String = "",
    val userId: String = "", // Recipient
    val type: String = "other", // friend_request, friend_accepted, new_book, like, comment, share, other
    val title: String = "",
    val message: String = "",
    val relatedId: String = "", // bookId, postId, userId, etc.
    val relatedUsername: String = "", // Who triggered the notification
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)

/**
 * Book Review model
 */
data class RealtimeBookReview(
    val reviewId: String = "",
    val bookId: String = "",
    val userId: String = "",
    val username: String = "",
    val rating: Float = 0f, // 0-5 stars
    val reviewText: String = "",
    val createdAt: Long = 0L
)

/**
 * Friendship model
 */
data class RealtimeFriendship(
    val friendshipId: String = "",
    val userId: String = "",
    val friendId: String = "",
    val friendUsername: String = "",
    val friendDisplayName: String = "",
    val createdAt: Long = 0L
)

/**
 * Post Like - tracking who liked what
 */
data class PostLike(
    val userId: String = "",
    val postId: String = "",
    val createdAt: Long = 0L
)

/**
 * Book Like - tracking who liked what book
 */
data class BookLike(
    val userId: String = "",
    val bookId: String = "",
    val createdAt: Long = 0L
)

/**
 * Realtime Database Structure:
 *
 * {
 *   "users": {
 *     "{uid}": { RealtimeUser object }
 *   },
 *   "usernames": {
 *     "{username}": "{uid}"  // Index for quick username lookup
 *   },
 *   "books": {
 *     "{bookId}": { RealtimeBook object }
 *   },
 *   "posts": {
 *     "{postId}": { RealtimePost object }
 *   },
 *   "comments": {
 *     "{postId}": {
 *       "{commentId}": { RealtimeComment object }
 *     }
 *   },
 *   "friendRequests": {
 *     "{userId}": {
 *       "{requestId}": { RealtimeFriendRequest object }
 *     }
 *   },
 *   "friendships": {
 *     "{userId}": {
 *       "{friendId}": { RealtimeFriendship object }
 *     }
 *   },
 *   "notifications": {
 *     "{userId}": {
 *       "{notificationId}": { RealtimeNotification object }
 *     }
 *   },
 *   "postLikes": {
 *     "{postId}": {
 *       "{userId}": { PostLike object }
 *     }
 *   },
 *   "bookLikes": {
 *     "{bookId}": {
 *       "{userId}": { BookLike object }
 *     }
 *   },
 *   "bookReviews": {
 *     "{bookId}": {
 *       "{reviewId}": { RealtimeBookReview object }
 *     }
 *   }
 * }
 */

