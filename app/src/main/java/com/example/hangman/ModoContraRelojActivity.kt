package com.example.hangman

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.hangman.data.FirebaseService
import com.example.hangman.databinding.ActivityModoContraRelojBinding
import com.example.hangman.models.Words
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

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
    private var nivelActual = 1

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!estaPausado) {
                tiempoRestanteMillis -= 100
                if (tiempoRestanteMillis <= 0) {
                    onTimeUp()
                    return
                }
                binding.txtTimer.text = "${(tiempoRestanteMillis / 1000)}s"
                actualizarAhorcadoFluido()
            }
            handler.postDelayed(this, 100)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModoContraRelojBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAyuda.setOnClickListener { mostrarPista() }
        binding.btnPausa.setOnClickListener { mostrarDialogoPausa() }

        obtenerNivelUsuario { nivel ->
            nivelActual = nivel
            cargarPuntos()
        }
    }

    private fun cargarPuntos() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    puntos = doc.getLong("puntos")?.toInt() ?: 0
                    binding.txtPuntos.text = puntos.toString()
                    startGame()
                }
        } else startGame()
    }

    private fun startGame() {
        palabraActual = Words.DICTIONARY.random().uppercase()
        letrasAdivinadas.clear()
        pistaUsada = false
        binding.imgAhorcado.setImageResource(R.drawable.ahorcado_1)
        binding.txtPuntos.text = puntos.toString()
        actualizarPalabraMostrada()
        generarTeclado()
        partidaStartMillis = System.currentTimeMillis()

        totalMillis = tiempoPorNivel(nivelActual)
        tiempoRestanteMillis = totalMillis
        binding.txtTimer.text = "${tiempoRestanteMillis / 1000}s"

        estaPausado = false
        handler.removeCallbacksAndMessages(null)
        handler.post(tickRunnable)
    }

    private fun tiempoPorNivel(nivel: Int): Long {
        return when (nivel) {
            1 -> 60000L
            2 -> 50000L
            3 -> 40000L
            4 -> 30000L
            else -> 20000L
        }
    }

    /** 🎯 Animación del ahorcado fluida según porcentaje de tiempo */
    private fun actualizarAhorcadoFluido() {
        val etapas = 8
        val porcentajeUsado = (totalMillis - tiempoRestanteMillis).toFloat() / totalMillis
        val etapaActual = ((porcentajeUsado * etapas).roundToInt()).coerceIn(1, etapas)
        val resId = resources.getIdentifier("ahorcado_$etapaActual", "drawable", packageName)
        binding.imgAhorcado.setImageResource(resId)
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
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4,4,4,4) }
                    setPadding(0,0,0,0)
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
        }
    }

    private fun verificarVictoria() {
        if (palabraActual.all { letrasAdivinadas.contains(it) }) {
            stopTimer()
            desactivarTeclado()
            val basePuntos = 10
            val bonusTiempo = calcularBonusTiempo()
            val puntosGanados = basePuntos + bonusTiempo
            sumarPuntos(puntosGanados)
            guardarPartida(true, puntosGanados)
            mostrarDialogoResultado("¡Felicidades! Adivinaste la palabra.", puntosGanados, true)
        }
    }

    /** 🎯 Bonus dinámico según tiempo restante y nivel */
    private fun calcularBonusTiempo(): Int {
        val factorNivel = 1 + (nivelActual - 1) * 0.5 // Nivel 1=1x, Nivel 2=1.5x, Nivel3=2x...
        val segundosRestantes = (tiempoRestanteMillis / 1000).toInt()
        return (segundosRestantes * factorNivel / 5).toInt() // cada 5s restantes = 1 punto * factorNivel
    }

    private fun onTimeUp() {
        stopTimer()
        desactivarTeclado()
        guardarPartida(false, 0)
        mostrarDialogoResultado("¡Tiempo agotado! La palabra era $palabraActual", 0, false)
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

    private fun mostrarDialogoResultado(mensaje: String, puntosGanados: Int, gano: Boolean) {
        handler.removeCallbacks(tickRunnable)
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
            setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_dialog_overlay))
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }

        dialog.show()
    }

    private fun mostrarDialogoPausa() {
        estaPausado = true
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pausa, null)
        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(dialogView)
        dialog.window?.apply {
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialogView.findViewById<Button>(R.id.btnContinuar).setOnClickListener {
            estaPausado = false
            handler.post(tickRunnable)
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btnLeave).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun mostrarPista() {
        if (pistaUsada) { mostrarModalAdvertencia("Ya usaste la ayuda en esta ronda."); return }
        if (puntos < 10) { mostrarModalAdvertencia("Necesitás al menos 10 puntos para usar la ayuda."); return }

        sumarPuntos(-10)
        pistaUsada = true

        val letrasDisponibles = palabraActual.toSet().filter { it !in letrasAdivinadas }
        if (letrasDisponibles.isEmpty()) { mostrarModalAdvertencia("Ya descubriste todas las letras de la palabra."); return }
        val letraAyuda = letrasDisponibles.random()

        val ayudaView = LayoutInflater.from(this).inflate(R.layout.dialog_ayuda, null)
        ayudaView.findViewById<TextView>(R.id.txtAyuda).text = "¡Ayuda! Una letra de la palabra es: $letraAyuda"
        AlertDialog.Builder(this, R.style.CustomAlertDialogStyle).setView(ayudaView).setCancelable(true).create().show()
    }

    private fun mostrarModalAdvertencia(mensaje: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_advertencia, null)
        view.findViewById<TextView>(R.id.txtMensajeAdvertencia).text = mensaje
        val btnCerrar = view.findViewById<Button>(R.id.btnCerrar)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        btnCerrar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun sumarPuntos(cantidad: Int) {
        puntos += cantidad
        binding.txtPuntos.text = puntos.toString()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("usuarios").document(uid)
                .update("puntos", puntos)
                .addOnSuccessListener { Log.d("ModoCR", "✅ Puntos actualizados correctamente") }
                .addOnFailureListener { e -> Log.e("ModoCR", "❌ Error al guardar puntos: ${e.message}") }
        }
    }

    private fun guardarPartida(gano: Boolean, puntosGanados: Int) {
        val duracion = ((System.currentTimeMillis() - partidaStartMillis) / 1000).toInt()
        val estado = if (gano) "ganada" else "perdida"
        FirebaseService.guardarPartidaAtomic(estado, palabraActual, puntosGanados, duracion)

        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("usuarios").document(uid)
        if (!gano) docRef.update("partidasPerdidas", FieldValue.increment(1))
            .addOnFailureListener { e -> Log.e("ModoCR", "Error al actualizar partidas perdidas: ${e.message}") }
    }

    private fun obtenerNivelUsuario(callback: (Int) -> Unit) {
        val uid = auth.currentUser?.uid ?: return callback(1)
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc -> callback((doc.getLong("nivel") ?: 1L).toInt()) }
            .addOnFailureListener { Log.e("ModoCR", "Error al obtener nivel: ${it.message}"); callback(1) }
    }

    private fun stopTimer() {
        handler.removeCallbacks(tickRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
