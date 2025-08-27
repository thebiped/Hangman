package com.example.hangman.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.hangman.R

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Referencias a los campos de entrada y botones
        val nombreInput = findViewById<EditText>(R.id.usernameEditText)
        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val passInput = findViewById<EditText>(R.id.passwordEditText)
        val confirmPassInput = findViewById<EditText>(R.id.confirmPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val goLogin = findViewById<TextView>(R.id.goLoginText)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        var modalView: View? = null

        // Manejo del registro con validación simple y modal animado
        registerButton.setOnClickListener {
            val nombre = nombreInput.text.toString()
            val email = emailInput.text.toString()
            val pass = passInput.text.toString()
            val confirmPass = confirmPassInput.text.toString()

            if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            } else if (pass != confirmPass) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            } else {
                // Mostrar modal con animación solo al registrar correctamente
                if (modalView == null) {
                    modalView = layoutInflater.inflate(R.layout.dialog_register_success, rootView, false)
                    rootView.addView(modalView)
                }
                modalView?.visibility = View.VISIBLE
                modalView?.alpha = 0f
                modalView?.animate()?.alpha(1f)?.setDuration(500)?.start()

                // Reproducir animación Lottie
                val lottieView = modalView?.findViewById<LottieAnimationView>(R.id.loadingLottie)
                lottieView?.playAnimation()

                // Tras 2 segundos, redirigir a LoginActivity y cerrar registro
                modalView?.postDelayed({
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }, 2000)
            }
        }

        // Navegar a pantalla de login si el usuario quiere
        goLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
