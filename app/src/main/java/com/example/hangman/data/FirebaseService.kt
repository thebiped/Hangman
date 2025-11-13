package com.example.hangman.data

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.android.gms.tasks.Task
import com.example.hangman.models.Partida
import com.example.hangman.models.UsuarioRanking

object FirebaseService {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // -------------------------------------------------------------------------------------------
    // Guarda una nueva partida y actualiza las estadísticas del usuario
    // -------------------------------------------------------------------------------------------
    fun guardarPartida(resultado: String, palabra: String, puntosGanados: Int, duracionSegundos: Int = 0) {
        val uid = auth.currentUser?.uid ?: return
        val partida = Partida(
            uid = uid,
            resultado = resultado,
            palabra = palabra,
            puntosGanados = puntosGanados,
            duracionSegundos = duracionSegundos,
            fecha = System.currentTimeMillis()
        )

        val partidasRef = db.collection("partidas")
        partidasRef.add(partida)
            .addOnSuccessListener {
                Log.d("FirebaseService", "✅ Partida guardada correctamente")
                actualizarEstadisticas(uid, resultado, puntosGanados, duracionSegundos)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseService", "❌ Error al guardar partida: ${e.message}")
            }
    }

    // -------------------------------------------------------------------------------------------
    // Actualiza estadísticas acumuladas del usuario
    // -------------------------------------------------------------------------------------------
    private fun actualizarEstadisticas(uid: String, resultado: String, puntosGanados: Int, duracionSegundos: Int) {
        val userRef = db.collection("usuarios").document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val partidasGanadas = (snapshot.getLong("partidasGanadas") ?: 0) + if (resultado == "ganada") 1 else 0
            val partidasPerdidas = (snapshot.getLong("partidasPerdidas") ?: 0) + if (resultado == "perdida") 1 else 0
            val horasJugadas = (snapshot.getDouble("horasJugadas") ?: 0.0) + (duracionSegundos / 3600.0)
            val puntuacionTotal = (snapshot.getLong("puntuacionTotal") ?: 0) + puntosGanados

            transaction.update(userRef, mapOf(
                "partidasGanadas" to partidasGanadas,
                "partidasPerdidas" to partidasPerdidas,
                "horasJugadas" to horasJugadas,
                "puntuacionTotal" to puntuacionTotal
            ))
        }.addOnSuccessListener {
            Log.d("FirebaseService", "✅ Estadísticas actualizadas correctamente")
        }.addOnFailureListener { e ->
            Log.e("FirebaseService", "❌ Error al actualizar estadísticas: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------------------------
    // Sube imagen de perfil y actualiza URL en Firestore
    // -------------------------------------------------------------------------------------------
    fun uploadProfileImage(uid: String, imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val ref = storage.reference.child("profile_images/$uid.jpg")
        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val url = uri.toString()
                    db.collection("usuarios").document(uid).update("imagenPerfil", url)
                        .addOnSuccessListener {
                            Log.d("FirebaseService", "✅ Imagen subida y URL actualizada")
                            onSuccess(url)
                        }
                        .addOnFailureListener(onFailure)
                }
            }
            .addOnFailureListener(onFailure)
    }

    // -------------------------------------------------------------------------------------------
    // Obtiene el ranking de los mejores jugadores
    // -------------------------------------------------------------------------------------------
    fun getRanking(limit: Long = 10, onComplete: (List<UsuarioRanking>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("usuarios")
            .orderBy("puntuacionTotal", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshot ->
                val ranking = snapshot.documents.mapIndexed { index, doc ->
                    UsuarioRanking(
                        posicion = index + 1,
                        nombre = doc.getString("nombreUsuario") ?: "Jugador",
                        puntuacionTotal = doc.getLong("puntuacionTotal") ?: 0,
                        partidasGanadas = doc.getLong("partidasGanadas") ?: 0,
                        partidasPerdidas = doc.getLong("partidasPerdidas") ?: 0,
                        horasJugadas = doc.getDouble("horasJugadas") ?: 0.0,
                        fotoPerfil = doc.getString("imagenPerfil")
                    )
                }
                onComplete(ranking)
            }
            .addOnFailureListener(onError)
    }

    // -------------------------------------------------------------------------------------------
    // Obtiene el historial de partidas de un usuario
    // -------------------------------------------------------------------------------------------
    fun getHistorialPartidas(uid: String, onComplete: (List<Partida>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("partidas")
            .whereEqualTo("uid", uid)
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val partidas = snapshot.documents.mapNotNull { it.toObject(Partida::class.java) }
                onComplete(partidas)
            }
            .addOnFailureListener(onError)
    }
}
