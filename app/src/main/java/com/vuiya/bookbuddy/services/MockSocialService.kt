package com.vuiya.bookbuddy.services

import com.vuiya.bookbuddy.models.*
import kotlinx.coroutines.delay
import kotlin.random.Random

class MockSocialService : SocialService {
    // In-memory storage for demo purposes
    private val users = mutableMapOf<String, UserProfile>()
    private val books = mutableMapOf<String, SocialBook>()
    private val friendRequests = mutableListOf<FriendRequest>()
    private val friendships = mutableSetOf<Pair<String, String>>() // (userId1, userId2) pairs
    private val follows = mutableSetOf<FollowRelationship>()
    private val posts = mutableListOf<Post>()
    private val comments = mutableListOf<Comment>()
    private val likes = mutableListOf<Like>()
    private val notifications = mutableListOf<Notification>()
    
    init {
        // Initialize with some mock data
        initializeMockData()
    }
    
    private fun initializeMockData() {
        // Add some mock users
        users["user1"] = UserProfile(
            userId = "user1",
            username = "john_doe",
            displayName = "John Doe",
            email = "john@example.com",
            bio = "Book lover and writer",
            totalBooks = 5,
            followersCount = 120,
            followingCount = 85
        )
        
        users["user2"] = UserProfile(
            userId = "user2",
            username = "jane_smith",
            displayName = "Jane Smith",
            email = "jane@example.com",
            bio = "Author and book reviewer",
            totalBooks = 12,
            followersCount = 245,
            followingCount = 150
        )
        
        users["user3"] = UserProfile(
            userId = "user3",
            username = "bob_wilson",
            displayName = "Bob Wilson",
            email = "bob@example.com",
            bio = "Tech enthusiast and sci-fi reader",
            totalBooks = 8,
            followersCount = 75,
            followingCount = 90
        )
        
        // Add some mock books
        books["book1"] = SocialBook(
            bookId = "book1",
            title = "The Great Adventure",
            author = "John Doe",
            description = "An exciting adventure story",
            language = "English",
            uploaderId = "user1",
            uploaderUsername = "john_doe",
            downloadsCount = 45,
            likesCount = 23,
            commentsCount = 12
        )
        
        books["book2"] = SocialBook(
            bookId = "book2",
            title = "Mystery in the City",
            author = "Jane Smith",
            description = "A thrilling mystery novel",
            language = "English",
            uploaderId = "user2",
            uploaderUsername = "jane_smith",
            downloadsCount = 78,
            likesCount = 56,
            commentsCount = 21
        )
        
        books["book3"] = SocialBook(
            bookId = "book3",
            title = "Future Tech",
            author = "Bob Wilson",
            description = "Exploring the future of technology",
            language = "English",
            uploaderId = "user3",
            uploaderUsername = "bob_wilson",
            downloadsCount = 32,
            likesCount = 18,
            commentsCount = 8
        )
        
        // Add some mock posts
        posts.add(Post(
            postId = "post1",
            userId = "user1",
            username = "john_doe",
            content = "Just finished reading 'The Great Adventure' and loved every page!",
            timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
            likesCount = 5,
            commentsCount = 2,
            sharesCount = 1
        ))
        
        posts.add(Post(
            postId = "post2",
            userId = "user2",
            username = "jane_smith",
            content = "Started uploading my new book 'Mystery in the City'. Check it out!",
            bookId = "book2",
            bookTitle = "Mystery in the City",
            timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
            likesCount = 12,
            commentsCount = 4,
            sharesCount = 3
        ))
        
        // Add some mock friend requests
        friendRequests.add(FriendRequest(
            requestId = "req1",
            senderId = "user2",
            receiverId = "user1",
            status = RequestStatus.PENDING
        ))
    }

    override suspend fun getCurrentUserProfile(): Result<UserProfile> {
        delay(500) // Simulate network delay
        return Result.success(users["user1"] ?: UserProfile())
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<Boolean> {
        delay(500)
        users[profile.userId] = profile
        return Result.success(true)
    }

    override suspend fun getUserProfile(userId: String): Result<UserProfile> {
        delay(500)
        return users[userId]?.let { Result.success(it) } ?: Result.failure(Exception("User not found"))
    }

    override suspend fun sendFriendRequest(receiverId: String): Result<Boolean> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val request = FriendRequest(
            requestId = "req_${System.currentTimeMillis()}",
            senderId = currentUser.userId,
            receiverId = receiverId,
            status = RequestStatus.PENDING
        )
        friendRequests.add(request)
        
        // Create notification for receiver
        notifications.add(Notification(
            notificationId = "notif_${System.currentTimeMillis()}",
            userId = receiverId,
            type = NotificationType.FRIEND_REQUEST,
            title = "New Friend Request",
            message = "${currentUser.displayName} wants to connect with you",
            relatedUserId = currentUser.userId
        ))
        
        return Result.success(true)
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Boolean> {
        delay(500)
        val request = friendRequests.find { it.requestId == requestId && it.status == RequestStatus.PENDING }
            ?: return Result.failure(Exception("Request not found"))
        
        friendRequests.remove(request)
        friendRequests.add(request.copy(status = RequestStatus.ACCEPTED))
        
        // Add friendship
        friendships.add(Pair(request.senderId, request.receiverId))
        
        // Create notification for sender
        notifications.add(Notification(
            notificationId = "notif_${System.currentTimeMillis()}",
            userId = request.senderId,
            type = NotificationType.FRIEND_ACCEPTED,
            title = "Friend Request Accepted",
            message = "${users[request.receiverId]?.displayName} accepted your friend request",
            relatedUserId = request.receiverId
        ))
        
        return Result.success(true)
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Boolean> {
        delay(500)
        val request = friendRequests.find { it.requestId == requestId && it.status == RequestStatus.PENDING }
            ?: return Result.failure(Exception("Request not found"))
        
        friendRequests.remove(request)
        friendRequests.add(request.copy(status = RequestStatus.REJECTED))
        
        return Result.success(true)
    }

    override suspend fun cancelFriendRequest(requestId: String): Result<Boolean> {
        delay(500)
        val request = friendRequests.find { it.requestId == requestId && it.status == RequestStatus.PENDING }
            ?: return Result.failure(Exception("Request not found"))
        
        friendRequests.remove(request)
        return Result.success(true)
    }

    override suspend fun unfriend(userId: String): Result<Boolean> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        
        friendships.remove(Pair(currentUser.userId, userId))
        friendships.remove(Pair(userId, currentUser.userId))
        
        return Result.success(true)
    }

    override suspend fun getPendingFriendRequests(): Result<List<FriendRequest>> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val pendingRequests = friendRequests.filter { it.receiverId == currentUser.userId && it.status == RequestStatus.PENDING }
        return Result.success(pendingRequests)
    }

    override suspend fun getFriendsList(): Result<List<UserProfile>> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val friendIds = friendships.filter { it.first == currentUser.userId || it.second == currentUser.userId }
            .map { if (it.first == currentUser.userId) it.second else it.first }
        val friendProfiles = friendIds.mapNotNull { users[it] }
        return Result.success(friendProfiles)
    }

    override suspend fun getFollowers(): Result<List<UserProfile>> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val followerIds = follows.filter { it.followingId == currentUser.userId }.map { it.followerId }
        val followerProfiles = followerIds.mapNotNull { users[it] }
        return Result.success(followerProfiles)
    }

    override suspend fun getFollowing(): Result<List<UserProfile>> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val followingIds = follows.filter { it.followerId == currentUser.userId }.map { it.followingId }
        val followingProfiles = followingIds.mapNotNull { users[it] }
        return Result.success(followingProfiles)
    }

    override suspend fun followUser(userId: String): Result<Boolean> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        
        val relationship = FollowRelationship(
            followId = "follow_${System.currentTimeMillis()}",
            followerId = currentUser.userId,
            followingId = userId
        )
        
        follows.add(relationship)
        
        // Update counts in user profiles
        users[currentUser.userId]?.let { user ->
            users[currentUser.userId] = user.copy(followingCount = user.followingCount + 1)
        }
        
        users[userId]?.let { user ->
            users[userId] = user.copy(followersCount = user.followersCount + 1)
        }
        
        // Create notification for followed user
        notifications.add(Notification(
            notificationId = "notif_${System.currentTimeMillis()}",
            userId = userId,
            type = NotificationType.FOLLOWER_ADDED,
            title = "New Follower",
            message = "${currentUser.displayName} started following you",
            relatedUserId = currentUser.userId
        ))
        
        return Result.success(true)
    }

    override suspend fun unfollowUser(userId: String): Result<Boolean> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        
        val relationship = follows.find { it.followerId == currentUser.userId && it.followingId == userId }
        if (relationship != null) {
            follows.remove(relationship)
            
            // Update counts in user profiles
            users[currentUser.userId]?.let { user ->
                users[currentUser.userId] = user.copy(followingCount = user.followingCount - 1)
            }
            
            users[userId]?.let { user ->
                users[userId] = user.copy(followersCount = user.followersCount - 1)
            }
        }
        
        return Result.success(true)
    }

    override suspend fun uploadBook(book: SocialBook, localFilePath: String): Result<String> {
        delay(1000) // Longer delay for upload simulation
        val newBookId = "book_${System.currentTimeMillis()}"
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        
        val uploadedBook = book.copy(
            bookId = newBookId,
            uploaderId = currentUser.userId,
            uploaderUsername = currentUser.username,
            uploadDate = System.currentTimeMillis()
        )
        
        books[newBookId] = uploadedBook
        
        // Create notification for followers
        val followers = follows.filter { it.followingId == currentUser.userId }.map { it.followerId }
        followers.forEach { followerId ->
            notifications.add(Notification(
                notificationId = "notif_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
                userId = followerId,
                type = NotificationType.BOOK_SHARED,
                title = "New Book Uploaded",
                message = "${currentUser.displayName} uploaded a new book: ${book.title}",
                relatedUserId = currentUser.userId,
                relatedBookId = newBookId
            ))
        }
        
        return Result.success(newBookId)
    }

    override suspend fun getPublicBooks(limit: Int, offset: Int): Result<List<SocialBook>> {
        delay(500)
        val sortedBooks = books.values.sortedByDescending { it.uploadDate }
        val paginatedBooks = sortedBooks.drop(offset).take(limit)
        return Result.success(paginatedBooks.toList())
    }

    override suspend fun getFriendBooks(limit: Int, offset: Int): Result<List<SocialBook>> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val friendIds = friendships.filter { it.first == currentUser.userId || it.second == currentUser.userId }
            .map { if (it.first == currentUser.userId) it.second else it.first }
        
        val friendBooks = books.values.filter { it.uploaderId in friendIds }
            .sortedByDescending { it.uploadDate }
            .drop(offset).take(limit)
        
        return Result.success(friendBooks.toList())
    }

    override suspend fun searchBooks(query: String, limit: Int, offset: Int): Result<List<SocialBook>> {
        delay(500)
        val matchingBooks = books.values.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.author.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }.sortedByDescending { it.likesCount }
        .drop(offset).take(limit)
        
        return Result.success(matchingBooks.toList())
    }

    override suspend fun downloadBook(bookId: String, destinationPath: String): Result<Boolean> {
        delay(1000) // Simulate download delay
        val book = books[bookId] ?: return Result.failure(Exception("Book not found"))
        
        // Increment download count
        books[bookId] = book.copy(downloadsCount = book.downloadsCount + 1)
        
        // In a real app, this would copy the file to destinationPath
        return Result.success(true)
    }

    override suspend fun deleteUploadedBook(bookId: String): Result<Boolean> {
        delay(500)
        val book = books[bookId] ?: return Result.failure(Exception("Book not found"))
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        
        if (book.uploaderId != currentUser.userId) {
            return Result.failure(Exception("Not authorized to delete this book"))
        }
        
        books.remove(bookId)
        return Result.success(true)
    }

    override suspend fun getBookDetails(bookId: String): Result<SocialBook> {
        delay(500)
        return books[bookId]?.let { Result.success(it) } ?: Result.failure(Exception("Book not found"))
    }

    override suspend fun createPost(content: String, bookId: String?): Result<String> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val newPostId = "post_${System.currentTimeMillis()}"
        
        val bookTitle = bookId?.let { books[it]?.title } ?: ""
        
        val post = Post(
            postId = newPostId,
            userId = currentUser.userId,
            username = currentUser.username,
            userProfilePic = currentUser.profilePictureUrl,
            content = content,
            bookId = bookId ?: "",
            bookTitle = bookTitle,
            timestamp = System.currentTimeMillis()
        )
        
        posts.add(0, post) // Add to beginning of list
        
        return Result.success(newPostId)
    }

    override suspend fun getFeedPosts(limit: Int, offset: Int): Result<List<Post>> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val friendIds = friendships.filter { it.first == currentUser.userId || it.second == currentUser.userId }
            .map { if (it.first == currentUser.userId) it.second else it.first }
        
        // Get posts from friends and followed users
        val relevantPosts = posts.filter { it.userId in friendIds || it.userId in follows.filter { f -> f.followerId == currentUser.userId }.map { f -> f.followingId } }
            .sortedByDescending { it.timestamp }
            .drop(offset).take(limit)
        
        return Result.success(relevantPosts.toList())
    }

    override suspend fun getFriendActivity(limit: Int, offset: Int): Result<List<Post>> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val friendIds = friendships.filter { it.first == currentUser.userId || it.second == currentUser.userId }
            .map { if (it.first == currentUser.userId) it.second else it.first }
        
        val friendPosts = posts.filter { it.userId in friendIds }
            .sortedByDescending { it.timestamp }
            .drop(offset).take(limit)
        
        return Result.success(friendPosts.toList())
    }

    override suspend fun getBookPosts(bookId: String, limit: Int, offset: Int): Result<List<Post>> {
        delay(500)
        val bookPosts = posts.filter { it.bookId == bookId }
            .sortedByDescending { it.timestamp }
            .drop(offset).take(limit)
        
        return Result.success(bookPosts.toList())
    }

    override suspend fun deletePost(postId: String): Result<Boolean> {
        delay(500)
        val post = posts.find { it.postId == postId }
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        
        if (post?.userId != currentUser.userId) {
            return Result.failure(Exception("Not authorized to delete this post"))
        }
        
        posts.remove(post)
        return Result.success(true)
    }

    override suspend fun likePost(postId: String): Result<Boolean> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val post = posts.find { it.postId == postId } ?: return Result.failure(Exception("Post not found"))
        
        // Check if already liked
        val existingLike = likes.find { it.postId == postId && it.userId == currentUser.userId }
        if (existingLike != null) {
            return Result.success(true) // Already liked
        }
        
        val like = Like(
            likeId = "like_${System.currentTimeMillis()}",
            userId = currentUser.userId,
            postId = postId
        )
        
        likes.add(like)
        
        // Update post's like count
        val postIndex = posts.indexOf(post)
        if (postIndex != -1) {
            posts[postIndex] = post.copy(likesCount = post.likesCount + 1)
        }
        
        // Create notification for post owner
        if (post.userId != currentUser.userId) {
            notifications.add(Notification(
                notificationId = "notif_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
                userId = post.userId,
                type = NotificationType.BOOK_LIKED,
                title = "Your Post Was Liked",
                message = "${currentUser.displayName} liked your post",
                relatedUserId = currentUser.userId,
                relatedBookId = post.bookId
            ))
        }
        
        return Result.success(true)
    }

    override suspend fun unlikePost(postId: String): Result<Boolean> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val existingLike = likes.find { it.postId == postId && it.userId == currentUser.userId }
        if (existingLike == null) {
            return Result.success(true) // Not liked, so nothing to unlike
        }
        
        likes.remove(existingLike)
        
        // Update post's like count
        val post = posts.find { it.postId == postId }
        post?.let {
            val postIndex = posts.indexOf(it)
            if (postIndex != -1) {
                posts[postIndex] = it.copy(likesCount = it.likesCount - 1)
            }
        }
        
        return Result.success(true)
    }

    override suspend fun addComment(postId: String, content: String): Result<String> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val post = posts.find { it.postId == postId } ?: return Result.failure(Exception("Post not found"))
        
        val commentId = "comment_${System.currentTimeMillis()}"
        val comment = Comment(
            commentId = commentId,
            postId = postId,
            userId = currentUser.userId,
            username = currentUser.username,
            userProfilePic = currentUser.profilePictureUrl,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        
        comments.add(comment)
        
        // Update post's comment count
        val postIndex = posts.indexOf(post)
        if (postIndex != -1) {
            posts[postIndex] = post.copy(commentsCount = post.commentsCount + 1)
        }
        
        // Create notification for post owner
        if (post.userId != currentUser.userId) {
            notifications.add(Notification(
                notificationId = "notif_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
                userId = post.userId,
                type = NotificationType.BOOK_COMMENTED,
                title = "Your Post Was Commented On",
                message = "${currentUser.displayName} commented on your post",
                relatedUserId = currentUser.userId,
                relatedBookId = post.bookId
            ))
        }
        
        return Result.success(commentId)
    }

    override suspend fun deleteComment(commentId: String): Result<Boolean> {
        delay(500)
        val comment = comments.find { it.commentId == commentId }
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        
        if (comment?.userId != currentUser.userId) {
            return Result.failure(Exception("Not authorized to delete this comment"))
        }
        
        comments.remove(comment)
        
        // Update post's comment count
        val post = posts.find { it.postId == comment?.postId }
        post?.let {
            val postIndex = posts.indexOf(it)
            if (postIndex != -1) {
                posts[postIndex] = it.copy(commentsCount = it.commentsCount - 1)
            }
        }
        
        return Result.success(true)
    }

    override suspend fun getCommentsForPost(postId: String, limit: Int, offset: Int): Result<List<Comment>> {
        delay(500)
        val postComments = comments.filter { it.postId == postId }
            .sortedBy { it.timestamp }
            .drop(offset).take(limit)
        
        return Result.success(postComments.toList())
    }

    override suspend fun getNotifications(limit: Int, offset: Int): Result<List<Notification>> {
        delay(500)
        val currentUser = users["user1"] ?: return Result.failure(Exception("Current user not found"))
        val userNotifications = notifications.filter { it.userId == currentUser.userId }
            .sortedByDescending { it.timestamp }
            .drop(offset).take(limit)
        
        return Result.success(userNotifications.toList())
    }

    override suspend fun markNotificationAsRead(notificationId: String): Result<Boolean> {
        delay(500)
        // In our mock implementation, we don't have a way to mark as read in the list
        // In a real implementation, we'd have a separate field or update the notification
        return Result.success(true)
    }

    override suspend fun markAllNotificationsAsRead(): Result<Boolean> {
        delay(500)
        // In our mock implementation, we don't have a way to mark as read in the list
        // In a real implementation, we'd update all notifications for the user
        return Result.success(true)
    }
}