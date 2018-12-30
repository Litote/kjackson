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

package org.litote.jackson.generator.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.litote.jackson.data.JacksonData
import org.litote.jackson.generator.model.other.SimpleReferencedData
import java.util.Date
import java.util.Locale

/**
 *
 */
@JacksonData
data class TestData(
    @JsonProperty("set2")
    val set: Set<SimpleReferencedData> = emptySet(),
    val list: List<List<Boolean>> = emptyList(),
    val nullableList: List<List<SimpleReferencedData?>>? = emptyList(),
    val name: String? = null,
    val date: Date? = null,
    val referenced: SimpleReferencedData? = null,
    val map2: Map<Locale, SimpleReferenced2Data>? = emptyMap(),
    val nullableFloat: Float? = null,
    val nullableBoolean: Boolean? = null,
    private val privateData: String = "",
    val byteArray: ByteArray? = null,
    val mutableSet: MutableSet<String> = mutableSetOf()
) {

    @Transient
    val test2: String = "should not be serialized"

    @JsonIgnore
    val test3: String = "should not be serialized"

    companion object {
        val test: String = "should not be serialized"
    }

}
