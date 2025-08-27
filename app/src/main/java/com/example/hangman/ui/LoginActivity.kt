package com.example.hangman.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import com.example.hangman.R
import com.example.hangman.WelcomeActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Referencias a los campos y botones del layout
        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val passwordInput = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registroText = findViewById<TextView>(R.id.registroText)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        var modalView: View? = null

        // Navegación a pantalla de registro
        registroText.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        // Manejo de login y animación modal éxito
        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val pass = passwordInput.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                // Mostrar modal animado solo si no está creado aún
                if (modalView == null) {
                    modalView = layoutInflater.inflate(R.layout.dialog_login_success, rootView, false)
                    rootView.addView(modalView)
                }
                modalView?.visibility = View.VISIBLE
                modalView?.alpha = 0f
                modalView?.animate()?.alpha(1f)?.setDuration(500)?.start()

                // Reproducir animación Lottie
                val lottieView = modalView?.findViewById<LottieAnimationView>(R.id.loadingLottie)
                lottieView?.playAnimation()

                // Después de 2 segundos redirigir a WelcomeActivity y cerrar login
                modalView?.postDelayed({
                    startActivity(Intent(this, WelcomeActivity::class.java))
                    finish()
                }, 2000)
            }
        }
    }
}
