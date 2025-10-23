package com.example.hangman

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hangman.databinding.ActivityTematicaBinding

class TematicaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTematicaBinding
    private var temaSeleccionado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Inicializa la pantalla de selección de temática
        super.onCreate(savedInstanceState)
        binding = ActivityTematicaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura interacciones generales de botones
        binding.btnCerrarTematica.setOnClickListener { finish() }
        binding.btnAnimales.setOnClickListener { showConfirmDialog("animales") }
        binding.btnDeportes.setOnClickListener { showConfirmDialog("deportes") }
        binding.btnComidas.setOnClickListener { showConfirmDialog("comidas") }
        binding.btnCiencia.setOnClickListener { showConfirmDialog("naturaleza") }
        binding.btnTecnologia.setOnClickListener { showConfirmDialog("tecnologia") }
        binding.btnEmociones.setOnClickListener { showConfirmDialog("emociones") }
    }

    // Muestra un diálogo de confirmación para la temática seleccionada
    private fun showConfirmDialog(tematica: String) {
        temaSeleccionado = tematica

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)
        val txtConfirm = dialogView.findViewById<android.widget.TextView>(R.id.txtConfirmacion)
        txtConfirm.text = "¿Querés jugar ahora en la temática «${tematica.replaceFirstChar { it.uppercase() }}\"?"

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<android.widget.Button>(R.id.btnCancelar).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.btnContinuar).setOnClickListener {
            dialog.dismiss()
            abrirModo(tematica)
        }

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(android.view.Gravity.CENTER)
        }

        dialog.show()
    }

    // Redirige a la pantalla de juego con la temática seleccionada
    private fun abrirModo(tematica: String) {
        val intent = Intent(this, ModoTematicaActivity::class.java)
        intent.putExtra("tematica", tematica)
        startActivity(intent)
    }
}
