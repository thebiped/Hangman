package com.example.hangman.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.hangman.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nombreInput = findViewById<EditText>(R.id.usernameEditText)
        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val passInput = findViewById<EditText>(R.id.passwordEditText)
        val confirmPassInput = findViewById<EditText>(R.id.confirmPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val goLogin = findViewById<TextView>(R.id.goLoginText)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        var modalView: View? = null

        registerButton.setOnClickListener {
            val nombre = nombreInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val pass = passInput.text.toString().trim()
            val confirmPass = confirmPassInput.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                showErrorModal(rootView, "Completa todos los campos")
                return@setOnClickListener
            } else if (pass != confirmPass) {
                showErrorModal(rootView, "Las contraseñas no coinciden")
                return@setOnClickListener
            }

            // Mostrar modal animado
            if (modalView == null) {
                modalView = layoutInflater.inflate(R.layout.dialog_register_success, rootView, false)
                rootView.addView(modalView)
            }

            val animation = modalView!!.findViewById<LottieAnimationView>(R.id.loadingLottie)
            val messageText = modalView!!.findViewById<TextView>(R.id.loadingMessage)

            modalView!!.visibility = View.VISIBLE
            modalView!!.alpha = 0f
            modalView!!.animate()?.alpha(1f)?.setDuration(400)?.start()
            messageText.text = "Creando cuenta..."
            animation.playAnimation()

            // Registro con Firebase
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    val usuario = hashMapOf(
                        "uid" to uid,
                        "nombreUsuario" to nombre,
                        "email" to email,
                        "descripcion" to "",
                        "imagenPerfil" to "",
                        "nivel" to 1,
                        "puntos" to 0,
                        "historial" to emptyList<Map<String, Any>>(),
                        "fechaRegistro" to System.currentTimeMillis()
                    )

                    db.collection("usuarios")
                        .document(uid)
                        .set(usuario)
                        .addOnSuccessListener {
                            messageText.text = "Registro exitoso"
                            animation.setAnimation(R.raw.progressbar_login) // ⚠️ reemplazá por tu animación de éxito

                            modalView?.postDelayed({
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }, 2000)
                        }
                        .addOnFailureListener { e ->
                            modalView?.visibility = View.GONE
                            showErrorModal(rootView, "Error al guardar usuario")
                        }
                }
                .addOnFailureListener { e ->
                    modalView?.visibility = View.GONE
                    val mensaje = when {
                        e.message?.contains("email") == true -> "El correo no es válido o ya está registrado"
                        e.message?.contains("password") == true -> "La contraseña es demasiado débil"
                        else -> "Error de registro"
                    }
                    showErrorModal(rootView, mensaje)
                }
        }

        goLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    // Modal de error reutilizable
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
        }, 2500)
    }
}
