package com.example.hangman

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.hangman.databinding.ActivityModoClasicoBinding
import com.example.hangman.models.Words
import com.google.android.material.button.MaterialButton
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat

class ModoClasicoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModoClasicoBinding
    private var palabraActual = ""
    private val letrasAdivinadas = mutableSetOf<Char>()
    private var intentosRestantes = 8
    private var nivelActual = 1
    private var puntos = 0
    private var pistaUsada = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModoClasicoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAyuda.setOnClickListener { mostrarPista() }
        startGame()   // Inicializa juego
        binding.btnPausa.setOnClickListener { mostrarDialogoPausa() }
        binding.txtPuntos.text = "Puntos: $puntos"
    }

    // Inicia o reinicia la ronda con palabra nueva y teclado fresco
    private fun startGame() {
        palabraActual = Words.getWordsByDifficulty(nivelActual).random().uppercase()
        letrasAdivinadas.clear()
        intentosRestantes = 8
        pistaUsada = false
        binding.txtIntentos.text = "Intentos: $intentosRestantes"
        binding.imgAhorcado.setImageResource(R.drawable.ahorcado_1)
        binding.txtPuntos.text = "Puntos: $puntos"
        actualizarPalabraMostrada()
        generarTeclado()
    }

    // Muestra la palabra con letras adivinadas y guiones bajos
    private fun actualizarPalabraMostrada() {
        val mostrada = palabraActual.map { c ->
            if (letrasAdivinadas.contains(c)) c else '_'
        }.joinToString(" ")
        binding.txtPalabra.text = mostrada
    }

    // Crea teclado dinámico con letras para que usuario pulse
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

    // Maneja la lógica al presionar una letra
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

    // Actualiza la imagen del ahorcado según fallos
    private fun updateHangman() {
        val fallos = 8 - intentosRestantes
        val resId = resources.getIdentifier("ahorcado_$fallos", "drawable", packageName)
        if (resId != 0) binding.imgAhorcado.setImageResource(resId)
    }

    // Muestra pista si hay puntos y no se usó aún
    private fun mostrarPista() {
        if (pistaUsada) {
            mostrarModalAdvertencia("Ya usaste la pista en esta ronda.")
            return
        }

        if (puntos < 10) {
            mostrarModalAdvertencia("Necesitás al menos 10 puntos para usar una pista.")
            return
        }

        puntos -= 10
        pistaUsada = true
        binding.txtPuntos.text = "Puntos: $puntos"

        val pista = palabraActual.first()

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_ayuda, null)
        val txtAyuda = view.findViewById<TextView>(R.id.txtAyuda)
        txtAyuda.text = "¡Pista! La primera letra es: $pista"

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.6f)
        }

        dialog.show()
    }

    // Desactiva todo el teclado (tras victoria o derrota)
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

    // Muestra modal con resultado final y opciones continuar o salir
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
            setDimAmount(0f)
        }

        dialog.show()
        desactivarTeclado()
    }

    // Muestra diálogo de pausa con opciones para continuar, resetear o salir
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

    // Muestra advertencia modal para alertas simples
    private fun mostrarModalAdvertencia(mensaje: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_advertencia, null)
        val txtMensaje = view.findViewById<TextView>(R.id.txtMensajeAdvertencia)
        val btnCerrar = view.findViewById<Button>(R.id.btnCerrar)

        txtMensaje.text = mensaje

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
            setDimAmount(0.6f)
        }

        btnCerrar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}
