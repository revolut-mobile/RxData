plugins {
    kotlin("jvm")
    id("java-library")
    id("com.vanniktech.maven.publish")
    `maven-override`
}

dependencies {
    implementation(project(":core"))
    implementation(Dependencies.kotlinStdLib)
    implementation(Dependencies.rxJava)

    testImplementation(Dependencies.jUnit)
    testImplementation(Dependencies.mockitoKotlin)
}
