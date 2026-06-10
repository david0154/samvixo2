package samvixo.nexuzy.com.ui.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import samvixo.nexuzy.com.ads.AdMobHelper
import samvixo.nexuzy.com.utils.AppConstants

/**
 * StatusFragment — shows friend statuses + AdMob Banner Ad
 * AdMob Banner is shown at the bottom of the Status screen.
 */
class StatusFragment : Fragment() {

    private lateinit var adContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // TODO: Inflate your actual status layout
        // val binding = FragmentStatusBinding.inflate(inflater, container, false)
        // adContainer = binding.adBannerContainer
        // return binding.root
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Show AdMob banner ad at the bottom of status screen
        // Uncomment when adContainer is wired to your layout:
        // AdMobHelper.showBannerAd(
        //     requireActivity(),
        //     adContainer,
        //     AppConstants.ADMOB_BANNER_STATUS
        // )
    }
}
