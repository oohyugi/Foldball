package com.yogi.foldball.core.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection

val LocalFoldballSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("FoldballSnackbarHostState is not provided")
}

fun tornShape(vararg points: Pair<Float, Float>) = GenericShape { size, _ ->
    points.forEachIndexed { index, (x, y) ->
        val point = Offset(x * size.width, y * size.height)
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
}

object FoldballShapes {
    val Card = tornShape(
        0f to .04f, .12f to 0f, .36f to .03f, .62f to 0f, .88f to .03f, 1f to .01f,
        .99f to .38f, 1f to .70f, .98f to 1f, .70f to .97f, .40f to 1f, .14f to .97f,
        .01f to 1f, .02f to .62f,
    )
    val Button = tornShape(
        .01f to .12f, .08f to .03f, .30f to .08f, .52f to .02f, .76f to .07f, .97f to .02f,
        1f to .30f, .98f to .62f, 1f to .92f, .74f to .97f, .50f to 1f, .26f to .95f,
        .06f to .99f, 0f to .70f, .02f to .40f,
    )
    val Chip = tornShape(
        .04f to .08f, .30f to .02f, .58f to .07f, .96f to .03f, .99f to .40f,
        .95f to .96f, .60f to 1f, .28f to .94f, .03f to .98f, 0f to .50f,
    )
    val Banner = tornShape(
        0f to .06f, .05f to .01f, .14f to .05f, .30f to 0f, .48f to .04f, .66f to 0f,
        .84f to .04f, .97f to 0f, 1f to .10f, .98f to .40f, 1f to .72f, .96f to .96f,
        .80f to 1f, .60f to .96f, .42f to 1f, .22f to .95f, .08f to 1f, .01f to .90f,
        .03f to .55f,
    )
}

@Composable
fun FoldballScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFA87647), FoldballColor.Wood, Color(0xFFA06E40))))
            .drawBehind {
                val spacing = 9.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(FoldballColor.WoodDark.copy(alpha = .10f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    y += spacing
                }
            }
            .padding(horizontal = 14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(FoldballSpace.md)) {
            topBar()
            content()
        }
    }
}

@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    shape: Shape = FoldballShapes.Card,
    tilt: Float = FoldballTilt.PaperCard,
    ruled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .rotate(tilt)
            .drawBehind {
                val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
                val path = (outline as? androidx.compose.ui.graphics.Outline.Generic)?.path
                    ?: Path().apply { addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size)) }
                translate(top = FoldballShadow.offsetY.toPx()) {
                    drawPath(path, FoldballShadow.paperColor.copy(alpha = FoldballShadow.paperOpacity))
                }
            }
            .clip(shape)
            .background(FoldballColor.Paper)
            .then(if (ruled) Modifier.drawBehind {
                var y = 22.dp.toPx()
                while (y < size.height) {
                    drawLine(FoldballColor.PaperLine.copy(alpha = .44f), Offset.Zero.copy(y = y), Offset(size.width, y), 1.dp.toPx())
                    y += 22.dp.toPx()
                }
            } else Modifier),
        content = content,
    )
}

enum class PaperButtonStyle(val container: Color, val content: Color) {
    Primary(FoldballColor.InkBlue, Color.White),
    Secondary(FoldballColor.Paper, FoldballColor.Ink),
    Danger(FoldballColor.TeamRed, Color.White),
}

@Composable
fun PaperButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PaperButtonStyle = PaperButtonStyle.Primary,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    compact: Boolean = false,
) {
    PressableSurface(
        onClick = onClick,
        shape = FoldballShapes.Button,
        modifier = modifier.heightIn(min = if (compact) 48.dp else 52.dp).widthIn(min = 48.dp),
        enabled = enabled,
        color = if (enabled) style.container else Color(0xFFB9B2A2),
        hardShadow = style == PaperButtonStyle.Primary,
    ) {
        Row(
            Modifier.padding(horizontal = if (compact) 8.dp else 20.dp, vertical = if (compact) 8.dp else 10.dp),
            horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Text(
                text.uppercase(),
                style = if (compact) FoldballType.Title.copy(fontSize = 20.sp) else FoldballType.Title,
                color = if (enabled) style.content else FoldballColor.Ink.copy(alpha = .55f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PaperChip(
    text: String,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    PressableSurface(
        onClick = onClick,
        shape = FoldballShapes.Chip,
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { stateDescription = if (selected) "Selected" else "Not selected" },
        enabled = enabled,
        color = if (selected) FoldballColor.Tape else FoldballColor.Paper,
        contentDescription = contentDescription,
    ) {
        Text(text, Modifier.align(Alignment.Center).padding(horizontal = 10.dp), style = FoldballType.Caption, color = FoldballColor.Ink)
    }
}

@Composable
fun PaperIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressableSurface(
        onClick = onClick,
        shape = FoldballShapes.Chip,
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentDescription = contentDescription,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(27.dp).align(Alignment.Center), tint = FoldballColor.Ink)
    }
}

@Composable
fun PaperToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm),
    ) {
        Canvas(Modifier.size(30.dp)) {
            drawRect(FoldballColor.Ink, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
            if (checked) {
                val check = Path().apply {
                    moveTo(size.width * .18f, size.height * .52f)
                    lineTo(size.width * .42f, size.height * .76f)
                    lineTo(size.width * .84f, size.height * .24f)
                }
                drawPath(check, FoldballColor.InkBlue, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
            }
        }
        Text(label, style = FoldballType.Label, color = FoldballColor.Ink)
    }
}

@Composable
fun CirclePickerItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    PressableSurface(
        onClick = onClick,
        shape = FoldballShapes.Chip,
        modifier = modifier.size(74.dp).semantics {
            this.selected = selected
            this.contentDescription = label
            stateDescription = if (selected) "Selected" else "Not selected"
        },
        color = FoldballColor.Paper,
        content = {
            content()
            if (selected) Canvas(Modifier.fillMaxSize().padding(3.dp)) {
                drawOval(FoldballColor.InkBlue, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
            }
        },
    )
}

@Composable
fun TapeLabel(text: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    Text(
        text.uppercase(),
        style = if (compact) FoldballType.Caption.copy(fontSize = 11.sp, letterSpacing = .5.sp) else FoldballType.Caption,
        color = FoldballColor.Ink,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.rotate(FoldballTilt.TapeLabel).background(FoldballColor.Tape)
            .padding(horizontal = if (compact) 5.dp else 14.dp, vertical = 3.dp),
    )
}

@Composable
fun InkToast(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier.rotate(FoldballTilt.Toast).clip(FoldballShapes.Chip).background(FoldballColor.Ink)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
            Text(text, style = FoldballType.Caption, color = FoldballColor.Paper)
            if (actionLabel != null && onAction != null) {
                PaperButton(actionLabel, onAction, style = PaperButtonStyle.Secondary)
            }
        }
    }
}

@Composable
fun FoldballSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { snackbarData ->
        InkToast(
            text = snackbarData.visuals.message,
            actionLabel = snackbarData.visuals.actionLabel,
            onAction = if (snackbarData.visuals.actionLabel == null) null else snackbarData::performAction,
        )
    }
}

@Composable
fun MenuCard(
    title: String,
    tilt: Float,
    locked: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    art: @Composable RowScope.() -> Unit,
) {
    PressableSurface(
        onClick = onClick,
        shape = FoldballShapes.Card,
        modifier = modifier.rotate(tilt).heightIn(min = 118.dp)
            .semantics { if (locked) stateDescription = "Locked" },
        enabled = !locked,
        color = FoldballColor.Paper,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title.uppercase(), style = FoldballType.Label, color = FoldballColor.Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = art)
        }
        if (locked) {
            Box(Modifier.fillMaxSize().background(FoldballColor.Ink.copy(alpha = .42f)))
        }
    }
}

@Composable
fun PressableSurface(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = FoldballColor.Paper,
    hardShadow: Boolean = false,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, tween(120), label = "paper-press")
    Box(
        modifier = modifier.scale(scale).drawBehind {
            val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
            val path = (outline as? androidx.compose.ui.graphics.Outline.Generic)?.path
                ?: Path().apply { addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size)) }
            translate(top = FoldballShadow.offsetY.toPx()) {
                drawPath(
                    path,
                    if (hardShadow) FoldballShadow.primaryColor
                    else FoldballShadow.paperColor.copy(alpha = FoldballShadow.paperOpacity),
                )
            }
        }.clip(shape).background(color).clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        ).then(if (contentDescription == null) Modifier else Modifier.semantics { this.contentDescription = contentDescription }),
        content = content,
    )
}

@Composable
fun PaperDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = "Back",
    danger: Boolean = false,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        PaperCard(shape = FoldballShapes.Banner, tilt = FoldballTilt.Banner, ruled = false) {
            Column(
                Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FoldballSpace.md),
            ) {
                Text(title.uppercase(), style = FoldballType.Title, color = FoldballColor.Ink, textAlign = TextAlign.Center)
                Text(message, style = FoldballType.Body, color = FoldballColor.Ink, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
                    if (dismissText != null) PaperButton(dismissText, onDismiss, style = PaperButtonStyle.Secondary)
                    PaperButton(confirmText, onConfirm, style = if (danger) PaperButtonStyle.Danger else PaperButtonStyle.Primary)
                }
            }
        }
    }
}
