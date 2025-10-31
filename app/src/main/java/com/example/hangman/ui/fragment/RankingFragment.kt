package com.example.hangman.ui.fragment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.hangman.R
import com.example.hangman.data.FirebaseService
import com.example.hangman.models.UsuarioRanking

class RankingFragment : Fragment() {

    private lateinit var contenedorRanking: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ranking, container, false)
        // Agregamos un LinearLayout vacío que será el contenedor dinámico
        contenedorRanking = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tablaRanking = view.findViewById<LinearLayout>(R.id.contenedorTablaRanking)
        tablaRanking.addView(contenedorRanking)

        cargarRanking()
        return view
    }
    private fun cargarRanking() {
        FirebaseService.getRanking(
            onComplete = { listaUsuarios ->
                contenedorRanking.removeAllViews()

                val listaOrdenada = listaUsuarios.sortedByDescending { it.puntuacionTotal }

                for ((index, user) in listaOrdenada.withIndex()) {
                    val item = layoutInflater.inflate(R.layout.ranking_item, contenedorRanking, false)

                    val txtPosicion = item.findViewById<TextView>(R.id.tvPosicion)
                    val txtNombre = item.findViewById<TextView>(R.id.tvNombreJugador)
                    val txtPuntos = item.findViewById<TextView>(R.id.tvPuntos)
                    val txtBonus = item.findViewById<TextView>(R.id.tvBonus)
                    val txtTotal = item.findViewById<TextView>(R.id.tvTotal)
                    val imgPerfil = item.findViewById<ImageView>(R.id.imgAvatar)

                    txtPosicion.text = "#${index + 1}"
                    txtNombre.text = user.nombreUsuario
                    txtPuntos.text = "${user.partidasGanadas}"
                    txtBonus.text = "${user.partidasPerdidas}"
                    val porcentajeGanadas = if (user.partidasGanadas + user.partidasPerdidas > 0)
                        (user.partidasGanadas * 100) / (user.partidasGanadas + user.partidasPerdidas) else 0
                    txtTotal.text = "${user.puntuacionTotal} pts | $porcentajeGanadas% WIN"

                    if (!user.imagenPerfil.isNullOrEmpty()) {
                        Glide.with(this).load(user.imagenPerfil).into(imgPerfil)
                    } else {
                        imgPerfil.setImageBitmap(generateInitialsAvatar(user.nombreUsuario))
                    }

                    contenedorRanking.addView(item)
                }
            },
            onError = { e -> e.printStackTrace() }
        )
    }


    private fun generateInitialsAvatar(name: String): Bitmap {
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(2)
        val size = 150
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = android.graphics.Paint().apply {
            color = Color.parseColor("#8B5CF6")
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
            textSize = 60f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawColor(Color.parseColor("#8B5CF6"))
        canvas.drawText(initials, size / 2f, size / 1.5f, paint)
        return bmp
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
