import java.net.URI

plugins {
    kotlin("jvm")
    antlr
    application
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "org.ldemetrios"
version = "1.0"

application {
    mainClass = "org.ldemetrios.astra.MainKt"
}

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":generator-common"))
    implementation(project(":api"))
    implementation("org.antlr:antlr4-runtime:4.+")
    implementation("com.github.1Jajen1.kotlin-pretty:kotlin-pretty:0.6.0")
    antlr("org.antlr:antlr4:4.+")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.compileKotlin {
    dependsOn("generateGrammarSource")
}

tasks.generateGrammarSource {
    arguments = arguments + listOf("-package", "org.ldemetrios.astra", "-visitor")
    outputDirectory = File("$buildDir/generated-src/antlr/main/org/ldemetrios/astra")
}

tasks.shadowJar {
    manifest {
        attributes(
            "Main-Class" to "org.ldemetrios.astra.MainKt" // Replace with your main class
        )
    }
    doLast {
        copy {
            from("${layout.buildDirectory.get()}/libs".also(::println))
            into("$rootDir/artifacts".also(::println))
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
        freeCompilerArgs += "-Xmulti-dollar-interpolation"
    }
}

sourceSets.main {
    kotlin.srcDir("src")
}
