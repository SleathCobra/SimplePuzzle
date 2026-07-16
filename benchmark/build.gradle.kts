plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.qtpie.simplepuzzle.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        create("benchmark") {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            matchingFallbacks += "release"
        }
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}
