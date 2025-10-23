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
import android.view.ViewGroup
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
import androidx.core.graphics.drawable.toDrawable

class ModoTematicaActivity : AppCompatActivity() {

    //Declarar variables
    private lateinit var binding: ActivityModoClasicoBinding
    private var palabraActual = ""
    private val letrasAdivinadas = mutableSetOf<Char>()
    private var intentosRestantes = 8
    private var puntos = 0
    private var pistaUsada = false
    private lateinit var tematica: String
    private var palabrasFiltradas = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModoClasicoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tematica = intent.getStringExtra("tematica") ?: "general"

        palabrasFiltradas = try {
            Words.getWordsByCategory(tematica).map { it.uppercase() }
        } catch (e: Exception) {
            emptyList()
        }

        if (palabrasFiltradas.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sin palabras")
                .setMessage("No se encontraron palabras para la temática \"$tematica\".")
                .setPositiveButton("Volver") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        // Configuración inicial del juego
        binding.btnAyuda.setOnClickListener { mostrarPista() }
        binding.btnPausa.setOnClickListener { mostrarDialogoPausa() }
        binding.txtPuntos.text = "Puntos: $puntos"

        startGame()
    }

    // Inicia el juego
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
    }


    // Actualiza la palabra mostrada al usuario
    private fun actualizarPalabraMostrada() {
        val mostrada = palabraActual.map { c ->
            if (letrasAdivinadas.contains(c)) c else '_'
        }.joinToString(" ")
        binding.txtPalabra.text = mostrada
    }

    // Genera el teclado y sus interacciones
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

    // Maneja la acción de presionar una letra
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

    // Actualiza la imagen del ahorcado
    private fun updateHangman() {
        val fallos = 8 - intentosRestantes
        val resId = resources.getIdentifier("ahorcado_$fallos", "drawable", packageName)
        if (resId != 0) binding.imgAhorcado.setImageResource(resId)
    }

    // Muestra la pista con confirmación
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

        val confirmView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)

        confirmView.setBackgroundColor(Color.TRANSPARENT)
        fun clearBackgroundRec(v: View) {
            v.background = null
            if (v is ViewGroup) for (i in 0 until v.childCount) clearBackgroundRec(v.getChildAt(i))
        }

        val dialogConfirm = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            .setView(confirmView)
            .setCancelable(false)
            .create()

        dialogConfirm.setOnShowListener {
            dialogConfirm.window?.apply {
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                decorView?.setPadding(0, 0, 0, 0)
            }
            dialogConfirm.findViewById<View>(android.R.id.content)?.background = null
            confirmView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

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

            dialogAyuda.setOnShowListener {
                dialogAyuda.window?.apply {
                    setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                    decorView?.setPadding(0, 0, 0, 0)
                }
                dialogAyuda.findViewById<View>(android.R.id.content)?.background = null
                ayudaView.setBackgroundColor(Color.TRANSPARENT)
            }

            dialogAyuda.show()
        }

        dialogConfirm.show()
    }

    // Desactiva todo el teclado
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

    // Verifica si el jugador ganó
    private fun verificarVictoria() {
        if (palabraActual.all { letrasAdivinadas.contains(it) }) {
            desactivarTeclado()
            puntos += 10
            binding.txtPuntos.text = "Puntos: $puntos"
            mostrarDialogoResultado("¡Felicidades! Adivinaste la palabra.", puntos = 10, gano = true)
        }
    }

    // Verifica si el jugador perdió
    private fun verificarDerrota() {
        if (intentosRestantes <= 0) {
            desactivarTeclado()
            binding.txtPalabra.text = palabraActual.toCharArray().joinToString(" ")
            mostrarDialogoResultado("Perdiste. La palabra era: $palabraActual", gano = false)
        }
    }

    // Muestra un modal de resultado
    private fun mostrarDialogoResultado(mensaje: String, puntos: Int = 0, gano: Boolean = false) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_result, null)
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
            setBackgroundDrawable(ContextCompat.getDrawable(this@ModoTematicaActivity, R.drawable.bg_dialog_overlay))
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setDimAmount(0f)
        }

        dialog.show()
        desactivarTeclado()
    }

    // Muestra el modal de pausa
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

    // Muestra advertencias simples
    private fun mostrarModalAdvertencia(mensaje: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_warm, null)
        val txtMensaje = view.findViewById<TextView>(R.id.txtMensajeAdvertencia)
        val btnCerrar = view.findViewById<Button>(R.id.btnCerrar)

        txtMensaje.text = mensaje

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setDimAmount(0.6f)
        }

        btnCerrar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
