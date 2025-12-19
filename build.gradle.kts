plugins {
    id("java")
    id("application")
}

group = "com.williamcallahan"
version = findProperty("version")?.toString()?.takeIf { it != "unspecified" } ?: "0.0.1-SNAPSHOT"

application {
    mainClass = "com.williamcallahan.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

repositories {
    mavenCentral()
    maven { url = uri("https://projectlombok.org/edge-releases") }
}

dependencies {
    implementation("io.github.williamagh:latte-tui:0.2.3")
    implementation("com.openai:openai-java:4.11.0")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.18.2"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("org.projectlombok:lombok:edge-SNAPSHOT")
    annotationProcessor("org.projectlombok:lombok:edge-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
