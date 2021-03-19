plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-android-extensions")
    id("com.vanniktech.maven.publish")
    `maven-override`
}

android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.3")


    defaultConfig {
        minSdkVersion(19)
        targetSdkVersion(29)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles.add(File("consumer-rules.pro"))
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlinVersion}")

    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.18")

    testImplementation("junit:junit:4.13")
    testImplementation("org.robolectric:robolectric:4.3")
}
