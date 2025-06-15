package com.rench.kvartstone.ui.Splash

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rench.kvartstone.R

class SplashFragment : Fragment(R.layout.fragment_splash) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val logo = view.findViewById<ImageView>(R.id.logoImage)


        logo.alpha = 0f
        logo.animate().alpha(1f).setDuration(1200).withEndAction {

            findNavController().navigate(R.id.action_splashFragment_to_mainMenuFragment)
        }
    }
}
