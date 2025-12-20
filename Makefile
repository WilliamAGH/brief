.PHONY: run run-local-tui build clean dist local-tui release-test release

run: build
	@set -a; [ -f .env ] && . ./.env; set +a; ./build/install/brief/bin/brief

run-local-tui:
	@cd ../tui4j && ./gradlew jar
	@TUI4J_LOCAL=true $(MAKE) run

build:
	@JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED" ./gradlew installDist -q 2>&1 | grep -v -E "(WARNING:|JAVA_TOOL_OPTIONS)" || true

local-tui:
	@cd ../tui4j && ./gradlew jar
	@TUI4J_LOCAL=true ./gradlew test

clean:
	@./gradlew clean 2>/dev/null

# Build distribution zip
dist:
	@./gradlew distZip -q
	@echo "Built: $$(ls build/distributions/*.zip)"

# Test release build with a version (usage: make release-test V=0.1.0)
release-test:
ifndef V
	@echo "Missing version. Usage:\n  make release-test V=0.1.0"
else
	@./gradlew clean distZip -Pversion=$(V) -q
	@unzip -o -q build/distributions/brief-$(V).zip -d build/distributions/
	@echo "Built: build/distributions/brief-$(V).zip"
	@echo "SHA256: $$(shasum -a 256 build/distributions/brief-$(V).zip | cut -d' ' -f1)"
	@echo "\nTest locally:"
	@echo "  ./build/distributions/brief-$(V)/bin/brief"
	@echo "\nWhen ready to publish:"
	@echo "  make release V=$(V)"
endif

# Create and push a release tag (usage: make release V=0.1.0)
release:
ifndef V
	@echo "Publish a release:\n  make release V=0.1.0\n\nTest first with:\n  make release-test V=0.1.0"
else
	@echo "Creating release v$(V)..."
	@git tag v$(V)
	@git push origin v$(V)
	@echo "Release v$(V) pushed. GitHub Actions will build and update homebrew-tap."
endif
