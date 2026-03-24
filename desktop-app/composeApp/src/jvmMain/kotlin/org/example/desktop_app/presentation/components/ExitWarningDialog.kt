package org.example.desktop_app.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import desktop_app.composeapp.generated.resources.Res
import desktop_app.composeapp.generated.resources.exit_and_cancel
import desktop_app.composeapp.generated.resources.exit_warning_text
import desktop_app.composeapp.generated.resources.exit_warning_title
import desktop_app.composeapp.generated.resources.keep_downloading
import org.example.desktop_app.presentation.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExitWarningDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    AppTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.exit_warning_title)) },
            text = { Text(stringResource(Res.string.exit_warning_text)) },
            confirmButton = {
                Button(onClick = onConfirmExit) {
                    Text(stringResource(Res.string.exit_and_cancel))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.keep_downloading))
                }
            }
        )
    }
}