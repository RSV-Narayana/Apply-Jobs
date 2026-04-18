#!/bin/bash

# LinkedIn Job Application Agent - Executable Script
# This script properly sets up and runs the application

set -e

echo "=========================================="
echo "LinkedIn Job Application Agent"
echo "=========================================="
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 25+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep -oP '(?<=version ")[^"]*' || echo "unknown")
echo "✓ Java detected: $JAVA_VERSION"

# Check if JAR exists
JAR_FILE="/Users/rsvnarayanas/Desktop/Work/Java/Spring-Boot-Apps/AI-Tasks/Apply-Jobs/target/Apply-Jobs-0.0.1-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo ""
    echo "❌ JAR file not found: $JAR_FILE"
    echo ""
    echo "Building project first..."
    cd /Users/rsvnarayanas/Desktop/Work/Java/Spring-Boot-Apps/AI-Tasks/Apply-Jobs
    mvn clean package -DskipTests
    echo ""
fi

echo "✓ JAR file found: $(ls -lh $JAR_FILE | awk '{print $9, "(" $5 ")"}')"
echo ""

# Check environment variables
MISSING_VARS=""
[ -z "$LINKEDIN_USERNAME" ] && MISSING_VARS="$MISSING_VARS LINKEDIN_USERNAME"
[ -z "$LINKEDIN_PASSWORD" ] && MISSING_VARS="$MISSING_VARS LINKEDIN_PASSWORD"
[ -z "$JOB_TITLE" ] && MISSING_VARS="$MISSING_VARS JOB_TITLE"
[ -z "$JOB_SKILLS" ] && MISSING_VARS="$MISSING_VARS JOB_SKILLS"

if [ ! -z "$MISSING_VARS" ]; then
    echo "❌ Missing environment variables:$MISSING_VARS"
    echo ""
    echo "Please set these variables:"
    echo "  export LINKEDIN_USERNAME=\"your_email@gmail.com\""
    echo "  export LINKEDIN_PASSWORD=\"your_password\""
    echo "  export JOB_TITLE=\"Full Stack Engineer\""
    echo "  export JOB_SKILLS=\"Java,Spring Boot,React,PostgreSQL\""
    echo ""
    exit 1
fi

echo "✓ All environment variables set:"
echo "  - LINKEDIN_USERNAME: ${LINKEDIN_USERNAME:0:20}..."
echo "  - LINKEDIN_PASSWORD: ****"
echo "  - JOB_TITLE: $JOB_TITLE"
echo "  - JOB_SKILLS: $JOB_SKILLS"
echo ""

echo "=========================================="
echo "Starting application..."
echo "=========================================="
echo ""

# Run the JAR with all environment variables
cd /Users/rsvnarayanas/Desktop/Work/Java/Spring-Boot-Apps/AI-Tasks/Apply-Jobs

# Use exec to replace this process with Java
exec java \
    -Dfile.encoding=UTF-8 \
    -Duser.language=en \
    -jar "$JAR_FILE" \
    linkedin

