package com.daedalusapps.echo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import android.content.Context
import com.daedalusapps.echo.util.CalendarIntegration
import com.daedalusapps.echo.data.model.TodoItem
import com.daedalusapps.echo.ui.components.SwipeToDeleteCard
import com.daedalusapps.echo.viewmodel.TodoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    todoViewModel: TodoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val todos by todoViewModel.todos.collectAsState()
    val isExtracting by todoViewModel.isExtracting.collectAsState()
    val extractError by todoViewModel.extractError.collectAsState()
    val lastExtractCount by todoViewModel.lastExtractCount.collectAsState()

    var showLookbackDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // TodoViewModel is Activity-scoped, so a completion/error from a previous visit to this
    // screen can still be sitting in state. Clear it once on entry, before the snackbar
    // effects below observe it, so only fresh completions from this visit show a snackbar.
    LaunchedEffect(Unit) {
        todoViewModel.clearLastExtractCount()
        todoViewModel.clearError()
    }

    LaunchedEffect(lastExtractCount) {
        if (lastExtractCount != null) {
            val count = lastExtractCount!!
            snackbar.showSnackbar(if (count > 0) "새 할 일 ${count}개를 추가했습니다" else "새로운 할 일이 없습니다")
            todoViewModel.clearLastExtractCount()
        }
    }

    LaunchedEffect(extractError) {
        extractError?.let {
            snackbar.showSnackbar(it)
            todoViewModel.clearError()
        }
    }

    if (showLookbackDialog) {
        LookbackDialog(
            context = context,
            onDismiss = { showLookbackDialog = false },
            onConfirm = { hours ->
                showLookbackDialog = false
                todoViewModel.updateFromRecordings(hours)
            }
        )
    }

    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onSave = { text ->
                todoViewModel.addTodo(text)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("할 일") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showLookbackDialog = true },
                            enabled = !isExtracting
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "녹음에서 업데이트")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (isExtracting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "할 일 추가")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it) } }
    ) { innerPadding ->
        if (todos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "아직 할 일이 없습니다.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "위의 ✨ 버튼으로 녹음에서 할 일을 추출하거나 + 버튼으로 직접 추가할 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todos, key = { it.id }) { todo ->
                    TodoSwipeToDeleteCard(
                        todo = todo,
                        onToggleDone = { todoViewModel.toggleDone(todo) },
                        onDelete = { todoViewModel.deleteTodo(todo) },
                        onEditSave = { newText -> todoViewModel.editTodo(todo, newText) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoSwipeToDeleteCard(
    todo: TodoItem,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onEditSave: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember(todo.id, showEditDialog) { mutableStateOf(todo.text) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("할 일 편집") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditSave(editText)
                        showEditDialog = false
                    },
                    enabled = editText.isNotBlank()
                ) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("취소") }
            }
        )
    }

    SwipeToDeleteCard(
        confirmTitle = "할 일을 삭제할까요?",
        confirmText = "This will permanently remove this todo.",
        onDelete = onDelete
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = todo.isDone, onCheckedChange = { onToggleDone() })
                Text(
                    text = todo.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
                    color = if (todo.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .clickable { showEditDialog = true }
                )
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        CalendarIntegration.addToCalendar(context, todo.text)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "캘린더에 추가",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTodoDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("할 일 추가") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("무엇을 해야 하나요?") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = text.isNotBlank()
            ) { Text("저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LookbackDialog(
    context: Context,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val prefs = remember { context.getSharedPreferences("daedalus_prefs", Context.MODE_PRIVATE) }
    val storedHours = remember { prefs.getLong(TODO_LOOKBACK_HOURS_KEY, TODO_LOOKBACK_HOURS_DEFAULT) }
    val initialSelection = remember { lookbackOptionFor(storedHours) }

    var selectedHours by remember {
        mutableStateOf(if (initialSelection is LookbackSelection.Standard) initialSelection.hours else null)
    }
    var customText by remember {
        mutableStateOf(if (initialSelection is LookbackSelection.Custom) initialSelection.hours.toString() else "")
    }
    val isCustomSelected = selectedHours == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("녹음에서 업데이트") },
        text = {
            Column(Modifier.selectableGroup()) {
                LOOKBACK_OPTIONS.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedHours == option.hours,
                                onClick = { selectedHours = option.hours }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedHours == option.hours, onClick = { selectedHours = option.hours })
                        Spacer(Modifier.width(8.dp))
                        Text(option.label)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isCustomSelected,
                            onClick = { selectedHours = null }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isCustomSelected, onClick = { selectedHours = null })
                    Spacer(Modifier.width(8.dp))
                    Text("직접 설정")
                }
                if (isCustomSelected) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("시간") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(start = 40.dp)
                    )
                }
            }
        },
        confirmButton = {
            val customHours = customText.toLongOrNull()
            val confirmHours = if (isCustomSelected) customHours else selectedHours
            Button(
                onClick = { if (confirmHours != null) onConfirm(confirmHours) },
                enabled = confirmHours != null && (!isCustomSelected || confirmHours > 0)
            ) { Text("업데이트") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
