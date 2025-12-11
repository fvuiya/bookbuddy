package com.vuiya.bookbuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vuiya.bookbuddy.models.*
import com.vuiya.bookbuddy.services.SocialService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SocialUiState(
    val feedPosts: List<Post> = emptyList(),
    val friendsList: List<UserProfile> = emptyList(),
    val publicBooks: List<SocialBook> = emptyList(),
    val notifications: List<Notification> = emptyList(),
    val showCreatePost: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SocialViewModel(
    private val socialService: SocialService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState
    
    init {
        loadFeed()
        loadFriends()
        loadPublicBooks()
        loadNotifications()
    }
    
    private fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            socialService.getFeedPosts().onSuccess { posts ->
                _uiState.value = _uiState.value.copy(
                    feedPosts = posts,
                    isLoading = false
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message
                )
            }
        }
    }
    
    private fun loadFriends() {
        viewModelScope.launch {
            socialService.getFriendsList().onSuccess { friends ->
                _uiState.value = _uiState.value.copy(friendsList = friends)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(error = exception.message)
            }
        }
    }
    
    private fun loadPublicBooks() {
        viewModelScope.launch {
            socialService.getPublicBooks().onSuccess { books ->
                _uiState.value = _uiState.value.copy(publicBooks = books)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(error = exception.message)
            }
        }
    }
    
    private fun loadNotifications() {
        viewModelScope.launch {
            socialService.getNotifications().onSuccess { notifications ->
                _uiState.value = _uiState.value.copy(notifications = notifications)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(error = exception.message)
            }
        }
    }
    
    fun setShowCreatePost(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCreatePost = show)
    }
    
    fun createPost(content: String, bookId: String?) {
        viewModelScope.launch {
            socialService.createPost(content, bookId).onSuccess { postId ->
                // Reload feed to include the new post
                loadFeed()
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(error = exception.message)
            }
        }
    }
    
    fun likePost(postId: String) {
        viewModelScope.launch {
            socialService.likePost(postId).onSuccess {
                // Update the post's like count in the UI
                val updatedPosts = _uiState.value.feedPosts.map { post ->
                    if (post.postId == postId) {
                        post.copy(likesCount = post.likesCount + 1)
                    } else {
                        post
                    }
                }
                _uiState.value = _uiState.value.copy(feedPosts = updatedPosts)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(error = exception.message)
            }
        }
    }
    
    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            socialService.addComment(postId, content).onSuccess { commentId ->
                // Update the post's comment count in the UI
                val updatedPosts = _uiState.value.feedPosts.map { post ->
                    if (post.postId == postId) {
                        post.copy(commentsCount = post.commentsCount + 1)
                    } else {
                        post
                    }
                }
                _uiState.value = _uiState.value.copy(feedPosts = updatedPosts)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(error = exception.message)
            }
        }
    }
}