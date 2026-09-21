package com.lookseesee.app.ui.setup

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lookseesee.app.R
import com.lookseesee.app.data.model.Album
import com.lookseesee.app.util.AppLanguage

@Composable
fun SetupScreen(
    onBeginSession: () -> Unit,
    viewModel: SetupViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.values.all { it }
        viewModel.onMediaPermissionResult(granted)
    }

    fun requestMediaPermission() {
        val permissions = arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        permissionLauncher.launch(permissions)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            LanguageToggle(
                language = state.language,
                onLanguageChange = viewModel::setLanguage,
            )
        }

        if (!state.hasMediaPermission) {
            PermissionCard(onGrant = { requestMediaPermission() })
        }

        Text(
            text = stringResource(R.string.setup_section_albums),
            style = MaterialTheme.typography.titleLarge,
        )
        if (state.isLoadingAlbums) {
            CircularProgressIndicator()
        } else if (state.albums.isEmpty()) {
            Text(
                text = stringResource(R.string.setup_albums_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.albums.forEach { album ->
                    AlbumTile(
                        album = album,
                        selected = album.bucketId in state.selectedAlbumIds,
                        onToggle = { viewModel.toggleAlbum(album.bucketId) },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.setup_section_timer),
            style = MaterialTheme.typography.titleLarge,
        )
        Column {
            Text(stringResource(R.string.setup_minutes_label, state.minutes))
            Slider(
                value = state.minutes.toFloat(),
                onValueChange = { viewModel.setMinutes(it.toInt()) },
                valueRange = 1f..60f,
                steps = 58,
            )
        }

        Text(
            text = stringResource(R.string.setup_section_pin),
            style = MaterialTheme.typography.titleLarge,
        )
        PinSetupSection(
            hasPin = state.hasPin,
            pinMismatch = state.pinMismatch,
            onSubmit = { pin, confirm -> viewModel.submitNewPin(pin, confirm) },
            onEditedAgain = viewModel::clearPinMismatch,
        )

        Text(
            text = stringResource(R.string.setup_section_interrupts),
            style = MaterialTheme.typography.titleLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.width(260.dp)) {
                Text(stringResource(R.string.setup_interrupts_toggle))
                Text(
                    text = stringResource(R.string.setup_interrupts_description),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val dndManager = remember { com.lookseesee.app.kiosk.DndManager(context) }
            val dndAccessLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { viewModel.setSilenceNotifications(true) }
            Switch(
                checked = state.silenceNotifications,
                onCheckedChange = { enabled ->
                    if (enabled && !dndManager.hasAccess()) {
                        dndAccessLauncher.launch(dndManager.requestAccessIntent())
                    } else {
                        viewModel.setSilenceNotifications(enabled)
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onBeginSession,
            enabled = state.readyToBegin,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.setup_begin_session))
        }
        if (!state.readyToBegin) {
            Text(
                text = stringResource(R.string.setup_begin_session_disabled_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LanguageToggle(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = language == AppLanguage.ENGLISH,
            onClick = { onLanguageChange(AppLanguage.ENGLISH) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text(stringResource(R.string.language_toggle_english)) }
        SegmentedButton(
            selected = language == AppLanguage.CHINESE_SIMPLIFIED,
            onClick = { onLanguageChange(AppLanguage.CHINESE_SIMPLIFIED) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text(stringResource(R.string.language_toggle_chinese)) }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.permission_rationale_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.permission_rationale_body))
            Button(onClick = onGrant) {
                Text(stringResource(R.string.permission_grant_button))
            }
        }
    }
}

@Composable
private fun AlbumTile(album: Album, selected: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onToggle),
    ) {
        Column {
            AsyncImage(
                model = album.thumbnailUri,
                contentDescription = album.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    text = album.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.setup_album_item_count, album.itemCount),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color.White, CircleShape),
            )
        }
    }
}

@Composable
private fun PinSetupSection(
    hasPin: Boolean,
    pinMismatch: Boolean,
    onSubmit: (String, String) -> Boolean,
    onEditedAgain: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (hasPin && pin.isEmpty() && confirmPin.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.setup_section_pin))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = pin,
                onValueChange = { new ->
                    if (new.length <= 4 && new.all { it.isDigit() }) {
                        pin = new
                        onEditedAgain()
                    }
                },
                label = { Text(stringResource(R.string.setup_pin_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { new ->
                    if (new.length <= 4 && new.all { it.isDigit() }) {
                        confirmPin = new
                        onEditedAgain()
                        if (pin.length == 4 && new.length == 4) {
                            onSubmit(pin, new)
                        }
                    }
                },
                label = { Text(stringResource(R.string.setup_pin_confirm_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.weight(1f),
            )
        }
        if (pinMismatch) {
            Text(
                text = stringResource(R.string.setup_pin_mismatch),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
