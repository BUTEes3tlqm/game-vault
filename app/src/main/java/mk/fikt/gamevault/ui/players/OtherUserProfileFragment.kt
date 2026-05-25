package mk.fikt.gamevault.ui.players

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.local.UserProfileEntity
import mk.fikt.gamevault.databinding.FragmentOtherUserProfileBinding
import mk.fikt.gamevault.ui.library.GameAdapter
import mk.fikt.gamevault.ui.reviews.ReviewAdapter
import java.util.Date

class OtherUserProfileFragment : Fragment() {

    private var _binding: FragmentOtherUserProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OtherUserProfileViewModel by viewModels { OtherUserProfileViewModel.Factory }
    private lateinit var gamesAdapter: GameAdapter
    private lateinit var reviewsAdapter: ReviewAdapter

    private val uid: String?
        get() = arguments?.getString(ARG_UID)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOtherUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.load(uid)

        gamesAdapter = GameAdapter(onClick = { /* read-only */ })
        binding.gamesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.gamesRecycler.adapter = gamesAdapter

        reviewsAdapter = ReviewAdapter()
        binding.reviewsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.reviewsRecycler.adapter = reviewsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profile.collect { renderHeader(it) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.games.collect { games ->
                    gamesAdapter.submitList(games)
                    binding.emptyText.isVisible = games.isEmpty()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reviews.collect { reviews ->
                    reviewsAdapter.submitList(reviews)
                    binding.reviewsEmptyText.isVisible = reviews.isEmpty()
                }
            }
        }
    }

    private fun renderHeader(profile: UserProfileEntity?) {
        if (profile == null) return
        binding.displayName.text = profile.displayName?.takeIf { it.isNotBlank() }
            ?: profile.email
            ?: getString(R.string.profile_anonymous)

        val dateStr = DateFormat.getDateFormat(requireContext()).format(Date(profile.joinedAt))
        binding.memberSince.text = getString(R.string.profile_member_since, dateStr)
        binding.gameCount.text = resources.getQuantityString(
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.gamesRecycler.adapter = null
        binding.reviewsRecycler.adapter = null
        _binding = null
    }

    companion object {
        const val ARG_UID = "uid"
    }
}
