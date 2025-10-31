package com.example.hangman

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.hangman.data.FirebaseService
import com.example.hangman.databinding.ActivityModoClasicoBinding
import com.example.hangman.models.Words
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ModoTematicaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModoClasicoBinding
    private var palabraActual = ""
    private val letrasAdivinadas = mutableSetOf<Char>()
    private var intentosRestantes = 8
    private var nivelActual = 1
    private var puntos = 0
    private var partidasGanadas = 0
    private var letrasExtraPorNivel = 0
    private var pistaUsada = false
    private var partidaStartMillis: Long = 0L

    private lateinit var tematica: String
    private var palabrasFiltradas = listOf<String>()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModoClasicoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tematica = intent.getStringExtra("tematica") ?: "general"
        palabrasFiltradas = Words.getWordsByCategory(tematica).map { it.uppercase() }

        if (palabrasFiltradas.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sin palabras")
                .setMessage("No hay palabras para la temática \"$tematica\".")
                .setPositiveButton("Volver") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        binding.btnAyuda.setOnClickListener { mostrarPista() }
        binding.btnPausa.setOnClickListener { mostrarDialogoPausa() }

        // Obtener puntos y nivel del usuario
        obtenerPuntosUsuario { pts ->
            puntos = pts
            binding.txtPuntos.text = "Puntos: $puntos"
            obtenerNivelUsuario { nivel ->
                nivelActual = nivel
                obtenerPartidasGanadas { startGame() }
            }
        }
    }

    private fun startGame() {
        val posiblesPalabras = palabrasFiltradas.filter { it.length <= nivelActual + letrasExtraPorNivel + 4 }
        palabraActual = posiblesPalabras.random().uppercase()

        letrasAdivinadas.clear()
        intentosRestantes = intentosPorNivel(nivelActual)
        pistaUsada = false
        binding.txtIntentos.text = "Intentos: $intentosRestantes"
        binding.imgAhorcado.setImageResource(R.drawable.ahorcado_1)
        binding.txtPuntos.text = "Puntos: $puntos"
        actualizarPalabraMostrada()
        generarTeclado()
        partidaStartMillis = System.currentTimeMillis()
    }

    private fun intentosPorNivel(nivel: Int) = when (nivel) {
        1 -> 8
        2 -> 7
        3 -> 6
        4 -> 5
        else -> 4
    }

    private fun actualizarPalabraMostrada() {
        val mostrada = palabraActual.map { if (letrasAdivinadas.contains(it)) it else '_' }.joinToString(" ")
        binding.txtPalabra.text = mostrada
    }

    private fun generarTeclado() {
        val tecladoContainer = binding.keyboardContainer
        tecladoContainer.removeAllViews()
        val letras = ('A'..'Z').toMutableList().apply { add('Ñ') }
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
            letras.subList(inicio, fin).forEach { letra ->
                val boton = MaterialButton(this).apply {
                    text = letra.toString()
                    setTextColor(Color.WHITE)
                    textSize = 20f
                    strokeWidth = 4
                    cornerRadius = 100
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9400D3"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 4, 4, 4) }
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
        boton.alpha = 0.8f

        if (palabraActual.contains(letra)) {
            boton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            letrasAdivinadas.add(letra)
            actualizarPalabraMostrada()
            verificarVictoria()
        } else {
            boton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
            intentosRestantes--
            binding.txtIntentos.text = "Intentos: $intentosRestantes"
            updateHangman()
            verificarDerrota()
        }
    }

    private fun updateHangman() {
        val fallos = 8 - intentosRestantes
        val resId = resources.getIdentifier("ahorcado_$fallos", "drawable", packageName)
        if (resId != 0) binding.imgAhorcado.setImageResource(resId)
    }

    private fun verificarVictoria() {
        if (palabraActual.all { letrasAdivinadas.contains(it) }) {
            desactivarTeclado()
            val puntosGanados = 10 + (nivelActual - 1) * 2
            sumarPuntos(puntosGanados)

            var mensajeExtra = ""
            partidasGanadas++
            if (partidasGanadas % 10 == 0) {
                nivelActual++
                letrasExtraPorNivel++
                actualizarNivelFirebase()
                mensajeExtra = "\n¡Subiste al nivel $nivelActual!"
            }

            guardarPartida(true, puntosGanados)
            mostrarDialogoResultado("¡Felicidades! Adivinaste la palabra.$mensajeExtra", puntosGanados, true)
        }
    }

    private fun verificarDerrota() {
        if (intentosRestantes <= 0) {
            desactivarTeclado()
            binding.txtPalabra.text = palabraActual.toCharArray().joinToString(" ")
            guardarPartida(false, 0)
            mostrarDialogoResultado("Perdiste. La palabra era: $palabraActual", 0, false)
        }
    }

    private fun desactivarTeclado() {
        for (i in 0 until binding.keyboardContainer.childCount) {
            val fila = binding.keyboardContainer.getChildAt(i)
            if (fila is LinearLayout) {
                for (j in 0 until fila.childCount) {
                    (fila.getChildAt(j) as? MaterialButton)?.apply {
                        isEnabled = false
                        alpha = 0.5f
                    }
                }
            }
        }
    }

    private fun mostrarDialogoPausa() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pausa, null)
        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(dialogView)
        dialog.window?.apply {
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
        dialogView.findViewById<Button>(R.id.btnContinuar).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnReset).setOnClickListener { startGame(); dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnLeave).setOnClickListener { finish() }
    }

    private fun mostrarDialogoResultado(mensaje: String, puntosGanados: Int, gano: Boolean) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_resultado, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        val txtResultado = dialogView.findViewById<TextView>(R.id.txtResultado)
        val txtPuntos = dialogView.findViewById<TextView>(R.id.txtPuntos)
        val btnSeguir = dialogView.findViewById<AppCompatButton>(R.id.btnSeguir)
        val btnSalir = dialogView.findViewById<AppCompatButton>(R.id.btnSalir)

        txtResultado.text = mensaje
        txtPuntos.visibility = if (gano) View.VISIBLE else View.GONE
        if (gano) txtPuntos.text = "¡Ganaste $puntosGanados puntos!"

        btnSeguir.setOnClickListener { dialog.dismiss(); startGame() }
        btnSalir.setOnClickListener { dialog.dismiss(); finish() }

        dialog.window?.apply {
            setBackgroundDrawable(ContextCompat.getDrawable(this@ModoTematicaActivity, R.drawable.bg_dialog_overlay))
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }
        dialog.show()
        desactivarTeclado()
    }

    private fun sumarPuntos(cantidad: Int) {
        puntos += cantidad
        binding.txtPuntos.text = "Puntos: $puntos"
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("usuarios").document(uid)
                .update("puntos", puntos)
                .addOnFailureListener { e -> Log.e("ModoTematica", "Error al guardar puntos: ${e.message}") }
        }
    }

    private fun actualizarNivelFirebase() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid).update("nivel", nivelActual)
            .addOnFailureListener { e -> Log.e("ModoTematica", "Error al guardar nivel: ${e.message}") }
    }

    private fun guardarPartida(gano: Boolean, puntosGanados: Int) {
        val duracion = ((System.currentTimeMillis() - partidaStartMillis) / 1000).toInt()
        val estado = if (gano) "ganada" else "perdida"

        FirebaseService.guardarPartidaAtomic(
            estado,
            palabraActual,
            puntosGanados,
            duracion
        )

        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("usuarios").document(uid)
        if (gano) {
            docRef.update("partidasGanadas", FieldValue.increment(1))
                .addOnFailureListener { e -> Log.e("ModoTematica", "Error al actualizar partidas ganadas: ${e.message}") }
        } else {
            docRef.update("partidasPerdidas", FieldValue.increment(1))
                .addOnFailureListener { e -> Log.e("ModoTematica", "Error al actualizar partidas perdidas: ${e.message}") }
        }
    }

    private fun obtenerNivelUsuario(callback: (Int) -> Unit) {
        val uid = auth.currentUser?.uid ?: return callback(1)
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc -> callback((doc.getLong("nivel") ?: 1L).toInt()) }
            .addOnFailureListener { Log.e("ModoTematica", "Error al obtener nivel: ${it.message}"); callback(1) }
    }

    private fun obtenerPartidasGanadas(callback: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return callback()
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                partidasGanadas = (doc.getLong("partidasGanadas") ?: 0L).toInt()
                callback()
            }
            .addOnFailureListener { Log.e("ModoTematica", "Error al obtener partidas ganadas: ${it.message}"); callback() }
    }

    private fun obtenerPuntosUsuario(callback: (Int) -> Unit) {
        val uid = auth.currentUser?.uid ?: return callback(0)
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc -> callback((doc.getLong("puntos") ?: 0L).toInt()) }
            .addOnFailureListener { Log.e("ModoTematica", "Error al obtener puntos: ${it.message}"); callback(0) }
    }

    private fun mostrarPista() {
        if (pistaUsada) { mostrarModalAdvertencia("Ya usaste la ayuda en esta ronda."); return }
        if (puntos < 10) { mostrarModalAdvertencia("Necesitás al menos 10 puntos para usar la ayuda."); return }

        val confirmView = LayoutInflater.from(this).inflate(R.layout.dialog_confirmacion_ayuda, null)
        val dialogConfirm = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            .setView(confirmView)
            .setCancelable(false)
            .create()

        confirmView.findViewById<Button>(R.id.btnCancelar).setOnClickListener { dialogConfirm.dismiss() }
        confirmView.findViewById<Button>(R.id.btnContinuar).setOnClickListener {
            dialogConfirm.dismiss()
            sumarPuntos(-10)
            pistaUsada = true

            val letrasDisponibles = palabraActual.toSet().filter { it !in letrasAdivinadas }
            if (letrasDisponibles.isEmpty()) { mostrarModalAdvertencia("Ya descubriste todas las letras."); return@setOnClickListener }
            val letraAyuda = letrasDisponibles.random()

            val ayudaView = LayoutInflater.from(this).inflate(R.layout.dialog_ayuda, null)
            ayudaView.findViewById<TextView>(R.id.txtAyuda).text = "¡Ayuda! Una letra es: $letraAyuda"
            AlertDialog.Builder(this, R.style.CustomAlertDialogStyle).setView(ayudaView).setCancelable(true).create().show()
        }

        dialogConfirm.show()
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
}
