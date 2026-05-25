package mk.fikt.gamevault.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import mk.fikt.gamevault.R
import mk.fikt.gamevault.databinding.FragmentDeleteAccountBinding
import mk.fikt.gamevault.di.AppContainer
import mk.fikt.gamevault.ui.auth.AuthActivity

class DeleteAccountFragment : Fragment() {

    private var _binding: FragmentDeleteAccountBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DeleteAccountViewModel by viewModels { DeleteAccountViewModel.Factory }

    private val googleReauthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val idToken = AppContainer.googleSignInHelper.parseIdToken(result.data)
        if (idToken != null) {
            viewModel.submitGoogleIdToken(idToken)
        } else {
            viewModel.reauthCancelled()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDeleteAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.userEmail.text = viewModel.userEmail.orEmpty()
        binding.userEmail.isVisible = !viewModel.userEmail.isNullOrBlank()

        val confirmWord = getString(R.string.delete_account_confirm_word)
        binding.confirmLayout.hint = getString(R.string.delete_account_type_hint_format, confirmWord)

        binding.confirmInput.doAfterTextChanged { editable ->
            binding.deleteButton.isEnabled = editable?.toString() == confirmWord
        }
        binding.deleteButton.setOnClickListener {
            showWarningDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { renderState(it) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        DeleteAccountViewModel.Event.Done -> goToAuth()
                    }
                }
            }
        }
    }

    private fun showWarningDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_account_warning_title)
            .setMessage(R.string.delete_account_warning_message)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.delete_account_warning_continue) { _, _ ->
                viewModel.deleteAccount()
            }
            .show()
    }

    private fun renderState(state: DeleteAccountViewModel.State) {
        val deleting = state is DeleteAccountViewModel.State.Deleting
        binding.progress.isVisible = deleting
        binding.deleteButton.isEnabled = !deleting &&
            binding.confirmInput.text?.toString() == getString(R.string.delete_account_confirm_word)
        binding.confirmInput.isEnabled = !deleting

        when (state) {
            is DeleteAccountViewModel.State.RequiresReauth -> when (state.kind) {
                DeleteAccountViewModel.ReauthKind.EMAIL_PASSWORD -> showPasswordDialog()
                DeleteAccountViewModel.ReauthKind.GOOGLE -> launchGoogleReauth()
            }
            is DeleteAccountViewModel.State.Error -> {
                Toast.makeText(requireContext(), R.string.delete_account_error, Toast.LENGTH_LONG).show()
                viewModel.dismissError()
            }
            else -> Unit
        }
    }

    private fun showPasswordDialog() {
        val ctx = requireContext()
        val layout = TextInputLayout(ctx).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            hint = getString(R.string.auth_password_hint)
        }
        val input = TextInputEditText(layout.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(input)
        val padding = (24 * resources.displayMetrics.density).toInt()
        val wrapper = LinearLayout(ctx).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(layout, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
        }
        val dialog: AlertDialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.delete_account_reauth_password_title)
            .setMessage(R.string.delete_account_reauth_password_message)
            .setView(wrapper)
            .setNegativeButton(R.string.common_cancel) { _, _ -> viewModel.reauthCancelled() }
            .setPositiveButton(R.string.common_confirm, null)
            .setOnCancelListener { viewModel.reauthCancelled() }
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pw = (input as EditText).text?.toString().orEmpty()
            if (pw.isNotBlank()) {
                viewModel.submitPassword(pw)
                dialog.dismiss()
            }
        }
    }

    private fun launchGoogleReauth() {
        val intent = AppContainer.googleSignInHelper.signInIntent()
        if (intent == null) {
            Toast.makeText(requireContext(), R.string.delete_account_error, Toast.LENGTH_LONG).show()
            viewModel.reauthCancelled()
            return
        }
        googleReauthLauncher.launch(intent)
    }

    private fun goToAuth() {
        AppContainer.authRepository.signOut()
        runCatching { AppContainer.googleSignInHelper.signOut() }
        val intent = Intent(requireContext(), AuthActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
