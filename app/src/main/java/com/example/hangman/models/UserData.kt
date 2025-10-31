package com.example.hangman.models

import java.io.Serializable

data class UserData(
    val uid: String = "",
    val nombreUsuario: String = "",
    val email: String = "",
    val imagenPerfil: String = "",
    val partidasGanadas: Long = 0,
    val partidasPerdidas: Long = 0,
    val horasJugadas: Double = 0.0,
    val puntos: Long = 0,
    val nivel: Long = 1
) : Serializable
