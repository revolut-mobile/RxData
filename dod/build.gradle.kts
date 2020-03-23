import Dependencies.jUnit
import Dependencies.kotlinStdLib
import Dependencies.mockitoKotlin
import Dependencies.rxJava
import com.novoda.gradle.release.PublishExtension

plugins {
    id("java-library")
    kotlin("jvm")
    id("com.novoda.bintray-release")
}

dependencies {
    implementation(project(":core"))
    implementation(kotlinStdLib)
    implementation(rxJava)

    testImplementation(jUnit)
    testImplementation(mockitoKotlin)
}

repositories {
    mavenCentral()
}

publishKotlinFix()

configure<PublishExtension> {
    val groupProjectID = "com.revolut.rxdata"
    val artifactProjectID = "dod"
    val publishVersionID = "1.2.1"

    bintrayUser = BINTRAY_USER
    bintrayKey = BINTRAY_KEY

    userOrg = "revolut-mobile"
    repoName = "RxData"
    groupId = groupProjectID
    artifactId = artifactProjectID
    publishVersion = publishVersionID
    desc = "RxData DataObservableDelegate"
    website = "https://github.com/revolut-mobile/RxData"
}

