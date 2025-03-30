plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "astra"
include("api")
include("generator")
include("samples")
include("samples:calculator")
findProject(":samples:calculator")?.name = "calculator"
include("samples:functions")
findProject(":samples:functions")?.name = "functions"
include("samples:recursion")
findProject(":samples:recursion")?.name = "recursion"
include("generator-common")
