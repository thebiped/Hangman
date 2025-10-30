package com.example.hangman.models

data class UsuarioRanking(
    val posicion: Int = 0,
    val nombre: String = "",
    val horasJugadas: Double = 0.0,
    val fotoPerfil: String? = null,
    val nombreUsuario: String = "",
    val puntuacionTotal: Long = 0,
    val partidasGanadas: Long = 0,
    val partidasPerdidas: Long = 0,
    val imagenPerfil: String? = null,
)
