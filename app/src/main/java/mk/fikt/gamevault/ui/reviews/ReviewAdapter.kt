package mk.fikt.gamevault.ui.reviews

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.local.ReviewEntity
import mk.fikt.gamevault.databinding.ItemReviewBinding

class ReviewAdapter : ListAdapter<ReviewEntity, ReviewAdapter.ReviewViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(r: ReviewEntity) {
            val ctx = binding.root.context
            binding.gameTitle.text = r.gameTitle
            binding.ratingText.text = ctx.getString(R.string.rating_format, "%.1f".format(r.rating))
            val byLine = ctx.getString(R.string.review_by, r.authorName)
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                r.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
            binding.author.text = "$byLine · $timeAgo"
            binding.text.text = r.text
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ReviewEntity>() {
            override fun areItemsTheSame(old: ReviewEntity, new: ReviewEntity) = old.id == new.id
            override fun areContentsTheSame(old: ReviewEntity, new: ReviewEntity) = old == new
        }
    }
}
