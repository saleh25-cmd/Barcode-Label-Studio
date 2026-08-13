package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.screens.GeneratorScreen
import com.example.ui.screens.LabelsListScreen
import com.example.ui.screens.PrintPreviewScreen
import com.example.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeStudioApp(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val settings by viewModel.settings.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGenerating by viewModel.isGeneratingBatch.collectAsState()
    val progress by viewModel.batchProgress.collectAsState()
    val message by viewModel.messageEvent.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Force RTL Direction for full Arabic interface
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Barcode Label Studio | استوديو الملصقات",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "المولد") },
                        label = { Text("الموّلد") },
                        modifier = Modifier.testTag("nav_tab_generator")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.ListAlt, contentDescription = "الملصقات") },
                        label = { Text("الملصقات (${labels.size})") },
                        modifier = Modifier.testTag("nav_tab_labels")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Print, contentDescription = "معاينة الطباعة") },
                        label = { Text("الطباعة و PDF") },
                        modifier = Modifier.testTag("nav_tab_print")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                        label = { Text("الإعدادات") },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = selectedTab, label = "tab_crossfade") { tabIndex ->
                    when (tabIndex) {
                        0 -> GeneratorScreen(
                            viewModel = viewModel,
                            settings = settings,
                            isGenerating = isGenerating,
                            progress = progress
                        )
                        1 -> LabelsListScreen(
                            viewModel = viewModel,
                            labels = labels,
                            settings = settings,
                            searchQuery = searchQuery
                        )
                        2 -> PrintPreviewScreen(
                            viewModel = viewModel,
                            labels = labels,
                            settings = settings
                        )
                        3 -> SettingsScreen(
                            viewModel = viewModel,
                            settings = settings
                        )
                    }
                }
            }
        }
    }
}
