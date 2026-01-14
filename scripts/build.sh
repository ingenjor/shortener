#!/bin/bash

# Build script for Shortener Service

echo "🔨 Building Shortener Service..."

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven is not installed"
    echo "📚 Please install Maven to build the project"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "❌ Error: Java 11 or higher is required"
    echo "📚 Current Java version: $JAVA_VERSION"
    exit 1
fi

# Create necessary directories
mkdir -p config logs

# Check if config file exists
if [ ! -f "config/application.yaml" ]; then
    echo "📝 Creating default configuration file..."
    cat > config/application.yaml << 'EOF'
app:
  name: "Shortener Service"
  version: "1.0.0"

link:
  short-code-length: 7
  default-ttl-hours: 24
  default-max-clicks: 100
  generation-algorithm: "BASE62"

notification:
  expire-notification: true
  limit-notification: true

cleanup:
  check-interval-minutes: 5
  auto-delete-expired: true

security:
  owner-only-operations: true
  user-session-ttl-hours: 168

logging:
  level: INFO
  file: "logs/shortener.log"
EOF
    echo "✅ Default configuration created at config/application.yaml"
fi

# Clean previous build
echo "🧹 Cleaning previous build..."
mvn clean

# Run tests
echo "🧪 Running tests..."
if ! mvn test; then
    echo "❌ Tests failed! Aborting build."
    exit 1
fi

# Build project
echo "🔧 Building project..."
if ! mvn package -DskipTests; then
    echo "❌ Build failed!"
    exit 1
fi

echo ""
echo "🎉 Build successful!"
echo "📦 JAR file created: target/shortener-1.0.0.jar"
echo ""
echo "🚀 To run the application:"
echo "  java -jar target/shortener-1.0.0.jar"
echo "  or"
echo "  ./scripts/run.sh"
