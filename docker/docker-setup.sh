#!/bin/bash
set -e

# Navigiere zum Root-Verzeichnis des Projekts (falls das Skript aus dem docker-Verzeichnis aufgerufen wird)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
cd "$PROJECT_ROOT"

echo "Baue das Docker-Image 'brief' (inklusive Gradle-Build im Container)..."
docker build -t brief -f "$SCRIPT_DIR/Dockerfile" "$PROJECT_ROOT"

echo ""
echo "Erfolg! Du kannst brief nun über Docker starten."
echo "Damit deine Konfiguration und API-Keys erhalten bleiben, mounte bitte dein Konfigurationsverzeichnis:"
echo ""
echo "docker run -it --rm -v ~/.config/brief:/root/.config/brief brief"
echo ""
echo "Falls du Umgebungsvariablen für API-Keys nutzt, kannst du diese ebenfalls übergeben:"
echo "docker run -it --rm -e OPENAI_API_KEY=\$OPENAI_API_KEY brief"
