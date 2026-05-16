package com.resetguest

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val BgCard = Color(0xFF0D1117)
private val BgSurface = Color(0xFF161B22)
private val BgElevated = Color(0xFF1C2128)
private val BorderSubtle = Color(0xFF21262D)
private val BorderMuted = Color(0xFF30363D)
private val AccentBlue = Color(0xFF58A6FF)
private val AccentGreen = Color(0xFF3FB950)
private val AccentRed = Color(0xFFF85149)
private val AccentPurple = Color(0xFF8B949E)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val TextMuted = Color(0xFF484F58)

@Composable
fun ResetGuestApp(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.background(BgDeep)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopBar(uiState = uiState, onClear = viewModel::clearLogs, onRefreshRoot = viewModel::checkRoot)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                RootStatusCard(uiState = uiState)
                ScriptPreviewCard()
                ActionButton(uiState = uiState, onExecute = viewModel::executeReset)
                if (uiState.logs.isNotEmpty()) {
                    LogPanel(logs = uiState.logs, modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TopBar(uiState: UiState, onClear: () -> Unit, onRefreshRoot: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1F6FEB), Color(0xFF58A6FF)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SettingsSuggest, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column {
                Text("ResetGuest", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                Text("ML Guest Account Reset", fontSize = 11.sp, color = TextMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (uiState.logs.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.DeleteSweep, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            val isChecking = uiState.appState == AppState.CHECKING_ROOT
            val rotationTarget = if (isChecking) 360f else 0f
            val infiniteTransition = rememberInfiniteTransition(label = "rot")
            val rotation by if (isChecking) {
                infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
                    label = "rot"
                )
            } else {
                remember { mutableStateOf(0f) }
            }
            IconButton(
                onClick = onRefreshRoot,
                modifier = Modifier.size(36.dp),
                enabled = uiState.appState != AppState.RUNNING && !isChecking
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    null,
                    tint = if (isChecking) AccentBlue else TextSecondary,
                    modifier = Modifier.size(18.dp).rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun RootStatusCard(uiState: UiState) {
    val bgColor: Color
    val borderColor: Color
    val icon: ImageVector
    val label: String
    val accentColor: Color

    when {
        !uiState.hasCheckedRoot || uiState.appState == AppState.CHECKING_ROOT -> {
            bgColor = BgSurface; borderColor = BorderMuted
            icon = Icons.Rounded.HourglassEmpty; label = "Checking root..."; accentColor = AccentBlue
        }
        uiState.isRootAvailable -> {
            bgColor = Color(0xFF0D1F12); borderColor = Color(0xFF1A4025)
            icon = Icons.Rounded.AdminPanelSettings; label = "Root Granted"; accentColor = AccentGreen
        }
        else -> {
            bgColor = Color(0xFF1F0D0D); borderColor = Color(0xFF40201A)
            icon = Icons.Rounded.GppBad; label = "Root Denied"; accentColor = AccentRed
        }
    }

    val sublabel = when {
        !uiState.hasCheckedRoot || uiState.appState == AppState.CHECKING_ROOT -> "Requesting su access"
        uiState.isRootAvailable -> "Shell UID 0 verified"
        else -> "su binary not available"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(borderColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(sublabel, fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (uiState.appState == AppState.CHECKING_ROOT) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentBlue, strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun ScriptPreviewCard() {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Code, null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                Text("Script Preview", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                Text("${RESET_COMMANDS.size} commands", fontSize = 11.sp, color = TextMuted)
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(BgDeep).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RESET_COMMANDS.forEach { cmd ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("$", fontSize = 11.sp, color = AccentGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(cmd, fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(uiState: UiState, onExecute: () -> Unit) {
    val isRunning = uiState.appState == AppState.RUNNING
    val isDisabled = !uiState.isRootAvailable || isRunning || uiState.appState == AppState.CHECKING_ROOT

    val buttonColor = when (uiState.appState) {
        AppState.SUCCESS -> AccentGreen
        AppState.FAILURE -> AccentRed
        else -> AccentBlue
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by if (isRunning) {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Button(
        onClick = onExecute,
        enabled = !isDisabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor.copy(alpha = pulseAlpha),
            disabledContainerColor = BgElevated
        )
    ) {
        AnimatedContent(targetState = uiState.appState, label = "btn", transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
        }) { state ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (state) {
                    AppState.RUNNING -> {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Text("Executing...", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                    }
                    AppState.SUCCESS -> {
                        Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Text("Reset Successful", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                    }
                    AppState.FAILURE -> {
                        Icon(Icons.Rounded.ErrorOutline, null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Text("Reset Failed — Retry", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                    }
                    AppState.NO_ROOT -> {
                        Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(18.dp), tint = TextMuted)
                        Text("No Root Access", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextMuted)
                    }
                    else -> {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Text("Execute Reset", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogPanel(logs: List<LogEntry>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) scope.launch { listState.animateScrollToItem(logs.size - 1) }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AccentGreen))
                Text("Execution Log", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Spacer(modifier = Modifier.weight(1f))
                Text("${logs.size} entries", fontSize = 11.sp, color = TextMuted)
            }
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().background(BgDeep).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs) { log -> LogEntryRow(log) }
            }
        }
    }
}

@Composable
private fun LogEntryRow(log: LogEntry) {
    val prefix: String
    val color: Color
    when (log.type) {
        LogType.SUCCESS -> { prefix = "✓"; color = AccentGreen }
        LogType.ERROR   -> { prefix = "✗"; color = AccentRed }
        LogType.COMMAND -> { prefix = "$"; color = Color(0xFFE3B341) }
        LogType.SYSTEM  -> { prefix = "»"; color = AccentBlue }
        LogType.INFO    -> { prefix = "·"; color = TextSecondary }
    }

    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestampMs))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (log.type == LogType.ERROR) Color(0xFF1F0D0D) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(timeStr, fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 1.dp))
        Text(prefix, fontSize = 11.sp, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
        Text(
            log.message,
            fontSize = 11.sp,
            color = if (log.type == LogType.COMMAND) color else TextSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}
