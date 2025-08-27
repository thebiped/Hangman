package com.example.hangman.models

object Words {
    val DICTIONARY = listOf(
        "casa", "perro", "gato", "sol", "luz", "flor", "mar", "pan", "voz", "paz",
        "cielo", "libro", "verde", "mesa", "silla", "nube", "piedra", "fuego", "lluvia", "viento",
        "correr", "camino", "puerta", "montaña", "bosque", "rio", "playa", "barco", "sombra", "estrella",
        "ventana", "escuela", "maestro", "jardin", "ciudad", "paisaje", "fruta", "cultura", "tiempo", "historia",
        "naturaleza", "universo", "computadora", "teléfono", "bicicleta", "canción", "musica", "felicidad", "amistad", "familia",
        "trabajo", "problema", "solución", "respuesta", "pregunta", "aventura", "memoria", "recuerdo", "verdad", "mentira",
        "corazón", "emocion", "silencio", "ruido", "peligro", "misterio", "secreto", "fantasma", "dragón", "planeta"
    )

    fun getWordsByDifficulty(level: Int): List<String> {
        return when (level) {
            1 -> DICTIONARY.filter { it.length <= 4 } // Fácil
            2 -> DICTIONARY.filter { it.length in 5..6 } // Medio
            3 -> DICTIONARY.filter { it.length >= 7 } // Difícil
            else -> DICTIONARY
        }
    }
}
