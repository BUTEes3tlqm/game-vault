package mk.fikt.gamevault.ui.reviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.local.GameEntity
import mk.fikt.gamevault.databinding.DialogWriteReviewBinding

class WriteReviewBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogWriteReviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReviewsViewModel by viewModels({ requireParentFragment() }) {
        ReviewsViewModel.Factory
    }

    private var selectedGameId: String? = null
    private var selectedGameTitle: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = DialogWriteReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lockedGameId = arguments?.getString(ARG_GAME_ID)
        val lockedTitle = arguments?.getString(ARG_GAME_TITLE)
        val isLocked = !lockedGameId.isNullOrBlank() && !lockedTitle.isNullOrBlank()

        if (isLocked) {
            selectedGameId = lockedGameId
            selectedGameTitle = lockedTitle
            binding.gameTitleInput.setText(lockedTitle, false)
            binding.gameTitleLayout.isEnabled = false
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.myGames.collect { games -> bindGameDropdown(games) }
                }
            }
        }

        binding.ratingSlider.addOnChangeListener { _, value, _ ->
            binding.ratingLabel.text = getString(R.string.field_rating) +
                "  " + getString(R.string.rating_format, "%.1f".format(value))
        }

        binding.postButton.setOnClickListener {
            val title = selectedGameTitle.orEmpty()
            val text = binding.reviewTextInput.text?.toString().orEmpty()
            if (title.isBlank() || selectedGameId.isNullOrBlank()) {
                binding.gameTitleLayout.error = getString(R.string.review_pick_game_error)
                return@setOnClickListener
            }
            if (text.isBlank()) {
                binding.reviewTextLayout.error = getString(R.string.validation_title_required)
                return@setOnClickListener
            }
            viewModel.postReview(
                gameTitle = title,
                rating = binding.ratingSlider.value,
                text = text,
                gameId = selectedGameId,
            )
            Toast.makeText(requireContext(), R.string.review_posted, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun bindGameDropdown(games: List<GameEntity>) {
        binding.noGamesHint.isVisible = games.isEmpty()
        binding.postButton.isEnabled = games.isNotEmpty()
        if (games.isEmpty()) {
            binding.gameTitleInput.setAdapter(null)
            return
        }
        val titles = games.map { it.title }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, titles)
        binding.gameTitleInput.setAdapter(adapter)
        binding.gameTitleInput.setOnItemClickListener { _, _, position, _ ->
            val game = games[position]
            selectedGameId = game.id
            selectedGameTitle = game.title
            binding.gameTitleLayout.error = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_GAME_TITLE = "gameTitle"
        private const val ARG_GAME_ID = "gameId"

        fun newInstance(gameTitle: String? = null, gameId: String? = null) =
            WriteReviewBottomSheet().apply {
                arguments = bundleOf(ARG_GAME_TITLE to gameTitle, ARG_GAME_ID to gameId)
            }
    }
}
