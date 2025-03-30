import java.net.URI

plugins {
    kotlin("jvm")
}

group = "org.ldemetrios"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jruby.joni:joni:2.2.3")
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
