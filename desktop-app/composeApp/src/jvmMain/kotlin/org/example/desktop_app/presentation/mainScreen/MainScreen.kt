package org.example.desktop_app.presentation.mainScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.example.desktop_app.presentation.components.VideoHistoryCard
import org.example.desktop_app.presentation.incomingJsonFlow
import org.example.desktop_app.presentation.theme.AppTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        incomingJsonFlow.collect { jsonString ->
            viewModel.processIntent(MainIntent.NewVideoReceived(jsonString))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MainEffect.ShowToast -> {
                    toastMessage = effect.message
                }
            }
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000L)
            toastMessage = null
        }
    }

    AppTheme(
//        darkTheme = false
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                when (val currentState = state) {
                    is MainState.Loading -> {
                        Box(
                            modifier =
                                Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                    is MainState.Dashboard -> {
                        DashboardScreen(
                            state = currentState,
                            onDownloadClick = { videoId ->
                                viewModel.processIntent(MainIntent.StartDownload(videoId))
                            },
                            onOpenFolderClick = { path ->
                                viewModel.processIntent(MainIntent.OpenFileInExplorer(path))
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.inverseSurface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = toastMessage ?: "Something Went Wrong",
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    state: MainState.Dashboard,
    onDownloadClick: (Long) -> Unit,
    onOpenFolderClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Show Errors if any exist
        state.errorMessage?.let { error ->
            Text(
                text = "❌ $error",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        // The Main FDM List
        if (state.downloads.isEmpty()) {
            // Empty State
            Box(modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Waiting for extension data...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            // The Scrollable List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = state.downloads,
                    key = { it.id }
                ) { video ->
                    VideoHistoryCard(
                        video = video,
                        onOpenFolderClick = { path ->
                            path?.let {
                                onOpenFolderClick(it)
                            }
                        },
                        onStartDownloadClick = { videoId ->
                            onDownloadClick(videoId)
                        }
                    )
                }
            }
        }
    }
}