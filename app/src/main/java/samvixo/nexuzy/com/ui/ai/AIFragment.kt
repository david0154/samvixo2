package samvixo.nexuzy.com.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import samvixo.nexuzy.com.ads.AdMobHelper
import samvixo.nexuzy.com.ai.DevilAiService
import samvixo.nexuzy.com.utils.AppConstants

/**
 * AIFragment — Devil AI chat interface + AdMob Banner Ad
 * Uses DevilAiService to communicate with https://aiapi.devilpvt.in
 */
class AIFragment : Fragment() {

    private lateinit var adContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // TODO: Inflate your actual AI layout
        // val binding = FragmentAiBinding.inflate(inflater, container, false)
        // adContainer = binding.adBannerContainer
        // return binding.root
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show AdMob banner ad on AI screen
        // AdMobHelper.showBannerAd(requireActivity(), adContainer, AppConstants.ADMOB_BANNER_AI)

        // Example: Send a message to Devil AI
        // sendMessageToAI("Hello Devil AI!")
    }

    private fun sendMessageToAI(prompt: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val response = DevilAiService.chat(prompt)
            if (!response.isError) {
                // TODO: Display response.response in your chat UI
            } else {
                // TODO: Show error message to user
            }
        }
    }
}
