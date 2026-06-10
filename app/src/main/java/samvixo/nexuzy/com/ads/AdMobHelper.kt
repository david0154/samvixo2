package samvixo.nexuzy.com.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import samvixo.nexuzy.com.utils.AppConstants

/**
 * AdMobHelper — Central AdMob management for Samvixo
 * Handles Banner Ads (Status, AI screens) and Interstitial Ads
 */
object AdMobHelper {

    private var interstitialAd: InterstitialAd? = null

    /**
     * Initialize AdMob SDK — call once in Application class
     */
    fun initialize(context: Context) {
        MobileAds.initialize(context) { initStatus ->
            val statusMap = initStatus.adapterStatusMap
            statusMap.forEach { (adapter, status) ->
                android.util.Log.d("AdMob", "Adapter: $adapter, Status: ${status.initializationState}")
            }
        }
    }

    /**
     * Load and show a Banner Ad in the given container
     * @param adUnitId Use AppConstants.ADMOB_BANNER_STATUS or ADMOB_BANNER_AI
     */
    fun showBannerAd(activity: Activity, container: LinearLayout, adUnitId: String = AppConstants.ADMOB_BANNER_STATUS) {
        val adView = AdView(activity).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
        }
        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    /**
     * Load an Interstitial Ad — preload before needed
     */
    fun loadInterstitialAd(context: Context) {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(context, AppConstants.ADMOB_INTERSTITIAL, request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    android.util.Log.e("AdMob", "Interstitial failed: ${error.message}")
                }
            })
    }

    /**
     * Show the preloaded Interstitial Ad
     */
    fun showInterstitialAd(activity: Activity, onDismissed: (() -> Unit)? = null) {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onDismissed?.invoke()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    onDismissed?.invoke()
                }
            }
            ad.show(activity)
        } ?: onDismissed?.invoke()
    }
}
