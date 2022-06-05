package com.erfangc.esdc

import java.time.LocalDate

data class Person(
    val name: String,
    val age: Int?,
    val birthDate: LocalDate?,
    val coins: Double,
    val label: String,
)
