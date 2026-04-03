SHELL := /bin/bash
.PHONY: run run-local run-local-tui build clean dist local-tui release-test release test test-clipboard-local lint lint-ast lint-deprecation hooks-install

run: build
	@set -a; [ -f .env ] && . ./.env; set +a; ./build/install/brief/bin/brief

run-local run-local-tui:
	@cd ../tui4j && ./gradlew jar
	@TUI4J_LOCAL_JAR=$$(ls -t ../tui4j/build/libs/tui4j-*.jar 2>/dev/null | grep -v -e '-sources' -e '-javadoc' | head -n 1); \
	if [ -z "$$TUI4J_LOCAL_JAR" ]; then \
		echo "No local tui4j jar found in ../tui4j/build/libs"; \
		exit 1; \
	fi; \
	TUI4J_LOCAL_PATH=$$TUI4J_LOCAL_JAR TUI4J_SOURCE=local $(MAKE) run

build:
	@set -o pipefail; JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED" ./gradlew installDist -q 2>&1 | { grep -v -E "(WARNING:|JAVA_TOOL_OPTIONS)" || true; }

test:
	@set -o pipefail; JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED" ./gradlew test -q 2>&1 | { grep -v -E "(WARNING:|JAVA_TOOL_OPTIONS)" || true; }

test-clipboard-local: build
	@set -o pipefail; JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED" ./gradlew testClasses -q 2>&1 | { grep -v -E "(WARNING:|JAVA_TOOL_OPTIONS)" || true; }
	@./scripts/test-clipboard-local.sh

lint: lint-ast lint-deprecation ## Run all linters

lint-ast: ## Run ast-grep rules for Java naming and type safety
	@ast-grep scan -c sgconfig.yml src/main/java/

lint-deprecation: ## Fail on deprecated API usage from project or dependency code
	@set -o pipefail; JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED" ./gradlew deprecationCheck --rerun-tasks -q 2>&1 | { grep -v -E "(WARNING:|JAVA_TOOL_OPTIONS)" || true; }

hooks-install:
	@if ! command -v lefthook >/dev/null 2>&1; then \
		echo "lefthook not found in PATH."; \
		echo "Install it first, for example: brew install lefthook"; \
		exit 1; \
	fi
	@lefthook install -f

local-tui:
	@cd ../tui4j && ./gradlew jar
	@TUI4J_LOCAL_JAR=$$(ls -t ../tui4j/build/libs/tui4j-*.jar 2>/dev/null | grep -v -e '-sources' -e '-javadoc' | head -n 1); \
	if [ -z "$$TUI4J_LOCAL_JAR" ]; then \
		echo "No local tui4j jar found in ../tui4j/build/libs"; \
		exit 1; \
	fi; \
	TUI4J_LOCAL_PATH=$$TUI4J_LOCAL_JAR TUI4J_SOURCE=local ./gradlew test

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
