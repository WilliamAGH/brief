plugins {
    id("java")
    id("application")
}

group = "com.williamcallahan"
version = findProperty("version")?.toString()?.takeIf { it != "unspecified" } ?: "0.0.1-SNAPSHOT"
val latteLocalRequested = (findProperty("latteLocal")?.toString() ?: System.getenv("LATTE_LOCAL")) == "true"
// Only use mavenLocal() and the SNAPSHOT build when explicitly requested.
// This avoids accidentally pulling a cached snapshot in CI/CD or other environments.
val latteLocalEnabled = latteLocalRequested
val latteVersion = if (latteLocalEnabled) "0.2.3-SNAPSHOT" else "0.2.3"

application {
    mainClass = "com.williamcallahan.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

repositories {
    if (latteLocalEnabled) {
        mavenLocal()
    }
    mavenCentral()
    maven { url = uri("https://projectlombok.org/edge-releases") }
}

dependencies {
    implementation("io.github.williamagh:latte-tui:$latteVersion")
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
