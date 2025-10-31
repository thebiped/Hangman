package com.example.hangman.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.hangman.R
import com.example.hangman.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class RankingFragment : Fragment(R.layout.fragment_ranking) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var currentUserName: String? = null
    private var rankingListener: ListenerRegistration? = null

    // TextViews del header de estadísticas
    private var txtPuntos: TextView? = null
    private var txtPartidasGanadas: TextView? = null
    private var txtPartidasPerdidas: TextView? = null
    private var txtHorasJugadas: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencias al header de puntos
        txtPuntos = view.findViewById(R.id.txtPuntos)
        txtPartidasGanadas = view.findViewById(R.id.txtPartidasGanadas)
        txtPartidasPerdidas = view.findViewById(R.id.txtPartidasPerdidas)
        txtHorasJugadas = view.findViewById(R.id.txtHorasJugadas)

        val contenedorRanking = view.findViewById<LinearLayout>(R.id.contenedorTablaRanking)

        auth.currentUser?.uid?.let { uid ->
            // Obtener estadísticas del usuario
            db.collection("usuarios").document(uid).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("RankingFragment", "Error snapshot: ${e.message}")
                } else if (snapshot != null && snapshot.exists()) {
                    val ganadas = snapshot.getLong("partidasGanadas") ?: 0
                    val perdidas = snapshot.getLong("partidasPerdidas") ?: 0
                    val horas = snapshot.getDouble("horasJugadas") ?: 0.0
                    val puntos = snapshot.getLong("puntos") ?: 0
                    currentUserName = snapshot.getString("nombreUsuario")

                    txtPartidasGanadas?.text = ganadas.toString()
                    txtPartidasPerdidas?.text = perdidas.toString()
                    txtHorasJugadas?.text = "${"%.2f".format(horas)}hs"
                    txtPuntos?.text = "$puntos"
                }
            }

            cargarRanking(contenedorRanking, uid)
        } ?: cargarRanking(contenedorRanking, null)
    }

    private fun cargarRanking(contenedor: LinearLayout, currentUid: String?) {
        rankingListener?.remove()

        rankingListener = db.collection("usuarios")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("RankingFragment", "Error cargando ranking: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    contenedor.removeAllViews()
                    val listaUsuarios = snapshot.documents.mapNotNull { doc ->
                        try {
                            UserData(
                                uid = doc.id,
                                nombreUsuario = doc.getString("nombreUsuario") ?: "Sin Nombre",
                                email = doc.getString("email") ?: "",
                                imagenPerfil = doc.getString("imagenPerfil") ?: "",
                                partidasGanadas = doc.getLong("partidasGanadas") ?: 0L,
                                partidasPerdidas = doc.getLong("partidasPerdidas") ?: 0L,
                                horasJugadas = doc.getDouble("horasJugadas") ?: 0.0,
                                puntos = doc.getLong("puntos") ?: 0L,
                                nivel = doc.getLong("nivel") ?: 1L
                            )
                        } catch (ex: Exception) {
                            Log.e("RankingFragment", "Error parseando usuario: ${ex.message}")
                            null
                        }
                    }.sortedByDescending { it.puntos }

                    // Crear filas dinámicamente
                    listaUsuarios.forEachIndexed { index, user ->
                        val row = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(8, 8, 8, 8)
                            weightSum = 6f
                            if (currentUserName == user.nombreUsuario) {
                                setBackgroundResource(R.drawable.bg_ranking_item)
                                scaleX = 1.03f
                                scaleY = 1.03f
                            }
                        }

                        val medalla = when (index) {
                            0 -> "🥇 "
                            1 -> "🥈 "
                            2 -> "🥉 "
                            else -> ""
                        }

                        fun createTextView(text: String, weight: Float) = TextView(requireContext()).apply {
                            this.text = text
                            setTextColor(Color.WHITE)
                            gravity = Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                        }

                        // Posición
                        row.addView(createTextView("#${index + 1}", 0.5f))
                        // Nombre
                        row.addView(createTextView("$medalla${user.nombreUsuario}", 2f))
                        // Ganadas
                        row.addView(createTextView("${user.partidasGanadas}", 1f))
                        // Perdidas
                        row.addView(createTextView("${user.partidasPerdidas}", 1f))
                        // Puntos + porcentaje
                        val porcentaje = if ((user.partidasGanadas + user.partidasPerdidas) > 0)
                            (user.partidasGanadas * 100) / (user.partidasGanadas + user.partidasPerdidas)
                        else 0
                        row.addView(createTextView("${user.puntos} $porcentaje%", 1.5f))

                        contenedor.addView(row)
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rankingListener?.remove()
    }
}
