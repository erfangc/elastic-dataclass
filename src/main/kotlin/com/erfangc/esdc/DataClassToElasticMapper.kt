package com.erfangc.esdc

import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.RangeQueryBuilder
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.Temporal
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSupertypeOf

class DataClassToElasticMapper<T : Any>(clazz: KClass<out T>) {

    private val properties = clazz.declaredMemberProperties.associateBy { it.name }

    fun convert(params: Map<String, List<String>>): QueryBuilder {
        val ret = QueryBuilders.boolQuery()
        /*
        Convert each param into a term query
         */
        val queryBuilders = parseParams(params).map { parsedQuery ->
            when (parsedQuery.queryType) {
                QueryType.TERMS -> {
                    toTermsQuery(parsedQuery)
                }
                QueryType.RANGE -> {
                    toRangeQueryBuilder(parsedQuery)
                }
                QueryType.MATCH -> {
                    toMatchQuery(parsedQuery)
                }
            }
        }
        queryBuilders.forEach { queryBuilder -> ret.must(queryBuilder) }
        return ret
    }

    private fun toMatchQuery(parsedQuery: ParsedQuery<T>): QueryBuilder {
        val values = parsedQuery.values
        if (values.size > 1) {
            error("duplicate queries found for ${parsedQuery.propertyName}")
        }
        return QueryBuilders.matchQuery(parsedQuery.propertyName, values.first())
    }

    private fun toTermsQuery(parsedQuery: ParsedQuery<T>): QueryBuilder {
        val propertyName = parsedQuery.propertyName
        val queryFieldName = if (parsedQuery.property.isOfType(String::class)) {
            "${propertyName}.keyword"
        } else {
            propertyName
        }
        return if (parsedQuery.values.size > 1) {
            QueryBuilders.termsQuery(queryFieldName, parsedQuery.values)
        } else {
            QueryBuilders.termQuery(queryFieldName, parsedQuery.values.first())
        }
    }

    private fun toRangeQueryBuilder(parsedQuery: ParsedQuery<T>): RangeQueryBuilder? {
        val lowerLimit = parsedQuery.lowerLimit
        val upperLimit = parsedQuery.upperLimit
        val rangeQuery = QueryBuilders.rangeQuery(parsedQuery.propertyName)
        if (lowerLimit != null) {
            if (parsedQuery.property.isOfType(Number::class)) {
                rangeQuery.from(lowerLimit.toDouble(), true)
            } else if (parsedQuery.property.isOfType(Temporal::class)) {
                try {
                    LocalDate.parse(lowerLimit)
                } catch (e: DateTimeParseException) {
                    error("cannot parse date ${lowerLimit}, please use yyyy-MM-dd")
                }
                rangeQuery.from(lowerLimit, true)
            } else {
                rangeQuery.from(lowerLimit, true)
            }
        }
        if (upperLimit != null) {
            if (parsedQuery.property.isOfType(Number::class)) {
                rangeQuery.to(upperLimit.toDouble(), true)
            } else if (parsedQuery.property.isOfType(Temporal::class)) {
                try {
                    LocalDate.parse(upperLimit)
                } catch (e: DateTimeParseException) {
                    error("cannot parse date ${upperLimit}, please use yyyy-MM-dd")
                }
                rangeQuery.to(upperLimit, true)
            } else {
                rangeQuery.to(upperLimit, true)
            }
        }
        return rangeQuery
    }

    private fun String.removeSuffix(suffix: CharSequence): String {
        if (endsWith(suffix, ignoreCase = true)) {
            return substring(0, length - suffix.length)
        }
        return this
    }

    private fun stripParams(params: Map<String, List<String>>): List<StrippedParam<T>> {

        return params.map { (param, values) ->

            val suffix = if (param.endsWith(suffix = "from", ignoreCase = true)) {
                Suffix.FROM
            } else if (param.endsWith(suffix = "to", ignoreCase = true)) {
                Suffix.TO
            } else if (param.endsWith(suffix = "match", ignoreCase = true)) {
                Suffix.MATCH
            } else {
                null
            }

            val property = getProperty(param)
            StrippedParam(
                propertyName = property.name,
                property = property,
                values = values,
                queryType = when (suffix) {
                    Suffix.FROM, Suffix.TO -> {
                        QueryType.RANGE
                    }
                    Suffix.MATCH -> {
                        QueryType.MATCH
                    }
                    else -> {
                        QueryType.TERMS
                    }
                },
                suffix = suffix,
                keyword = property.isOfType(String::class),
            )
        }
        
    }

    private fun getProperty(param: String): KProperty1<out T, Any?> {
        val propertyNameSuffixRemoved = param
            .removeSuffix(suffix = "from")
            .removeSuffix(suffix = "to")
            .removeSuffix(suffix = "match")
        return properties[param]
            ?: properties[propertyNameSuffixRemoved]
            ?: error("invalid param given $param")
    }

    private fun parseParams(params: Map<String, List<String>>): List<ParsedQuery<T>> {
        return stripParams(params)
            .groupBy { it.propertyName }
            .map { (propertyName, strippedParams) ->
                val first = strippedParams.first()
                val values = strippedParams.flatMap { it.values }
                val lowerLimits = strippedParams.find { it.suffix == Suffix.FROM }?.values
                val upperLimits = strippedParams.find { it.suffix == Suffix.TO }?.values

                if (strippedParams.size > 2) {
                    error("duplicate properties $propertyName")
                } else if (strippedParams.size > 1) {
                    ParsedQuery(
                        propertyName = propertyName,
                        property = first.property,
                        values = values,
                        lowerLimit = lowerLimits?.first(),
                        upperLimit = upperLimits?.first(),
                        queryType = first.queryType,
                    )
                } else {
                    ParsedQuery(
                        propertyName = propertyName,
                        property = first.property,
                        values = values,
                        lowerLimit = lowerLimits?.first(),
                        upperLimit = upperLimits?.first(),
                        queryType = first.queryType,
                    )
                }
            }
    }

    /**
     * Determines whether a given KType is of the provided class while ignoring nullability
     */
    private fun <T : Any, K : Any> KProperty1<out T, *>.isOfType(kClass: KClass<K>): Boolean {
        val markedNullable = returnType.isMarkedNullable
        val comparison = kClass.createType(nullable = markedNullable)
        return this == comparison || comparison.isSupertypeOf(returnType)
    }
}