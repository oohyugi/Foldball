package com.yogi.foldball.gamekit.presentation.team
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.club_logo_01
import foldball.shared.generated.resources.club_logo_02
import foldball.shared.generated.resources.club_logo_03
import foldball.shared.generated.resources.club_logo_04
import foldball.shared.generated.resources.club_logo_05
import foldball.shared.generated.resources.club_logo_06
import foldball.shared.generated.resources.club_logo_07
import foldball.shared.generated.resources.club_logo_08
import foldball.shared.generated.resources.club_logo_09
import foldball.shared.generated.resources.club_logo_10
import foldball.shared.generated.resources.club_logo_11
import foldball.shared.generated.resources.club_logo_12
import foldball.shared.generated.resources.club_logo_13
import foldball.shared.generated.resources.club_logo_14
import foldball.shared.generated.resources.club_logo_15
import foldball.shared.generated.resources.club_logo_16
import foldball.shared.generated.resources.club_logo_17
import foldball.shared.generated.resources.club_logo_18
import foldball.shared.generated.resources.club_logo_19
import foldball.shared.generated.resources.club_logo_20
import foldball.shared.generated.resources.club_logo_21
import foldball.shared.generated.resources.club_logo_22
import foldball.shared.generated.resources.club_logo_23
import foldball.shared.generated.resources.club_logo_24
import foldball.shared.generated.resources.club_logo_25
import org.jetbrains.compose.resources.painterResource
@Composable fun ClubLogo(id: Int?, description: String, modifier: Modifier = Modifier) {
    val resource = when (id) {
        1 -> Res.drawable.club_logo_01
        2 -> Res.drawable.club_logo_02
        3 -> Res.drawable.club_logo_03
        4 -> Res.drawable.club_logo_04
        5 -> Res.drawable.club_logo_05
        6 -> Res.drawable.club_logo_06
        7 -> Res.drawable.club_logo_07
        8 -> Res.drawable.club_logo_08
        9 -> Res.drawable.club_logo_09
        10 -> Res.drawable.club_logo_10
        11 -> Res.drawable.club_logo_11
        12 -> Res.drawable.club_logo_12
        13 -> Res.drawable.club_logo_13
        14 -> Res.drawable.club_logo_14
        15 -> Res.drawable.club_logo_15
        16 -> Res.drawable.club_logo_16
        17 -> Res.drawable.club_logo_17
        18 -> Res.drawable.club_logo_18
        19 -> Res.drawable.club_logo_19
        20 -> Res.drawable.club_logo_20
        21 -> Res.drawable.club_logo_21
        22 -> Res.drawable.club_logo_22
        23 -> Res.drawable.club_logo_23
        24 -> Res.drawable.club_logo_24
        25 -> Res.drawable.club_logo_25
        else -> return
    }
    Image(painterResource(resource), contentDescription = description, modifier = modifier)
}
