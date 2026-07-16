import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

application {
    mainClass.set("com.qtpie.simplepuzzle.assets.PuzzleAssetCliKt")
}

dependencies {
    implementation(project(":core-model"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}

val representativeDefinition = rootProject.file("puzzles/cosmic-journey/puzzle.json")
val representativeSource = rootProject.file("app/src/main/res/drawable/puzzle.png")
val representativeOutput = rootProject.file("app/src/main/assets/puzzles/cosmic-journey")

tasks.register<JavaExec>("generatePuzzleAssets") {
    group = "asset pipeline"
    description = "Generates deterministic runtime assets for the representative puzzle."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    args(representativeDefinition.absolutePath, representativeOutput.absolutePath)
    inputs.file(representativeDefinition)
    inputs.file(representativeSource)
    outputs.dir(representativeOutput)
}

tasks.register<Sync>("syncPuzzleThumbnails") {
    group = "asset pipeline"
    description = "Copies generated low-resolution thumbnails into Android resources."
    dependsOn("generatePuzzleAssets")
    from(representativeOutput.resolve("thumbnail.png"))
    into(rootProject.file("app/src/main/res/drawable-nodpi"))
    rename { "cosmic_journey_thumbnail.png" }
}
