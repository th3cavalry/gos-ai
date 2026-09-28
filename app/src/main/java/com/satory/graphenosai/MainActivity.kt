package com.satory.graphenosai

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.satory.graphenosai.service.AssistantAccessibilityService
import com.satory.graphenosai.service.AssistantService
import com.satory.graphenosai.ui.SettingsManager
import com.satory.graphenosai.ui.SettingsScreen
import com.satory.graphenosai.ui.VoskLanguageManagerScreen
import com.satory.graphenosai.ui.theme.AiintegratedintoandroidTheme
import com.satory.graphenosai.audio.VoskTranscriber
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var assistantService: AssistantService? = null
    private var serviceBound = mutableStateOf(false)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? AssistantService.AssistantBinder
            assistantService = localBinder?.getService()
            serviceBound.value = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            assistantService = null
            serviceBound.value = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
    }
    
    override fun onStart() {
        super.onStart()
        Intent(this, AssistantService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    override fun onStop() {
        super.onStop()
        if (serviceBound.value) {
            unbindService(serviceConnection)
            serviceBound.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        setContent {
            AiintegratedintoandroidTheme {
                val navController = rememberNavController()
                val bound by serviceBound
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        FloatingNavigationBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (route != currentRoute) {
                                    navController.navigate(route) {
                                        popUpTo("main") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = "main") {
                            composable("main") {
                                MainScreen(
                                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                                    onOpenAssistantSettings = ::openAssistantSettings,
                                    onLaunchAssistant = ::launchAssistant,
                                    onOpenApiKeySettings = { navController.navigate("settings") },
                                    onOpenSettings = { navController.navigate("settings") },
                                    onOpenVoiceLanguages = { navController.navigate("voice_languages") }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    assistantService = if (bound) assistantService else null,
                                    onNavigateToLanguages = { navController.navigate("voice_languages") }
                                )
                            }
                            composable("voice_languages") {
                                VoskLanguageManagerScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    assistantService = if (bound) assistantService else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openAssistantSettings() {
        try {
            startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun launchAssistant() {
        val intent = Intent(this, AssistantService::class.java).apply {
            action = AssistantService.ACTION_ACTIVATE
        }
        startForegroundService(intent)
    }
}

@Composable
fun FloatingNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val settingsSelected = currentRoute == "settings" || currentRoute == "voice_languages"

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "main",
            onClick = { onNavigate("main") },
            icon = {
                Icon(
                    if (currentRoute == "main") Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
        NavigationBarItem(
            selected = settingsSelected,
            onClick = { onNavigate("settings") },
            icon = {
                Icon(
                    if (settingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAssistantSettings: () -> Unit,
    onLaunchAssistant: () -> Unit,
    onOpenApiKeySettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVoiceLanguages: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AssistantApplication
    val settingsManager = remember { SettingsManager(context) }
    
    var hasApiKey by remember { mutableStateOf(app.secureKeyManager.hasOpenRouterApiKey()) }
    val isHermesProvider = settingsManager.apiProvider == SettingsManager.PROVIDER_HERMES
    val keyConfigured = if (isHermesProvider) app.secureKeyManager.hasHermesApiKey() else hasApiKey
    val apiKeyDescription = when {
        isHermesProvider && keyConfigured -> "Hermes configured"
        isHermesProvider -> "Hermes API key needed"
        hasApiKey -> "OpenRouter configured"
        else -> "OpenRouter not configured"
    }
    val isAccessibilityEnabled = AssistantAccessibilityService.isServiceRunning
    
    val isDefaultAssistant = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) ?: false
        } else false
    }
    
    val voskTranscriber = remember { VoskTranscriber(context) }
    var isModelDownloaded by remember { mutableStateOf(!voskTranscriber.needsModelDownload()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "AI Assistant",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "Your privacy-first intelligent assistant",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        ElevatedCard(
            onClick = onLaunchAssistant,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Ask me anything",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    tonalElevation = 0.dp
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Launch",
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        Text(
            "Setup Progress",
            style = MaterialTheme.typography.titleMedium
        )

        SetupProgressCard(
            title = "API Key",
            subtitle = apiKeyDescription,
            isComplete = keyConfigured,
            icon = Icons.Filled.Key,
            action = "Configure",
            onAction = onOpenApiKeySettings
        )
        SetupProgressCard(
            title = "Voice Model",
            subtitle = if (isModelDownloaded) "Downloaded" else "Required for voice input",
            isComplete = isModelDownloaded,
            icon = Icons.Filled.RecordVoiceOver,
            action = "Download",
            onAction = onOpenVoiceLanguages
        )
        SetupProgressCard(
            title = "Accessibility Service",
            subtitle = if (isAccessibilityEnabled) "Enabled" else "Required for shortcuts",
            isComplete = isAccessibilityEnabled,
            icon = Icons.Filled.TouchApp,
            action = "Enable",
            onAction = onOpenAccessibilitySettings
        )
        SetupProgressCard(
            title = "Default Assistant",
            subtitle = if (isDefaultAssistant) "Set as default" else "Set for home button activation",
            isComplete = isDefaultAssistant,
            icon = Icons.Filled.Home,
            action = "Set",
            onAction = onOpenAssistantSettings
        )

        Text(
            "Activation Methods",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        ActivationMethodCard(
            icon = Icons.Filled.Home,
            title = "Long-press Home",
            description = "Hold home button or swipe from corner"
        )
        ActivationMethodCard(
            icon = Icons.Filled.VolumeUp,
            title = "Volume Keys",
            description = "Hold Volume Up + Down together"
        )
        ActivationMethodCard(
            icon = Icons.Filled.Dashboard,
            title = "Quick Settings Tile",
            description = "Add tile to notification panel"
        )

        if (!isModelDownloaded) {
            ModelDownloadCard(
                voskTranscriber = voskTranscriber,
                onDownloadComplete = { isModelDownloaded = true }
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Privacy-First Design",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                val privacyItems = listOf(
                    "On-device speech recognition (Vosk)",
                    "Encrypted API key storage",
                    "No device identifiers sent to cloud",
                    "Configurable privacy-focused search providers",
                    "Minimal permissions required",
                    "TLS 1.3 for all network requests"
                )
                privacyItems.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("\u2022", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        if (!keyConfigured && !isHermesProvider) {
            ApiKeyInputCard(
                onApiKeySaved = { hasApiKey = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SetupProgressCard(
    title: String,
    subtitle: String,
    isComplete: Boolean,
    icon: ImageVector,
    action: String,
    onAction: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isComplete) 1.dp else 2.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isComplete)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (isComplete) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isComplete) Icons.Filled.Check else icon,
                        contentDescription = null,
                        tint = if (isComplete) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isComplete) {
                FilledTonalButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(action, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Complete",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun ActivationMethodCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ApiKeyInputCard(onApiKeySaved: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as AssistantApplication
    
    var apiKey by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "OpenRouter API Key",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Get your key from openrouter.ai",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                visualTransformation = if (isVisible) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { isVisible = !isVisible }) {
                        Icon(
                            if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle"
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (apiKey.isNotBlank()) {
                        app.secureKeyManager.setOpenRouterApiKey(apiKey)
                        apiKey = ""
                        onApiKeySaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKey.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Securely")
            }
        }
    }
}

@Composable
fun ModelDownloadCard(
    voskTranscriber: VoskTranscriber,
    onDownloadComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var downloadState by remember { mutableStateOf<VoskTranscriber.DownloadState>(VoskTranscriber.DownloadState.NotStarted) }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Download Vosk Model",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Required for on-device voice recognition. The model (~40 MB) will be downloaded once and stored locally.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (val state = downloadState) {
                is VoskTranscriber.DownloadState.NotStarted -> {
                    Button(
                        onClick = {
                            scope.launch {
                                voskTranscriber.downloadModel().collect { newState ->
                                    downloadState = newState
                                    if (newState is VoskTranscriber.DownloadState.Complete) {
                                        onDownloadComplete()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Model (~40 MB)")
                    }
                }
                
                is VoskTranscriber.DownloadState.Downloading -> {
                    Column {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Downloading: ${state.progress}% (${state.bytesDownloaded / 1_000_000} MB / ${state.totalBytes / 1_000_000} MB)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                is VoskTranscriber.DownloadState.Extracting -> {
                    Column {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Extracting: ${state.progress}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                is VoskTranscriber.DownloadState.Complete -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Download complete!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                is VoskTranscriber.DownloadState.Error -> {
                    Column {
                        Text(
                            "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                downloadState = VoskTranscriber.DownloadState.NotStarted
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}
