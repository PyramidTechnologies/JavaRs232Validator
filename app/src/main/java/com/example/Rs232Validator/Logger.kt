package com.example.Rs232Validator

import PTI.Rs232Validator.Loggers.ILogger
import PTI.Rs232Validator.Loggers.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Logger : ILogger {
    private val TimestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss a")

    override fun LogTrace(format: String, vararg args: Any?) {
        Log(LogLevel.Trace, format, *args)
    }

    override fun LogDebug(format: String, vararg args: Any?) {
        Log(LogLevel.Debug, format, *args)
    }

    override fun LogInfo(format: String, vararg args: Any?) {
        Log(LogLevel.Info, format, *args)
    }

    override fun LogError(format: String, vararg args: Any?) {
        Log(LogLevel.Error, format, *args)
    }

    private fun Log(level: LogLevel, format: String, vararg args: Any?) {
        val entry = LogEntry(
            level,
            LocalDateTime.now().format(TimestampFormat),
            String.format(format, *args)
        )

        _LogEntries.update { it + entry }
    }

    private val _LogEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _LogEntries.asStateFlow()
}



data class LogEntry(
    val Level: LogLevel,
    val Timestamp: String,
    val Message: String
)