package com.vuiya.bookbuddy.services

import com.vuiya.bookbuddy.models.*

interface SocialService {
    // User Profile Methods
    suspend fun getCurrentUserProfile(): Result<UserProfile>
    suspend fun updateUserProfile(profile: UserProfile): Result<Boolean>
    suspend fun getUserProfile(userId: String): Result<UserProfile>
    
    // Friend/Follow Methods
    suspend fun sendFriendRequest(receiverId: String): Result<Boolean>
    suspend fun acceptFriendRequest(requestId: String): Result<Boolean>
    suspend fun rejectFriendRequest(requestId: String): Result<Boolean>
    suspend fun cancelFriendRequest(requestId: String): Result<Boolean>
    suspend fun unfriend(userId: String): Result<Boolean>
    suspend fun getPendingFriendRequests(): Result<List<FriendRequest>>
    suspend fun getFriendsList(): Result<List<UserProfile>>
    suspend fun getFollowers(): Result<List<UserProfile>>
    suspend fun getFollowing(): Result<List<UserProfile>>
    suspend fun followUser(userId: String): Result<Boolean>
    suspend fun unfollowUser(userId: String): Result<Boolean>
    
    // Book Sharing Methods
    suspend fun uploadBook(book: SocialBook, localFilePath: String): Result<String> // Returns bookId
    suspend fun getPublicBooks(limit: Int = 20, offset: Int = 0): Result<List<SocialBook>>
    suspend fun getFriendBooks(limit: Int = 20, offset: Int = 0): Result<List<SocialBook>>
    suspend fun searchBooks(query: String, limit: Int = 20, offset: Int = 0): Result<List<SocialBook>>
    suspend fun downloadBook(bookId: String, destinationPath: String): Result<Boolean>
    suspend fun deleteUploadedBook(bookId: String): Result<Boolean>
    suspend fun getBookDetails(bookId: String): Result<SocialBook>
    
    // Feed/Post Methods
    suspend fun createPost(content: String, bookId: String?): Result<String> // Returns postId
    suspend fun getFeedPosts(limit: Int = 20, offset: Int = 0): Result<List<Post>>
    suspend fun getFriendActivity(limit: Int = 20, offset: Int = 0): Result<List<Post>>
    suspend fun getBookPosts(bookId: String, limit: Int = 20, offset: Int = 0): Result<List<Post>>
    suspend fun deletePost(postId: String): Result<Boolean>
    
    // Interaction Methods
    suspend fun likePost(postId: String): Result<Boolean>
    suspend fun unlikePost(postId: String): Result<Boolean>
    suspend fun addComment(postId: String, content: String): Result<String> // Returns commentId
    suspend fun deleteComment(commentId: String): Result<Boolean>
    suspend fun getCommentsForPost(postId: String, limit: Int = 20, offset: Int = 0): Result<List<Comment>>
    
    // Notification Methods
    suspend fun getNotifications(limit: Int = 20, offset: Int = 0): Result<List<Notification>>
    suspend fun markNotificationAsRead(notificationId: String): Result<Boolean>
    suspend fun markAllNotificationsAsRead(): Result<Boolean>
}