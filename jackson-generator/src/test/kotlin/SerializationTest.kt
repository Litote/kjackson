/*
 * Copyright (C) 2018 Litote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.litote.jackson.generator

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.litote.jackson.generator.model.SimpleReferenced2Data
import org.litote.jackson.generator.model.TestData
import org.litote.jackson.generator.model.other.SimpleReferencedData
import org.litote.jackson.getJacksonModulesFromServiceLoader
import org.litote.jackson.registerModulesFromServiceLoader
import java.util.Date

/**
 *
 */
class SerializationTest {

    @Test
    fun `serialize and deserialize is ok`() {
        println(Class.forName("org.litote.jackson.generator.model.InternalDataClass"))
        println(this.javaClass.getResource("/META-INF/services/org.litote.jackson.JacksonModuleServiceLoader"))
        println(getJacksonModulesFromServiceLoader())
        val mapper = ObjectMapper()
            .registerKotlinModule()
            .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
            .registerModulesFromServiceLoader()
            .registerModule(SimpleModule().apply { addAbstractTypeMapping(Set::class.java, LinkedHashSet::class.java) })
        val date = Date()
        val data = TestData(
            setOf(SimpleReferencedData(version = 2, pojo2 = SimpleReferenced2Data(12.0)), SimpleReferencedData()),
            listOf(listOf(true, false)),
            listOf(listOf(SimpleReferencedData(pojo2 = SimpleReferenced2Data(12.0)), SimpleReferencedData())),
            "name",
            date,
            SimpleReferencedData(),
            null
        )

        val json = mapper.writeValueAsString(data)
        println(json)
        val r: TestData = mapper.readValue(json, TestData::class.java)
        assertEquals(data, r)
    }

    @Test
    fun `deserialize partial data is ok`() {
        val mapper = ObjectMapper()
            .registerKotlinModule()
            .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
            .registerModulesFromServiceLoader()
        val data = TestData(
            setOf(SimpleReferencedData(pojo2 = SimpleReferenced2Data(12.0))),
            listOf(listOf(true)),
            null,
            "name"
        )

        data.set.first().apply {
            version = 2
        }
        val r: TestData = mapper.readValue(
            """{"notPresent":"ok", "notPresent2":{"a":"b"},"set2":[{"version":2,"pojo2":{"price":12.0},"pojo":null,"labels":{}}],"list":[[true]],"nullableList":null,"name":"name"}""",
            TestData::class.java
        )
        assertEquals(data, r)
    }
}