# elastic-dataclass

Declares a Kotlin data class

```kotlin
data class Person(
    val name: String,
    val age: Int?,
    val birthDate: LocalDate?,
    val coins: Double,
    val label: String,
)
```

Instantiate a Mapper class

```kotlin
val dataClassToElasticMapper = DataClassToElasticMapper(Person::class)
```

Convert a `Map<String, List<String>>` which mirrors in shape to a HTTP request's query params

```kotlin
val queryBuilder = dataClassToElasticMapper.convert(
    mapOf(
        "birthDateFrom" to listOf("2020-12-31"),
        "birthDateTo" to listOf("2022-12-31"),
        "age" to listOf("12"),
        "nameMatch" to listOf("John"),
        "label" to listOf("Foo", "Bar")
    )
)
```
