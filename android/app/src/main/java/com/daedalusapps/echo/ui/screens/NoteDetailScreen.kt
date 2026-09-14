package com.daedalusapps.echo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Arrow뒤로
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.삭제
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.재생Arrow
import androidx.compose.material.icons.filled.정지
import androidx.compose.material3.Slider
import androidx.media3.common.MediaItem
import androidx.media3.common.재생er
import androidx.media3.exoplayer.Exo재생er
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kotlinx.coroutines.delay
import com.daedalusapps.echo.ai.음성 기록Formatter
import com.daedalusapps.echo.ui.mindmap.MindMapCanvas
import com.daedalusapps.echo.viewmodel.RecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    filename: String,
    recordingViewModel: RecordingViewModel,
    on뒤로: () -> Unit
) {
    val context = LocalContext.current
    val note by recordingViewModel.currentNote.collectAsState()
    val isProcessing by recordingViewModel.isProcessing.collectAsState()
    val is질문ing by recordingViewModel.is질문ing.collectAsState()
    val syncProgress by recordingViewModel.syncProgress.collectAsState()
    val aiError by recordingViewModel.aiError.collectAsState()
    val askAnswer by recordingViewModel.askAnswer.collectAsState()
    val exportIntent by recordingViewModel.exportIntent.collectAsState()

    // Launch share sheet when export intent is ready
    LaunchedEffect(exportIntent) {
        exportIntent?.let { intent ->
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("NoteDetailScreen", "Failed to start export activity", e)
            } finally {
                recordingViewModel.clearExportIntent()
            }
        }
    }

    // 재생er setup
    val player = remember { Exo재생er.Builder(context).build() }
    var is재생ing by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var playbackDuration by remember { mutableLongStateOf(0L) }

    // Sync is재생ing when audio finishes naturally
    DisposableEffect(player) {
        val listener = object : 재생er.Listener {
            override fun on재생backStateChanged(playbackState: Int) {
                if (playbackState == 재생er.STATE_ENDED || playbackState == 재생er.STATE_IDLE) {
                    is재생ing = false
                    playbackPosition = 0L
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Poll position while playing
    LaunchedEffect(is재생ing) {
        while (is재생ing) {
            playbackPosition = player.currentPosition.coerceAtLeast(0L)
            playbackDuration = player.duration.takeIf { it > 0L } ?: (note?.durationMillis ?: 0L)
            delay(200)
        }
    }

    val transcript = note?.transcript.orEmpty()
    val summary = note?.summary.orEmpty()
    val mindMap = note?.mindMap.orEmpty()

    // Load note on first composition
    LaunchedEffect(filename) {
        recordingViewModel.loadNote(filename)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var show삭제Dialog by remember { mutableStateOf(false) }
    var showInNoteSearch by remember { mutableStateOf(false) }
    var inNoteQuery by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }

    if (show삭제Dialog) {
        AlertDialog(
            onDismissRequest = { show삭제Dialog = false },
            title = { Text("녹음을 삭제할까요?") },
            text = { Text("녹음 파일과 AI가 생성한 모든 분석 데이터가 영구적으로 삭제됩니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        show삭제Dialog = false
                        recordingViewModel.deleteRecording(filename)
                        on뒤로()
                    }
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { show삭제Dialog = false }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val displayTitle = if (!note?.title.isNullOrBlank()) {
                        note!!.title
                    } else {
                        formatFilename(filename)
                    }
                    Text(
                        text = displayTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = on뒤로) {
                        Icon(Icons.AutoMirrored.Filled.Arrow뒤로, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { showInNoteSearch = !showInNoteSearch; if (!showInNoteSearch) inNoteQuery = "" }) {
                        Icon(Icons.Default.Search, contentDescription = "노트에서 검색")
                    }
                    IconButton(onClick = { show삭제Dialog = true }) {
                        Icon(Icons.Default.삭제, contentDescription = "삭제")
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "추가 옵션")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            val audioExists = remember(note?.localPath, filename) {
                                (note?.localPath?.takeIf { it.isNotBlank() }?.let { File(it) }
                                    ?: File(context.getExternalFilesDir(null), "Recordings/$filename")).exists()
                            }
                            DropdownMenuItem(
                                text = { Text("오디오 내보내기") },
                                onClick = {
                                    showOverflowMenu = false
                                    recordingViewModel.exportAudio(filename)
                                },
                                enabled = audioExists && !isProcessing
                            )
                            DropdownMenuItem(
                                text = { Text("마크다운 내보내기") },
                                onClick = {
                                    showOverflowMenu = false
                                    recordingViewModel.exportMarkdown(filename)
                                },
                                enabled = transcript.isNotEmpty() && !isProcessing
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                if (aiError != null) {
                    Text(
                        text = "AI Error: $aiError",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                // 재생back progress bar
                val totalDuration = playbackDuration.takeIf { it > 0L } ?: (note?.durationMillis ?: 0L)
                if (totalDuration > 0L) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Slider(
                            value = if (totalDuration > 0L) playbackPosition.toFloat() / totalDuration.toFloat() else 0f,
                            onValueChange = { fraction ->
                                val seekTo = (fraction * totalDuration).toLong()
                                player.seekTo(seekTo)
                                playbackPosition = seekTo
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(playbackPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDuration(totalDuration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 재생 / 정지 Button
                    Button(
                        onClick = {
                            if (is재생ing) {
                                player.stop()
                                is재생ing = false
                                playbackPosition = 0L
                            } else {
                                val file = note?.localPath?.takeIf { it.isNotBlank() }?.let { File(it) }
                                    ?: File(context.getExternalFilesDir(null), "Recordings/$filename")
                                if (file.exists()) {
                                    player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                                    player.prepare()
                                    player.play()
                                    playbackDuration = note?.durationMillis ?: 0L
                                    is재생ing = true
                                } else {
                                    Log.e("재생back", "File not found: ${file.absolutePath}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (is재생ing) Icons.Default.정지 else Icons.Default.재생Arrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (is재생ing) "정지" else "재생", maxLines = 1)
                    }

                    Button(
                        onClick = { recordingViewModel.analyze(filename) },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("분석")
                    }
                    OutlinedButton(
                        onClick = { recordingViewModel.exportMarkdown(filename) },
                        enabled = transcript.isNotEmpty() && !isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("MD 내보내기")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {


            TabRow(selectedTabIndex = selectedTab) {
                listOf("음성 기록", "요약", "마인드맵", "질문").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index != 3) recordingViewModel.clear질문Answer()
                        },
                        text = { Text(title) }
                    )
                }
            }

            if (showInNoteSearch) {
                OutlinedTextField(
                    value = inNoteQuery,
                    onValueChange = { inNoteQuery = it },
                    placeholder = { Text("노트에서 찾기…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (inNoteQuery.isNotEmpty()) {
                            IconButton(onClick = { inNoteQuery = "" }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> 음성 기록Tab(transcript, inNoteQuery)
                    1 -> 요약Tab(summary, inNoteQuery)
                    2 -> MindMapTab(mindMap)
                    3 -> 질문Tab(
                        answer = askAnswer,
                        is질문ing = is질문ing,
                        on질문 = { q -> recordingViewModel.askNoteQuestion(filename, q) }
                    )
                }
            }
        }
    }
}

@Composable
private fun 음성 기록Tab(transcript: String, query: String) {
    if (transcript.isEmpty()) {
        PlaceholderText("Tap '분석' to transcribe this recording.")
    } else {
        val formatted = remember(transcript) { 음성 기록Formatter.formatParagraphs(transcript) }
        val highlightedText = remember(formatted, query) { highlightMatches(formatted, query) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = highlightedText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun 요약Tab(summary: String, query: String) {
    if (summary.isEmpty()) {
        PlaceholderText("No summary yet. Tap '분석' to generate one.")
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val sections = parse요약Sections(summary)
            if (sections.isEmpty()) {
                Text(highlightMatches(summary, query), style = MaterialTheme.typography.bodyMedium)
            } else {
                sections.forEach { (key, value) ->
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = highlightMatches(value, query), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MindMapTab(mindMap: String) {
    if (mindMap.isEmpty()) {
        PlaceholderText("No mind map yet. Tap '분석' to generate one.")
    } else {
        MindMapCanvas(markdown = mindMap)
    }
}

@Composable
private fun PlaceholderText(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun 질문Tab(
    answer: String?,
    is질문ing: Boolean,
    on질문: (String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            placeholder = { Text("질문 a question about this note…") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        Button(
            onClick = { if (question.isNotBlank()) on질문(question) },
            enabled = question.isNotBlank() && !is질문ing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (is질문ing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (is질문ing) "Thinking…" else "질문")
        }
        if (answer != null) {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (!is질문ing) {
            Text(
                text = "Answers come from the note's summary.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Turns a JSON summary blob into readable (header, text) pairs.
 * Falls back to empty list if the input isn't JSON-like.
 */
private fun parse요약Sections(raw: String): List<Pair<String, String>> {
    val trimmed = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    if (!trimmed.startsWith("{")) return emptyList()
    return try {
        val sections = mutableListOf<Pair<String, String>>()
        // Simple regex-based extraction: "key": value (string or array)
        val fieldRegex = Regex(""""(\w+)"\s*:\s*("(?:[^"\\]|\\.)*"|\[[\s\S]*?]|\{[\s\S]*?})""")
        fieldRegex.findAll(trimmed).forEach { match ->
            val key = match.groupValues[1]
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
            val valueRaw = match.groupValues[2].trim()
            val value = when {
                valueRaw.startsWith('"') -> valueRaw.removeSurrounding("\"").replace("\\n", "\n").replace("\\\"", "\"")
                valueRaw.startsWith('[') -> {
                    // Extract strings from array
                    Regex(""""([^"\\]*)"""").findAll(valueRaw)
                        .map { "• ${it.groupValues[1]}" }
                        .joinToString("\n")
                }
                else -> valueRaw
            }
            if (value.isNotBlank()) sections.add(key to value)
        }
        sections
    } catch (_: Exception) {
        emptyList()
    }
}

private fun formatFilename(filename: String): String {
    val base = filename.substringBeforeLast(".")
    val match = Regex("""(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})""").find(base) ?: return base
    val (year, month, day, hour, min, sec) = match.destructured
    return "$year-$month-$day $hour:$min:$sec"
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

internal fun highlightMatches(text: String, query: String): AnnotatedString = buildAnnotatedString {
    if (query.isBlank()) { append(text); return@buildAnnotatedString }
    val lower = text.lowercase()
    val q = query.lowercase()
    var start = 0
    while (true) {
        val match = findWhitespaceInsensitiveMatch(lower, q, start)
        if (match == null) { append(text.substring(start)); break }
        val (matchStart, matchEnd) = match
        append(text.substring(start, matchStart))
        withStyle(SpanStyle(background = Color(0xFFFFFF00), color = Color.Black)) {
            append(text.substring(matchStart, matchEnd))
        }
        start = matchEnd
    }
}

/**
 * Finds the next occurrence of [query] in [text] starting at or after [from], treating any run
 * of whitespace in [query] as matching any run of whitespace in [text]. This lets a query that
 * spans a formatting-introduced line break (e.g. a paragraph break inserted between sentences)
 * still highlight correctly, since search matches against the raw, unformatted transcript.
 *
 * Both [text] and [query] are expected to already be lowercased by the caller. Runs in O(n*m)
 * worst case (n = text length, m = query length) via a plain two-pointer scan - no regex, so no
 * catastrophic backtracking risk on long transcripts.
 */
internal fun findWhitespaceInsensitiveMatch(text: String, query: String, from: Int): Pair<Int, Int>? {
    for (start in from..text.length) {
        var ti = start
        var qi = 0
        var ok = true
        while (qi < query.length) {
            val qc = query[qi]
            if (qc.isWhitespace()) {
                if (ti >= text.length || !text[ti].isWhitespace()) { ok = false; break }
                while (ti < text.length && text[ti].isWhitespace()) ti++
                while (qi < query.length && query[qi].isWhitespace()) qi++
            } else {
                if (ti >= text.length || text[ti] != qc) { ok = false; break }
                ti++
                qi++
            }
        }
        if (ok && qi == query.length) return start to ti
    }
    return null
}

