package mk.fikt.gamevault.ui.details

import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.local.GameEntity
import mk.fikt.gamevault.data.local.ReviewEntity
import mk.fikt.gamevault.data.model.GameStatus
import mk.fikt.gamevault.databinding.FragmentGameDetailsBinding
import mk.fikt.gamevault.databinding.ItemReviewBinding
import mk.fikt.gamevault.ui.reviews.WriteReviewBottomSheet
import mk.fikt.gamevault.util.requireAccount
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GameDetailsFragment : Fragment() {

    private var _binding: FragmentGameDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GameDetailsViewModel by viewModels { GameDetailsViewModel.Factory }

    private val gameId: String?
        get() = arguments?.getString(ARG_GAME_ID)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentGameDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.load(gameId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.game.collect { renderGame(it) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reviewsForGame.collect { renderReviews(it) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { e ->
                    when (e) {
                        GameDetailsViewModel.Event.Deleted -> findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun renderGame(game: GameEntity?) {
        if (game == null) return
        val ctx = requireContext()

        binding.title.text = game.title
        binding.statusChip.text = ctx.getString(game.status.labelRes)
        binding.statusChip.chipBackgroundColor =
            android.content.res.ColorStateList.valueOf(statusColor(game.status))
        binding.platform.text = ctx.getString(game.platform.labelRes)
        binding.year.text = game.releaseYear?.let { "· $it" }.orEmpty()
        binding.genre.text = game.genre?.let { "· $it" }.orEmpty()
        binding.year.isVisible = game.releaseYear != null
        binding.genre.isVisible = !game.genre.isNullOrBlank()

        binding.ratingValue.text = getString(R.string.rating_format, "%.1f".format(game.personalRating))
        binding.hoursValue.text = getString(R.string.hours_format, "%.0f".format(game.hoursPlayed))
        binding.progressValue.text = getString(R.string.progress_format, game.progressPercent)
        binding.progressBar.setProgressCompat(game.progressPercent, true)

        binding.notesCard.isVisible = !game.notes.isNullOrBlank()
        binding.notesText.text = game.notes.orEmpty()

        if (!game.coverUri.isNullOrBlank()) {
            binding.cover.load(Uri.parse(game.coverUri)) {
                crossfade(true)
                placeholder(R.drawable.ic_logo)
                error(R.drawable.ic_logo)
            }
        } else {
            binding.cover.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_logo, requireContext().theme)
            )
        }

        binding.editButton.setOnClickListener {
            requireAccount {
                findNavController().navigate(
                    R.id.addEditGameFragment,
                    bundleOf("gameId" to game.id),
                )
            }
        }
        binding.writeReviewButton.isVisible = game.status.isReviewable
        binding.writeReviewButton.setOnClickListener {
            requireAccount {
                WriteReviewBottomSheet.newInstance(gameTitle = game.title, gameId = game.id)
                    .show(parentFragmentManager, "write_review")
            }
        }
        binding.deleteButton.setOnClickListener {
            requireAccount {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_confirm_title)
                    .setMessage(R.string.delete_confirm_message)
                    .setNegativeButton(R.string.common_cancel, null)
                    .setPositiveButton(R.string.common_delete) { _, _ -> viewModel.delete() }
                    .show()
            }
        }
    }

    private fun renderReviews(reviews: List<ReviewEntity>) {
        binding.reviewsContainer.removeAllViews()
        binding.reviewsEmpty.isVisible = reviews.isEmpty()
        if (reviews.isEmpty()) return
        val inflater = LayoutInflater.from(requireContext())
        reviews.take(10).forEach { r ->
            val itemBinding = ItemReviewBinding.inflate(inflater, binding.reviewsContainer, false)
            itemBinding.gameTitle.text = r.authorName
            itemBinding.ratingText.text = getString(R.string.rating_format, "%.1f".format(r.rating))
            val ago = DateUtils.getRelativeTimeSpanString(
                r.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
            itemBinding.author.text = ago
            itemBinding.text.text = r.text
            binding.reviewsContainer.addView(itemBinding.root)
        }
    }

    private fun statusColor(status: GameStatus): Int {
        val res = when (status) {
            GameStatus.PLAYING -> R.color.gv_status_playing
            GameStatus.COMPLETED -> R.color.gv_status_completed
            GameStatus.BACKLOG -> R.color.gv_status_backlog
            GameStatus.DROPPED -> R.color.gv_status_dropped
            GameStatus.WISHLIST -> R.color.gv_status_wishlist
        }
        return androidx.core.content.ContextCompat.getColor(requireContext(), res)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_GAME_ID = "gameId"
        fun newInstance(gameId: String) = GameDetailsFragment().apply {
            arguments = bundleOf(ARG_GAME_ID to gameId)
        }
    }
}
