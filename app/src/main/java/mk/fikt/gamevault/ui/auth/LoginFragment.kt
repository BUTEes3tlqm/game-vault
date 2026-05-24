package mk.fikt.gamevault.ui.auth

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.auth.AuthOutcome
import mk.fikt.gamevault.databinding.FragmentLoginBinding
import mk.fikt.gamevault.di.AppContainer

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val idToken = AppContainer.googleSignInHelper.parseIdToken(result.data)
            if (idToken != null) {
                viewModel.signInWithGoogleIdToken(idToken)
            } else {
                Toast.makeText(requireContext(), R.string.auth_error_unknown, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wireInputs()
        wireButtons()
        observeState()
    }

    private fun wireInputs() {
        binding.emailInput.doAfterTextChanged { binding.emailLayout.error = null }
        binding.passwordInput.doAfterTextChanged { binding.passwordLayout.error = null }
    }

    private fun wireButtons() {
        binding.signInButton.setOnClickListener {
            val email = binding.emailInput.text?.toString().orEmpty()
            val password = binding.passwordInput.text?.toString().orEmpty()
            if (!validate(email, password)) return@setOnClickListener
            viewModel.signIn(email, password)
        }
        binding.googleButton.setOnClickListener {
            val intent = AppContainer.googleSignInHelper.signInIntent()
            if (intent != null) {
                googleSignInLauncher.launch(intent)
            } else {
                Toast.makeText(requireContext(), R.string.auth_error_unknown, Toast.LENGTH_SHORT).show()
            }
        }
        binding.facebookButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.auth_fb_not_enabled, Toast.LENGTH_LONG).show()
        }
        binding.anonymousButton.setOnClickListener {
            viewModel.signInAnonymously()
        }
        binding.goToRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun validate(email: String, password: String): Boolean {
        var ok = true
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.auth_error_invalid_email)
            ok = false
        }
        if (password.length < 6) {
            binding.passwordLayout.error = getString(R.string.auth_error_weak_password)
            ok = false
        }
        return ok
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.loginProgress.isVisible = state is AuthViewModel.UiState.Loading
                    setButtonsEnabled(state !is AuthViewModel.UiState.Loading)
                    when (state) {
                        is AuthViewModel.UiState.Success -> {
                            (requireActivity() as? AuthActivity)?.goToMain()
                        }
                        is AuthViewModel.UiState.Error -> {
                            val msgRes = when (val o = state.outcome) {
                                is AuthOutcome.Failure -> o.error.messageRes()
                                AuthOutcome.NotConfigured -> R.string.auth_error_unknown
                                else -> R.string.auth_error_unknown
                            }
                            Toast.makeText(requireContext(), msgRes, Toast.LENGTH_LONG).show()
                            viewModel.reset()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.signInButton.isEnabled = enabled
        binding.googleButton.isEnabled = enabled
        binding.facebookButton.isEnabled = enabled
        binding.anonymousButton.isEnabled = enabled
        binding.goToRegisterButton.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
