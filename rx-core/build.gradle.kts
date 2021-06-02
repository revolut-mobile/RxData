plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
    `maven-override`
}

dependencies {
    implementation(Dependencies.rxJava)
    implementation(Dependencies.kotlinStdLib)

    testImplementation(Dependencies.jUnit)
    testImplementation(Dependencies.mockitoKotlin)
}
