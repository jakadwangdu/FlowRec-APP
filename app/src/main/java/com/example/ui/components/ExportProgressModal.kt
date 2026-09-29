package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.editor.export.ExportProgress
import com.example.editor.export.ExportState
import com.example.ui.theme.AccentRed

/**
 * Real-time hardware video export progress dialog.
 * Reflects actual encoding stages, real frame counts, elapsed/remaining time,
 * and supports graceful export cancellation.
 */
@Composable
fun ExportProgressModal(
    progressDetails: ExportProgress,
    projectName: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progressDetails.progressPercent / 100f).coerceIn(0f, 1f),
        label = "export_progress"
    )

    val percentInt = progressDetails.progressPercent.toInt().coerceIn(0, 100)
    val remainingSec = (progressDetails.estimatedRemainingTimeMs / 1000L).toInt().coerceAtLeast(0)
    val remainingFormatted = String.format("%02d:%02d", remainingSec / 60, remainingSec % 60)
    val elapsedSec = (progressDetails.elapsedTimeMs / 1000L).toInt().coerceAtLeast(0)
    val elapsedFormatted = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)

    val titleText = when (progressDetails.state) {
        ExportState.PREPARING -> "Preparing Render..."
        ExportState.EXPORTING -> "Rendering MP4 Video..."
        ExportState.FINALIZING -> "Finalizing & Saving..."
        ExportState.COMPLETED -> "Export Completed!"
        ExportState.CANCELLED -> "Export Cancelled"
        ExportState.FAILED -> "Export Failed"
        else -> "Exporting..."
    }

    Dialog(onDismissRequest = { /* Require explicit cancellation */ }) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier
                .fillMaxWidth()
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                .testTag("export_progress_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$percentInt%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = projectName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (progressDetails.currentStepMessage.isNotBlank()) {
                    Text(
                        text = progressDetails.currentStepMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Elapsed: $elapsedFormatted",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (progressDetails.state == ExportState.EXPORTING) {
                        Text(
                            text = "Remaining: ~$remainingFormatted",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccentRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_cancel_export")
                ) {
                    Text("Cancel Export", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }
    }
}
