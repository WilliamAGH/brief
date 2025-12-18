.PHONY: run build clean

run: build
	@set -a; [ -f .env ] && . ./.env; set +a; ./build/install/brief/bin/brief

build:
	@JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED" ./gradlew installDist -q 2>&1 | grep -v -E "(WARNING:|JAVA_TOOL_OPTIONS)" || true

clean:
	@./gradlew clean 2>/dev/null
