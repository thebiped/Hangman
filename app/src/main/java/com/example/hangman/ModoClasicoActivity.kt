package com.example.hangman

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.hangman.databinding.ActivityModoClasicoBinding
import com.example.hangman.models.Words
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ModoClasicoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModoClasicoBinding
    private var palabraActual = ""
    private val letrasAdivinadas = mutableSetOf<Char>()
    private var intentosRestantes = 8
    private var nivelActual = 1
    private var puntos = 0
    private var pistaUsada = false

    private var partidaStartMillis: Long = 0L

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModoClasicoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAyuda.setOnClickListener { mostrarPista() }
        binding.btnPausa.setOnClickListener { mostrarDialogoPausa() }

        binding.txtPuntos.text = "Puntos: $puntos"

        // Obtener nivel desde Firestore antes de iniciar el juego
        obtenerNivelUsuario { nivel ->
            nivelActual = nivel
            startGame()
        }
    }

    private fun startGame() {
        palabraActual = Words.getWordsByDifficulty(nivelActual).random().uppercase()
        letrasAdivinadas.clear()
        intentosRestantes = cuandoIntentosPorNivel(nivelActual)
        pistaUsada = false
        binding.txtIntentos.text = "Intentos: $intentosRestantes"
        binding.imgAhorcado.setImageResource(R.drawable.ahorcado_1)
        binding.txtPuntos.text = "Puntos: $puntos"
        actualizarPalabraMostrada()
        generarTeclado()
        partidaStartMillis = System.currentTimeMillis()
    }

    private fun cuandoIntentosPorNivel(nivel: Int): Int {
        return when (nivel) {
            1 -> 8
            2 -> 7
            3 -> 6
            4 -> 5
            else -> 4
        }
    }

    private fun actualizarPalabraMostrada() {
        val mostrada = palabraActual.map { c ->
            if (letrasAdivinadas.contains(c)) c else '_'
        }.joinToString(" ")
        binding.txtPalabra.text = mostrada
    }

    private fun generarTeclado() {
        val tecladoContainer = binding.keyboardContainer
        tecladoContainer.removeAllViews()

        val letras = ('A'..'Z').toMutableList()
        letras.add('Ñ')

        val letrasPorFila = 9
        val totalFilas = (letras.size + letrasPorFila - 1) / letrasPorFila

        for (fila in 0 until totalFilas) {
            val filaLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val inicio = fila * letrasPorFila
            val fin = minOf(inicio + letrasPorFila, letras.size)
            val subLista = letras.subList(inicio, fin)

            for (letra in subLista) {
                val boton = MaterialButton(this).apply {
                    text = letra.toString()
                    setTextColor(Color.WHITE)
                    textSize = 18f
                    strokeWidth = 4
                    cornerRadius = 100
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9400D3"))
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        setMargins(4, 4, 4, 4)
                    }
                    setPadding(0, 0, 0, 0)
                    setOnClickListener {
                        manejarLetra(letra, this)
                        isEnabled = false
                        alpha = 0.5f
                    }
                }
                filaLayout.addView(boton)
            }
            tecladoContainer.addView(filaLayout)
        }
    }

    private fun manejarLetra(letra: Char, boton: MaterialButton) {
        boton.isEnabled = false
        boton.alpha = 0.5f
        if (palabraActual.contains(letra)) {
            boton.strokeColor = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            letrasAdivinadas.add(letra)
            actualizarPalabraMostrada()
            verificarVictoria()
        } else {
            boton.strokeColor = ColorStateList.valueOf(Color.parseColor("#F44336"))
            intentosRestantes--
            binding.txtIntentos.text = "Intentos restantes: $intentosRestantes"
            updateHangman()
            verificarDerrota()
        }
    }

    private fun updateHangman() {
        val fallos = 8 - intentosRestantes
        val resId = resources.getIdentifier("ahorcado_$fallos", "drawable", packageName)
        if (resId != 0) binding.imgAhorcado.setImageResource(resId)
    }

    @SuppressLint("MissingInflatedId")
    private fun mostrarPista() {
        if (pistaUsada) {
            mostrarModalAdvertencia("Ya usaste la ayuda en esta ronda.")
            return
        }

        if (puntos < 10) {
            mostrarModalAdvertencia("Necesitás al menos 10 puntos para usar la ayuda.")
            return
        }

        val confirmView = LayoutInflater.from(this).inflate(R.layout.dialog_confirmacion_ayuda, null)
        val dialogConfirm = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            .setView(confirmView)
            .setCancelable(false)
            .create()

        confirmView.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            dialogConfirm.dismiss()
        }

        confirmView.findViewById<Button>(R.id.btnContinuar).setOnClickListener {
            dialogConfirm.dismiss()

            puntos -= 10
            pistaUsada = true
            binding.txtPuntos.text = "Puntos: $puntos"

            val letrasDisponibles = palabraActual.toSet().filter { it !in letrasAdivinadas }
            if (letrasDisponibles.isEmpty()) {
                mostrarModalAdvertencia("Ya descubriste todas las letras de la palabra.")
                return@setOnClickListener
            }
            val letraAyuda = letrasDisponibles.random()

            val ayudaView = LayoutInflater.from(this).inflate(R.layout.dialog_ayuda, null)
            val txtAyuda = ayudaView.findViewById<TextView>(R.id.txtAyuda)
            txtAyuda.text = "¡Ayuda! Una letra de la palabra es: $letraAyuda"

            val dialogAyuda = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
                .setView(ayudaView)
                .setCancelable(true)
                .create()

            dialogAyuda.show()
        }

        dialogConfirm.show()
    }

    private fun desactivarTeclado() {
        for (i in 0 until binding.keyboardContainer.childCount) {
            val fila = binding.keyboardContainer.getChildAt(i)
            if (fila is LinearLayout) {
                for (j in 0 until fila.childCount) {
                    val boton = fila.getChildAt(j)
                    if (boton is MaterialButton) {
                        boton.isEnabled = false
                        boton.alpha = 0.5f
                    }
                }
            }
        }
    }

    private fun verificarVictoria() {
        if (palabraActual.all { letrasAdivinadas.contains(it) }) {
            desactivarTeclado()
            val puntosGanados = 10 + (nivelActual - 1) * 2
            puntos += puntosGanados
            binding.txtPuntos.text = "Puntos: $puntos"
            val duracion = (System.currentTimeMillis() - partidaStartMillis) / 1000
            mostrarDialogoResultado("¡Felicidades! Adivinaste la palabra.", puntos = puntosGanados, gano = true)
            guardarPartidaEnFirestore(palabraActual, true, puntosGanados, duracion)
        }
    }

    private fun verificarDerrota() {
        if (intentosRestantes <= 0) {
            desactivarTeclado()
            binding.txtPalabra.text = palabraActual.toCharArray().joinToString(" ")
            val duracion = (System.currentTimeMillis() - partidaStartMillis) / 1000
            mostrarDialogoResultado("Perdiste. La palabra era: $palabraActual", gano = false)
            guardarPartidaEnFirestore(palabraActual, false, 0, duracion)
        }
    }

    private fun mostrarDialogoResultado(mensaje: String, puntos: Int = 0, gano: Boolean = false) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_resultado, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val txtResultado = dialogView.findViewById<TextView>(R.id.txtResultado)
        val txtPuntos = dialogView.findViewById<TextView>(R.id.txtPuntos)
        val btnSeguir = dialogView.findViewById<AppCompatButton>(R.id.btnSeguir)
        val btnSalir = dialogView.findViewById<AppCompatButton>(R.id.btnSalir)

        txtResultado.text = mensaje

        if (gano) {
            txtPuntos.visibility = View.VISIBLE
            txtPuntos.text = "¡Ganaste $puntos puntos!"
        } else {
            txtPuntos.visibility = View.GONE
        }

        btnSeguir.setOnClickListener {
            dialog.dismiss()
            startGame()
        }

        btnSalir.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.window?.apply {
            setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_dialog_overlay))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }

        dialog.show()
        desactivarTeclado()
    }

    private fun mostrarModalAdvertencia(mensaje: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_advertencia, null)
        val txtMensaje = view.findViewById<TextView>(R.id.txtMensajeAdvertencia)
        val btnCerrar = view.findViewById<Button>(R.id.btnCerrar)
        txtMensaje.text = mensaje

        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        btnCerrar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ---------------- FIREBASE ----------------
    private fun obtenerNivelUsuario(callback: (Int) -> Unit) {
        val uid = auth.currentUser?.uid ?: return callback(1)
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val nivel = (doc.getLong("nivel") ?: 1L).toInt()
                callback(nivel)
            }
            .addOnFailureListener { callback(1) }
    }

    private fun guardarPartidaEnFirestore(palabra: String, gano: Boolean, puntosGanados: Int, duracionSegundos: Long) {
        val uid = auth.currentUser?.uid ?: return
        val partida = mapOf(
            "palabra" to palabra,
            "resultado" to if (gano) "GANADA" else "PERDIDA",
            "puntos" to puntosGanados,
            "duracion" to duracionSegundos,
            "modo" to "ModoClasico",
            "fecha" to System.currentTimeMillis()
        )
        db.collection("usuarios").document(uid).collection("partidas")
            .add(partida)
            .addOnSuccessListener {
                actualizarEstadisticasUsuario(uid, gano, puntosGanados, duracionSegundos)
            }
    }

    private fun actualizarEstadisticasUsuario(uid: String, gano: Boolean, puntosGanados: Int, duracionSegundos: Long) {
        val userRef = db.collection("usuarios").document(uid)
        db.runTransaction { t ->
            val s = t.get(userRef)
            val puntos = s.getLong("puntosTotales") ?: 0
            val ganadas = s.getLong("partidasGanadas") ?: 0
            val perdidas = s.getLong("partidasPerdidas") ?: 0
            val horas = s.getDouble("horasJugadas") ?: 0.0
            val nuevos = puntos + puntosGanados
            val nivel = calcularNivelDesdePuntos(nuevos)
            t.update(userRef, mapOf(
                "puntosTotales" to nuevos,
                "partidasGanadas" to ganadas + if (gano) 1 else 0,
                "partidasPerdidas" to perdidas + if (!gano) 1 else 0,
                "horasJugadas" to horas + (duracionSegundos / 3600.0),
                "nivel" to nivel
            ))
        }
    }

    private fun calcularNivelDesdePuntos(p: Long): Long =
        when {
            p < 1000 -> 1
            p < 3000 -> 2
            p < 6000 -> 3
            p < 10000 -> 4
            else -> 5
        }
    @SuppressLint("MissingInflatedId")
    private fun mostrarDialogoPausa() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pausa, null)

        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(dialogView)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        dialogView.findViewById<Button>(R.id.btnContinuar).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btnReset).setOnClickListener {
            startGame()
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btnLeave).setOnClickListener {
            finish()
        }
    }

}
