package com.example.hangman

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.hangman.databinding.ActivityModoContraRelojBinding
import com.example.hangman.models.Words
import com.google.android.material.button.MaterialButton

class ModoContraRelojActivity : AppCompatActivity() {

    //Declarar variables
    private lateinit var binding: ActivityModoContraRelojBinding
    private var palabraActual = ""
    private val letrasAdivinadas = mutableSetOf<Char>()
    private var puntos = 0
    private var pistaUsada = false
    private var palabrasDisponibles = listOf<String>()
    private var tematica: String? = null
    private var totalMillis: Long = 60000
    private var tiempoRestanteMillis: Long = totalMillis
    private val intervaloBase: Long = 50L
    private var velocidadFactor = 1.0f
    private val velocidadMax = 5f
    private var estaPausado = false
    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!estaPausado) {
                val decremento = (intervaloBase * velocidadFactor).toLong()
                tiempoRestanteMillis -= decremento

                if (tiempoRestanteMillis < 0L) tiempoRestanteMillis = 0L

                updateHangman()
                binding.txtTimer.text = "${(tiempoRestanteMillis / 1000)}s"

                if (tiempoRestanteMillis <= 0L) {
                    onTimeUp()
                    return
                }
            }
            handler.postDelayed(this, intervaloBase)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModoContraRelojBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val timeSeconds = intent.getLongExtra("timeSeconds", 60L)
        totalMillis = timeSeconds * 1000L
        tiempoRestanteMillis = totalMillis

        tematica = intent.getStringExtra("tematica")
        palabrasDisponibles = if (!tematica.isNullOrBlank()) {
            Words.getWordsByCategory(tematica!!).map { it.uppercase() }
        } else {
            Words.DICTIONARY.map { it.uppercase() }
        }

        if (palabrasDisponibles.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sin palabras")
                .setMessage("No hay palabras disponibles para jugar contra reloj.")
                .setPositiveButton("Volver") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        // Configuración inicial del juego
        binding.txtPuntos.text = "Puntos: $puntos"
        binding.txtTimer.text = "${timeSeconds}s"
        binding.btnAyuda.setOnClickListener { mostrarPista() }
        binding.btnPausa.setOnClickListener { mostrarDialogoPausa() }

        startGame()
    }

    // Inicia el juego
    private fun startGame() {
        palabraActual = palabrasDisponibles.random().uppercase()
        letrasAdivinadas.clear()
        pistaUsada = false
        velocidadFactor = 1.0f
        estaPausado = false

        tiempoRestanteMillis = totalMillis
        binding.txtTimer.text = "${totalMillis / 1000}s"
        binding.imgAhorcado.setImageResource(R.drawable.ahorcado_1)
        actualizarPalabraMostrada()
        generarTeclado()
        startTimer()
    }

    //Inicializa el contador
    private fun startTimer() {
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
    }

    //Finaliza el contador
    private fun stopTimer() {
        handler.removeCallbacks(tickRunnable)
    }

    // Actualiza la imagen del ahorcado
    private fun updateHangman() {
        val etapas = 8
        val tiempoTranscurrido = totalMillis - tiempoRestanteMillis
        val etapaActual = ((tiempoTranscurrido.toFloat() / totalMillis) * etapas).toInt().coerceIn(1, etapas)
        val resId = resources.getIdentifier("ahorcado_$etapaActual", "drawable", packageName)
        binding.imgAhorcado.setImageResource(resId)
    }

    // Actualiza la palabra mostrada al usuario
    private fun actualizarPalabraMostrada() {
        val mostrada = palabraActual.map { c -> if (letrasAdivinadas.contains(c)) c else '_' }.joinToString(" ")
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
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
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
        if (palabraActual.contains(letra)) {
            letrasAdivinadas.add(letra)
            actualizarPalabraMostrada()
            verificarVictoria()
        } else {
            velocidadFactor = (velocidadFactor + 0.5f).coerceAtMost(velocidadMax)
        }
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
            stopTimer()
            desactivarTeclado()
            puntos += 10
            binding.txtPuntos.text = "Puntos: $puntos"
            mostrarDialogoResultado("¡Felicidades! Adivinaste la palabra.", puntos = 10, gano = true)
        }
    }

    // Muestra la pista con confirmación
    private fun mostrarPista() {
        stopTimer()

        if (pistaUsada) {
            mostrarModalAdvertencia("Ya usaste la ayuda en esta ronda.")
            startTimer()
            return
        }
        if (puntos < 10) {
            mostrarModalAdvertencia("Necesitás al menos 10 puntos para usar la ayuda.")
            startTimer()
            return
        }

        puntos -= 10
        pistaUsada = true
        binding.txtPuntos.text = "Puntos: $puntos"

        val letrasDisponibles = palabraActual.toSet().filter { it !in letrasAdivinadas }
        if (letrasDisponibles.isEmpty()) return
        val letraAyuda = letrasDisponibles.random()

        val txtAyuda = LayoutInflater.from(this).inflate(R.layout.dialog_ayuda, null)
            .findViewById<TextView>(R.id.txtAyuda)
        txtAyuda.text = "¡Ayuda! Una letra de la palabra es: $letraAyuda"
        val dialog = AlertDialog.Builder(this)
            .setView(txtAyuda.parent as View)
            .setCancelable(true)
            .create()

        dialog.setOnDismissListener {
            startTimer()
        }

        dialog.show()
    }

    // Muestra un modal de resultado
    private fun mostrarDialogoResultado(mensaje: String, puntos: Int = 0, gano: Boolean = false) {
        stopTimer()
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
        } else txtPuntos.visibility = View.GONE

        btnSeguir.setOnClickListener {
            dialog.dismiss()
            startGame()
        }

        btnSalir.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setDimAmount(0f)
        }
        dialog.show()
        desactivarTeclado()
    }

    // Muestra el modal de pausa
    private fun mostrarDialogoPausa() {
        stopTimer()
        estaPausado = true

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pausa, null)
        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(dialogView)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        dialogView.findViewById<AppCompatButton>(R.id.btnContinuar).setOnClickListener {
            dialog.dismiss()
            estaPausado = false
            startTimer()
        }
        dialogView.findViewById<AppCompatButton>(R.id.btnReset).setOnClickListener {
            startGame()
            dialog.dismiss()
        }
        dialogView.findViewById<AppCompatButton>(R.id.btnLeave).setOnClickListener { finish() }
    }

    // Muestra advertencias simples
    private fun mostrarModalAdvertencia(mensaje: String) {
        stopTimer()
        estaPausado = true

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_warm, null)
        val txtMensaje = view.findViewById<TextView>(R.id.txtMensajeAdvertencia)
        val btnCerrar = view.findViewById<AppCompatButton>(R.id.btnCerrar)
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
            estaPausado = false
            startTimer()
        }

        dialog.show()
    }

    private fun onTimeUp() {
        stopTimer()
        desactivarTeclado()
        mostrarDialogoResultado("¡Se acabó el tiempo!", puntos, gano = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }
}
