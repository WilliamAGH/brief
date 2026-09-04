# Brief Docker Setup

[English](README.md) · [Deutsch](README_de.md)

Dieses Verzeichnis enthält die Dateien zum Erstellen eines Docker-Images für Brief.

## Verwendung

1. **Setup und Build**:
   Führen Sie das Setup-Skript aus dem Hauptverzeichnis oder aus diesem Verzeichnis aus:
   ```bash
   ./docker/docker-setup.sh
   ```
   Dieses Skript führt einen Multi-Stage Docker-Build aus, der Gradle im ersten Schritt nutzt, um das Projekt zu bauen. Es ist kein lokales Java auf dem Host-System erforderlich.

2. **Ausführen**:
   Starten Sie den Container interaktiv. Es wird empfohlen, eine `.env`-Datei für die API-Keys zu verwenden und das Konfigurationsverzeichnis zu mounten:
   ```bash
   docker run -it --rm --env-file ~/.config/brief/.env -v ~/.config/brief:/home/brief/.config/brief brief
   ```

## Hinweise

- **Java Version**: Das Build-Image nutzt JDK 25, das finale Runtime-Image nutzt `eclipse-temurin:25-jre-noble`.
- **Terminal**: Für eine korrekte Darstellung der TUI wird `TERM=xterm-256color` im Container gesetzt.
- **Speicherort**: Die Anwendung im Container erwartet die Konfiguration unter `/home/brief/.config/brief`.
