package mk.fikt.gamevault.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.local.GameEntity
import mk.fikt.gamevault.databinding.ItemGameBinding

class GameAdapter(
    private val onClick: (GameEntity) -> Unit,
) : ListAdapter<GameEntity, GameAdapter.GameViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemGameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GameViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GameViewHolder(
        private val binding: ItemGameBinding,
        private val onClick: (GameEntity) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(game: GameEntity) {
            val ctx = binding.root.context
            binding.title.text = game.title
            binding.platform.text = ctx.getString(game.platform.labelRes)
            binding.statusChip.text = ctx.getString(game.status.labelRes)
            binding.statusChip.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(statusColor(ctx, game))
            binding.ratingText.text =
                if (game.personalRating > 0f) ctx.getString(R.string.rating_format, "%.1f".format(game.personalRating))
                else ""
            binding.genre.text = game.genre.orEmpty()
            binding.genre.visibility = if (game.genre.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE

            if (!game.coverUri.isNullOrBlank()) {
                binding.cover.load(android.net.Uri.parse(game.coverUri)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_logo)
                    error(R.drawable.ic_logo)
                }
            } else {
                binding.cover.setImageResource(R.drawable.ic_logo)
            }

            binding.root.setOnClickListener { onClick(game) }
        }

        private fun statusColor(ctx: android.content.Context, game: GameEntity): Int {
            val res = when (game.status) {
                mk.fikt.gamevault.data.model.GameStatus.PLAYING -> R.color.gv_status_playing
                mk.fikt.gamevault.data.model.GameStatus.COMPLETED -> R.color.gv_status_completed
                mk.fikt.gamevault.data.model.GameStatus.BACKLOG -> R.color.gv_status_backlog
                mk.fikt.gamevault.data.model.GameStatus.DROPPED -> R.color.gv_status_dropped
                mk.fikt.gamevault.data.model.GameStatus.WISHLIST -> R.color.gv_status_wishlist
            }
            return androidx.core.content.ContextCompat.getColor(ctx, res)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GameEntity>() {
            override fun areItemsTheSame(old: GameEntity, new: GameEntity) = old.id == new.id
            override fun areContentsTheSame(old: GameEntity, new: GameEntity) = old == new
        }
    }
}
