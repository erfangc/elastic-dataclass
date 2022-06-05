package com.erfangc.esdc

import kotlin.reflect.KProperty1

data class StrippedParam<T>(
    val propertyName: String,
    val property: KProperty1<out T, *>,
    val values: List<String>,
    val suffix: Suffix?,
    val keyword: Boolean,
    val queryType: QueryType,
)