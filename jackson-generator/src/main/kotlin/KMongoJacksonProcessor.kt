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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterSpec.Companion.parametersOf
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.litote.jackson.data.JacksonData
import org.litote.jackson.data.JacksonDataRegistry
import org.litote.kgenerator.AnnotatedClass
import org.litote.kgenerator.AnnotatedClassSet
import org.litote.kgenerator.AnnotatedProperty
import org.litote.kgenerator.KGenerator
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.tools.StandardLocation
import kotlin.reflect.KParameter

/**
 *
 */
@SupportedAnnotationTypes(
    "org.litote.jackson.data.JacksonData",
    "org.litote.jackson.data.JacksonDataRegistry"
)
internal class KMongoJacksonProcessor : KGenerator() {

    private val AnnotatedProperty.jsonProperty: String
        get() = getAnnotation<JsonProperty>()?.value ?: simpleName.toString()

    private fun AnnotatedClass.jacksonProperties(): List<AnnotatedProperty> =
        properties {
            !it.hasAnnotation<JsonIgnore>()
        }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val classes = getAnnotatedClasses<JacksonData, JacksonDataRegistry>(roundEnv)
            .filter {
                val superclass = (it as TypeElement).superclass
                if (superclass is DeclaredType && superclass.toString() != "java.lang.Object") {
                    log("$it: inheritance not supported for now - skip")
                    false
                } else {
                    true
                }
            }

        classes.forEach {
            process(it)
        }

        if (classes.isNotEmpty()) {
            writeModuleLoader(classes)
        }

        return classes.isNotEmpty()
    }

    private fun process(element: AnnotatedClass) {
        writeSerializer(element)
        writeDeserializer(element)
    }

    private fun writeModuleLoader(elements: AnnotatedClassSet) {
        debug { "write META-INF/services/org.litote.jackson.JacksonModuleServiceLoader" }
        writeFile(
            "META-INF/services",
            "org.litote.jackson.JacksonModuleServiceLoader",
            elements
                .toList()
                .flatMap { e ->
                    listOf(generatedSerializer(e), generatedDeserializer(e))
                        .map { e.getPackage() + "." + it }
                }
                .joinToString("\n"),
            StandardLocation.CLASS_OUTPUT
        )
    }

    private fun writeSerializer(element: AnnotatedClass) {
        val sourceClassName = element.asClassName()
        val className = generatedSerializer(element)
        val fileBuilder = FileSpec.builder(element.getPackage(), className)

        val superclass =
            ClassName.bestGuess("com.fasterxml.jackson.databind.ser.std.StdSerializer").parameterizedBy(sourceClassName)

        val moduleLoader = ClassName.bestGuess("org.litote.jackson.JacksonModuleServiceLoader")
        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(INTERNAL)
            .superclass(superclass)
            .addSuperinterface(moduleLoader)
            .addSuperclassConstructorParameter(CodeBlock.of("%T::class.java", sourceClassName))
            .addFunction(
                FunSpec.builder("module")
                    .addModifiers(OVERRIDE)
                    .addStatement(
                        "return %T().addSerializer(%T::class.java, this)",
                        ClassName.bestGuess("com.fasterxml.jackson.databind.module.SimpleModule"),
                        sourceClassName
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("serialize")
                    .addModifiers(OVERRIDE)
                    .addParameter(
                        ParameterSpec.builder("value", sourceClassName).build()
                    )
                    .addParameter(
                        ParameterSpec.builder(
                            "gen",
                            ClassName.bestGuess("com.fasterxml.jackson.core.JsonGenerator")
                        ).build()
                    )
                    .addParameter(
                        ParameterSpec.builder(
                            "serializers",
                            ClassName.bestGuess("com.fasterxml.jackson.databind.SerializerProvider")
                        ).build()
                    )
                    .addStatement(
                        "gen.writeStartObject()"
                    )
                    .apply {
                        element.jacksonProperties().forEach { e ->
                            debug { "${e.simpleName}-${e.asType()}" }
                            val propertyName = e.simpleName
                            val jsonField = e.jsonProperty
                            val type = e.asType()
                            val nullable = asTypeName(e).isNullable
                            val fieldAccessor =
                                element.propertyReference(
                                    e,
                                    {
                                        findPropertyValue(
                                            sourceClassName,
                                            javaToKotlinType(e),
                                            "value",
                                            "$propertyName"
                                        )
                                    }) {
                                    CodeBlock.of("value.$propertyName")
                                }

                            val fieldName = "_${propertyName}_"

                            addStatement("gen.writeFieldName(\"$jsonField\")")
                            addStatement("val $fieldName = $fieldAccessor")
                            addStatement(
                                if (nullable) {
                                    "if($fieldName == null) { gen.writeNull() } else {"
                                } else {
                                    ""
                                } +
                                        when (type.toString()) {
                                            "java.lang.String" -> "gen.writeString($fieldName)"
                                            "java.lang.Boolean", "boolean" -> "gen.writeBoolean($fieldName)"
                                            "java.lang.Integer",
                                            "java.lang.Long",
                                            "java.lang.Short",
                                            "java.lang.Float",
                                            "java.lang.Double",
                                            "java.math.BigInteger",
                                            "java.math.BigDecimal",
                                            "short",
                                            "int",
                                            "long",
                                            "float",
                                            "double" -> "gen.writeNumber($fieldName)"
                                            else -> "serializers.defaultSerializeValue($fieldName, gen)"
                                        } +
                                        if (nullable) {
                                            "}"
                                        } else {
                                            ""
                                        }
                            )
                        }
                    }
                    .addStatement(
                        "gen.writeEndObject()"
                    )

                    .build()
            )


        //add classes
        fileBuilder.addType(
            classBuilder
                .build()
        )

        writeFile(fileBuilder)
    }

    private fun writeDeserializer(element: AnnotatedClass) {
        val sourceClassName = element.asClassName()
        val className = generatedDeserializer(element)
        val fileBuilder = FileSpec.builder(element.getPackage(), className)
        fileBuilder.addImport("kotlin.reflect.full", "findParameterByName")
        fileBuilder.addImport("kotlin.reflect.full", "primaryConstructor")

        val superclass = ClassName.bestGuess("com.fasterxml.jackson.databind.deser.std.StdDeserializer")
            .parameterizedBy(sourceClassName)

        val moduleLoader = ClassName.bestGuess("org.litote.jackson.JacksonModuleServiceLoader")

        val properties = element.jacksonProperties()

        fun VariableElement.field() = "_${simpleName}_"
        fun ParameterSpec.field() = "_${name}_"
        fun VariableElement.updated() = "_${simpleName}_set"

        val classBuilder = TypeSpec.classBuilder(className)
            .superclass(superclass)
            .addModifiers(INTERNAL)
            .addSuperinterface(moduleLoader)
            .addSuperclassConstructorParameter(CodeBlock.of("%T::class.java", sourceClassName))
            .addType(
                TypeSpec.companionObjectBuilder()
                    .apply {

                        addProperty(
                            PropertySpec.builder(
                                "primaryConstructor",
                                ClassName.bestGuess("kotlin.reflect.KFunction").parameterizedBy(sourceClassName)
                            )
                                .delegate(
                                    CodeBlock.of(
                                        "lazy(%L) { %T::class.primaryConstructor!! }",
                                        "LazyThreadSafetyMode.PUBLICATION",
                                        sourceClassName
                                    )
                                )
                                .addModifiers(PRIVATE)
                                .build()
                        )

                        addProperty(
                            PropertySpec.builder(
                                "parameters",
                                Map::class.asTypeName().parameterizedBy(
                                    String::class.asTypeName(),
                                    KParameter::class.asTypeName()
                                )
                            )
                                .delegate(
                                    CodeBlock.of(
                                        "lazy(%L) { kotlin.collections.mapOf(%L) }",
                                        "LazyThreadSafetyMode.PUBLICATION",
                                        properties.joinToString { "\"${it.simpleName}\" to primaryConstructor.findParameterByName(\"${it.simpleName}\")!!" }
                                    )
                                )
                                .addModifiers(PRIVATE)
                                .build()
                        )


                        properties.forEach { e ->
                            if (asTypeName(e) is ParameterizedTypeName) {
                                val propertyName = e.field()
                                val typeReference = ClassName.bestGuess("com.fasterxml.jackson.core.type.TypeReference")
                                    .parameterizedBy(
                                        javaToKotlinType(asTypeName(e))
                                    )

                                addProperty(
                                    PropertySpec.builder("${propertyName}_reference", typeReference)
                                        .initializer(
                                            CodeBlock.of(
                                                "object : %T() {}",
                                                typeReference
                                            )
                                        )
                                        .addModifiers(PRIVATE)
                                        .build()
                                )
                            }
                        }
                    }
                    .build()
            )
            .addFunction(
                FunSpec.builder("module")
                    .addModifiers(OVERRIDE)
                    .addStatement(
                        "return %T().addDeserializer(%T::class.java, this)",
                        ClassName.bestGuess("com.fasterxml.jackson.databind.module.SimpleModule"),
                        sourceClassName
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("deserialize")
                    .addModifiers(OVERRIDE)
                    .addParameter(
                        ParameterSpec.builder(
                            "p",
                            ClassName.bestGuess("com.fasterxml.jackson.core.JsonParser")
                        ).build()
                    )
                    .addParameter(
                        ParameterSpec.builder(
                            "ctxt",
                            ClassName.bestGuess("com.fasterxml.jackson.databind.DeserializationContext")
                        ).build()
                    )
                    .returns(sourceClassName)
                    .addStatement("with(p) {⇥")
                    .apply {
                        //generate fields
                        properties.forEach { e ->
                            val type = javaToKotlinType(e).run {
                                val paramType = (this as? com.squareup.kotlinpoet.ParameterizedTypeName)
                                when (paramType?.rawType?.canonicalName) {
                                    "kotlin.collections.Set" -> ClassName.bestGuess("kotlin.collections.MutableSet").parameterizedBy(
                                        paramType.typeArguments.first()
                                    )
                                    "kotlin.collections.List" -> ClassName.bestGuess("kotlin.collections.MutableList").parameterizedBy(
                                        paramType.typeArguments.first()
                                    )
                                    "kotlin.collections.Map" -> ClassName.bestGuess("kotlin.collections.MutableMap").parameterizedBy(
                                        paramType.typeArguments.first(),
                                        paramType.typeArguments[1]
                                    )
                                    else -> this
                                }
                            }
                            addStatement("var %N: %T = null", e.field(), type.copy(nullable = true))
                            addStatement("var %N = false", e.updated())
                        }
                    }
                    //generate while
                    .addStatement(
                        "while (currentToken != %T && currentToken != %T) { ⇥",
                        ClassName.bestGuess("com.fasterxml.jackson.core.JsonToken.END_OBJECT"),
                        ClassName.bestGuess("com.fasterxml.jackson.core.JsonToken.END_ARRAY")
                    )
                    .addStatement(
                        "if(currentToken != %T) { nextToken() }",
                        ClassName.bestGuess("com.fasterxml.jackson.core.JsonToken.FIELD_NAME")
                    )

                    .addStatement(
                        "if (currentToken == %T || currentToken == %T) { break } ",
                        ClassName.bestGuess("com.fasterxml.jackson.core.JsonToken.END_OBJECT"),
                        ClassName.bestGuess("com.fasterxml.jackson.core.JsonToken.END_ARRAY")
                    )     
                    .addStatement("val fieldName = currentName")
                    .addStatement("nextToken()")
                    //.addStatement("if (currentToken == null || fieldName == null) { break } ")
                    .addStatement("when (fieldName) {⇥ ")
                    //generate set
                    .apply {
                        properties.forEach { e ->
                            val propertyName = e.field()
                            val jsonField = e.jsonProperty
                            val type = asTypeName(e)
                            val writeMethod = when (type.copy(nullable = false).toString()) {
                                "java.lang.String" -> CodeBlock.of("text")
                                "java.lang.Boolean", "boolean" -> CodeBlock.of("booleanValue")
                                "java.lang.Integer", "int" -> CodeBlock.of("intValue")
                                "java.lang.Long", "long" -> CodeBlock.of("longValue")
                                "java.lang.Short", "short" -> CodeBlock.of("shortValue")
                                "java.lang.Float", "float" -> CodeBlock.of("floatValue")
                                "java.lang.Double", "double" -> CodeBlock.of("doubleValue")
                                "java.math.BigInteger" -> CodeBlock.of("bigIntegerValue")
                                "java.math.BigDecimal" -> CodeBlock.of("decimalValue")
                                else -> if (asTypeName(e) is ParameterizedTypeName) {
                                    CodeBlock.of("readValueAs(${propertyName}_reference)")
                                } else {
                                    CodeBlock.of("readValueAs(%T::class.java)", javaToKotlinType(e))
                                }
                            }
                            addStatement(
                                "%S -> {\n%N = if(currentToken == JsonToken.VALUE_NULL) null\n else p.%L;\n%N = true\n}",
                                jsonField,
                                propertyName,
                                writeMethod,
                                e.updated()
                            )
                        }
                        addStatement(
                            "else -> {\nif (currentToken == %T || currentToken == %T)\np.skipChildren()\nnextToken()\n}",
                            ClassName.bestGuess("com.fasterxml.jackson.core.JsonToken.START_OBJECT"),
                            ClassName.bestGuess("com.fasterxml.jackson.core.JsonToken.START_ARRAY")
                        )
                    }
                    //generate end while
                    .addStatement("}⇤ ")
                    .addStatement("}⇤ ")
                    .apply {
                        val parameters =
                            parametersOf(element.enclosedElements.first { it.kind == ElementKind.CONSTRUCTOR } as ExecutableElement)
                                .mapNotNull {
                                    log(it)
                                    log(it.defaultValue)
                                    val p = properties.firstOrNull { e -> e.simpleName.toString() == it.name }
                                    if (p == null) {
                                        null
                                    } else {
                                        "${it.name} = ${it.field()}${if (asTypeName(p).isNullable) "" else "!!"}"
                                    }
                                }
                        addStatement(
                            "return if(%L)\n%T(%L)\nelse {\n%L\n}",
                            properties.joinToString(" && ") { it.updated() },
                            sourceClassName,
                            parameters.joinToString(),
                            CodeBlock.of(
                                "val map·=·mutableMapOf<KParameter,·Any?>()\n"
                                        + "${properties.joinToString("\n") { "if(${it.updated()})\nmap[parameters.getValue(\"${it.simpleName}\")] = ${it.field()}" }} \n"
                                        + "primaryConstructor.callBy(map) "
                            )
                        )
                        addStatement("⇤} ")
                    }
                    .build()
            )


        //add classes
        fileBuilder.addType(
            classBuilder
                .build()
        )

        writeFile(fileBuilder)
    }

    private fun generatedSerializer(element: Element): String = "${element.simpleName}_Serializer"
    private fun generatedDeserializer(element: Element): String = "${element.simpleName}_Deserializer"
}