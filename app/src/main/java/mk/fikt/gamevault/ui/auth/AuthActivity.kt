package mk.fikt.gamevault.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import mk.fikt.gamevault.databinding.ActivityAuthBinding
import mk.fikt.gamevault.di.AppContainer
import mk.fikt.gamevault.ui.main.MainActivity

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (AppContainer.authRepository.currentUser() != null) {
            goToMain()
            return
        }
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
