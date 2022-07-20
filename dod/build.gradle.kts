import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    id("java-library")
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation(Dependencies.kotlinStdLib)
    implementation(Dependencies.rxJava)
    implementation(project(":model"))

    testImplementation(project(":rx-extensions"))
    testImplementation(Dependencies.jupiterApi)
    testImplementation(Dependencies.jupiterEngine)
    testImplementation(Dependencies.jupiterParams)
    testImplementation(Dependencies.mockitoKotlin)
    testImplementation(Dependencies.mockitoInline)
}

plugins.withId("com.vanniktech.maven.publish") {
    mavenPublish {
        sonatypeHost = SonatypeHost.S01
    }
}