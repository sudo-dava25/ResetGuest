package com.resetguest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppState { IDLE, CHECKING_ROOT, RUNNING, SUCCESS, FAILURE, NO_ROOT }

data class LogEntry(
    val message: String,
    val type: LogType,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class LogType { INFO, SUCCESS, ERROR, COMMAND, SYSTEM }

data class UiState(
    val appState: AppState = AppState.IDLE,
    val isRootAvailable: Boolean = false,
    val logs: List<LogEntry> = emptyList(),
    val lastResultDurationMs: Long = 0L,
    val lastExitCode: Int = 0,
    val hasCheckedRoot: Boolean = false
)

val RESET_COMMANDS = listOf(
    "rm -rf /data/user/0/com.mobile.legends/shared_prefs",
    "rm -rf /data/user/0/com.google.android.gms/shared_prefs"
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        checkRoot()
    }

    fun checkRoot() {
        viewModelScope.launch {
            _uiState.update { it.copy(appState = AppState.CHECKING_ROOT, logs = emptyList()) }
            appendLog("Checking root access...", LogType.SYSTEM)

            val hasRoot = RootExecutor.checkRootAccess()

            _uiState.update {
                it.copy(
                    appState = if (hasRoot) AppState.IDLE else AppState.NO_ROOT,
                    isRootAvailable = hasRoot,
                    hasCheckedRoot = true
                )
            }

            if (hasRoot) {
                appendLog("Root access granted", LogType.SUCCESS)
            } else {
                appendLog("Root access denied — su binary not found or permission rejected", LogType.ERROR)
            }
        }
    }

    fun executeReset() {
        if (_uiState.value.appState == AppState.RUNNING) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    appState = AppState.RUNNING,
                    logs = emptyList()
                )
            }

            appendLog("Initiating guest reset sequence...", LogType.SYSTEM)
            appendLog("Requesting root shell...", LogType.INFO)

            RESET_COMMANDS.forEachIndexed { index, cmd ->
                appendLog(cmd, LogType.COMMAND)
                delay(80)
            }

            val result = RootExecutor.executeScript(
                commands = RESET_COMMANDS,
                onStepComplete = { step ->
                    viewModelScope.launch {
                        if (step.success) {
                            appendLog(step.output.ifEmpty { "Commands executed" }, LogType.SUCCESS)
                        } else {
                            appendLog("Error: ${step.output}", LogType.ERROR)
                        }
                    }
                }
            )

            _uiState.update {
                it.copy(
                    appState = if (result.success) AppState.SUCCESS else AppState.FAILURE,
                    lastResultDurationMs = result.durationMs,
                    lastExitCode = result.exitCode
                )
            }

            if (result.success) {
                appendLog("Reset completed successfully in ${result.durationMs}ms", LogType.SUCCESS)
            } else {
                appendLog(
                    "Reset failed (exit ${result.exitCode}): ${result.errorOutput.take(200)}",
                    LogType.ERROR
                )
            }
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList(), appState = AppState.IDLE) }
    }

    private fun appendLog(message: String, type: LogType) {
        _uiState.update { state ->
            state.copy(logs = state.logs + LogEntry(message, type))
        }
    }
}
