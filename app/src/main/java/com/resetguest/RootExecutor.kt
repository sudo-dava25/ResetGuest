package com.resetguest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class ExecutionResult(
    val success: Boolean,
    val output: String,
    val errorOutput: String,
    val exitCode: Int,
    val durationMs: Long
)

data class StepResult(
    val command: String,
    val success: Boolean,
    val output: String
)

object RootExecutor {

    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine() ?: ""
            val exitCode = process.waitFor()
            exitCode == 0 && output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun executeScript(
        commands: List<String>,
        onStepComplete: (StepResult) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val allOutput = StringBuilder()
        val allErrors = StringBuilder()
        var overallSuccess = true

        try {
            val process = Runtime.getRuntime().exec("su")
            val writer = OutputStreamWriter(process.outputStream)
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            for (command in commands) {
                if (command.isBlank()) continue
                writer.write("$command\n")
                writer.flush()
            }

            writer.write("echo __DONE__\n")
            writer.flush()
            writer.write("exit\n")
            writer.flush()
            writer.close()

            val outputLines = StringBuilder()
            var line: String?
            while (stdoutReader.readLine().also { line = it } != null) {
                if (line == "__DONE__") break
                outputLines.appendLine(line)
            }

            val errorLines = StringBuilder()
            while (stderrReader.ready()) {
                stderrReader.readLine()?.let { errorLines.appendLine(it) }
            }

            val exitCode = process.waitFor()
            val output = outputLines.toString().trim()
            val errors = errorLines.toString().trim()

            allOutput.append(output)
            allErrors.append(errors)

            if (exitCode != 0) overallSuccess = false

            onStepComplete(
                StepResult(
                    command = commands.joinToString("\n"),
                    success = exitCode == 0,
                    output = output.ifEmpty { if (exitCode == 0) "Completed successfully" else "Exit code: $exitCode" }
                )
            )

            ExecutionResult(
                success = overallSuccess && exitCode == 0,
                output = allOutput.toString(),
                errorOutput = allErrors.toString(),
                exitCode = exitCode,
                durationMs = System.currentTimeMillis() - startTime
            )

        } catch (e: SecurityException) {
            ExecutionResult(
                success = false,
                output = "",
                errorOutput = "Security exception: ${e.message}",
                exitCode = -1,
                durationMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ExecutionResult(
                success = false,
                output = "",
                errorOutput = "Execution failed: ${e.message}",
                exitCode = -1,
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }
}
