
group = "org.ldemetrios"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("io.kotest:kotest-runner-junit5:5.7.0")
    testImplementation("io.kotest:kotest-assertions-core:5.7.0")
    testImplementation("io.kotest:kotest-property:5.7.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

sourceSets.main {
    kotlin.srcDir("src")
}

sourceSets.test {
    kotlin.srcDir("test")
}
