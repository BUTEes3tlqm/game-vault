package mk.fikt.gamevault.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import mk.fikt.gamevault.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.emailInput.doAfterTextChanged { binding.emailLayout.error = null }
        binding.passwordInput.doAfterTextChanged { binding.passwordLayout.error = null }
        binding.confirmPasswordInput.doAfterTextChanged { binding.confirmPasswordLayout.error = null }

        binding.signUpButton.setOnClickListener {
            val name = binding.displayNameInput.text?.toString().orEmpty()
            val email = binding.emailInput.text?.toString().orEmpty()
            val pwd = binding.passwordInput.text?.toString().orEmpty()
            val confirm = binding.confirmPasswordInput.text?.toString().orEmpty()
            if (!validate(email, pwd, confirm)) return@setOnClickListener
            viewModel.signUp(email, pwd, name)
        }
        binding.goToLoginButton.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.registerProgress.isVisible = state is AuthViewModel.UiState.Loading
                    binding.signUpButton.isEnabled = state !is AuthViewModel.UiState.Loading
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

    private fun validate(email: String, pwd: String, confirm: String): Boolean {
        var ok = true
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.auth_error_invalid_email)
            ok = false
        }
        if (pwd.length < 6) {
            binding.passwordLayout.error = getString(R.string.auth_error_weak_password)
            ok = false
        }
        if (pwd != confirm) {
            binding.confirmPasswordLayout.error = getString(R.string.auth_error_passwords_mismatch)
            ok = false
        }
        return ok
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
