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

package org.litote.jackson.generator.model.other

import org.litote.jackson.data.JacksonData
import org.litote.jackson.generator.model.SimpleReferenced2Data
import org.litote.jackson.generator.model.TestData
import java.util.Locale

/**
 *
 */
@JacksonData
data class SimpleReferencedData(
    var version: Int = 0,
    /** TODO private*/ var pojo2: SimpleReferenced2Data? = null,
    var pojo: TestData? = null,
    val labels: Map<Locale, List<String>> = emptyMap(),
    @Transient val g: String = "e"
)