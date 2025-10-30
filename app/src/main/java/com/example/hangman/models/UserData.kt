package com.example.hangman.models

/**
 * Modelo de datos del usuario guardado en Firestore.
 */
data class UserData(
    val nombreUsuario: String = "",
    val email: String = "",
    val fotoPerfilUrl: String = "",
    val partidasGanadas: Int = 0,
    val partidasPerdidas: Int = 0,
    val horasJugadas: Int = 0,
    val puntosTotales: Int = 0
)
