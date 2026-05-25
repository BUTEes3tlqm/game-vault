package mk.fikt.gamevault.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import mk.fikt.gamevault.BuildConfig
import mk.fikt.gamevault.R
import mk.fikt.gamevault.databinding.FragmentSettingsBinding
import mk.fikt.gamevault.di.AppContainer

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        binding.notificationsSwitch.isChecked = granted
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wireLanguage()
        wireTheme()
        wireNotifications()
        binding.versionText.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
        binding.deleteAccountButton.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_deleteAccount)
        }
    }

    private fun wireLanguage() {
        when (AppContainer.prefs.languageTag) {
            "en" -> binding.langEn.isChecked = true
            "mk" -> binding.langMk.isChecked = true
            else -> binding.langSystem.isChecked = true
        }
        binding.languageGroup.setOnCheckedChangeListener { _, checkedId ->
            val tag = when (checkedId) {
                R.id.langEn -> "en"
                R.id.langMk -> "mk"
                else -> ""
            }
            AppContainer.prefs.languageTag = tag
            AppContainer.analytics.logLanguageChanged(tag.ifBlank { "system" })
        }
    }

    private fun wireTheme() {
        when (AppContainer.prefs.themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.themeLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.themeDark.isChecked = true
            else -> binding.themeSystem.isChecked = true
        }
        binding.themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.themeLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.themeDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppContainer.prefs.themeMode = mode
            AppContainer.analytics.logThemeChanged(
                when (mode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> "light"
                    AppCompatDelegate.MODE_NIGHT_YES -> "dark"
                    else -> "system"
                }
            )
        }
    }

    private fun wireNotifications() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val granted = !needsPermission || ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        binding.notificationsSwitch.isChecked = granted
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && needsPermission && !granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
