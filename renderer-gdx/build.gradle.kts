import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.library)
    jacoco
}

android {
    namespace = "com.qtpie.simplepuzzle.renderer.gdx"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

val gdxArmNatives = configurations.create("gdxArmNatives")
val gdxArm64Natives = configurations.create("gdxArm64Natives")
val gdxX86Natives = configurations.create("gdxX86Natives")
val gdxX8664Natives = configurations.create("gdxX8664Natives")

dependencies {
    api(project(":core-model"))
    api(libs.gdx.core)
    api(libs.gdx.backend.android)
    implementation(libs.ktx.app)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    gdxArmNatives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-armeabi-v7a")
    gdxArm64Natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-arm64-v8a")
    gdxX86Natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86")
    gdxX8664Natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86_64")

    testImplementation(libs.junit)
}

@CacheableTask
abstract class ExtractGdxNativesTask @Inject constructor(
    private val fileSystemOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val archives: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun extract() {
        val outputRoot = outputDirectory.get().asFile
        fileSystemOperations.delete { delete(outputRoot) }
        archives.files.forEach { archive ->
            val abi = when {
                "natives-armeabi-v7a" in archive.name -> "armeabi-v7a"
                "natives-arm64-v8a" in archive.name -> "arm64-v8a"
                "natives-x86_64" in archive.name -> "x86_64"
                "natives-x86" in archive.name -> "x86"
                else -> error("Unknown libGDX native archive: ${archive.name}")
            }
            fileSystemOperations.copy {
                from(archiveOperations.zipTree(archive))
                into(outputRoot.resolve(abi))
                include("*.so")
            }
        }
    }
}

val extractGdxNatives = tasks.register<ExtractGdxNativesTask>("extractGdxNatives") {
    archives.from(gdxArmNatives, gdxArm64Natives, gdxX86Natives, gdxX8664Natives)
    outputDirectory.set(layout.buildDirectory.dir("generated/gdx-natives/jniLibs"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(
            extractGdxNatives,
            ExtractGdxNativesTask::outputDirectory,
        )
    }
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
