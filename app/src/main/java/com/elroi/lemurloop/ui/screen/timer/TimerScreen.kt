package com.elroi.lemurloop.ui.screen.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.timer.CountdownTimer
import com.elroi.lemurloop.domain.timer.keepTimerScreenOn
import com.elroi.lemurloop.ui.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var renameTarget by remember { mutableStateOf<CountdownTimer?>(null) }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.timer_title)) },
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_add_timer))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.content_desc_timer_settings))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            KeepTimerScreenOn(keepTimerScreenOn(state.timers))
            if (state.limitReached) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.timer_limit_reached),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::clearLimitMessage) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_cancel))
                    }
                }
            }
            if (state.undoVisible) {
                TextButton(onClick = viewModel::undo) {
                    Text(stringResource(R.string.timer_undo))
                }
            }
            if (state.ready && state.timers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.timer_empty))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.timers, key = { it.id }) { timer ->
                        TimerRow(
                            timer = timer,
                            nowEpochMillis = state.nowEpochMillis,
                            onOpen = { onOpen(timer.id) },
                            onPause = { viewModel.pause(timer.id) },
                            onPlay = { viewModel.play(timer.id) },
                            onAddMinute = { viewModel.addOneMinute(timer.id) },
                            onRefresh = { viewModel.refresh(timer.id) },
                            onDismiss = viewModel::dismissRinging,
                            onRename = { renameTarget = timer },
                            onDuplicate = { viewModel.duplicate(timer.id) },
                            onDelete = { viewModel.delete(timer.id) }
                        )
                    }
                }
            }
        }
    }
    renameTarget?.let { timer ->
        RenameTimerDialog(
            initial = timer.label,
            onDismiss = { renameTarget = null },
            onSave = { label ->
                viewModel.rename(timer.id, label)
                renameTarget = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerRow(
    timer: CountdownTimer,
    nowEpochMillis: Long,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onAddMinute: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.timer_delete))
            }
        }
    ) {
        Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timer.label.ifBlank { stringResource(R.string.timer_title) },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TimerDigits(
                        text = countdownText(timer, nowEpochMillis),
                        style = MaterialTheme.typography.titleLarge
                    )
                    TimerRowMenu(onRename = onRename, onDuplicate = onDuplicate)
                }
                TimerActions(timer, onPause, onPlay, onAddMinute, onRefresh, onDismiss)
            }
        }
    }
}

@Composable
private fun TimerRowMenu(onRename: () -> Unit, onDuplicate: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.content_desc_overflow_menu))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.timer_rename)) },
                onClick = { open = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.timer_duplicate)) },
                onClick = { open = false; onDuplicate() }
            )
        }
    }
}

@Composable
private fun RenameTimerDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var label by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.timer_rename)) },
        text = {
            OutlinedTextField(value = label, onValueChange = { label = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onSave(label) }) { Text(stringResource(R.string.btn_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
