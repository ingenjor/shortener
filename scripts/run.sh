#!/bin/bash

# Run script for Shortener Service

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
JAR_FILE="$PROJECT_DIR/target/shortener-1.0.0.jar"

echo "🚀 Starting Shortener Service..."

# Check if JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR file not found: $JAR_FILE"
    echo "🔨 Building project first..."
    "$SCRIPT_DIR/build.sh"

    if [ ! -f "$JAR_FILE" ]; then
        echo "❌ Build failed or JAR file not created"
        exit 1
    fi
fi

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "❌ Error: Java 11 or higher is required"
    echo "📚 Current Java version: $JAVA_VERSION"
    exit 1
fi

# Create logs directory if it doesn't exist
mkdir -p "$PROJECT_DIR/logs"

# Run the application
echo "🎯 Launching application..."
echo "📋 Logs will be written to: $PROJECT_DIR/logs/shortener.log"
echo "⏸️  Press Ctrl+C to stop"
echo "─".repeat(50)

cd "$PROJECT_DIR" && java -jar "$JAR_FILE"
