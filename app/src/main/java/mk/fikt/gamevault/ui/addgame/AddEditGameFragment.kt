package mk.fikt.gamevault.ui.addgame

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.model.GamePlatform
import mk.fikt.gamevault.data.model.GameStatus
import mk.fikt.gamevault.databinding.FragmentAddEditGameBinding
import mk.fikt.gamevault.util.CoverFileProvider

class AddEditGameFragment : Fragment() {

    private var _binding: FragmentAddEditGameBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddEditGameViewModel by viewModels { AddEditGameViewModel.Factory }

    private var pendingCoverUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCoverUri?.let { viewModel.updateCoverUri(it.toString()) }
        } else {
            pendingCoverUri = null
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(requireContext(), R.string.permission_camera_denied, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddEditGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val incomingId = arguments?.getString("gameId")
        viewModel.load(incomingId)

        setupPlatformDropdown()
        wireInputs()
        wireStatusChips()
        wireButtons()
        observeForm()
        observeEvents()
    }

    private fun setupPlatformDropdown() {
        val labels = GamePlatform.entries.map { getString(it.labelRes) }
        binding.platformInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        )
        binding.platformInput.setOnItemClickListener { _, _, pos, _ ->
            viewModel.updatePlatform(GamePlatform.entries[pos])
        }
    }

    private fun wireInputs() {
        binding.titleInput.doAfterTextChanged {
            binding.titleLayout.error = null
            viewModel.updateTitle(it?.toString().orEmpty())
        }
        binding.yearInput.doAfterTextChanged {
            binding.yearLayout.error = null
            viewModel.updateYear(it?.toString().orEmpty())
        }
        binding.genreInput.doAfterTextChanged { viewModel.updateGenre(it?.toString().orEmpty()) }
        binding.hoursInput.doAfterTextChanged { viewModel.updateHours(it?.toString().orEmpty()) }
        binding.notesInput.doAfterTextChanged { viewModel.updateNotes(it?.toString().orEmpty()) }

        binding.ratingSlider.addOnChangeListener { _, value, _ ->
            viewModel.updateRating(value)
            binding.ratingLabel.text = getString(R.string.field_rating) +
                "  " + getString(R.string.rating_format, "%.1f".format(value))
        }
        binding.progressSlider.addOnChangeListener { _, value, _ ->
            val v = value.toInt()
            viewModel.updateProgress(v)
            binding.progressLabel.text = getString(R.string.field_progress) +
                "  " + getString(R.string.progress_format, v)
        }
    }

    private fun wireStatusChips() {
        binding.statusChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val s = when (checkedIds.firstOrNull()) {
                R.id.statusChipPlaying -> GameStatus.PLAYING
                R.id.statusChipCompleted -> GameStatus.COMPLETED
                R.id.statusChipBacklog -> GameStatus.BACKLOG
                R.id.statusChipDropped -> GameStatus.DROPPED
                R.id.statusChipWishlist -> GameStatus.WISHLIST
                else -> GameStatus.BACKLOG
            }
            viewModel.updateStatus(s)
        }
    }

    private fun wireButtons() {
        binding.takePhotoButton.setOnClickListener { ensureCameraPermissionThenLaunch() }
        binding.removeCoverButton.setOnClickListener { viewModel.updateCoverUri(null) }
        binding.saveButton.setOnClickListener { viewModel.save() }
        binding.deleteButton.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_delete) { _, _ -> viewModel.delete() }
                .show()
        }
    }

    private fun ensureCameraPermissionThenLaunch() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val file = CoverFileProvider.newCoverFile(requireContext())
        val uri = CoverFileProvider.uriFor(requireContext(), file)
        pendingCoverUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun observeForm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.form.collect { f ->
                    if (binding.titleInput.text?.toString() != f.title) {
                        binding.titleInput.setText(f.title)
                    }
                    if (binding.yearInput.text?.toString() != f.releaseYear) {
                        binding.yearInput.setText(f.releaseYear)
                    }
                    if (binding.genreInput.text?.toString() != f.genre) {
                        binding.genreInput.setText(f.genre)
                    }
                    if (binding.hoursInput.text?.toString() != f.hoursPlayed) {
                        binding.hoursInput.setText(f.hoursPlayed)
                    }
                    if (binding.notesInput.text?.toString() != f.notes) {
                        binding.notesInput.setText(f.notes)
                    }
                    binding.platformInput.setText(getString(f.platform.labelRes), false)
                    if (binding.ratingSlider.value != f.personalRating) {
                        binding.ratingSlider.value = f.personalRating
                    }
                    if (binding.progressSlider.value.toInt() != f.progressPercent) {
                        binding.progressSlider.value = f.progressPercent.toFloat()
                    }
                    checkChipForStatus(f.status)

                    binding.coverPlaceholder.isVisible = f.coverUri.isNullOrBlank()
                    if (!f.coverUri.isNullOrBlank()) {
                        binding.coverImage.load(Uri.parse(f.coverUri)) {
                            crossfade(true)
                            placeholder(R.drawable.ic_logo)
                        }
                    } else {
                        binding.coverImage.setImageDrawable(null)
                    }

                    binding.deleteButton.isVisible = f.id != null
                }
            }
        }
    }

    private fun checkChipForStatus(s: GameStatus) {
        val id = when (s) {
            GameStatus.PLAYING -> R.id.statusChipPlaying
            GameStatus.COMPLETED -> R.id.statusChipCompleted
            GameStatus.BACKLOG -> R.id.statusChipBacklog
            GameStatus.DROPPED -> R.id.statusChipDropped
            GameStatus.WISHLIST -> R.id.statusChipWishlist
        }
        if (binding.statusChips.checkedChipId != id) binding.statusChips.check(id)
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { e ->
                    when (e) {
                        AddEditGameViewModel.Event.Saved,
                        AddEditGameViewModel.Event.Deleted -> {
                            viewModel.consumeEvent()
                            findNavController().popBackStack()
                        }
                        is AddEditGameViewModel.Event.ValidationFailed -> {
                            when (e.field) {
                                AddEditGameViewModel.Event.Field.TITLE ->
                                    binding.titleLayout.error = getString(R.string.validation_title_required)
                                AddEditGameViewModel.Event.Field.YEAR ->
                                    binding.yearLayout.error = getString(R.string.validation_year_invalid)
                            }
                            viewModel.consumeEvent()
                        }
                        null -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
