plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    implementation(Dependencies.kotlinStdLib)
    implementation(Dependencies.rxJava)

    testImplementation(Dependencies.jUnit)
    testImplementation(Dependencies.mockitoKotlin)
}

apply(from = "$rootDir/gradle/publish_bintray.gradle.kts")