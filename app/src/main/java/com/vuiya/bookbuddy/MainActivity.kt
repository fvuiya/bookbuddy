package com.vuiya.bookbuddy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vuiya.bookbuddy.ui.theme.BookBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookBuddyTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BookBuddy") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureCard(
                    title = "Camera OCR",
                    iconResId = R.drawable.ic_camera,
                    onClick = { context.startActivity(Intent(context, CameraActivity::class.java)) }
                )
                FeatureCard(
                    title = "PDF & EPUB Reader",
                    iconResId = R.drawable.ic_pdf,
                    onClick = { context.startActivity(Intent(context, FileSelectorActivity::class.java)) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureCard(
                    title = "Library",
                    iconResId = R.drawable.ic_library,
                    onClick = { context.startActivity(Intent(context, LibraryActivity::class.java)) }
                )
                FeatureCard(
                    title = "Social",
                    iconResId = R.drawable.ic_social,
                    onClick = { context.startActivity(Intent(context, SocialActivity::class.java)) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureCard(
                    title = "Settings",
                    iconResId = R.drawable.ic_settings,
                    onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(title: String, iconResId: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(150.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(painter = painterResource(id = iconResId), contentDescription = title, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BookBuddyTheme {
        MainScreen()
    }
}
