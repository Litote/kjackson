val jackson: String by project

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.core", "jackson-databind", jackson)
}

