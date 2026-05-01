package com.elroi.lemurloop.ui.screen.alarm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.elroi.lemurloop.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAlarmBottomSheet(
    onDismiss: () -> Unit,
    onChat: () -> Unit,
    onWizard: () -> Unit,
    onDetailed: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.new_alarm_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SheetOptionRow(
                icon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(28.dp)) },
                title = stringResource(R.string.new_alarm_option_chat),
                onClick = onChat
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SheetOptionRow(
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(28.dp)) },
                title = stringResource(R.string.new_alarm_option_wizard),
                onClick = onWizard
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SheetOptionRow(
                icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(28.dp)) },
                title = stringResource(R.string.new_alarm_option_detailed),
                onClick = onDetailed
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SheetOptionRow(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
