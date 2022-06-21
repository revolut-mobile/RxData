import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation(Dependencies.rxJava)
    implementation(Dependencies.kotlinStdLib)
    implementation(project(":model"))

    testImplementation(Dependencies.jupiterEngine)
    testImplementation(Dependencies.jupiterParams)
    testImplementation(Dependencies.jupiterApi)
    testImplementation(Dependencies.mockitoKotlin)
    testImplementation(Dependencies.mockitoInline)
}

plugins.withId("com.vanniktech.maven.publish") {
    mavenPublish {
        sonatypeHost = SonatypeHost.S01
    }
}