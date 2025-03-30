import java.io.ByteArrayOutputStream

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
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

val sourceSetNames = listOf(
    "main", "test"
)

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()  // Add repositories to subprojects
    }

    dependencies {
        implementation(project(":api"))
    }

    for (name in sourceSetNames) {
        sourceSets {
            named(name) {
                kotlin {
                    srcDir("${layout.buildDirectory.get()}/generated-src/astra/$name".also(::println))
                }
            }
        }
    }

    tasks.register("generateAstra") {
        dependsOn(":generator:shadowJar")
        doLast {
            val bundles = mapOf(
                "main" to File("$projectDir/src")
            )

            for ((name, dir) in bundles) {
                val grammars = dir.walkTopDown().filter { it.extension == "g" }
                for (grammar in grammars) {
                    val location = grammar.parentFile.relativeTo(dir)
                    val dest = "${layout.buildDirectory.get()}/generated-src/astra/$name/$location"
                    println(grammar.absolutePath + ", " + dest)
                    val error = ByteArrayOutputStream()
                    val result = exec {
                        commandLine(
                            "java",
                            "-jar",
                            "$rootDir/artifacts/generator-1.0-all.jar",
//                            "$rootDir/artifacts/recursion-1.0-all.jar",
                            grammar.absolutePath,
                            dest
                        )
                        errorOutput = error
                        isIgnoreExitValue = true
                    }
                    if (result.exitValue != 0) {
                        println(error.toString())
                        result.assertNormalExitValue()
                    }
                }
            }
        }
    }

    tasks.compileKotlin {
        dependsOn("generateAstra")
    }
}