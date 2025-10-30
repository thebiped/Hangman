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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WelcomeActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout base con fondo degradado
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

        // Logo
        val logoImage = ImageView(this).apply {
            setImageResource(R.drawable.logo)
            layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                bottomMargin = 24
            }
        }

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        // Agrega vistas al layout
        layout.addView(logoImage)
        layout.addView(textContainer)
        setContentView(layout)

        // Animación inicial del logo
        val scaleLogo = ScaleAnimation(
            3f, 1f, 3f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            fillAfter = true
        }
        logoImage.startAnimation(scaleLogo)

        // 🔥 Cargar nombre del usuario desde Firestore
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("nombreUsuario") ?: "Jugador"
                    mostrarAnimacion(textContainer, logoImage, nombre)
                }
                .addOnFailureListener {
                    mostrarAnimacion(textContainer, logoImage, "Jugador")
                }
        } else {
            mostrarAnimacion(textContainer, logoImage, "Jugador")
        }
    }

    private fun mostrarAnimacion(textContainer: LinearLayout, logoImage: ImageView, nombre: String) {
        val texto = "¡Bienvenido, $nombre!"
        val letras = mutableListOf<TextView>()

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

        // Después de la animación → redirige al menú principal
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
