package com.vuiya.bookbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.vuiya.bookbuddy.firebase.FirebaseAuthManager
import com.vuiya.bookbuddy.models.firebase.RealtimeUser
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SignUpActivity : ComponentActivity() {

    private lateinit var authManager: FirebaseAuthManager
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = FirebaseAuthManager(this)

        setContent {
            BookBuddyTheme {
                SignUpScreen(
                    onSignUp = { email, password, displayName, username ->
                        handleSignUp(email, password, displayName, username)
                    },
                    onBackToLogin = { finish() }
                )
            }
        }
    }

    private fun handleSignUp(email: String, password: String, displayName: String, username: String) {
        lifecycleScope.launch {
            // Check if username is already taken
            val usernameExists = checkUsernameExists(username)
            if (usernameExists) {
                Toast.makeText(this@SignUpActivity, "Username already taken", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Create account
            authManager.signUpWithEmail(email, password)
                .onSuccess { user ->
                    // Update display name
                    authManager.updateDisplayName(displayName)

                    // Create user profile in Realtime Database
                    val userProfile = RealtimeUser(
                        uid = user.uid,
                        email = email,
                        displayName = displayName,
                        username = username,
                        bio = "",
                        profilePictureUrl = "",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        friendsCount = 0,
                        booksPublished = 0
                    )

                    // Save to Realtime Database
                    database.child("users").child(user.uid)
                        .setValue(userProfile)
                        .addOnSuccessListener {
                            // Also add username to usernames index for quick lookup
                            database.child("usernames").child(username).setValue(user.uid)
                            Toast.makeText(this@SignUpActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            navigateToMainActivity()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this@SignUpActivity, "Failed to create profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .onFailure { exception ->
                    Toast.makeText(this@SignUpActivity, "Sign up failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private suspend fun checkUsernameExists(username: String): Boolean = suspendCoroutine { continuation ->
        database.child("usernames").child(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    continuation.resume(snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(false)
                }
            })
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, SocialActivity::class.java))
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUp: (String, String, String, String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val passwordMatch = password == confirmPassword
    val canSignUp = email.isNotBlank() && password.isNotBlank() &&
                    displayName.isNotBlank() && username.isNotBlank() &&
                    passwordMatch && password.length >= 6

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Join BookBuddy",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Display Name field
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Username field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase().replace(" ", "") },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = "Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Lowercase letters and numbers only") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("At least 6 characters") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password") },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = confirmPassword.isNotBlank() && !passwordMatch,
                supportingText = {
                    if (confirmPassword.isNotBlank() && !passwordMatch) {
                        Text("Passwords don't match", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up button
            Button(
                onClick = {
                    if (canSignUp) {
                        isLoading = true
                        onSignUp(email, password, displayName, username)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && canSignUp
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign Up")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to Login link
            Row {
                Text("Already have an account? ")
                TextButton(onClick = onBackToLogin) {
                    Text("Login")
                }
            }
        }
    }
}

