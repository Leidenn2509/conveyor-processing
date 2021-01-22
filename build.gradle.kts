import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.20"
}

group = "me.leidenn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/hotkeytlt/maven") }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    implementation ("com.github.h0tk3y.betterParse:better-parse:0.4.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}