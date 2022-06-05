package com.erfangc.esdc

import kotlin.reflect.KProperty1

data class ParsedQuery<T>(
    val property: KProperty1<out T, *>,
    val propertyName: String,
    val values: List<String> = emptyList(),
    val lowerLimit: String? = null,
    val upperLimit: String? = null,
    val queryType: QueryType,
)