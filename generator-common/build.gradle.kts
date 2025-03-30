plugins {
    kotlin("jvm")
}

group = "org.ldemetrios"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":api"))
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
