package app.rotatescreen.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    onNavigateBack: () -> Unit
) {
    var logs by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var isCollecting by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    // Collect logs
    LaunchedEffect(isCollecting) {
        if (isCollecting) {
            try {
                while (isActive) {
                    try {
                        val newLogs = collectLogs()
                        logs = newLogs.takeLast(500) // Keep last 500 entries
                    } catch (e: Exception) {
                        android.util.Log.e("LogViewerScreen", "Error collecting logs", e)
                    }
                    delay(1000) // Update every second
                }
            } catch (e: Exception) {
                android.util.Log.e("LogViewerScreen", "LaunchedEffect cancelled or error", e)
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            isCollecting = false
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            try {
                listState.animateScrollToItem(logs.size - 1)
            } catch (e: Exception) {
                // Ignore scroll errors when composable is being disposed
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rotation Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { logs = emptyList() }) {
                        Icon(Icons.Default.Delete, "Clear logs")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter info
            Text(
                text = "Showing rotation-related logs (${logs.size} entries)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )

            Divider()

            // Log list
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(logs) { log ->
                    LogEntryItem(log)
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(log: LogEntry) {
    val backgroundColor = when (log.level) {
        "E" -> Color(0x22FF0000) // Red tint for errors
        "W" -> Color(0x22FFA500) // Orange tint for warnings
        "D" -> Color(0x220000FF) // Blue tint for debug
        else -> Color.Transparent
    }

    val textColor = when (log.level) {
        "E" -> Color(0xFFFF6B6B)
        "W" -> Color(0xFFFFB347)
        "D" -> Color(0xFF6B9BFF)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Header: timestamp, level, tag
            Row {
                Text(
                    text = log.timestamp,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.level,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textColor,
                    modifier = Modifier
                        .background(textColor.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.tag,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textColor.copy(alpha = 0.8f)
                )
            }

            // Message
            Text(
                text = log.message,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = textColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Reads this app's own logcat output.
 *
 * Runs on Dispatchers.IO: this spawns a process, blocks on stream reads and
 * waits up to two seconds for it to exit, and was previously doing all of that
 * on the main thread once per second.
 *
 * stderr is merged into stdout so there is a single stream to drain. The old
 * version started a fresh Thread every second purely to discard stderr, and
 * that thread raced the finally block that closed the reader under it.
 */
private suspend fun collectLogs(): List<LogEntry> = withContext(Dispatchers.IO) {
    val process = try {
        ProcessBuilder(
            "logcat",
            "-d", // Dump and exit
            "-v", "time", // Time format
            "-t", "500", // Last 500 lines only
            "OrientationControl:*",
            "OrientationSelector:*",
            "CurrentAppTileService:*",
            "ForegroundAppDetector:*",
            "MainViewModel:*",
            "ScreenSelector:*",
            "AppConfigScreen:*",
            "*:S" // Silence everything else
        ).redirectErrorStream(true).start()
    } catch (e: Exception) {
        android.util.Log.e("LogViewerScreen", "Could not start logcat", e)
        return@withContext emptyList()
    }

    try {
        // useLines closes the reader; anything logcat writes to stderr simply
        // fails to parse and is dropped.
        process.inputStream.bufferedReader().useLines { lines ->
            lines.mapNotNull(::parseLogLine).toList()
        }
    } catch (e: Exception) {
        android.util.Log.e("LogViewerScreen", "Error reading logcat output", e)
        emptyList()
    } finally {
        if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
            android.util.Log.w("LogViewerScreen", "logcat did not exit; destroying")
        }
        process.destroyForcibly()
    }
}

private fun parseLogLine(line: String): LogEntry? {
    try {
        // Format: 01-18 12:34:56.789 D/Tag( 1234): Message
        val timeEndIndex = line.indexOf(' ', line.indexOf(' ') + 1)
        if (timeEndIndex == -1) return null

        val timestamp = line.substring(0, timeEndIndex).trim()
        val rest = line.substring(timeEndIndex + 1).trim()

        val levelTagMatch = Regex("([VDIWEF])/([^(]+)\\(\\s*\\d+\\): (.*)").find(rest)
        if (levelTagMatch != null) {
            val (level, tag, message) = levelTagMatch.destructured
            return LogEntry(
                timestamp = timestamp,
                level = level,
                tag = tag.trim(),
                message = message
            )
        }
    } catch (e: Exception) {
        // Ignore parse errors
    }
    return null
}
