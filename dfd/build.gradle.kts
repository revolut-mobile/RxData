plugins {
    kotlin("jvm")
    id("java-library")
}

dependencies {
    implementation(Dependencies.kotlinStdLib)
    implementation(Dependencies.coroutinesCore)

    implementation(project(":flow-core"))

    testImplementation(Dependencies.coroutinesCoreTest)
    testImplementation(Dependencies.jUnit)
    testImplementation(Dependencies.mockitoKotlin)
}