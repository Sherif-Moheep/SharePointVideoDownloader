package org.example.desktop_app.presentation.mainScreen

import androidx.compose.animation.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import desktop_app.composeapp.generated.resources.Res
import desktop_app.composeapp.generated.resources.arrow_upward
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.desktop_app.presentation.components.VideoHistoryCard
import org.example.desktop_app.presentation.incomingJsonFlow
import org.example.desktop_app.presentation.theme.AppTheme
import org.jetbrains.compose.resources.painterResource

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

    AppTheme{
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Show button only when scrolled past first item
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        floatingActionButton = {
            // The FAB and its animation slot perfectly in here
            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_upward),
                        contentDescription = "Scroll to top"
                    )
                }
            }
        }
    ) { paddingValues ->
        // Scaffold provides paddingValues to prevent content from overlapping the FAB/NavBars
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // 👈 Apply the padding here
        ) {
            state.errorMessage?.let { error ->
                Text(
                    text = "❌ $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (state.downloads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Waiting for extension data...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Look how much cleaner this is! No more Box wrapper needed.
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items = state.downloads, key = { it.id }) { video ->
                        VideoHistoryCard(
                            video = video,
                            onOpenFolderClick = { path -> path?.let { onOpenFolderClick(it) } },
                            onStartDownloadClick = { videoId -> onDownloadClick(videoId) }
                        )
                    }
                }
            }
        }
    }
}