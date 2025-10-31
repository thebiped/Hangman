package com.example.hangman.data

import android.net.Uri
import android.util.Log
import com.example.hangman.models.Partida
import com.example.hangman.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseService {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // 🔹 Subir foto de perfil
    fun uploadProfileImage(uid: String, uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val ref = storage.reference.child("profile_images/$uid.jpg")
        ref.putFile(uri)
            .continueWithTask { ref.downloadUrl }
            .addOnSuccessListener { onSuccess(it.toString()) }
            .addOnFailureListener { onFailure(it) }
    }

    // 🔹 Guardado atómico: usuario + partida
    fun guardarPartidaAtomic(resultado: String, palabra: String, puntosGanados: Int, duracionSegundos: Int) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("usuarios").document(uid)
        val partidaRef = userRef.collection("partidas").document()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            val partidasGanadas = snapshot.getLong("partidasGanadas") ?: 0
            val partidasPerdidas = snapshot.getLong("partidasPerdidas") ?: 0
            val puntosActuales = snapshot.getLong("puntos") ?: 0
            val horasJugadas = snapshot.getDouble("horasJugadas") ?: 0.0

            val nuevasHoras = horasJugadas + (duracionSegundos / 3600.0)
            val nuevosPuntos = if (resultado == "ganada") puntosActuales + puntosGanados else puntosActuales
            val nuevasGanadas = if (resultado == "ganada") partidasGanadas + 1 else partidasGanadas
            val nuevasPerdidas = if (resultado == "perdida") partidasPerdidas + 1 else partidasPerdidas
            val nuevoNivel = calcularNivelDesdePuntos(nuevosPuntos)

            // Actualizar usuario
            transaction.update(userRef, mapOf(
                "puntos" to nuevosPuntos,
                "partidasGanadas" to nuevasGanadas,
                "partidasPerdidas" to nuevasPerdidas,
                "horasJugadas" to nuevasHoras,
                "nivel" to nuevoNivel
            ))

            // Guardar partida
            transaction.set(partidaRef, mapOf(
                "resultado" to resultado,
                "palabra" to palabra,
                "puntos" to puntosGanados,
                "duracionSegundos" to duracionSegundos,
                "fecha" to System.currentTimeMillis()
            ))
        }.addOnSuccessListener {
            Log.d("FirebaseService", "✅ Partida guardada correctamente ($resultado)")
        }.addOnFailureListener { e ->
            Log.e("FirebaseService", "❌ Error al guardar partida: ${e.message}")
        }
    }

    private fun calcularNivelDesdePuntos(puntos: Long): Long {
        return when {
            puntos < 50 -> 1
            puntos < 100 -> 2
            puntos < 200 -> 3
            puntos < 350 -> 4
            else -> 5
        }
    }

    // 🔹 Obtener historial de partidas
    fun getHistorialPartidas(uid: String, onComplete: (List<Partida>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("usuarios").document(uid).collection("partidas")
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { snap ->
                val lista = snap.documents.mapNotNull { it.toObject(Partida::class.java) }
                onComplete(lista)
            }
            .addOnFailureListener { onError(it) }
    }

    // 🔹 Ranking global
    fun getRanking(onComplete: (List<UserData>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("usuarios")
            .orderBy("puntos", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val lista = result.documents.mapNotNull { it.toObject(UserData::class.java) }
                onComplete(lista)
            }
            .addOnFailureListener {
                Log.e("FirebaseService", "❌ Error al obtener ranking: ${it.message}")
                onError(it)
            }
    }
}
