package com.lookseesee.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lookseesee.app.R

/**
 * A modal PIN prompt. [onSubmit] is called with the entered digits once the field
 * reaches [maxDigits]; the caller decides whether it was correct and either
 * dismisses or sets [isError].
 */
@Composable
fun PinDialog(
    title: String = stringResource(R.string.pin_dialog_title),
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    isError: Boolean,
    maxDigits: Int = 4,
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { new ->
                        if (new.length <= maxDigits && new.all { it.isDigit() }) {
                            pin = new
                            if (new.length == maxDigits) onSubmit(new)
                        }
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isError) {
                    Text(
                        text = stringResource(R.string.pin_dialog_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(pin) }, enabled = pin.length == maxDigits) {
                Text(title)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pin_dialog_cancel))
            }
        },
    )
}
