import com.novoda.gradle.release.PublishExtension

plugins {
    kotlin("jvm")
    id("com.novoda.bintray-release")
}

dependencies {
    implementation(Dependencies.rxJava)
    implementation(Dependencies.kotlinStdLib)
}

repositories {
    mavenCentral()
}

publishKotlinFix()

configure<PublishExtension> {
    val groupProjectID = "com.revolut.rxdata"
    val artifactProjectID = "core"
    val publishVersionID = "1.2.5"

    bintrayUser = BINTRAY_USER
    bintrayKey = BINTRAY_KEY

    userOrg = "revolut-mobile"
    repoName = "RxData"
    groupId = groupProjectID
    artifactId = artifactProjectID
    publishVersion = publishVersionID
    desc = "RxData Core Models and Extensions"
    website = "https://github.com/revolut-mobile/RxData"
}
