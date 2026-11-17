plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor ("org.projectlombok:lombok:1.18.48")
    compileOnly ("org.projectlombok:lombok:1.18.48")
    implementation ("io.vavr:vavr:0.10.7")
    implementation("org.apache.commons:commons-lang3:3.22.0")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.21.8"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.yaml:snakeyaml:2.7")
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.slf4j:slf4j-api:2.0.20")
    implementation ("ch.qos.logback:logback-classic:1.6.7")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}