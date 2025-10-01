package com.example.hangman

data class Partida(val uid: String = "",
                   val resultado: String = "", // "ganada" o "perdida"
                   val palabra: String = "",
                   val puntosGanados: Int = 0,
                   val duracionSegundos: Int = 0,
                   val fecha: Long = System.currentTimeMillis())
