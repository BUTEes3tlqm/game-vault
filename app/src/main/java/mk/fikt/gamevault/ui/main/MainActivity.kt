package mk.fikt.gamevault.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import mk.fikt.gamevault.databinding.ActivityMainBinding
import mk.fikt.gamevault.di.AppContainer
import mk.fikt.gamevault.ui.auth.AuthActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppContainer.authRepository.currentUser() == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(binding.mainNavHost.id) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
    }
}
