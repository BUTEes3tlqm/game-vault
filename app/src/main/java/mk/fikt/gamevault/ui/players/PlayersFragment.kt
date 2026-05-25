package mk.fikt.gamevault.ui.players

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
import mk.fikt.gamevault.data.local.UserProfileEntity
import mk.fikt.gamevault.databinding.FragmentPlayersBinding

class PlayersFragment : Fragment() {

    private var _binding: FragmentPlayersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlayersViewModel by viewModels { PlayersViewModel.Factory }
    private lateinit var adapter: PlayerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PlayerAdapter(::onPlayerClick)
        binding.playersRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.playersRecycler.adapter = adapter

        binding.searchInput.doAfterTextChanged {
            viewModel.setQuery(it?.toString().orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.players.collect { list ->
                    adapter.submitList(list)
                    binding.emptyState.isVisible = list.isEmpty()
                }
            }
        }
    }

    private fun onPlayerClick(profile: UserProfileEntity) {
        findNavController().navigate(
            R.id.otherUserProfileFragment,
            bundleOf("uid" to profile.uid),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playersRecycler.adapter = null
        _binding = null
    }
}
