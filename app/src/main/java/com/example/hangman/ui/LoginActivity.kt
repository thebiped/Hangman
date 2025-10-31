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
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val passwordInput = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registroText = findViewById<TextView>(R.id.registroText)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        var modalView: View? = null
        var errorModalView: View? = null

        registroText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass = passwordInput.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                showErrorModal(rootView, "Por favor completa todos los campos")
                return@setOnClickListener
            }

            // Mostrar modal de carga
            if (modalView == null) {
                modalView = layoutInflater.inflate(R.layout.dialog_login_success, rootView, false)
                rootView.addView(modalView)
            }

            val animation = modalView!!.findViewById<LottieAnimationView>(R.id.loadingLottie)
            val messageText = modalView!!.findViewById<TextView>(R.id.loadingMessage)

            modalView!!.visibility = View.VISIBLE
            modalView!!.alpha = 0f
            modalView!!.animate()?.alpha(1f)?.setDuration(400)?.start()
            messageText.text = "Verificando datos..."
            animation.playAnimation()

            // Firebase login
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    db.collection("usuarios").document(uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val nombre = document.getString("nombreUsuario") ?: "Jugador"

                                // Cambiar texto a "Inicio de sesión exitoso"
                                messageText.text = "Inicio de sesión exitoso"
                                animation.setAnimation(R.raw.progressbar_login) // ⚠️ Usa tu propia animación si querés

                                modalView?.postDelayed({
                                    startActivity(Intent(this, WelcomeActivity::class.java))
                                    finish()
                                }, 2000)
                            } else {
                                modalView?.visibility = View.GONE
                                showErrorModal(rootView, "El usuario no existe")
                            }
                        }
                        .addOnFailureListener {
                            modalView?.visibility = View.GONE
                            showErrorModal(rootView, "Error al obtener datos del usuario")
                        }
                }
                .addOnFailureListener { e ->
                    modalView?.visibility = View.GONE
                    val mensaje = when {
                        e.message?.contains("password") == true -> "Contraseña incorrecta"
                        e.message?.contains("no user record") == true -> "El usuario no existe"
                        else -> "Error al iniciar sesión"
                    }
                    showErrorModal(rootView, mensaje)
                }
        }
    }

    // Mostrar un modal temporal para errores
    private fun showErrorModal(rootView: ViewGroup, mensaje: String) {
        val errorModal = layoutInflater.inflate(R.layout.dialog_error_message, rootView, false)
        val textView = errorModal.findViewById<TextView>(R.id.errorText)
        textView.text = mensaje

        rootView.addView(errorModal)
        errorModal.alpha = 0f
        errorModal.animate().alpha(1f).setDuration(300).start()

        errorModal.postDelayed({
            errorModal.animate().alpha(0f).setDuration(500).withEndAction {
                rootView.removeView(errorModal)
            }.start()
        }, 2000)
    }
}
