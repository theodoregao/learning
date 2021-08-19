plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.20"
    //id("org.jetbrains.kotlin.jvm") version "1.4.32"
    //id("org.jetbrains.kotlin.jvm") version "1.3.72"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
