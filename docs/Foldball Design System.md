[Foundations](#foundations) [Shared components](#components) [Theme setup](#theme)

# Foldball Design System

Semua layar Foldball dibangun dari satu meja belajar: kayu sebagai alas, kertas sobek sebagai permukaan, tinta bolpoin sebagai teks, selotip kuning untuk penekanan. Halaman ini berisi token dan komponen shared untuk Jetpack Compose.

**Kertas di atas kayu**Setiap kontrol adalah selembar kertas atau potongan selotip di atas meja.

**Miring sedikit**Rotasi tiap elemen berbeda tipis. Jangan pernah 0 derajat untuk kartu.

**Bayangan keras**Offset ke bawah, tanpa blur besar. Seperti kertas yang ditempel, bukan melayang.

## Foundations

### Color

**Wood**#9A6A3C, alas semua layar

**WoodDark**#6F4524, serat & bayangan

**Paper**#F6F1E4, kartu & dialog

**PaperLine**#C9D4EA, garis buku tulis

**Ink**#1C1C22, teks & outline

**InkBlue**#1F4FC4, aksi utama, tim biru

**TeamRed**#D64545, tim merah, bahaya

**Tape**#F4D34A, label & fokus

**Grass**#3F8F43, lapangan

**Cardboard**#C99A62, pinggir arena

### Typography

| Style | Contoh | Font | Spec |
| --- | --- | --- | --- |
| display | FoldBall | Permanent Marker | 58sp, spacing -1 |
| title | PLAY | Permanent Marker | 40sp / 26sp |
| label | Team | Patrick Hand | 20sp, caps, +0.06em |
| body | Flick the ball into the goal | Patrick Hand | 17sp, line 24sp |
| caption | Paper football game | Patrick Hand | 14sp, caps, +0.08em |

### Spacing, rotation, elevation

xs 4dp

sm 8dp

md 12dp

lg 16dp

xl 24dp

| Token | Nilai | Dipakai untuk |
| --- | --- | --- |
| Tilt.Banner | -1.6° | Banner judul |
| Tilt.Cards | +1.0° / -1.2° / -0.8° / +1.3° | Grid menu, urut kiri-kanan, atas-bawah |
| Shadow.Soft | y 4dp, alpha 45% | Kartu, chip, banner |
| Shadow.Hard | y 4dp, tanpa blur, biru tua | Tombol PLAY dan tombol utama |
| Touch target | min 48dp | Semua yang bisa ditekan |
| Press scale | 0.97, 120ms | Tombol dan kartu saat ditekan |

### Torn paper shapes

Empat bentuk sobekan, semuanya poligon dengan titik pecahan 0..1 sehingga ukurannya fleksibel. Di Compose dipakai lewat satu fungsi `tornShape`.

CardShape

ButtonShape

ChipShape

BannerShape

```
fun tornShape(vararg p: Pair<Float, Float>) = GenericShape { size, _ ->
    p.forEachIndexed { i, (x, y) ->
        val px = x * size.width
        val py = y * size.height
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

object FoldballShapes {
    val Card = tornShape(
        0f to .04f, .12f to 0f, .36f to .03f, .62f to 0f, .88f to .03f, 1f to .01f,
        .99f to .38f, 1f to .70f, .98f to 1f, .70f to .97f, .40f to 1f, .14f to .97f,
        .01f to 1f, .02f to .62f
    )
    val Button = tornShape(
        .01f to .12f, .08f to .03f, .30f to .08f, .52f to .02f, .76f to .07f, .97f to .02f,
        1f to .30f, .98f to .62f, 1f to .92f, .74f to .97f, .50f to 1f, .26f to .95f,
        .06f to .99f, 0f to .70f, .02f to .40f
    )
    val Chip = tornShape(
        .04f to .08f, .30f to .02f, .58f to .07f, .96f to .03f, .99f to .40f,
        .95f to .96f, .60f to 1f, .28f to .94f, .03f to .98f, 0f to .50f
    )
    val Banner = tornShape(
        0f to .06f, .05f to .01f, .14f to .05f, .30f to 0f, .48f to .04f, .66f to 0f,
        .84f to .04f, .97f to 0f, 1f to .10f, .98f to .40f, 1f to .72f, .96f to .96f,
        .80f to 1f, .60f to .96f, .42f to 1f, .22f to .95f, .08f to 1f, .01f to .90f, .03f to .55f
    )
}
```

## Shared components

Semua composable ini ada di modul `core:designsystem` dan dipakai ulang oleh menu, pemilihan tim, arena, dan layar pertandingan. Layar tidak boleh membuat Box + clip sendiri.

### FoldballScaffold

`layar apa pun`

Alas kayu untuk setiap layar. Menggambar serat lewat `drawBehind` dan menangani insets sistem.

```
@Composable
fun FoldballScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFA87647), Wood, Color(0xFFA06E40))))
            .drawBehind { drawWoodGrain(WoodDark.copy(alpha = .10f), spacing = 9.dp.toPx()) }
            .systemBarsPadding()
            .padding(horizontal = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            topBar()
            content()
        }
    }
}
```

### PaperCard

`base surface`

Surface dasar untuk kartu, banner, dan dialog.

```
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    shape: Shape = FoldballShapes.Card,
    tilt: Float = 0f,
    ruled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) = Box(
    modifier
        .rotate(tilt)
        .softShadow(shape)
        .clip(shape)
        .background(Paper)
        .then(if (ruled) Modifier.drawBehind { drawRuledLines(PaperLine, 22.dp.toPx()) } else Modifier),
    content = content
)
```

### PaperChip

`icon button`

```
@Composable
fun PaperChip(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = PressableSurface(
    onClick = onClick,
    shape = FoldballShapes.Chip,
    modifier = modifier.size(48.dp)
) {
    Icon(icon, contentDescription, Modifier.size(28.dp).align(Alignment.Center), tint = Ink)
}
```

### PaperButton

`primary / secondary / danger / disabled`

```
enum class PaperButtonStyle(val container: Color, val content: Color) {
    Primary(InkBlue, Color.White),
    Secondary(Paper, Ink),
    Danger(TeamRed, Color.White)
}

@Composable
fun PaperButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PaperButtonStyle = PaperButtonStyle.Primary,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) = PressableSurface(
    onClick = onClick,
    enabled = enabled,
    shape = FoldballShapes.Button,
    color = if (enabled) style.container else Color(0xFFB9B2A2),
    shadow = if (style == PaperButtonStyle.Primary) ShadowStyle.Hard else ShadowStyle.Soft,
    modifier = modifier.heightIn(min = 52.dp)
) {
    Row(Modifier.padding(horizontal = 26.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        leading?.invoke()
        Text(text, style = FoldballType.Title, color = if (enabled) style.content else Color(0xFF6D675B))
    }
}

// PressableSurface: scale 0.97 saat pressed, spring 120ms
@Composable
fun PressableSurface(onClick: () -> Unit, shape: Shape, modifier: Modifier = Modifier,
                     enabled: Boolean = true, color: Color = Paper,
                     shadow: ShadowStyle = ShadowStyle.Soft, content: @Composable BoxScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, tween(120))
    Box(
        modifier.scale(scale).shadowFor(shadow, shape).clip(shape).background(color)
            .clickable(src, null, enabled = enabled, onClick = onClick),
        content = content
    )
}
```

### TapeLabel

`penekanan`

Paper football gameNew

```
@Composable
fun TapeLabel(text: String, modifier: Modifier = Modifier, color: Color = Tape,
              contentColor: Color = Ink) =
    Text(
        text.uppercase(), style = FoldballType.Caption, color = contentColor,
        modifier = modifier.rotate(-1f).background(color).padding(horizontal = 14.dp, vertical = 2.dp)
    )
```

### MenuCard

`grid menu`

```
@Composable
fun MenuCard(
    title: String,
    tilt: Float,
    locked: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    art: @Composable RowScope.() -> Unit
) = PressableSurface(
    onClick = onClick, enabled = !locked, shape = FoldballShapes.Card,
    modifier = modifier.rotate(tilt).heightIn(min = 118.dp).semantics { if (locked) stateDescription = "Locked" }
) {
    Column(Modifier.padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
           horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.SpaceBetween) {
        Text(title.uppercase(), style = FoldballType.Label, color = Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = art)
    }
    if (locked) Box(Modifier.matchParentSize().background(Color(0x733C3732)))
}
```

### PaperToggle

`pengaturan`

```
@Composable
fun PaperToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) =
    Row(
        Modifier.toggleable(checked, role = Role.Switch, onValueChange = onChange).minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(Modifier.size(30.dp)) {
            drawRoundRect(Ink, style = Stroke(3.dp.toPx()), cornerRadius = CornerRadius(6.dp.toPx()))
            if (checked) drawCheckStroke(InkBlue, width = 3.4.dp.toPx())
        }
        Text(label, style = FoldballType.Label.copy(letterSpacing = 0.sp), color = Ink)
    }
```

### CirclePicker

`pilih tim / bola`

```
@Composable
fun CirclePickerItem(selected: Boolean, label: String, onClick: () -> Unit,
                     content: @Composable BoxScope.() -> Unit) =
    PressableSurface(onClick, FoldballShapes.Chip, Modifier.size(74.dp).semantics { this.selected = selected; contentDescription = label }) {
        content()
        if (selected) Canvas(Modifier.matchParentSize().rotate(-4f).padding(2.dp)) {
            drawOval(InkBlue, style = Stroke(3.dp.toPx()))   // lingkaran coretan tangan
        }
    }
```

### ScoreBoard

`layar match`

2BLUE

:

1RED

```
@Composable
fun ScoreBoard(blue: Int, red: Int, modifier: Modifier = Modifier) =
    PaperCard(modifier, shape = FoldballShapes.Chip, ruled = false) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ScoreDigit(blue, InkBlue, "BLUE")
            Text(":", style = FoldballType.Title, color = Ink)
            ScoreDigit(red, TeamRed, "RED")
        }
    }
```

### PaperDialog & InkToast

`konfirmasi, umpan balik`

#### Quit match?

Skor saat ini tidak akan disimpan.

Team saved

```
@Composable
fun PaperDialog(title: String, message: String, onDismiss: () -> Unit,
                confirmText: String, onConfirm: () -> Unit,
                dismissText: String = "STAY", danger: Boolean = false) =
    Dialog(onDismissRequest = onDismiss) {
        PaperCard(shape = FoldballShapes.Banner, tilt = .6f) {
            Column(Modifier.padding(22.dp, 26.dp), horizontalAlignment = Alignment.CenterHorizontally,
                   verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = FoldballType.Title, color = Ink)
                Text(message, style = FoldballType.Body, color = Color(0xFF444444), textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaperButton(dismissText, onDismiss, style = PaperButtonStyle.Secondary)
                    PaperButton(confirmText, onConfirm,
                        style = if (danger) PaperButtonStyle.Danger else PaperButtonStyle.Primary)
                }
            }
        }
    }

// Toast: SnackbarHost dengan container Ink, teks Paper, shape Chip, tilt -0.8f
```

## Theme setup

Token warna, tipografi, dan spacing dikumpulkan di satu tempat. Tema ini sengaja satu mode saja (tidak ada dark mode), karena meja kayu adalah identitas game.

```
// designsystem/Color.kt
val Wood      = Color(0xFF9A6A3C)
val WoodDark  = Color(0xFF6F4524)
val Paper     = Color(0xFFF6F1E4)
val PaperLine = Color(0xFFC9D4EA)
val Ink       = Color(0xFF1C1C22)
val InkBlue   = Color(0xFF1F4FC4)
val TeamRed   = Color(0xFFD64545)
val Tape      = Color(0xFFF4D34A)
val Grass     = Color(0xFF3F8F43)
val Cardboard = Color(0xFFC99A62)

// designsystem/Type.kt  (Google Fonts via androidx.compose.ui:ui-text-google-fonts)
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
val Marker = FontFamily(Font(GoogleFont("Permanent Marker"), provider))
val Hand   = FontFamily(Font(GoogleFont("Patrick Hand"), provider))

object FoldballType {
    val Display = TextStyle(fontFamily = Marker, fontSize = 58.sp, letterSpacing = (-1).sp)
    val Title   = TextStyle(fontFamily = Marker, fontSize = 26.sp)
    val Label   = TextStyle(fontFamily = Hand, fontSize = 20.sp, letterSpacing = 1.2.sp)
    val Body    = TextStyle(fontFamily = Hand, fontSize = 17.sp, lineHeight = 24.sp)
    val Caption = TextStyle(fontFamily = Hand, fontSize = 14.sp, letterSpacing = 1.1.sp)
}

// designsystem/Spacing.kt
object Space { val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp }

// designsystem/Theme.kt
@Composable
fun FoldballTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = InkBlue, onPrimary = Color.White,
            surface = Paper, onSurface = Ink, error = TeamRed, background = Wood),
        content = content
    )
}
```