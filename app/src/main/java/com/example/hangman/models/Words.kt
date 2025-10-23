package com.example.hangman.models

object Words {
    // Diccionario principal de palabras
    val DICTIONARY = listOf(
        "casa", "perro", "gato", "sol", "luz", "flor", "mar", "pan", "voz", "paz",
        "cielo", "libro", "verde", "mesa", "silla", "nube", "piedra", "fuego", "lluvia", "viento",
        "correr", "camino", "puerta", "montaña", "bosque", "rio", "playa", "barco", "sombra", "estrella",
        "ventana", "escuela", "maestro", "jardin", "ciudad", "paisaje", "fruta", "cultura", "tiempo", "historia",
        "naturaleza", "universo", "computadora", "teléfono", "bicicleta", "canción", "musica", "felicidad", "amistad", "familia",
        "trabajo", "problema", "solución", "respuesta", "pregunta", "aventura", "memoria", "recuerdo", "verdad", "mentira",
        "corazón", "emocion", "silencio", "ruido", "peligro", "misterio", "secreto", "fantasma", "dragón", "planeta"
    )

    // Devuelve palabras filtradas según el nivel de dificultad
    fun getWordsByDifficulty(level: Int): List<String> {
        return when (level) {
            1 -> DICTIONARY.filter { it.length <= 4 }
            2 -> DICTIONARY.filter { it.length in 5..6 }
            3 -> DICTIONARY.filter { it.length >= 7 }
            else -> DICTIONARY
        }
    }

    // Devuelve palabras filtradas según la categoría seleccionada
    fun getWordsByCategory(category: String): List<String> {
        return when(category.lowercase()) {
            "animales" -> listOf("perro", "gato", "elefante", "jirafa", "dragón")
            "deportes" -> listOf("futbol", "tenis", "natacion", "basket", "golf")
            "comidas" -> listOf("pizza", "hamburguesa", "ensalada", "pasta", "tarta")
            "naturaleza" -> listOf("rio", "montaña", "bosque", "estrella", "nube")
            "tecnologia" -> listOf("computadora", "telefono", "bicicleta", "robot", "internet")
            "emociones" -> listOf("felicidad", "amistad", "amor", "miedo", "tristeza")
            else -> DICTIONARY
        }
    }
}

