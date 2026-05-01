package com.elroi.lemurloop.ui.screen.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elroi.lemurloop.R
import com.elroi.lemurloop.ui.viewmodel.ChatMessageUi
import com.elroi.lemurloop.ui.viewmodel.LemurChatApiError
import com.elroi.lemurloop.ui.viewmodel.LemurChatUiState
import com.elroi.lemurloop.ui.viewmodel.LemurChatViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LemurChatScreen(
    onNavigateUp: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWizard: () -> Unit,
    onNavigateToDetailed: (String?) -> Unit,
    viewModel: LemurChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val assistantName = stringResource(R.string.assistant_name)
    val missingKeyMsg = stringResource(R.string.lemur_chat_missing_key_message)

    LaunchedEffect(state.messages.size, state.isSending) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }

    if (state.apiErrorDialog != null) {
        ChatApiErrorDialog(
            error = state.apiErrorDialog!!,
            onDismiss = { viewModel.dismissApiError() },
            onOpenSettings = {
                viewModel.dismissApiError()
                onNavigateToSettings()
            }
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.lemur_chat_title))
                        Text(
                            text = stringResource(R.string.lemur_chat_privacy_hint_short),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.content_desc_settings))
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = state.inputText,
                enabled = !state.isSending,
                onTextChange = viewModel::onInputChange,
                onSend = { viewModel.sendUserMessage(assistantName, missingKeyMsg) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }
            item {
                AlarmDraftPreviewCard(
                    ui = state,
                    onActivate = {
                        if (viewModel.persistAlarmActive()) onNavigateUp()
                    },
                    onSaveInactive = {
                        if (viewModel.persistAlarmInactive()) onNavigateUp()
                    },
                    onOpenWizard = onNavigateToWizard,
                    onOpenDetailed = {
                        val id = state.persistedAlarmId ?: state.draftAlarm?.id
                        onNavigateToDetailed(id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessageUi
) {
    val align = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .align(align),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun AlarmDraftPreviewCard(
    ui: LemurChatUiState,
    onActivate: () -> Unit,
    onSaveInactive: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenDetailed: () -> Unit
) {
    val draft = ui.draftAlarm ?: return
    val timeFmt = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }
    var menuOpen by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.lemur_chat_preview_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.content_desc_overflow_menu))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lemur_chat_overflow_wizard)) },
                            onClick = {
                                menuOpen = false
                                onOpenWizard()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lemur_chat_overflow_detailed)) },
                            onClick = {
                                menuOpen = false
                                onOpenDetailed()
                            }
                        )
                    }
                }
            }
            Text(
                text = draft.time.format(timeFmt),
                style = MaterialTheme.typography.headlineSmall
            )
            draft.label?.let {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = if (draft.daysOfWeek.isEmpty()) {
                    stringResource(R.string.lemur_chat_preview_one_time)
                } else {
                    draft.daysOfWeek.joinToString()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onActivate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.lemur_chat_preview_activate))
                }
                OutlinedButton(
                    onClick = onSaveInactive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.lemur_chat_preview_save_inactive))
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            placeholder = { Text(stringResource(R.string.lemur_chat_input_hint)) },
            minLines = 1,
            maxLines = 4
        )
        FilledTonalButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.lemur_chat_send)
            )
        }
    }
}

@Composable
private fun ChatApiErrorDialog(
    error: LemurChatApiError,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val title = stringResource(R.string.lemur_chat_error_dialog_title)
    val body = when (error) {
        LemurChatApiError.NO_API_KEY -> stringResource(R.string.lemur_chat_error_no_key_body)
        LemurChatApiError.NETWORK,
        LemurChatApiError.UNKNOWN -> stringResource(R.string.lemur_chat_error_network_body)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.lemur_chat_error_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}
