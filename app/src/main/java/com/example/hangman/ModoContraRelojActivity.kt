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
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.hangman.databinding.ActivityModoContraRelojBinding
import com.example.hangman.models.Words
import com.example.hangman.utils.GameStatsManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.max

class ModoContraRelojActivity : AppCompatActivity() {

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
    private var partidaStartMillis: Long = 0L

    private val tickRunnable = object : Runnable {
        override fun run() {
            try {
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
            } catch (t: Throwable) {
                Log.e("ModoContraReloj", "tickRunnable error", t)
            }
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

        binding.txtPuntos.text = "Puntos: $puntos"
        binding.txtTimer.text = "${timeSeconds}s"
        binding.btnAyuda.setOnClickListener { mostrarPista() }
        binding.btnPausa.setOnClickListener { mostrarDialogoPausa() }

        startGame()
    }

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
        // importante: inicializamos la marca de inicio de la partida
        partidaStartMillis = System.currentTimeMillis()
        startTimer()
        Log.d("ModoContraReloj", "Nueva partida: palabra='$palabraActual' tiempo=${totalMillis/1000}s")
    }

    private fun startTimer() {
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
    }

    private fun stopTimer() {
        handler.removeCallbacks(tickRunnable)
    }

    private fun updateHangman() {
        val etapas = 8
        val tiempoTranscurrido = totalMillis - tiempoRestanteMillis
        val etapaActual = ((tiempoTranscurrido.toFloat() / totalMillis) * etapas).toInt().coerceIn(1, etapas)
        val resId = resources.getIdentifier("ahorcado_$etapaActual", "drawable", packageName)
        if (resId != 0) {
            binding.imgAhorcado.setImageResource(resId)
        } else {
            // si no encontró el recurso, logueamos (evita NullPointerByResource)
            Log.w("ModoContraReloj", "Drawable ahorcado_$etapaActual no encontrado.")
        }
    }

    private fun actualizarPalabraMostrada() {
        val mostrada = palabraActual.map { c -> if (letrasAdivinadas.contains(c)) c else '_' }.joinToString(" ")
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

    private fun manejarLetra(letra: Char, boton: MaterialButton) {
        if (palabraActual.contains(letra)) {
            letrasAdivinadas.add(letra)
            actualizarPalabraMostrada()
            verificarVictoria()
        } else {
            velocidadFactor = (velocidadFactor + 0.5f).coerceAtMost(velocidadMax)
        }
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
            stopTimer()
            desactivarTeclado()
            val puntosGanados = 10 + (binding.txtTimer.text.toString().replace("s","").toIntOrNull() ?: 0) / 5
            puntos += puntosGanados
            binding.txtPuntos.text = "Puntos: $puntos"
            val duracion = (System.currentTimeMillis() - partidaStartMillis) / 1000
            // guardado local + firestore
            mostrarDialogoResultado("¡Felicidades! Adivinaste la palabra.", puntosGanados = puntosGanados, gano = true)
            guardarPartidaEnFirestore(palabraActual, true, puntosGanados, duracion)
        }
    }

    private fun mostrarPista() {
        stopTimer()
        if (pistaUsada) { mostrarModalAdvertencia("Ya usaste la ayuda en esta ronda."); return }
        if (puntos < 10) { mostrarModalAdvertencia("Necesitás al menos 10 puntos para usar la ayuda."); return }

        puntos -= 10
        pistaUsada = true
        binding.txtPuntos.text = "Puntos: $puntos"

        val letrasDisponibles = palabraActual.toSet().filter { it !in letrasAdivinadas }
        if (letrasDisponibles.isEmpty()) { startTimer(); return }
        val letraAyuda = letrasDisponibles.random()

        val ayudaView = LayoutInflater.from(this).inflate(R.layout.dialog_ayuda, null)
        val txtAyuda = ayudaView.findViewById<TextView>(R.id.txtAyuda)
        txtAyuda.text = "¡Ayuda! Una letra de la palabra es: $letraAyuda"
        val dialog = AlertDialog.Builder(this)
            .setView(ayudaView)
            .setCancelable(true)
            .create()
        dialog.setOnDismissListener { startTimer() }
        dialog.show()
    }

    private fun mostrarDialogoResultado(mensaje: String, puntosGanados: Int = 0, gano: Boolean = false) {
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
            txtPuntos.text = "¡Ganaste $puntosGanados puntos! 👑"
        } else {
            txtPuntos.visibility = View.VISIBLE
            txtPuntos.text = "Palabra: $palabraActual ☠️"
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
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setDimAmount(0f)
        }
        dialog.show()
        desactivarTeclado()
    }

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
        val duracion = (System.currentTimeMillis() - partidaStartMillis) / 1000
        mostrarDialogoResultado("¡Se acabó el tiempo!", puntosGanados = 0, gano = false)
        guardarPartidaEnFirestore(palabraActual, false, 0, duracion)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        handler.removeCallbacksAndMessages(null)
    }

    private fun guardarPartidaEnFirestore(
        palabra: String,
        gano: Boolean,
        puntosGanados: Int,
        duracionSegundos: Long
    ) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val partida = hashMapOf(
            "palabra" to palabra,
            "resultado" to if (gano) "GANADA" else "PERDIDA",
            "puntos" to puntosGanados,
            "duracion" to duracionSegundos,
            "modo" to "ModoContraReloj",
            "fecha" to FieldValue.serverTimestamp()
        )

        db.collection("usuarios").document(uid).collection("partidas")
            .add(partida)
            .addOnSuccessListener {
                // Actualizamos estadísticas (transacción)
                actualizarEstadisticasUsuario(uid, gano, puntosGanados, duracionSegundos)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error guardando partida: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.w("ModoContraReloj", "guardarPartidaEnFirestore failed", e)
            }
    }

    private fun actualizarEstadisticasUsuario(uid: String, gano: Boolean, puntosGanados: Int, duracionSegundos: Long) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("usuarios").document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val puntosActuales = snapshot.getLong("puntosTotales") ?: 0L
            val ganadas = snapshot.getLong("partidasGanadas") ?: 0L
            val perdidas = snapshot.getLong("partidasPerdidas") ?: 0L
            val horasJugadas = snapshot.getDouble("horasJugadas") ?: 0.0

            val nuevosPuntos = puntosActuales + puntosGanados.toLong()
            val nuevasGanadas = ganadas + if (gano) 1L else 0L
            val nuevasPerdidas = perdidas + if (!gano) 1L else 0L
            val nuevasHoras = horasJugadas + (duracionSegundos.toDouble() / 3600.0)

            val nuevoNivel = calcularNivelDesdePuntos(nuevosPuntos)
            val updates: MutableMap<String, Any> = mutableMapOf(
                "puntosTotales" to nuevosPuntos,
                "partidasGanadas" to nuevasGanadas,
                "partidasPerdidas" to nuevasPerdidas,
                "horasJugadas" to nuevasHoras,
                "nivel" to nuevoNivel
            )

            transaction.update(userRef, updates)
            null
        }.addOnSuccessListener {
            Log.d("ModoContraReloj", "Estadísticas actualizadas correctamente.")
        }.addOnFailureListener { e ->
            Log.w("ModoContraReloj", "Error actualizando estadísticas: ${e.message}", e)
        }
    }

    private fun calcularNivelDesdePuntos(puntos: Long): Long {
        return when {
            puntos < 1000L -> 1L
            puntos < 3000L -> 2L
            puntos < 6000L -> 3L
            puntos < 10000L -> 4L
            else -> 5L
        }
    }
}
