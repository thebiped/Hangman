package com.example.hangman.ui.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.*
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
    private lateinit var txtDescripcion: TextView
    private lateinit var txtNivel: TextView
    private lateinit var txtPuntos: TextView
    private lateinit var txtGanadas: TextView
    private lateinit var txtPerdidas: TextView
    private lateinit var txtHoras: TextView
    private lateinit var imgPerfil: ImageView
    private lateinit var contenedorHistorial: LinearLayout

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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
        txtDescripcion = view.findViewById(R.id.txtDescripcion)
        txtNivel = view.findViewById(R.id.txtNivel)
        txtPuntos = view.findViewById(R.id.txtPuntosTotales)
        txtGanadas = view.findViewById(R.id.txtPartidasGanadas)
        txtPerdidas = view.findViewById(R.id.txtPartidasPerdidas)
        txtHoras = view.findViewById(R.id.txtHorasJugadas)
        imgPerfil = view.findViewById(R.id.imgPerfil)
        contenedorHistorial = view.findViewById(R.id.contenedorHistorial)

        view.findViewById<AppCompatButton>(R.id.btnCerrarSesion).setOnClickListener {
            cerrarSesion()
        }

        view.findViewById<AppCompatButton>(R.id.btnEditarPerfil).setOnClickListener {
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
                txtDescripcion.text = "Descripción vacía"
                txtPuntos.text = puntos.toString()
                txtGanadas.text = ganadas.toString()
                txtPerdidas.text = perdidas.toString()
                txtHoras.text = String.format("%.1f h", horas)

                val nivel = calcularNivel(puntos)
                txtNivel.text = "Nivel $nivel"
                // Anima la barra de nivel según el progreso
                val barraNivel = view?.findViewById<View>(R.id.barraNivel)
                val txtDificultad = view?.findViewById<TextView>(R.id.txtDificultad)
                val contenedor = view?.findViewById<RelativeLayout>(R.id.barraNivelContainer)

                contenedor?.post {
                    val maxWidth = contenedor.width - 4 // margen
                    val porcentaje = when (nivel) {
                        1 -> 0.3f
                        2 -> 0.5f
                        3 -> 0.7f
                        4 -> 0.9f
                        else -> 1.0f
                    }

                    val targetWidth = (maxWidth * porcentaje).toInt()
                    val anim = android.view.animation.ScaleAnimation(
                        0f, porcentaje,
                        1f, 1f,
                        android.view.animation.Animation.RELATIVE_TO_SELF, 0f,
                        android.view.animation.Animation.RELATIVE_TO_SELF, 0f
                    ).apply {
                        duration = 800
                        fillAfter = true
                    }
                    barraNivel?.startAnimation(anim)

                    txtDificultad?.text = when (nivel) {
                        1 -> "Principiante"
                        2 -> "Normal"
                        3 -> "Avanzado"
                        4 -> "Experto"
                        else -> "Maestro"
                    }
                }


                if (!foto.isNullOrEmpty()) {
                    Glide.with(this).load(foto).into(imgPerfil)
                } else {
                    imgPerfil.setImageBitmap(generateInitialsAvatar(nombre))
                }

                // Historial de partidas recientes
                FirebaseService.getHistorialPartidas(
                    uid,
                    onComplete = { partidas: List<com.example.hangman.models.Partida> ->
                        contenedorHistorial.removeAllViews()
                        for (partida in partidas.take(5)) {
                            val fila = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.HORIZONTAL
                                setPadding(16, 8, 16, 8)
                            }

                            val estado = TextView(requireContext()).apply {
                                text = partida.resultado.replaceFirstChar { it.uppercase() }
                                setTextColor(
                                    if (partida.resultado == "ganada") Color.parseColor("#EC4899")
                                    else Color.parseColor("#EF4444")
                                )
                                setTypeface(null, Typeface.BOLD)
                                setPadding(0, 0, 10, 0)
                            }

                            val palabra = TextView(requireContext()).apply {
                                text = "Palabra: ${partida.palabra}"
                                setTextColor(Color.WHITE)
                                setPadding(0, 0, 10, 0)
                            }

                            val puntosTxt = TextView(requireContext()).apply {
                                text = "+${partida.puntosGanados}Pts"
                                setTextColor(Color.WHITE)
                                setTypeface(null, Typeface.BOLD)
                            }

                            fila.addView(estado)
                            fila.addView(palabra)
                            fila.addView(puntosTxt)
                            contenedorHistorial.addView(fila)
                        }
                    },
                    onError = { e: Exception ->
                        Toast.makeText(requireContext(), "Error al cargar historial: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
    }

    private fun generateInitialsAvatar(name: String): Bitmap {
        val inicial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val size = 250
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint().apply {
            color = Color.parseColor("#8B5CF6") // violeta
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 100f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(inicial, (canvas.width / 2f), yPos, textPaint)
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
