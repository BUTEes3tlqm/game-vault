package mk.fikt.gamevault.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import mk.fikt.gamevault.R
import mk.fikt.gamevault.databinding.ActivityMainBinding
import mk.fikt.gamevault.di.AppContainer
import mk.fikt.gamevault.ui.auth.AuthActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppContainer.authRepository.currentUser() == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets()

        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager
            .findFragmentById(binding.mainNavHost.id) as NavHostFragment
        navController = navHost.navController

        appBarConfig = AppBarConfiguration(
            setOf(
                R.id.libraryFragment,
                R.id.reviewsFragment,
                R.id.playersFragment,
                R.id.profileFragment,
            )
        )
        setupActionBarWithNavController(navController, appBarConfig)
        // Keep setupWithNavController only for the highlight-on-destination-change side effect;
        // we override the click handler below to always navigate fresh (no saved state restore).
        binding.bottomNav.setupWithNavController(navController)

        binding.bottomNav.setOnItemSelectedListener { item ->
            val startId = navController.graph.findStartDestination().id
            val opts = NavOptions.Builder()
                .setPopUpTo(startId, /* inclusive = */ false, /* saveState = */ false)
                .setLaunchSingleTop(true)
                .build()
            runCatching { navController.navigate(item.itemId, null, opts) }.isSuccess
        }
        binding.bottomNav.setOnItemReselectedListener { item ->
            val opts = NavOptions.Builder()
                .setPopUpTo(item.itemId, /* inclusive = */ false, /* saveState = */ false)
                .setLaunchSingleTop(true)
                .build()
            navController.navigate(item.itemId, null, opts)
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()

    private fun applyEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainNavHost) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(left = bars.left, right = bars.right)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(bottom = bars.bottom, left = bars.left, right = bars.right)
            insets
        }
    }
}
