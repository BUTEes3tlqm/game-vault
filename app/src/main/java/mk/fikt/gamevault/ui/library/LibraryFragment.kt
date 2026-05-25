package mk.fikt.gamevault.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.local.GameEntity
import mk.fikt.gamevault.data.model.GameStatus
import mk.fikt.gamevault.databinding.FragmentLibraryBinding
import mk.fikt.gamevault.ui.details.GameDetailsFragment
import mk.fikt.gamevault.util.requireAccount

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LibraryViewModel by viewModels { LibraryViewModel.Factory }
    private lateinit var adapter: GameAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = GameAdapter(::onGameClick)
        binding.gamesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.gamesRecycler.adapter = adapter

        binding.searchInput.doAfterTextChanged {
            viewModel.setQuery(it?.toString().orEmpty())
        }
        binding.filterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val status: GameStatus? = when (checkedIds.firstOrNull()) {
                R.id.chipPlaying -> GameStatus.PLAYING
                R.id.chipCompleted -> GameStatus.COMPLETED
                R.id.chipBacklog -> GameStatus.BACKLOG
                R.id.chipDropped -> GameStatus.DROPPED
                R.id.chipWishlist -> GameStatus.WISHLIST
                else -> null
            }
            viewModel.setStatusFilter(status)
        }
        binding.addFab.setOnClickListener {
            requireAccount {
                findNavController().navigate(R.id.action_library_to_addGame)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.games.collect { games ->
                    adapter.submitList(games)
                    binding.emptyState.isVisible = games.isEmpty()
                }
            }
        }
    }

    private val isTwoPane: Boolean
        get() = view?.findViewById<View>(R.id.detailContainer) != null

    private fun onGameClick(game: GameEntity) {
        if (isTwoPane) {
            childFragmentManager.beginTransaction()
                .replace(R.id.detailContainer, GameDetailsFragment.newInstance(game.id))
                .commit()
        } else {
            findNavController().navigate(
                R.id.action_library_to_details,
                bundleOf("gameId" to game.id),
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.gamesRecycler.adapter = null
        _binding = null
    }
}
