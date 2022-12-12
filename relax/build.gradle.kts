plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
    signing
}

group = "com.emergetools.test"
version = "0.1.0"

android {
    namespace = "com.emergetools.relax"
    compileSdk = 33

    defaultConfig {
        minSdk = 23
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

dependencies {
    implementation("androidx.test.ext:junit:1.1.4")
    implementation("androidx.tracing:tracing:1.1.0")
    api("androidx.test.uiautomator:uiautomator:2.2.0")
    testImplementation("junit:junit:4.13.2")
}

val ossrhUsername: String? by project
val ossrhPassword: String? by project

publishing {
    repositories {
        maven {
            name = "SonatypeStaging"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }

        maven {
            name = "SonatypeSnapshots"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }

    publications {
        register<MavenPublication>("release") {
            artifactId = "relax"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Relax")
                description.set("Clear and concise Android UI tests using UI Automator")
                url.set("https://github.com/EmergeTools/relax")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/EmergeTools/relax")
                }
                developers {
                    developer {
                        id.set("nathanael")
                        name.set("Nathanael Silverman")
                        email.set("nathanael@emergetools.com")
                    }
                    developer {
                        id.set("ryan")
                        name.set("Ryan Brooks")
                        email.set("ryan@emergetools.com")
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["release"])
}
