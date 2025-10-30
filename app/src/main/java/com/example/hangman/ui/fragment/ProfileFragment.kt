package com.example.hangman.ui.fragment

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.hangman.R
import com.example.hangman.data.FirebaseService
import com.example.hangman.ui.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var selectedImageUri: Uri? = null

    private lateinit var txtNombre: TextView
    private lateinit var txtNivel: TextView
    private lateinit var txtPuntos: TextView
    private lateinit var txtGanadas: TextView
    private lateinit var txtPerdidas: TextView
    private lateinit var txtHoras: TextView
    private lateinit var imgPerfil: ImageView

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                imgPerfil.setImageURI(uri)
                FirebaseService.uploadProfileImage(auth.currentUser?.uid ?: return@let, uri,
                    onSuccess = { url ->
                        Glide.with(this).load(url).into(imgPerfil)
                        Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    })
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtNombre = view.findViewById(R.id.txtNombre)
        txtNivel = view.findViewById(R.id.txtNivel)
        txtPuntos = view.findViewById(R.id.txtPuntos)
        txtGanadas = view.findViewById(R.id.txtGanadas)
        txtPerdidas = view.findViewById(R.id.txtPerdidas)
        txtHoras = view.findViewById(R.id.txtHoras)
        imgPerfil = view.findViewById(R.id.imgPerfil)

        view.findViewById<AppCompatButton>(R.id.btnCerrarSesion).setOnClickListener {
            cerrarSesion()
        }

        imgPerfil.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK)
            gallery.type = "image/*"
            pickImageLauncher.launch(gallery)
        }

        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombreUsuario") ?: "Jugador"
                val puntos = doc.getLong("puntuacionTotal") ?: 0
                val ganadas = doc.getLong("partidasGanadas") ?: 0
                val perdidas = doc.getLong("partidasPerdidas") ?: 0
                val horas = doc.getDouble("horasJugadas") ?: 0.0
                val foto = doc.getString("imagenPerfil")

                txtNombre.text = nombre
                txtPuntos.text = puntos.toString()
                txtGanadas.text = ganadas.toString()
                txtPerdidas.text = perdidas.toString()
                txtHoras.text = String.format("%.1f h", horas)

                val nivel = calcularNivel(puntos)
                txtNivel.text = "Nivel $nivel"

                if (!foto.isNullOrEmpty()) {
                    Glide.with(this).load(foto).into(imgPerfil)
                } else {
                    imgPerfil.setImageBitmap(generateInitialsAvatar(nombre))
                }
            }
    }

    private fun generateInitialsAvatar(name: String): Bitmap {
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(2)
        val size = 200
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = android.graphics.Paint().apply {
            color = Color.parseColor("#8B5CF6")
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
            textSize = 80f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawColor(Color.parseColor("#8B5CF6"))
        canvas.drawText(initials, size / 2f, size / 1.5f, paint)
        return bmp
    }

    private fun cerrarSesion() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun calcularNivel(puntos: Long): Int {
        return when {
            puntos < 100 -> 1
            puntos < 250 -> 2
            puntos < 500 -> 3
            puntos < 1000 -> 4
            puntos < 2000 -> 5
            else -> 6
        }
    }
}
