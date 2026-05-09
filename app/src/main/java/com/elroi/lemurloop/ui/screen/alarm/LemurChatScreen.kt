package com.elroi.lemurloop.ui.screen.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elroi.lemurloop.R
import com.elroi.lemurloop.ui.viewmodel.ChatMessageUi
import com.elroi.lemurloop.ui.viewmodel.LemurChatApiError
import com.elroi.lemurloop.ui.viewmodel.LemurChatUiState
import com.elroi.lemurloop.ui.viewmodel.LemurChatViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Extra bottom [LazyColumn] inset so thread content clears [ChatInputBar] in the scaffold bottom bar.
 * Approximate upper bound for OutlinedTextField (up to four lines) + send row + padding; tune in QA if clipped.
 * Keep in sync when [ChatInputBar] layout changes.
 */
private val LemurChatComposerBottomInset = 120.dp

/**
 * LazyColumn `item` count before `items(state.messages)`, when [showOnboarding] is true.
 * Must match each gated `item { }` above the message list (onboarding row, starter chips row).
 * PR2: pass `includeLocalAssistantBubble = true` and insert the bubble as the first gated item (+1).
 */
private fun leadingItemCountBeforeMessages(
    showOnboarding: Boolean,
    includeLocalAssistantBubble: Boolean = false,
): Int = when {
    !showOnboarding -> 0
    includeLocalAssistantBubble -> 3
    else -> 2
}

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
    val showOnboarding = state.messages.isEmpty()
    val leadingBeforeMessages =
        leadingItemCountBeforeMessages(showOnboarding = showOnboarding, includeLocalAssistantBubble = false)

    LaunchedEffect(state.messages.size, state.isSending, showOnboarding) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(leadingBeforeMessages + state.messages.lastIndex)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
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
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 8.dp + LemurChatComposerBottomInset
            )
        ) {
            if (showOnboarding) {
                item(key = "onboarding") {
                    LemurChatOnboarding(
                        onOpenWizard = onNavigateToWizard,
                        onOpenDetailed = { onNavigateToDetailed(null) }
                    )
                }
                item(key = "starter_chips") {
                    val starterLabels = stringArrayResource(R.array.lemur_chat_starter_labels)
                    val starterPrompts = stringArrayResource(R.array.lemur_chat_starter_prompts)
                    LemurChatStarterChips(
                        labels = starterLabels,
                        prompts = starterPrompts,
                        chipsEnabled = !state.isSending,
                        onPromptSelected = { text ->
                            viewModel.sendStarterMessage(text, assistantName, missingKeyMsg)
                        }
                    )
                }
            }
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
private fun LemurChatOnboarding(
    onOpenWizard: () -> Unit,
    onOpenDetailed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.lemur_chat_onboarding_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.lemur_chat_onboarding_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onOpenWizard) {
                Text(stringResource(R.string.lemur_chat_overflow_wizard))
            }
            TextButton(onClick = onOpenDetailed) {
                Text(stringResource(R.string.lemur_chat_overflow_detailed))
            }
        }
    }
}

@Composable
private fun LemurChatStarterChips(
    labels: Array<String>,
    prompts: Array<String>,
    chipsEnabled: Boolean,
    onPromptSelected: (String) -> Unit
) {
    val count = minOf(labels.size, prompts.size)
    if (count == 0) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(count, key = { i -> "$i-${labels[i]}" }) { index ->
            val labelText = labels[index]
            val fullPrompt = prompts[index]
            val chipA11y = stringResource(R.string.lemur_chat_starter_chip_a11y, fullPrompt)
            SuggestionChip(
                onClick = { onPromptSelected(fullPrompt) },
                label = {
                    Text(
                        text = labelText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                enabled = chipsEnabled,
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = chipA11y }
            )
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
                text = stringResource(R.string.lemur_chat_preview_advanced_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = draft.time.format(timeFmt),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp)
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
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(8.dp),
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
