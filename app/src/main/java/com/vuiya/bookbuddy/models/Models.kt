package com.vuiya.bookbuddy.models


data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val profilePictureUrl: String = "",
    val joinDate: Long = System.currentTimeMillis(),
    val totalBooks: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

data class SocialBook(
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val downloadUrl: String = "",
    val uploadDate: Long = System.currentTimeMillis(),
    val uploaderId: String = "",
    val uploaderUsername: String = "",
    val language: String = "",
    val fileSize: Long = 0,
    val downloadsCount: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isPublic: Boolean = true
)

data class FriendRequest(
    val requestId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val status: RequestStatus = RequestStatus.PENDING, // PENDING, ACCEPTED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
)

enum class RequestStatus {
    PENDING, ACCEPTED, REJECTED
}

data class Notification(
    val notificationId: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.GENERIC,
    val title: String = "",
    val message: String = "",
    val relatedUserId: String = "", // Who triggered the notification
    val relatedBookId: String = "", // Related book ID if applicable
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

enum class NotificationType {
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    BOOK_SHARED,
    BOOK_LIKED,
    BOOK_COMMENTED,
    FOLLOWER_ADDED,
    GENERIC
}

data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfilePic: String = "",
    val content: String = "",
    val bookId: String = "", // Optional - if posting about a specific book
    val bookTitle: String = "", // Title of the book if applicable
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0
)

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfilePic: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Like(
    val likeId: String = "",
    val userId: String = "",
    val postId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class FollowRelationship(
    val followId: String = "",
    val followerId: String = "",
    val followingId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)