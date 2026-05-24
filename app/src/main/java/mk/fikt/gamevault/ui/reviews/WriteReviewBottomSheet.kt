package mk.fikt.gamevault.ui.reviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import mk.fikt.gamevault.R
import mk.fikt.gamevault.databinding.DialogWriteReviewBinding

class WriteReviewBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogWriteReviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReviewsViewModel by viewModels({ requireParentFragment() }) {
        ReviewsViewModel.Factory
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = DialogWriteReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefilledTitle = arguments?.getString(ARG_GAME_TITLE)
        if (!prefilledTitle.isNullOrBlank()) {
            binding.gameTitleInput.setText(prefilledTitle)
            binding.gameTitleInput.isEnabled = false
        }
        binding.ratingSlider.addOnChangeListener { _, value, _ ->
            binding.ratingLabel.text = getString(R.string.field_rating) +
                "  " + getString(R.string.rating_format, "%.1f".format(value))
        }
        binding.postButton.setOnClickListener {
            val title = binding.gameTitleInput.text?.toString().orEmpty()
            val text = binding.reviewTextInput.text?.toString().orEmpty()
            if (title.isBlank()) {
                binding.gameTitleLayout.error = getString(R.string.validation_title_required)
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
                gameId = arguments?.getString(ARG_GAME_ID),
            )
            Toast.makeText(requireContext(), R.string.review_posted, Toast.LENGTH_SHORT).show()
            dismiss()
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
