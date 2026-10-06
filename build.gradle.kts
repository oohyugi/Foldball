plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

val kotlinFormatter by configurations.creating {
    attributes {
        attribute(
            org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.Bundling.SHADOWED)
        )
        attribute(
            org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.Usage.JAVA_RUNTIME)
        )
    }
}

dependencies {
    kotlinFormatter(libs.ktlint.cli)
}

val kotlinSourcePatterns = listOf(
    "shared/src/**/*.kt",
    "androidApp/src/**/*.kt",
    "desktopApp/src/**/*.kt",
    "*.gradle.kts",
    "shared/*.gradle.kts",
    "androidApp/*.gradle.kts",
    "desktopApp/*.gradle.kts"
)

tasks.register<JavaExec>("checkKotlinStyle") {
    group = "verification"
    description = "Check Kotlin source formatting with the repository EditorConfig."
    mustRunAfter("formatKotlin")
    classpath = kotlinFormatter
    mainClass.set("com.pinterest.ktlint.Main")
    args(kotlinSourcePatterns)
}

tasks.register<JavaExec>("formatKotlin") {
    group = "formatting"
    description = "Format Kotlin sources; generated and reference assets are excluded."
    classpath = kotlinFormatter
    mainClass.set("com.pinterest.ktlint.Main")
    args("--format")
    args(kotlinSourcePatterns)
}

tasks.register<Exec>("checkArchitecture") {
    group = "verification"
    description = "Check domain, data, feature, and GameKit import boundaries."
    commandLine("python3", layout.projectDirectory.file("tools/check_architecture.py").asFile.absolutePath)
}
