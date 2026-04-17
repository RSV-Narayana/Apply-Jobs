#!/bin/bash

# LinkedIn Job Application Agent Runner Script
# Usage: ./run_linkedin_agent.sh

set -e

echo "=========================================="
echo "LinkedIn Job Application Agent"
echo "=========================================="
echo ""

# Check if environment variables are set
if [ -z "$LINKEDIN_USERNAME" ] || [ -z "$LINKEDIN_PASSWORD" ]; then
    echo "❌ Error: LinkedIn credentials not set!"
    echo ""
    echo "Please set the following environment variables:"
    echo "  export LINKEDIN_USERNAME=\"your_email@example.com\""
    echo "  export LINKEDIN_PASSWORD=\"your_password\""
    echo "  export JOB_TITLE=\"Full Stack Engineer\""
    echo "  export JOB_SKILLS=\"Java,Spring Boot,React,PostgreSQL\""
    echo ""
    exit 1
fi

# Set default values if not provided
JOB_TITLE="${JOB_TITLE:-Full Stack Engineer}"
JOB_SKILLS="${JOB_SKILLS:-Java,Spring Boot,React,PostgreSQL}"
JOB_MATCHING_THRESHOLD="${JOB_MATCHING_THRESHOLD:-50}"
OUTPUT_DIRECTORY="${OUTPUT_DIRECTORY:-.}"

echo "✅ Configuration:"
echo "   Job Title: $JOB_TITLE"
echo "   Job Skills: $JOB_SKILLS"
echo "   Matching Threshold: $JOB_MATCHING_THRESHOLD%"
echo "   Output Directory: $OUTPUT_DIRECTORY"
echo ""

# Build the project
echo "Building project..."
./gradlew clean build -x test > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful"
echo ""

# Run the agent
echo "Starting LinkedIn Job Application Agent..."
echo ""

java -jar build/libs/Apply-Jobs-0.0.1-SNAPSHOT.jar linkedin

echo ""
echo "=========================================="
echo "Agent completed!"
echo "Check results in: applied_jobs_linkedin_$(date +%Y-%m-%d).txt"
echo "=========================================="

