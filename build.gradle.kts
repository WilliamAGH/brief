plugins {
    id("java")
    id("application")
}

group = "com.williamcallahan"
version = findProperty("version")?.toString()?.takeIf { it != "unspecified" } ?: "0.1.2-SNAPSHOT"
val tui4jLocalRequested = listOf(
    findProperty("tui4jLocal")?.toString(),
    System.getenv("TUI4J_LOCAL"),
).any { it == "true" }
val isCi = (System.getenv("CI") == "true") || (System.getenv("GITHUB_ACTIONS") == "true")
val tui4jSnapshotVersion = "0.2.4-SNAPSHOT"
val tui4jReleaseVersion = "0.2.4"
val tui4jJlineVersion = "3.26.1"
val tui4jIcuVersion = "76.1"
val tui4jCommonsTextVersion = "1.13.0"
val homeDir = System.getProperty("user.home")
// TUI4J_LOCAL_PATH env var allows explicit JAR path; otherwise check common locations
val tui4jLocalPathOverride = System.getenv("TUI4J_LOCAL_PATH")?.let { file(it) }
val tui4jLocalJarCandidates = listOfNotNull(
    tui4jLocalPathOverride,
    file("../tui4j/build/libs/tui4j-${tui4jSnapshotVersion}.jar"),
    file("$homeDir/Developer/git/tui4j/build/libs/tui4j-${tui4jSnapshotVersion}.jar"),
)
val tui4jLocalJar = tui4jLocalJarCandidates.firstOrNull { it.exists() } ?: tui4jLocalJarCandidates.first()
val tui4jLocalJarExists = tui4jLocalJarCandidates.any { it.exists() }
// Auto-enable local JAR for developer ergonomics, but never on CI
val tui4jLocalEnabled = tui4jLocalRequested || (!isCi && tui4jLocalJarExists)
val tui4jVersion = if (tui4jLocalEnabled) tui4jSnapshotVersion else tui4jReleaseVersion

application {
    mainClass = "com.williamcallahan.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

repositories {
    mavenCentral()
    maven { url = uri("https://projectlombok.org/edge-releases") }
}

dependencies {
    if (tui4jLocalEnabled) {
        implementation(files(tui4jLocalJar))
        implementation("org.jline:jline-terminal-jni:$tui4jJlineVersion")
        implementation("com.ibm.icu:icu4j:$tui4jIcuVersion")
        implementation("org.apache.commons:commons-text:$tui4jCommonsTextVersion")
    } else {
        implementation("com.williamcallahan:tui4j:$tui4jVersion")
    }
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
