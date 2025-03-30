plugins {
    id("com.gradleup.shadow") version "9.0.0-beta4"
    application
}

group = "org.ldemetrios"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":generator-common"))
    implementation(project(":api"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "org.ldemetrios.astra.samples.recursion.MainKt" // Replace with your main class
}

tasks.shadowJar {
    manifest {
        attributes(
            "Main-Class" to "org.ldemetrios.astra.samples.recursion.MainKt" // Replace with your main class
        )
    }
    doLast {
        copy {
            from("${layout.buildDirectory.get()}/libs")
            into("$rootDir/artifacts")
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

kotlin {
    jvmToolchain(21)
}

sourceSets.main {
    kotlin.srcDir("src")
}
