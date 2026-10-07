package com.yogi.foldball.feature.club.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballSpace
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperDialog
import com.yogi.foldball.domain.team.TeamProfile

@Composable
fun EditClubNameDialog(
    name: String,
    shortName: String,
    badgeId: Int,
    onDismiss: () -> Unit,
    onApply: (String, String) -> Unit
) {
    var draftName by rememberSaveable { mutableStateOf(name) }
    var draftShortName by rememberSaveable { mutableStateOf(shortName) }
    val valid = TeamProfile.create(draftName, badgeId, draftShortName.ifBlank { null }) != null
    PaperDialog(
        title = "Edit club name",
        message = "Apply updates your draft. Save club keeps your changes.",
        onDismiss = onDismiss,
        confirmText = "Apply",
        confirmEnabled = valid,
        onConfirm = { if (valid) onApply(draftName.trim(), draftShortName) },
        dismissText = "Cancel",
        stackedActions = true,
        content = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
                OutlinedTextField(
                    draftName,
                    { draftName = it.take(TeamProfile.MAX_NAME_LENGTH) },
                    label = { Text("Club name", style = FoldballType.Caption) },
                    supportingText = {
                        Text("${draftName.length}/${TeamProfile.MAX_NAME_LENGTH} characters", style = FoldballType.Caption, color = FoldballColor.InkMuted)
                    },
                    textStyle = FoldballType.Body,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    draftShortName,
                    { draftShortName = it.filter(Char::isLetterOrDigit).uppercase().take(TeamProfile.MAX_SHORT_NAME_LENGTH) },
                    label = { Text("Short name (up to 4 letters)", style = FoldballType.Caption) },
                    placeholder = { Text("Auto", style = FoldballType.Body) },
                    textStyle = FoldballType.Body,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
