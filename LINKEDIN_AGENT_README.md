# Running the LinkedIn Job Application Agent

## Overview

The Apply-Jobs application includes a fully functional LinkedIn Job Application Agent that automates job applications on LinkedIn.

## Prerequisites

1. **Java 25+** installed
2. **Gradle** (included via gradlew)
3. **Chrome/Chromium browser** installed
4. **LinkedIn account** with valid credentials
5. **Environment variables configured**

## Environment Variables

Before running the agent, set the following environment variables:

```bash
# Required: LinkedIn Credentials
export LINKEDIN_USERNAME="your_email@gmail.com"
export LINKEDIN_PASSWORD="your_password"

# Required: Job Search Parameters
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL,Docker"

# Optional: Matching Configuration
export JOB_MATCHING_THRESHOLD=50  # Skill match percentage (default: 50)
export OUTPUT_DIRECTORY="/path/to/output"  # Where to save results (default: current directory)
```

## Running the Agent

### Option 1: Run as Spring Boot Application

```bash
# Build the project
./gradlew clean build -x test

# Run with LinkedIn agent
java -jar build/libs/Apply-Jobs-0.0.1-SNAPSHOT.jar linkedin
```

### Option 2: Run via Gradle

```bash
# Build and run in one command
./gradlew bootRun --args='linkedin'
```

### Option 3: Run the standalone runner

```bash
# Build first
./gradlew clean build -x test

# Run the dedicated runner
java -cp build/libs/Apply-Jobs-0.0.1-SNAPSHOT.jar com.example.applyjobs.LinkedInJobApplicationRunner
```

## Workflow

The LinkedIn agent performs the following steps:

1. **Initialize Browser**: Opens Chrome browser with Selenium WebDriver
2. **Login**: Authenticates with provided LinkedIn credentials
3. **Navigate to Jobs**: Goes to LinkedIn Jobs section
4. **Search**: Searches for the specified job title (tries recent searches first)
5. **Extract Jobs**: Extracts the latest 5 job listings
6. **Match Jobs**: Filters jobs based on:
   - Job title matching (must contain the specified job title)
   - Skills matching (must have at least 50% of required skills)
7. **Apply to Jobs**: For each matching job:
   - Clicks the Apply button
   - Fills out the Easy Apply form
   - Handles additional questions intelligently
   - Unchecks "Follow company" checkbox on review step
   - Submits the application
8. **Log Results**: Records all applications in `applied_jobs_linkedin_YYYY-MM-DD.txt`
9. **Cleanup**: Closes the browser and generates summary

## Output

Results are logged to: `applied_jobs_linkedin_YYYY-MM-DD.txt`

Format (tab-separated):
```
Job Title    | Company Name    | Application Date | Status
Full Stack Engineer | TechCorp | 2024-04-17 | SUCCESS
```

## Example: Complete Workflow

```bash
# 1. Set environment variables
export LINKEDIN_USERNAME="your_email@example.com"
export LINKEDIN_PASSWORD="your_password"
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL,Docker"
export JOB_MATCHING_THRESHOLD=50

# 2. Build the project
./gradlew clean build -x test

# 3. Run the agent
java -jar build/libs/Apply-Jobs-0.0.1-SNAPSHOT.jar linkedin

# 4. Check results
cat applied_jobs_linkedin_$(date +%Y-%m-%d).txt
```

## Troubleshooting

### Login Issues
- Verify LinkedIn credentials are correct
- Check if 2FA is enabled (agent may need manual intervention)
- Ensure network connectivity

### No Jobs Found
- Check if the job title matches recent searches on LinkedIn
- Try searching with a simpler job title

### Application Failures
- Check browser console for JavaScript errors
- Verify LinkedIn UI hasn't changed significantly
- Increase timeout values in WaitHelper if network is slow

### Output File Not Created
- Ensure OUTPUT_DIRECTORY has write permissions
- Check logs for I/O errors

## Logs

All operations are logged with SLF4J. Check console output for:
- INFO: Workflow progress
- WARN: Non-critical issues (e.g., missing elements)
- ERROR: Critical failures

## Advanced Configuration

### Custom Response Generation

Edit `LinkedInApplicationHandler.generateResponseForQuestion()` to customize responses for different question types.

### Adjust Wait Timeouts

Modify timeout values in `WaitHelper` constructor calls:
```java
new WaitHelper(driver, 60)  // 60 second timeout
```

### Rate Limiting

Adjust delays between applications in `LinkedInApplicationHandler`:
```java
private static final int MIN_DELAY = 10000;  // 10 seconds
private static final int MAX_DELAY = 30000;  // 30 seconds
```

## Support

For issues or questions, refer to:
- `.github/agents/LINKEDIN_AGENT.md` - Detailed implementation
- `.github/agents/BROWSER_AUTOMATION.md` - Selenium patterns
- `.github/agents/JOB_MATCHING.md` - Matching algorithm
- `AGENTS.md` - Quick reference for developers

## Important Notes

⚠️ **Use Responsibly**
- Respect LinkedIn's Terms of Service
- Don't apply to jobs that don't match your profile
- Monitor the agent's behavior
- Consider the impact on job postings

✅ **Best Practices**
- Start with a small test run (2-3 jobs)
- Monitor the log file for accuracy
- Adjust matching criteria as needed
- Schedule runs during reasonable hours

