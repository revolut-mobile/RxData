plugins {
    kotlin("jvm")
    id("java-library")
    id("com.vanniktech.maven.publish")
}
dependencies {

    testImplementation(Dependencies.jupiterEngine)
    testImplementation(Dependencies.jupiterParams)
    testImplementation(Dependencies.jupiterApi)
    testImplementation(Dependencies.mockitoKotlin)
    testImplementation(Dependencies.mockitoInline)
}

plugins.withId("com.vanniktech.maven.publish") {
    mavenPublish {
        sonatypeHost = com.vanniktech.maven.publish.SonatypeHost.S01
    }
}