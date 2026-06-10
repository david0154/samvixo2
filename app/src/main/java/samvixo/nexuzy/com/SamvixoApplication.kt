package samvixo.nexuzy.com

import android.app.Application
import com.google.android.gms.ads.MobileAds
import samvixo.nexuzy.com.ads.AdMobHelper

/**
 * SamvixoApplication — Application class
 * Initializes AdMob and other global SDKs on startup
 */
class SamvixoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize AdMob
        AdMobHelper.initialize(this)

        // Preload Interstitial Ad
        AdMobHelper.loadInterstitialAd(this)
    }
}
