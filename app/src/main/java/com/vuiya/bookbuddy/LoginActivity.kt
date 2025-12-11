package com.vuiya.bookbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.vuiya.bookbuddy.firebase.FirebaseAuthManager
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var authManager: FirebaseAuthManager

    // Activity result launcher for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        lifecycleScope.launch {
            authManager.handleGoogleSignInResult(data)
                .onSuccess { user ->
                    Toast.makeText(this@LoginActivity, "Welcome ${user.displayName}!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                }
                .onFailure { exception ->
                    Toast.makeText(this@LoginActivity, "Google Sign-In failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = FirebaseAuthManager(this)
        // TODO: Replace with actual web client ID from google-services.json
        // authManager.initializeGoogleSignIn("YOUR_WEB_CLIENT_ID")

        // Check if user is already signed in
        if (authManager.isSignedIn) {
            navigateToMainActivity()
            return
        }

        setContent {
            BookBuddyTheme {
                LoginScreen(
                    onEmailLogin = { email, password -> handleEmailLogin(email, password) },
                    onGoogleLogin = { handleGoogleLogin() },
                    onAnonymousLogin = { handleAnonymousLogin() },
                    onSignUpClick = { navigateToSignUp() }
                )
            }
        }
    }

    private fun handleEmailLogin(email: String, password: String) {
        lifecycleScope.launch {
            authManager.signInWithEmail(email, password)
                .onSuccess { user ->
                    Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                }
                .onFailure { exception ->
                    Toast.makeText(this@LoginActivity, "Login failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun handleGoogleLogin() {
        val signInIntent = authManager.getGoogleSignInIntent()
        if (signInIntent != null) {
            googleSignInLauncher.launch(signInIntent)
        } else {
            Toast.makeText(this, "Google Sign-In not configured", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAnonymousLogin() {
        lifecycleScope.launch {
            authManager.signInAnonymously()
                .onSuccess {
                    Toast.makeText(this@LoginActivity, "Signed in as guest", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                }
                .onFailure { exception ->
                    Toast.makeText(this@LoginActivity, "Guest sign-in failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, SocialActivity::class.java))
        finish()
    }

    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onEmailLogin: (String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onAnonymousLogin: () -> Unit,
    onSignUpClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to BookBuddy") }
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
                text = "Login",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login button
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        onEmailLogin(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider with "OR"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = "  OR  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign-In button
            OutlinedButton(
                onClick = onGoogleLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in with Google")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Continue as Guest button
            OutlinedButton(
                onClick = onAnonymousLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue as Guest")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign up link
            Row {
                Text("Don't have an account? ")
                TextButton(onClick = onSignUpClick) {
                    Text("Sign Up")
                }
            }
        }
    }
}

