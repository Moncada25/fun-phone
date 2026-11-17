package com.bookverse.contacts

import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

data class Phone(
    val number: String,
    val type: String
)

data class Contact(
    val name: String,
    val phones: List<Phone>,
    val birthday: Long,
    val email: String,
    val company: String,
    val isFavorite: Boolean,
    val socialMedia: List<String>,
    val remotePhotoUri: String
)

fun randomPhone(): String {
    val prefixes = listOf("310", "311", "312", "313", "314", "315", "316", "317", "318", "319")
    return "+57${prefixes.random()}${Random.nextInt(1000000, 9999999)}"
}

fun randomBirthday(): Long {
    val startYear = 1970
    val endYear = 2005
    val year = Random.nextInt(startYear, endYear)
    val month = Random.nextInt(1, 13)
    val day = Random.nextInt(1, 28)
    return LocalDate.of(year, month, day)
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()
}

fun main() {
    val firstNames = listOf(
        "Juan",
        "María",
        "Andrés",
        "Camila",
        "Daniel",
        "Laura",
        "Carlos",
        "Sofía",
        "Mateo",
        "Valentina",
        "Javier",
        "Natalia",
        "Alejandro",
        "Paula",
        "Miguel",
        "Andrea",
        "Fernando",
        "Lucía",
        "Diego",
        "Ana"
    )

    val lastNames = listOf(
        "Gómez",
        "Rodríguez",
        "Martínez",
        "López",
        "Hernández",
        "Pérez",
        "García",
        "Torres",
        "Ramírez",
        "Flores",
        "Vargas",
        "Castro",
        "Rojas",
        "Morales",
        "Ortiz",
        "Medina",
        "Cruz",
        "Reyes",
        "Sánchez",
        "Navarro"
    )

    val companies = listOf(
        "Tech Solutions", "Creative Studio", "Digital Labs", "Consulting Group",
        "Marketing Agency", "Health Services", "Logistics Co", "Software House",
        "Design Studio", "Innovation Hub"
    )

    val contacts = mutableListOf<Contact>()

    repeat(1000) {
        val firstName = firstNames.random()
        val lastName = lastNames.random()
        val fullName = "$firstName $lastName"
        val email = "${firstName.lowercase()}.${lastName.lowercase()}@example.com"

        contacts.add(
            Contact(
                name = fullName,
                phones = listOf(
                    Phone(randomPhone(), "MOBILE"),
                    Phone(randomPhone(), "WORK")
                ),
                birthday = randomBirthday(),
                email = email,
                company = companies.random(),
                isFavorite = Random.nextBoolean(),
                socialMedia = emptyList(),
                remotePhotoUri = "null"
            )
        )
    }

    val json = buildString {
        append("[\n")
        contacts.forEachIndexed { index, c ->
            append(
                """
  {
    "name": "${c.name}",
    "phones": [
      { "number": "${c.phones[0].number}", "type": "MOBILE" },
      { "number": "${c.phones[1].number}", "type": "WORK" }
    ],
    "birthday": ${c.birthday},
    "email": "${c.email}",
    "company": "${c.company}",
    "isFavorite": ${c.isFavorite},
    "socialMedia": [],
    "remotePhotoUri": "null"
  }${if (index < contacts.size - 1) "," else ""}
"""
            )
        }
        append("]")
    }

    File("contacts_1000.json").writeText(json)
}