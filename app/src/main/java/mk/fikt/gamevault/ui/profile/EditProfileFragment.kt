package mk.fikt.gamevault.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.local.UserProfileEntity
import mk.fikt.gamevault.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels { EditProfileViewModel.Factory }

    private var prefilled = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveButton.setOnClickListener {
            viewModel.save(
                displayName = binding.displayNameInput.text?.toString()?.trim(),
                photoUrl = binding.photoUrlInput.text?.toString()?.trim(),
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profile.collect { renderProfile(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveState.collect { state ->
                    when (state) {
                        EditProfileViewModel.SaveState.Saving -> {
                            binding.saveButton.isEnabled = false
                        }
                        EditProfileViewModel.SaveState.Saved -> {
                            Toast.makeText(requireContext(),
                                getString(R.string.edit_profile_saved),
                                Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                            findNavController().popBackStack()
                        }
                        EditProfileViewModel.SaveState.Error -> {
                            binding.saveButton.isEnabled = true
                            Toast.makeText(requireContext(),
                                getString(R.string.common_error_generic),
                                Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                        EditProfileViewModel.SaveState.Idle -> {
                            binding.saveButton.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun renderProfile(profile: UserProfileEntity?) {
        if (profile == null || prefilled) return
        prefilled = true
        binding.displayNameInput.setText(profile.displayName.orEmpty())
        binding.photoUrlInput.setText(profile.photoUrl.orEmpty())
        binding.emailText.text = profile.email.orEmpty()

        if (!profile.photoUrl.isNullOrBlank()) {
            binding.avatarPreview.load(profile.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_profile)
                error(R.drawable.ic_profile)
            }
        } else {
            binding.avatarPreview.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
