package com.example.hangman

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.hangman.data.FirebaseService
import com.example.hangman.databinding.ActivityModoContraRelojBinding
import com.example.hangman.models.Words
import com.google.android.material.button.MaterialButton

class ModoContraRelojActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModoContraRelojBinding
    private var palabraActual = ""
    private val letrasAdivinadas = mutableSetOf<Char>()
    private var puntos = 0
    private var pistaUsada = false
    private var totalMillis: Long = 60000
    private var tiempoRestanteMillis: Long = totalMillis
    private val handler = Handler(Looper.getMainLooper())
    private var partidaStartMillis: Long = 0L
    private var estaPausado = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!estaPausado) {
                tiempoRestanteMillis -= 100
                if (tiempoRestanteMillis <= 0) {
                    onTimeUp(); return
                }
                binding.txtTimer.text = "${(tiempoRestanteMillis / 1000)}s"
            }
            handler.postDelayed(this, 100)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModoContraRelojBinding.inflate(layoutInflater)
        setContentView(binding.root)

        totalMillis = intent.getLongExtra("timeSeconds", 60L) * 1000L
        binding.txtTimer.text = "${totalMillis / 1000}s"
        binding.btnAyuda.setOnClickListener { mostrarPista() }
        binding.btnPausa.setOnClickListener { mostrarDialogoPausa() }

        startGame()
    }

    private fun startGame() {
        palabraActual = Words.DICTIONARY.random().uppercase()
        letrasAdivinadas.clear()
        pistaUsada = false
        puntos = 0
        binding.txtPuntos.text = "Puntos: $puntos"
        binding.imgAhorcado.setImageResource(R.drawable.ahorcado_1)
        actualizarPalabraMostrada()
        generarTeclado()
        partidaStartMillis = System.currentTimeMillis()
        tiempoRestanteMillis = totalMillis
        handler.post(tickRunnable)
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
            }
            fila.forEach { letra ->
                val boton = MaterialButton(this).apply {
                    text = letra.toString()
                    setTextColor(Color.WHITE)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#9400D3"))
                    setOnClickListener {
                        manejarLetra(letra)
                        isEnabled = false
                        alpha = 0.5f
                    }
                }
                filaLayout.addView(boton)
            }
            tecladoContainer.addView(filaLayout)
        }
    }

    private fun manejarLetra(letra: Char) {
        if (palabraActual.contains(letra)) {
            letrasAdivinadas.add(letra)
            actualizarPalabraMostrada()
            verificarVictoria()
        }
    }

    private fun verificarVictoria() {
        if (palabraActual.all { letrasAdivinadas.contains(it) }) {
            val puntosGanados = 10
            puntos += puntosGanados
            binding.txtPuntos.text = "Puntos: $puntos"
            val duracion = ((System.currentTimeMillis() - partidaStartMillis) / 1000).toInt()

            FirebaseService.guardarPartida("ganada", palabraActual, puntosGanados, duracion)
            mostrarDialogoResultado("¡Ganaste! Palabra: $palabraActual", puntosGanados, true)
        }
    }

    private fun onTimeUp() {
        val duracion = ((System.currentTimeMillis() - partidaStartMillis) / 1000).toInt()
        FirebaseService.guardarPartida("perdida", palabraActual, 0, duracion)
        mostrarDialogoResultado("¡Tiempo agotado! La palabra era $palabraActual", 0, false)
    }

    private fun mostrarPista() {
        if (pistaUsada || puntos < 10) return
        pistaUsada = true
        puntos -= 10
        val letraAyuda = palabraActual.filterNot { letrasAdivinadas.contains(it) }.randomOrNull()
        letraAyuda?.let {
            AlertDialog.Builder(this)
                .setMessage("Una letra es: $it")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun mostrarDialogoResultado(mensaje: String, puntosGanados: Int, gano: Boolean) {
        handler.removeCallbacks(tickRunnable)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_result, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        dialogView.findViewById<TextView>(R.id.txtResultado).text = mensaje
        dialogView.findViewById<TextView>(R.id.txtPuntos).text = "Puntos: $puntosGanados"
        dialogView.findViewById<AppCompatButton>(R.id.btnSeguir).setOnClickListener { dialog.dismiss(); startGame() }
        dialogView.findViewById<AppCompatButton>(R.id.btnSalir).setOnClickListener { finish() }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun mostrarDialogoPausa() {
        estaPausado = true
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_pausa, null)
        val dialog = Dialog(this)
        dialog.setContentView(view)
        view.findViewById<AppCompatButton>(R.id.btnContinuar).setOnClickListener {
            estaPausado = false
            handler.post(tickRunnable)
            dialog.dismiss()
        }
        view.findViewById<AppCompatButton>(R.id.btnLeave).setOnClickListener { finish() }
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
