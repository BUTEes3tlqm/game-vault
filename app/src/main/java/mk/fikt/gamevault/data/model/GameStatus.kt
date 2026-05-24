package mk.fikt.gamevault.data.model

import androidx.annotation.StringRes
import mk.fikt.gamevault.R

enum class GameStatus(@StringRes val labelRes: Int) {
    PLAYING(R.string.status_playing),
    COMPLETED(R.string.status_completed),
    BACKLOG(R.string.status_backlog),
    DROPPED(R.string.status_dropped),
    WISHLIST(R.string.status_wishlist);

    companion object {
        fun fromName(value: String?): GameStatus =
            entries.firstOrNull { it.name == value } ?: BACKLOG
    }
}
