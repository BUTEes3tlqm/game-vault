package mk.fikt.gamevault

import android.app.Application
import mk.fikt.gamevault.data.messaging.GameVaultMessagingService
import mk.fikt.gamevault.data.messaging.PushSubscriptions
import mk.fikt.gamevault.di.AppContainer

class GameVaultApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        AppContainer.prefs.applyOnStartup()
        GameVaultMessagingService.ensureChannel(this)
        if (AppContainer.firebaseAvailable) {
            PushSubscriptions.subscribeToBroadcast()
        }
    }
}

