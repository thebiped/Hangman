package com.example.hangman.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object GameStatsManager {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun actualizarEstadisticas(
        puntosGanados: Int,
        gano: Boolean,
        duracionSegundos: Long
    ) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("usuarios").document(uid)

        db.runTransaction { t ->
            val snapshot = t.get(userRef)
            val puntos = snapshot.getLong("puntosTotales") ?: 0
            val ganadas = snapshot.getLong("partidasGanadas") ?: 0
            val perdidas = snapshot.getLong("partidasPerdidas") ?: 0
            val horas = snapshot.getDouble("horasJugadas") ?: 0.0

            val nuevosPuntos = puntos + puntosGanados
            val nuevoNivel = calcularNivelDesdePuntos(nuevosPuntos)

            t.update(userRef, mapOf(
                "puntosTotales" to nuevosPuntos,
                "partidasGanadas" to ganadas + if (gano) 1 else 0,
                "partidasPerdidas" to perdidas + if (!gano) 1 else 0,
                "horasJugadas" to horas + (duracionSegundos / 3600.0),
                "nivel" to nuevoNivel
            ))
        }
    }

    fun calcularNivelDesdePuntos(puntos: Long): Long {
        return when {
            puntos < 50 -> 1
            puntos < 100 -> 2
            puntos < 200 -> 3
            puntos < 350 -> 4
            else -> 5
        }
    }
}
