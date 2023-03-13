import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    id("java-library")
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation(Dependencies.kotlinStdLib)
    implementation(Dependencies.coroutinesCore)
    implementation(Dependencies.coroutinesRx2)
    implementation(project(":model"))
    implementation(project(":dod"))

    testImplementation(project(":flow-extensions"))
    testImplementation(Dependencies.coroutinesCoreTest)
    testImplementation(Dependencies.jupiterApi)
    testImplementation(Dependencies.jupiterEngine)
    testImplementation(Dependencies.jupiterParams)
    testImplementation(Dependencies.mockitoKotlin)
    testImplementation(Dependencies.mockitoInline)
    testImplementation(Dependencies.turbineCoroutinesTest)
}

plugins.withId("com.vanniktech.maven.publish") {
    mavenPublish {
        sonatypeHost = SonatypeHost.S01
    }
}