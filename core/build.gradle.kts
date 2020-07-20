plugins {
    kotlin("jvm")
}

dependencies {
    implementation(Dependencies.rxJava)
    implementation(Dependencies.kotlinStdLib)

    testImplementation(Dependencies.jUnit)
    testImplementation(Dependencies.mockitoKotlin)
}

apply(from = "$rootDir/gradle/publish_bintray.gradle.kts")