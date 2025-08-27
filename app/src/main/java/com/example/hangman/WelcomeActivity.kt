package com.example.hangman

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crear layout principal con fondo degradado y orientación vertical centrada
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0B1120"), Color.parseColor("#1E293B"))
            )
        }

        // Imagen del logo con tamaño fijo y margen inferior
        val logoImage = ImageView(this).apply {
            setImageResource(R.drawable.logo_hangman)
            layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                bottomMargin = 24
            }
        }

        // Contenedor horizontal para las letras animadas del texto
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val texto = "¡Bienvenido, biped!"
        val letras = mutableListOf<TextView>()

        // Crear TextViews para cada letra con animación de escala y aparición con retraso
        for ((i, char) in texto.withIndex()) {
            val letraView = TextView(this).apply {
                text = char.toString()
                setTextColor(Color.WHITE)
                textSize = 26f
                setTypeface(null, Typeface.BOLD)
                alpha = 0f
            }

            val delay = i * 80L
            Handler().postDelayed({
                val scale = ScaleAnimation(
                    0.5f, 1f, 0.5f, 1f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                ).apply {
                    duration = 300
                    fillAfter = true
                }

                letraView.startAnimation(scale)
                letraView.alpha = 1f
            }, delay)

            letras.add(letraView)
            textContainer.addView(letraView)
        }

        // Añadir logo y texto animado al layout principal
        layout.addView(logoImage)
        layout.addView(textContainer)
        setContentView(layout)

        // Animación de escala para el logo al inicio
        val scaleLogo = ScaleAnimation(
            3f, 1f, 3f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            fillAfter = true
        }
        logoImage.startAnimation(scaleLogo)

        // Después de la animación, realizar fade-out y redirigir a la siguiente actividad
        Handler().postDelayed({
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 800
                fillAfter = true
            }

            logoImage.startAnimation(fadeOut)
            textContainer.startAnimation(fadeOut)

            Handler().postDelayed({
                startActivity(Intent(this, NavbarBottomActivity::class.java))
                finish()
            }, 800)
        }, 2800)
    }
}
