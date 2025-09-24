package com.example.hangman

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.hangman.ui.LoginActivity
import com.example.hangman.ui.RegisterActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
//import com.example.hangman.models.Words
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlin.random.Random

class MainActivity : AppCompatActivity() {

//    private val db = FirebaseFirestore.getInstance()
//    private val TAG = "FirestoreExample"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<Button>(R.id.startButton)
        val loginButton = findViewById<Button>(R.id.loginButton)

        // Botón para ir a registro
        startButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Botón para ir a login
        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

//        val btnSend = findViewById<Button>(R.id.btnGuardar)
//
//        btnSend.setOnClickListener {
//            val randomWord = Words.DICTIONARY.random()
//            db.collection("words")
//                .get()
//                .addOnSuccessListener { result ->
//                    val nextId = (result.size() + 1).toString()
//
//                    val wordData = hashMapOf(
//                        "word" to randomWord,
//                        "timestamp" to System.currentTimeMillis()
//                    )
//
//                    db.collection("words")
//                        .document(nextId)
//                        .set(wordData)
//                        .addOnSuccessListener {
//                            Log.d(TAG, "Palabra enviada con ID: $nextId")
//                        }
//                        .addOnFailureListener { e ->
//                            Log.w(TAG, "Error al enviar palabra", e)
//                        }
//                }
//                .addOnFailureListener { e ->
//                    Log.w(TAG, "Error al obtener documentos", e)
//                }
//        }

    }
}
