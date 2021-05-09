import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    base
    kotlin("jvm") version "1.5.0" apply false
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.4.32"
}

allprojects {

    group = "org.litote.jackson"

    repositories {
        mavenLocal()
        jcenter()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }
}

dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}

subprojects {

    val projectName: String by project
    val projectDescription: String by project

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("maven-publish")
        plugin("signing")
        plugin("org.jetbrains.dokka")
    }

    dependencies {
        dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.4.32")
    }

    val sourcesJar by tasks.registering(Jar::class) {
        classifier = "sources"
        from("$projectDir/src/main/kotlin")
    }

    val javadocJar by tasks.registering(Jar::class) {
        dependsOn("dokkaJavadoc")
        classifier = "javadoc"
        from("$buildDir/dokka")
    }

    publishing {
        repositories {
            maven {
                val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                credentials {
                    username = property("sonatypeUsername").toString()
                    password = property("sonatypePassword").toString()
                }
            }
        }
        publications {
            register("mavenJava", MavenPublication::class) {
                from(components["java"])
                artifact(sourcesJar.get())
                artifact(javadocJar.get())
                pom {
                    name.set(projectName)
                    description.set(projectDescription)
                    url.set("https://github.com/Litote/kjackson")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("zigzago")
                            name.set("Julien Buret")
                            email.set("zigzago@litote.org")
                        }
                    }
                    scm {
                        connection.set("scm:git:git@github.com:Litote/kjackson.git")
                        developerConnection.set("scm:git:git@github.com:Litote/kjackson.git")
                        url.set("git@github.com:Litote/kjackson.git")
                    }
                }
            }
        }
    }

    signing {
        sign(publishing.publications["mavenJava"])
    }

}
