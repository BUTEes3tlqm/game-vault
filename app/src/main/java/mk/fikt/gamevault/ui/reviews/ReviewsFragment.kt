package mk.fikt.gamevault.ui.reviews

import android.os.Bundle
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
import kotlinx.coroutines.launch
import mk.fikt.gamevault.databinding.FragmentReviewsBinding

class ReviewsFragment : Fragment() {

    private var _binding: FragmentReviewsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReviewsViewModel by viewModels { ReviewsViewModel.Factory }
    private lateinit var adapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ReviewAdapter()
        binding.reviewsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.reviewsRecycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
        binding.writeReviewFab.setOnClickListener {
            WriteReviewBottomSheet.newInstance().show(childFragmentManager, "write_review")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reviews.collect { list ->
                    adapter.submitList(list)
                    binding.emptyState.isVisible = list.isEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.reviewsRecycler.adapter = null
        _binding = null
    }
}
