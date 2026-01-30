# Development Guide

## Deterministic Build Setup

This project uses a reproducible build system across macOS/Linux local dev and CI/CD, with **Gradle Toolchains** and **Temurin JDK 25**.

### Local Development

#### Prerequisites

The project is configured to use **Gradle Toolchains** with **Temurin JDK 25** for build-time determinism.

##### Option 1: Using mise (recommended)

[mise](https://mise.jdnow.dev/) is a modern version manager that reads `.tool-versions`.

```bash
# Install mise (one-time)
curl https://mise.jdnow.dev | sh

# Then, in the repo:
mise install
```

This sets `JAVA_HOME` correctly for your terminal, Gradle, and IntelliJ.

##### Option 2: Using asdf

[asdf](https://asdf-vm.com/) is a general version manager.

```bash
# Install asdf (one-time)
git clone https://github.com/asdf-vm/asdf.git ~/.asdf
cd ~/.asdf && git checkout "$(git describe --abbrev=0 --tags)"

# Add the Java plugin
asdf plugin add java https://github.com/halcyon/asdf-java.git

# In the repo:
asdf install
```

#### How It Works

1. **`.tool-versions` file** pins `java = temurin 25`.
2. **Gradle Wrapper** (`./gradlew`) pins Gradle 9.2.0.
3. **Gradle Toolchains** auto-downloads Temurin JDK 25 if missing (enabled by Foojay resolver in `settings.gradle.kts`).
4. **Result**: Consistent Java version across:
   - Shell commands (`gradle build`, `java -version`)
   - IntelliJ "Gradle JVM" setting
   - IntelliJ "Project SDK" setting

#### Verifying Setup

```bash
# Check Java version (should be Temurin 25.x.x)
java -version

# Verify Gradle uses correct toolchain
./gradlew --version

# Build
make build
```

### CI/CD (GitHub Actions)

The `.github/workflows/build.yml` workflow:

- Runs on **ubuntu-24.04** (pinned, not `-latest`)
- Uses `actions/setup-java@v5` with `distribution: temurin` + `java-version: 25`
- Logs Java, Gradle, and OS versions for drift detection

**Release workflow** (`.github/workflows/release.yml`):

- Triggered on version tags (`v*`)
- Uses same Temurin Java 25 + latest Actions versions
- Builds distribution ZIP and updates Homebrew tap

### Gradle Configuration

#### `settings.gradle.kts` (Foojay Resolver)

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
```

Enables **auto-download** of Temurin JDK if missing.

#### `build.gradle.kts` (Toolchain Vendor)

```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.ADOPTOPENJDK
    }
}
```

Explicitly specifies Temurin JDK 25 for all build tasks.

#### `gradle.properties` (Daemon & Caching)

```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx512m --enable-native-access=ALL-UNNAMED
```

Enables parallel builds, incremental build caching, and Gradle daemon for faster rebuilds.

---

## JDK Patch Versioning

### The Limitation

**Gradle Toolchains cannot pin patch-level JDK versions** (e.g., 25.0.3). It can only pin major version (25) + vendor (Temurin).

### Strategy

1. **Local dev**: Patch version determined by `mise`/`asdf`.
2. **CI/CD**: GitHub Actions logs exact Java patch version for drift detection.
3. **When to bump patch**: Patch updates to JDK are **intentional commits**. Never rely on automatic patch upgrades.

### Intentional Patch Bumping

When you want to upgrade from 25.0.3 → 25.0.4:

```bash
# Update local version manager
mise use java@temurin 25.0.4  # or asdf local java temurin-25.0.4

# Update .tool-versions
cat .tool-versions
# java = temurin 25.0.4

# Commit with audit trail
git add .tool-versions
git commit -m "Bump JDK patch: 25.0.3 → 25.0.4"
```

CI will log the new patch version automatically.

---

## Common Commands

### Development

```bash
# Install Java (one-time)
mise install  # or: asdf install

# Build
make build

# Run
make run

# Run against local tui4j (builds tui4j first)
make run-local

# Force Maven tui4j even if local jar exists
TUI4J_SOURCE=maven make run

# Clean
make clean

# Test
./gradlew test

# Local TUI4J tests (builds tui4j first)
make run-local-tui

# Create distribution
make dist
```

### Release

```bash
# Test release build locally
make release-test V=0.2.0

# Create and push release tag (triggers GitHub Actions)
make release V=0.2.0
```

---

## Troubleshooting

#### "gradle: command not found"
- Run `mise install` or `asdf install` to set `JAVA_HOME`
- Verify: `echo $JAVA_HOME` should point to a Temurin 25 JDK

#### Gradle downloads JDK every time
- Ensure `settings.gradle.kts` has Foojay resolver (v0.9.0)
- Check `~/.gradle/jdks/` for cached toolchains

#### IntelliJ doesn't pick up Java version
- Open IntelliJ settings → Build, Execution, Deployment → Gradle
- Set "Gradle JVM" to "Use JAVA_HOME"
- Set "Project SDK" to Temurin 25 (or refresh if auto-detected)

#### CI build fails but local works
- Check CI logs for Java version (e.g., `java -version`)
- Ensure your local patch matches
- If CI is on 25.0.4 and you're on 25.0.3, bump locally and re-test

---

## TUI4J source selection

By default, the build will use a local tui4j jar if one exists, unless running in CI.
You can override this explicitly:

```bash
# Use local tui4j jar
TUI4J_SOURCE=local make run

# Force Maven tui4j artifact
TUI4J_SOURCE=maven make run
```

Gradle property form is also supported:

```bash
./gradlew -Ptui4jSource=local installDist
./gradlew -Ptui4jSource=maven installDist
```

---

## References

- [Gradle Toolchains](https://docs.gradle.org/current/userguide/toolchains.html)
- [Foojay Resolver Convention](https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention)
- [Eclipse Temurin JDK](https://adoptium.net/)
- [mise version manager](https://mise.jdnow.dev/)
- [asdf version manager](https://asdf-vm.com/)
- [TUI4J](https://github.com/WilliamAGH/tui4j)
