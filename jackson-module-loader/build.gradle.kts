val jackson: String by project

dependencies {
    compile(kotlin("stdlib"))
    compile("com.fasterxml.jackson.core", "jackson-databind", jackson)
}

