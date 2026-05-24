package mk.fikt.gamevault.data.model

import androidx.annotation.StringRes
import mk.fikt.gamevault.R

enum class GamePlatform(@StringRes val labelRes: Int) {
    PC(R.string.platform_pc),
    PLAYSTATION(R.string.platform_playstation),
    XBOX(R.string.platform_xbox),
    SWITCH(R.string.platform_switch),
    MOBILE(R.string.platform_mobile),
    OTHER(R.string.platform_other);

    companion object {
        fun fromName(value: String?): GamePlatform =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}
