package com.yogi.foldball.core.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.ic_back
import org.jetbrains.compose.resources.painterResource

/** Reusable Back action; the screen owner decides its navigation behavior. */
@Composable
fun PaperBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = "Go back"
) {
    PaperIconButton(
        icon = painterResource(Res.drawable.ic_back),
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier.size(48.dp),
        enabled = enabled
    )
}

/** Fixed paper header. Safe drawing insets remain the responsibility of the route root. */
@Composable
fun FoldballTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    actions: (@Composable () -> Unit)? = null,
    subtitle: String? = null
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)
    ) {
        PaperBackButton(onBack, enabled = backEnabled)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            PaperCard(tilt = FoldballTilt.Banner, ruled = false) {
                Column(
                    Modifier.padding(horizontal = FoldballSpace.lg, vertical = FoldballSpace.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FoldballSpace.xs)
                ) {
                    Text(
                        title,
                        style = FoldballType.Title,
                        color = FoldballColor.Ink,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            subtitle,
                            style = FoldballType.Caption,
                            color = FoldballColor.InkMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        if (actions != null) actions() else Spacer(Modifier.size(48.dp))
    }
}
