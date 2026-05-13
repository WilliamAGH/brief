# Brief Docker Setup

[English](README.md) · [Deutsch](README_de.md)

This directory contains the files for creating a Docker image for Brief.

## Usage

1. **Setup and Build**:
   Run the setup script from the root directory or from this directory:
   ```bash
   ./docker/docker-setup.sh
   ```
   This script performs a multi-stage Docker build, which uses Gradle in the first step to build the project. No local Java is required on the host system.

2. **Run**:
   Start the container interactively. It is recommended to use a `.env` file for the API keys and to mount the configuration directory:
   ```bash
   docker run -it --rm --env-file ~/.config/brief/.env -v ~/.config/brief:/root/.config/brief brief
   ```

## Notes

- **Java Version**: The build image uses JDK 25, the final runtime image uses `eclipse-temurin:25-jre-noble`.
- **Terminal**: For correct TUI display, `TERM=xterm-256color` is set in the container.
- **Storage Location**: The application in the container expects the configuration at `/root/.config/brief`.
