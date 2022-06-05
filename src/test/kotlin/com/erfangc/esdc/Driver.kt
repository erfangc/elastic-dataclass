package com.erfangc.esdc

import org.elasticsearch.search.builder.SearchSourceBuilder
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class DataClassToElasticMapperTest {
    @Test
    fun main() {
        val dataClassToElasticMapper = DataClassToElasticMapper(Person::class)
        val queryBuilder = dataClassToElasticMapper.convert(
            mapOf(
                "birthDateFrom" to listOf("2020-12-31"),
                "birthDateTo" to listOf("2022-12-31"),
                "age" to listOf("12"),
                "nameMatch" to listOf("John"),
                "label" to listOf("Foo", "Bar")
            )
        )
        val json = SearchSourceBuilder.searchSource().query(queryBuilder).toString()
        JSONAssert.assertEquals(
            """
                {
                  "query": {
                    "bool": {
                      "must": [
                        {
                          "range": {
                            "birthDate": {
                              "from": "2020-12-31",
                              "to": "2022-12-31",
                              "include_lower": true,
                              "include_upper": true,
                              "boost": 1.0
                            }
                          }
                        },
                        {
                          "term": {
                            "age": {
                              "value": "12",
                              "boost": 1.0
                            }
                          }
                        },
                        {
                          "match": {
                            "name": {
                              "query": "John",
                              "operator": "OR",
                              "prefix_length": 0,
                              "max_expansions": 50,
                              "fuzzy_transpositions": true,
                              "lenient": false,
                              "zero_terms_query": "NONE",
                              "auto_generate_synonyms_phrase_query": true,
                              "boost": 1.0
                            }
                          }
                        },
                        {
                          "terms": {
                            "label.keyword": [
                              "Foo",
                              "Bar"
                            ],
                            "boost": 1.0
                          }
                        }
                      ],
                      "adjust_pure_negative": true,
                      "boost": 1.0
                    }
                  }
                }
            """.trimIndent(),
            json,
            true,
        )
    }
} 