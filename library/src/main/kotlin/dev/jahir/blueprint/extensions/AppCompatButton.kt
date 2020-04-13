package dev.jahir.blueprint.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.appcompat.widget.AppCompatButton
import dev.jahir.frames.extensions.views.gone
import dev.jahir.frames.extensions.views.isVisible
import dev.jahir.frames.extensions.views.visible

internal fun AppCompatButton.animateVisibility(show: Boolean) {
    if (isVisible == show) return
    post {
        scaleX = if (show) 0F else 1F
        scaleY = if (show) 0F else 1F
        alpha = if (show) 0F else 1F
        if (show) visible()
        animate()
            .scaleX(if (show) 1F else 0F)
            .scaleY(if (show) 1F else 0F)
            .alpha(if (show) 1F else 0F)
            .setStartDelay(50)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    if (!show) gone()
                }
            })
            .start()
    }
}