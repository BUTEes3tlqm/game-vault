package mk.fikt.gamevault.ui.players

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.local.UserProfileEntity
import mk.fikt.gamevault.databinding.ItemPlayerBinding

class PlayerAdapter(
    private val onClick: (UserProfileEntity) -> Unit,
) : ListAdapter<UserProfileEntity, PlayerAdapter.PlayerViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlayerViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PlayerViewHolder(
        private val binding: ItemPlayerBinding,
        private val onClick: (UserProfileEntity) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: UserProfileEntity) {
            val ctx = binding.root.context
            binding.displayName.text = profile.displayName?.takeIf { it.isNotBlank() }
                ?: profile.email
                ?: ctx.getString(R.string.profile_anonymous)
            binding.gameCount.text = ctx.resources.getQuantityString(
                R.plurals.library_game_count, profile.gameCount, profile.gameCount,
            )

            if (!profile.photoUrl.isNullOrBlank()) {
                binding.avatar.load(profile.photoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            } else {
                binding.avatar.setImageResource(R.drawable.ic_profile)
            }

            binding.root.setOnClickListener { onClick(profile) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UserProfileEntity>() {
            override fun areItemsTheSame(old: UserProfileEntity, new: UserProfileEntity) =
                old.uid == new.uid
            override fun areContentsTheSame(old: UserProfileEntity, new: UserProfileEntity) =
                old == new
        }
    }
}
