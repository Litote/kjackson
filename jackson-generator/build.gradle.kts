import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

val jackson: String by project
val junit: String by project
val kreflect: String by project
val kgenerator: String by project
val kotlinVersion: String by project

plugins {
    kotlin("kapt")
}

dependencies {
    compile(kotlin("stdlib"))
    compile(project(":jackson-module-loader"))
    compile(project(":jackson-data"))
    compile("org.litote", "kgenerator", kgenerator)
    compile("org.litote", "kreflect", kreflect)
    compile("com.fasterxml.jackson.module", "jackson-module-kotlin", jackson)

    testImplementation("org.jetbrains.kotlin", "kotlin-test-junit5", kotlinVersion)
    testImplementation("org.junit.jupiter", "junit-jupiter-engine", junit)
    testImplementation("org.junit.jupiter", "junit-jupiter-api", junit)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", junit)

    kaptTest("org.litote.jackson", "jackson-generator", version.toString())
}

//for intellij
kotlin.sourceSets["test"].kotlin.srcDirs("$buildDir/generated/source/kapt/test")
kotlin.sourceSets["test"].resources.srcDirs("$buildDir/tmp/kapt3/classes/test")

/*
tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }
}*/

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}