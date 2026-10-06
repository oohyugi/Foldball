package com.yogi.foldball.domain.team

object ClubBadges {
    val ids: List<Int> = (1..16).toList()
}

@ConsistentCopyVisibility
data class TeamProfile private constructor(
    val name: String,
    val shortName: String,
    val badgeId: Int
) {
    init {
        require(name.isNotBlank() && name == name.trim() && name.length <= MAX_NAME_LENGTH)
        require(
            shortName.isNotBlank() && shortName.length <= MAX_SHORT_NAME_LENGTH &&
                shortName.all(Char::isLetterOrDigit)
        )
        require(badgeId in ClubBadges.ids)
    }

    fun toMatchIdentity() = TeamIdentity(name, shortName, badgeId)

    companion object {
        const val MAX_NAME_LENGTH = 20
        const val MAX_SHORT_NAME_LENGTH = 4

        fun create(name: String, badgeId: Int, shortName: String? = null): TeamProfile? {
            val cleanName = name.trim()
            if (cleanName.isEmpty() || cleanName.length > MAX_NAME_LENGTH ||
                badgeId !in ClubBadges.ids
            ) {
                return null
            }
            val abbreviation = shortName?.trim()?.uppercase()
                ?: cleanName.filter(
                    Char::isLetterOrDigit
                ).uppercase().take(MAX_SHORT_NAME_LENGTH).ifEmpty {
                    "CLUB"
                }
            if (abbreviation.isEmpty() || abbreviation.length > MAX_SHORT_NAME_LENGTH ||
                !abbreviation.all(Char::isLetterOrDigit)
            ) {
                return null
            }
            return TeamProfile(cleanName, abbreviation, badgeId)
        }
    }
}
