package com.example.hangman

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.hangman.data.FirebaseService
import com.example.hangman.databinding.ActivityModoClasicoBinding
import com.example.hangman.models.Words
import com.google.android.material.button.MaterialButton

class ModoTematicaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModoClasicoBinding
    private var palabraActual = ""
    private val letrasAdivinadas = mutableSetOf<Char>()
    private var intentosRestantes = 8
    private var puntos = 0
    private var pistaUsada = false
    private lateinit var tematica: String
    private var palabrasFiltradas = listOf<String>()
    private var partidaStartMillis: Long = 0L

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
        binding.txtPuntos.text = "Puntos: $puntos"
        startGame()
    }

    private fun startGame() {
        palabraActual = palabrasFiltradas.random().uppercase()
        letrasAdivinadas.clear()
        intentosRestantes = 8
        pistaUsada = false
        binding.txtIntentos.text = "Intentos: $intentosRestantes"
        binding.imgAhorcado.setImageResource(R.drawable.ahorcado_1)
        binding.txtPuntos.text = "Puntos: $puntos"
        actualizarPalabraMostrada()
        generarTeclado()
        partidaStartMillis = System.currentTimeMillis()
    }

    private fun actualizarPalabraMostrada() {
        val mostrada = palabraActual.map { if (letrasAdivinadas.contains(it)) it else '_' }.joinToString(" ")
        binding.txtPalabra.text = mostrada
    }

    private fun generarTeclado() {
        val tecladoContainer = binding.keyboardContainer
        tecladoContainer.removeAllViews()
        val letras = ('A'..'Z').toMutableList().apply { add('Ñ') }

        letras.chunked(9).forEach { fila ->
            val filaLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            fila.forEach { letra ->
                val boton = MaterialButton(this).apply {
                    text = letra.toString()
                    setTextColor(Color.WHITE)
                    textSize = 18f
                    strokeWidth = 4
                    cornerRadius = 100
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9400D3"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(4, 4, 4, 4)
                    }
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

    private fun verificarVictoria() {
        if (palabraActual.all { letrasAdivinadas.contains(it) }) {
            desactivarTeclado()
            puntos += 10
            binding.txtPuntos.text = "Puntos: $puntos"
            val duracion = ((System.currentTimeMillis() - partidaStartMillis) / 1000).toInt()
            FirebaseService.guardarPartida("ganada", palabraActual, 10, duracion)
            mostrarDialogoResultado("¡Felicidades! Adivinaste la palabra.", puntos = 10, gano = true)
        }
    }

    private fun verificarDerrota() {
        if (intentosRestantes <= 0) {
            desactivarTeclado()
            binding.txtPalabra.text = palabraActual.toCharArray().joinToString(" ")
            val duracion = ((System.currentTimeMillis() - partidaStartMillis) / 1000).toInt()
            FirebaseService.guardarPartida("perdida", palabraActual, 0, duracion)
            mostrarDialogoResultado("Perdiste. La palabra era: $palabraActual", gano = false)
        }
    }

    private fun desactivarTeclado() {
        for (i in 0 until binding.keyboardContainer.childCount) {
            val fila = binding.keyboardContainer.getChildAt(i)
            if (fila is LinearLayout) for (j in 0 until fila.childCount) {
                (fila.getChildAt(j) as? MaterialButton)?.apply {
                    isEnabled = false; alpha = 0.5f
                }
            }
        }
    }

    private fun mostrarDialogoResultado(mensaje: String, puntos: Int = 0, gano: Boolean = false) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_result, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        val txtResultado = dialogView.findViewById<TextView>(R.id.txtResultado)
        val txtPuntos = dialogView.findViewById<TextView>(R.id.txtPuntos)
        val btnSeguir = dialogView.findViewById<AppCompatButton>(R.id.btnSeguir)
        val btnSalir = dialogView.findViewById<AppCompatButton>(R.id.btnSalir)

        txtResultado.text = mensaje
        txtPuntos.visibility = if (gano) View.VISIBLE else View.GONE
        if (gano) txtPuntos.text = "¡Ganaste $puntos puntos!"

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

        dialogView.findViewById<Button>(R.id.btnContinuar).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnReset).setOnClickListener { startGame(); dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnLeave).setOnClickListener { finish() }
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
