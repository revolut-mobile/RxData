import com.novoda.gradle.release.PublishExtension

plugins {
    id("com.android.library")
    id("com.novoda.bintray-release")
    kotlin("android")
    id("kotlin-android-extensions")
}

android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.2")


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

publishKotlinFix()

configure<PublishExtension> {
    val groupProjectID = "com.revolut.rxdata"
    val artifactProjectID = "scheduler"
    val publishVersionID = "1.1.1"

    bintrayUser = BINTRAY_USER
    bintrayKey = BINTRAY_KEY

    userOrg = "revolut-mobile"
    repoName = "RxData"
    groupId = groupProjectID
    artifactId = artifactProjectID
    publishVersion = publishVersionID
    desc = "RxData Experimental Scheduler with immediate same-thread dispatching"
    website = "https://github.com/revolut-mobile/RxData"
}
