package mk.fikt.gamevault.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.model.GameStatus
import mk.fikt.gamevault.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels { StatisticsViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        observe()
    }

    private fun setupCharts() {
        binding.statusPie.description.isEnabled = false
        binding.statusPie.setUsePercentValues(true)
        binding.statusPie.setEntryLabelColor(textColor())
        binding.statusPie.setHoleColor(android.graphics.Color.TRANSPARENT)
        binding.statusPie.legend.textColor = textColor()

        binding.genreBar.description.isEnabled = false
        binding.genreBar.setFitBars(true)
        binding.genreBar.axisLeft.textColor = textColor()
        binding.genreBar.axisRight.isEnabled = false
        binding.genreBar.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.genreBar.xAxis.textColor = textColor()
        binding.genreBar.xAxis.granularity = 1f
        binding.genreBar.legend.textColor = textColor()
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.emptyState.isVisible = state.isEmpty
                    binding.statsContent.isVisible = !state.isEmpty

                    binding.cardTotal.value.text = state.totalGames.toString()
                    binding.cardTotal.label.text = getString(R.string.stats_total_games)

                    binding.cardCompleted.value.text = state.completed.toString()
                    binding.cardCompleted.label.text = getString(R.string.stats_completed)

                    binding.cardHours.value.text = "%.0f".format(state.totalHours)
                    binding.cardHours.label.text = getString(R.string.stats_hours_played)

                    if (!state.isEmpty) {
                        renderPie(state.statusCounts)
                        renderBar(state.topGenres)
                    }
                }
            }
        }
    }

    private fun renderPie(counts: Map<GameStatus, Int>) {
        val entries = counts.entries
            .filter { it.value > 0 }
            .map { (status, count) -> PieEntry(count.toFloat(), getString(status.labelRes)) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                statusColor(R.color.gv_status_playing),
                statusColor(R.color.gv_status_completed),
                statusColor(R.color.gv_status_backlog),
                statusColor(R.color.gv_status_dropped),
                statusColor(R.color.gv_status_wishlist),
            )
            valueTextColor = android.graphics.Color.WHITE
            valueTextSize = 12f
        }
        binding.statusPie.data = PieData(dataSet)
        binding.statusPie.invalidate()
    }

    private fun renderBar(genres: List<mk.fikt.gamevault.data.local.GenreCountRow>) {
        if (genres.isEmpty()) {
            binding.genreBar.clear()
            return
        }
        val entries = genres.mapIndexed { i, g -> BarEntry(i.toFloat(), g.count.toFloat()) }
        val labels = genres.map { it.genre }
        val dataSet = BarDataSet(entries, getString(R.string.stats_by_genre)).apply {
            color = ContextCompat.getColor(requireContext(), R.color.gv_neon_cyan)
            valueTextColor = textColor()
            valueTextSize = 11f
        }
        binding.genreBar.data = BarData(dataSet)
        binding.genreBar.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.genreBar.xAxis.labelCount = labels.size
        binding.genreBar.invalidate()
    }

    private fun textColor(): Int =
        com.google.android.material.color.MaterialColors.getColor(
            binding.root, com.google.android.material.R.attr.colorOnSurface
        )

    private fun statusColor(res: Int) = ContextCompat.getColor(requireContext(), res)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
