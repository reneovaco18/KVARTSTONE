package com.rench.kvartstone.ui.anim

import android.animation.*
import android.graphics.Path
import android.view.View
import android.view.ViewGroup

fun animateSpell(from : View,
                 to   : View,
                 root : ViewGroup,
                 onEnd: () -> Unit) {

    val bolt = View(from.context).apply {
        layoutParams = ViewGroup.LayoutParams(26, 26)
        setBackgroundResource(android.R.drawable.presence_online)
    }
    root.addView(bolt)

    val s = IntArray(2).also { from.getLocationOnScreen(it) }
    val e = IntArray(2).also { to  .getLocationOnScreen(it) }

    val path = Path().apply {
        moveTo(s[0].toFloat(), s[1].toFloat())
        lineTo(e[0].toFloat(), e[1].toFloat())
    }

    ObjectAnimator.ofFloat(bolt, View.X, View.Y, path).apply {
        duration = 300
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: Animator) {
                root.removeView(bolt)
                onEnd()
            }
        })
        start()
    }
}
