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

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Referencias a los campos de entrada y botones
        val nombreInput = findViewById<EditText>(R.id.usernameEditText)
        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val passInput = findViewById<EditText>(R.id.passwordEditText)
        val confirmPassInput = findViewById<EditText>(R.id.confirmPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val goLogin = findViewById<TextView>(R.id.goLoginText)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        var modalView: View? = null

        // Manejo del registro
        registerButton.setOnClickListener {
            val nombre = nombreInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val pass = passInput.text.toString().trim()
            val confirmPass = confirmPassInput.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (pass != confirmPass) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mostrar modal animado
            if (modalView == null) {
                modalView = layoutInflater.inflate(R.layout.dialog_register_success, rootView, false)
                rootView.addView(modalView)
            }
            modalView?.visibility = View.VISIBLE
            modalView?.alpha = 0f
            modalView?.animate()?.alpha(1f)?.setDuration(500)?.start()
            modalView?.findViewById<LottieAnimationView>(R.id.loadingLottie)?.playAnimation()

            // Registro con Firebase Authentication
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    // Datos del usuario para guardar en Firestore
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

                    // Guardar datos adicionales en Firestore
                    db.collection("usuarios")
                        .document(uid)
                        .set(usuario)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()

                            // Redirigir a LoginActivity después de 2 segundos
                            modalView?.postDelayed({
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }, 2000)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al guardar usuario: ${e.message}", Toast.LENGTH_SHORT).show()
                            modalView?.visibility = View.GONE
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error de registro: ${e.message}", Toast.LENGTH_SHORT).show()
                    modalView?.visibility = View.GONE
                }
        }

        // Navegar a pantalla de login si el usuario quiere
        goLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
