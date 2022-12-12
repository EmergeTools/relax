plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.emergetools.relaxtests"
    compileSdk = 33

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    defaultConfig {
        minSdk = 23
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("release") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
        }
    }

    targetProjectPath = ":app"

    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.1.4")
    implementation("androidx.test:runner:1.5.1")
    implementation(project(":relax"))
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "release"
    }
}
