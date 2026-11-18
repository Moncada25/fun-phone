package com.bookverse.contacts.data.hangman

import java.util.Locale

object HangmanWords {
    const val COUNTRIES = "country"
    const val ANIMALS = "animal"
    const val COLORS = "color"
    const val FRUITS = "fruit"
    const val PROFESSIONS = "profession"
    const val SPORTS = "sport"

    val categories = listOf(ANIMALS, COLORS, COUNTRIES, FRUITS, PROFESSIONS, SPORTS)

    private val wordsEs = mapOf(
        ANIMALS to listOf(
            "gato", "perro", "raton", "caballo", "jirafa", "elefante", "leon", "tigre", "conejo", "zorro", "oso", "vaca", "cerdo", "oveja", "mono", "pato", "gallina", "burro", "lobo", "ardilla"
        ),
        COLORS to listOf(
            "rojo", "azul", "verde", "amarillo", "morado", "naranja", "negro", "blanco", "gris", "rosa", "turquesa", "beige", "dorado", "plateado", "lila", "fucsia", "cian", "marfil", "vino", "oliva"
        ),
        COUNTRIES to listOf(
            "mexico", "canada", "brasil", "colombia", "francia", "alemania", "italia", "china", "japon", "rusia", "argentina", "chile", "peru", "espana", "portugal", "india", "egipto", "suecia", "noruega", "grecia"
        ),
        FRUITS to listOf(
            "manzana", "pera", "platano", "naranja", "sandia", "melon", "uva", "kiwi", "fresa", "cereza", "mango", "papaya", "limon", "pina", "durazno", "ciruela", "granada", "higo", "coco", "guayaba"
        ),
        PROFESSIONS to listOf(
            "doctor", "ingeniero", "abogado", "maestro", "arquitecto", "panadero", "bombero", "policia", "enfermero", "carpintero", "pintor", "mecanico", "electricista", "dentista", "chef", "actor", "cantante", "escritor", "periodista", "piloto"
        ),
        SPORTS to listOf(
            "futbol", "beisbol", "tenis", "baloncesto", "natacion", "ciclismo", "voleibol", "golf", "boxeo", "rugby", "hockey", "atletismo", "surf", "esgrima", "remo", "escalada", "judo", "karate", "ajedrez", "patinaje"
        )
    )

    private val wordsEn = mapOf(
        ANIMALS to listOf(
            "cat", "dog", "mouse", "horse", "giraffe", "elephant", "lion", "tiger", "rabbit", "fox", "bear", "cow", "pig", "sheep", "monkey", "duck", "hen", "donkey", "wolf", "squirrel"
        ),
        COLORS to listOf(
            "red", "blue", "green", "yellow", "purple", "orange", "black", "white", "gray", "pink", "turquoise", "beige", "gold", "silver", "lilac", "fuchsia", "cyan", "ivory", "wine", "olive"
        ),
        COUNTRIES to listOf(
            "mexico", "canada", "brazil", "colombia", "france", "germany", "italy", "china", "japan", "russia", "argentina", "chile", "peru", "spain", "portugal", "india", "egypt", "sweden", "norway", "greece"
        ),
        FRUITS to listOf(
            "apple", "pear", "banana", "orange", "watermelon", "melon", "grape", "kiwi", "strawberry", "cherry", "mango", "papaya", "lemon", "pineapple", "peach", "plum", "pomegranate", "fig", "coconut", "guava"
        ),
        PROFESSIONS to listOf(
            "doctor", "engineer", "lawyer", "teacher", "architect", "baker", "firefighter", "police", "nurse", "carpenter", "painter", "mechanic", "electrician", "dentist", "chef", "actor", "singer", "writer", "journalist", "pilot"
        ),
        SPORTS to listOf(
            "soccer", "baseball", "tennis", "basketball", "swimming", "cycling", "volleyball", "golf", "boxing", "rugby", "hockey", "athletics", "surfing", "fencing", "rowing", "climbing", "judo", "karate", "chess", "skating"
        )
    )

    private val wordsDe = mapOf(
        ANIMALS to listOf(
            "katze", "hund", "maus", "pferd", "giraffe", "elefant", "loewe", "tiger", "kaninchen", "fuchs", "baer", "kuh", "schwein", "schaf", "affe", "ente", "huhn", "esel", "wolf", "eichhoernchen"
        ),
        COLORS to listOf(
            "rot", "blau", "gruen", "gelb", "lila", "orange", "schwarz", "weiss", "grau", "rosa", "tuerkis", "beige", "gold", "silber", "violett", "pink", "cyan", "elfenbein", "weinrot", "oliv"
        ),
        COUNTRIES to listOf(
            "mexiko", "kanada", "brasilien", "kolumbien", "frankreich", "deutschland", "italien", "china", "japan", "russland", "argentinien", "chile", "peru", "spanien", "portugal", "indien", "aegypten", "schweden", "norwegen", "griechenland"
        ),
        FRUITS to listOf(
            "apfel", "birne", "banane", "orange", "wassermelone", "melone", "traube", "kiwi", "erdbeere", "kirsche", "mango", "papaya", "zitrone", "ananas", "pfirsich", "pflaume", "granatapfel", "feige", "kokosnuss", "guave"
        ),
        PROFESSIONS to listOf(
            "arzt", "ingenieur", "anwalt", "lehrer", "architekt", "baecker", "feuerwehrmann", "polizist", "krankenpfleger", "tischler", "maler", "mechaniker", "elektriker", "zahnarzt", "koch", "schauspieler", "saenger", "schriftsteller", "journalist", "pilot"
        ),
        SPORTS to listOf(
            "fussball", "baseball", "tennis", "basketball", "schwimmen", "radfahren", "volleyball", "golf", "boxen", "rugby", "hockey", "leichtathletik", "surfen", "fechten", "rudern", "klettern", "judo", "karate", "schach", "eislaufen"
        )
    )

    private val wordsFr = mapOf(
        ANIMALS to listOf(
            "chat", "chien", "souris", "cheval", "girafe", "elephant", "lion", "tigre", "lapin", "renard", "ours", "vache", "cochon", "mouton", "singe", "canard", "poule", "ane", "loup", "ecureuil"
        ),
        COLORS to listOf(
            "rouge", "bleu", "vert", "jaune", "violet", "orange", "noir", "blanc", "gris", "rose", "turquoise", "beige", "or", "argente", "lilas", "fuchsia", "cyan", "ivoire", "bordeaux", "olive"
        ),
        COUNTRIES to listOf(
            "mexique", "canada", "bresil", "colombie", "france", "allemagne", "italie", "chine", "japon", "russie", "argentine", "chili", "perou", "espagne", "portugal", "inde", "egypte", "suede", "norvege", "grece"
        ),
        FRUITS to listOf(
            "pomme", "poire", "banane", "orange", "pasteque", "melon", "raisin", "kiwi", "fraise", "cerise", "mangue", "papaye", "citron", "ananas", "peche", "prune", "grenade", "figue", "noixdecoco", "goyave"
        ),
        PROFESSIONS to listOf(
            "medecin", "ingenieur", "avocat", "professeur", "architecte", "boulanger", "pompier", "policier", "infirmier", "charpentier", "peintre", "mecanicien", "electricien", "dentiste", "cuisinier", "acteur", "chanteur", "ecrivain", "journaliste", "pilote"
        ),
        SPORTS to listOf(
            "football", "baseball", "tennis", "basketball", "natation", "cyclisme", "volleyball", "golf", "boxe", "rugby", "hockey", "athletisme", "surf", "escrime", "aviron", "escalade", "judo", "karate", "echecs", "patinage"
        )
    )

    private val wordsPt = mapOf(
        ANIMALS to listOf(
            "gato", "cachorro", "rato", "cavalo", "girafa", "elefante", "leao", "tigre", "coelho", "raposa", "urso", "vaca", "porco", "ovelha", "macaco", "pato", "galinha", "burro", "lobo", "esquilo"
        ),
        COLORS to listOf(
            "vermelho", "azul", "verde", "amarelo", "roxo", "laranja", "preto", "branco", "cinza", "rosa", "turquesa", "bege", "dourado", "prateado", "lilas", "fucsia", "ciano", "marfim", "vinho", "oliva"
        ),
        COUNTRIES to listOf(
            "mexico", "canada", "brasil", "colombia", "franca", "alemanha", "italia", "china", "japao", "russia", "argentina", "chile", "peru", "espanha", "portugal", "india", "egito", "suecia", "noruega", "grecia"
        ),
        FRUITS to listOf(
            "maca", "pera", "banana", "laranja", "melancia", "melao", "uva", "kiwi", "morango", "cereja", "manga", "mamao", "limao", "abacaxi", "pessego", "ameixa", "roma", "figo", "coco", "goiaba"
        ),
        PROFESSIONS to listOf(
            "medico", "engenheiro", "advogado", "professor", "arquiteto", "padeiro", "bombeiro", "policial", "enfermeiro", "carpinteiro", "pintor", "mecanico", "eletricista", "dentista", "cozinheiro", "ator", "cantor", "escritor", "jornalista", "piloto"
        ),
        SPORTS to listOf(
            "futebol", "beisebol", "tenis", "basquete", "natacao", "ciclismo", "voleibol", "golfe", "boxe", "rugbi", "hockey", "atletismo", "surfe", "esgrima", "remo", "escalada", "judo", "carate", "xadrez", "patinacao"
        )
    )

    private val wordsByLanguage = mapOf(
        "en" to wordsEn,
        "es" to wordsEs,
        "de" to wordsDe,
        "fr" to wordsFr,
        "pt" to wordsPt,
    )

    fun getWords(category: String, locale: Locale): List<String> {
        return wordsByLanguage[locale.language.lowercase(Locale.ROOT)]?.get(category)
            ?: wordsEn[category].orEmpty()
    }

    fun getRandomWord(category: String, locale: Locale): String =
        getWords(category, locale).randomOrNull().orEmpty()
}
