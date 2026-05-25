package mk.fikt.gamevault.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.databinding.FragmentProfileBinding
import mk.fikt.gamevault.di.AppContainer
import mk.fikt.gamevault.ui.auth.AuthActivity
import mk.fikt.gamevault.util.requireAccount

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = AppContainer.authRepository.currentUser()
        val fallback = user?.displayName ?: user?.email ?: getString(R.string.profile_anonymous)
        binding.signedInAs.text = getString(R.string.profile_signed_in_as, fallback)

        val uid = user?.uid
        if (uid != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    AppContainer.userProfileRepository.observe(uid).collect { profile ->
                        val label = profile?.displayName?.takeIf { it.isNotBlank() }
                            ?: profile?.email
                            ?: fallback
                        binding.signedInAs.text = getString(R.string.profile_signed_in_as, label)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppContainer.gameRepository.observeCount().collect { count ->
                    binding.gameCount.text = getString(R.string.profile_games_count, count)
                }
            }
        }

        binding.editProfileButton.setOnClickListener {
            requireAccount {
                findNavController().navigate(R.id.action_profile_to_edit)
            }
        }
        binding.statsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_stats)
        }
        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }
        binding.signOutButton.setOnClickListener {
            AppContainer.authRepository.signOut()
            AppContainer.googleSignInHelper.signOut()
            startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
