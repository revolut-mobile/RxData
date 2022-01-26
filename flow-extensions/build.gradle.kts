import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation(Dependencies.coroutinesCore)
    implementation(Dependencies.kotlinStdLib)
    implementation(project(":model"))

    testImplementation(Dependencies.jUnit)
    testImplementation(Dependencies.mockitoKotlin)
    testImplementation(Dependencies.mockitoInline)
    testImplementation(Dependencies.coroutinesCoreTest)
    testImplementation(Dependencies.turbineCoroutinesTest)
}

plugins.withId("com.vanniktech.maven.publish") {
    mavenPublish {
        sonatypeHost = SonatypeHost.S01
    }
}
