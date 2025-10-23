package com.example.hangman.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.hangman.R
import com.example.hangman.WelcomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        // Manejo de login
        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val pass = passwordInput.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mostrar modal animado
            if (modalView == null) {
                modalView = layoutInflater.inflate(R.layout.dialog_login_success, rootView, false)
                rootView.addView(modalView)
            }
            modalView?.visibility = View.VISIBLE
            modalView?.alpha = 0f
            modalView?.animate()?.alpha(1f)?.setDuration(500)?.start()
            modalView?.findViewById<LottieAnimationView>(R.id.loadingLottie)?.playAnimation()

            // Login con Firebase Authentication
            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    // Obtener datos del usuario desde Firestore
                    FirebaseFirestore.getInstance().collection("usuarios")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val nombre = document.getString("nombreUsuario") ?: ""
                                Toast.makeText(this, "Bienvenido $nombre", Toast.LENGTH_SHORT).show()

                                // Redirigir a WelcomeActivity
                                modalView?.postDelayed({
                                    startActivity(Intent(this, WelcomeActivity::class.java))
                                    finish()
                                }, 2000)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al obtener datos: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}