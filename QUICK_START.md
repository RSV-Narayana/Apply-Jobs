# Quick Start: Running the LinkedIn Agent

## Step 1: Set Environment Variables

```bash
# LinkedIn credentials (REQUIRED)
export LINKEDIN_USERNAME="your_email@gmail.com"
export LINKEDIN_PASSWORD="your_secure_password"

# Job search parameters (REQUIRED)
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL,Docker"

# Optional: Fine-tune matching
export JOB_MATCHING_THRESHOLD=50
export OUTPUT_DIRECTORY=$(pwd)
```

## Step 2: Build the Project

```bash
cd /Users/rsvnarayanas/Desktop/Work/Java/Spring-Boot-Apps/AI-Tasks/Apply-Jobs
./gradlew clean build -x test
```

**Expected Output:**
```
BUILD SUCCESSFUL in 700ms
```

## Step 3: Run the LinkedIn Agent

### Option A: Using the JAR directly
```bash
java -jar build/libs/Apply-Jobs-0.0.1-SNAPSHOT.jar linkedin
```

### Option B: Using the provided script
```bash
./run_linkedin_agent.sh
```

### Option C: Using Gradle
```bash
./gradlew bootRun --args='linkedin'
```

## Step 4: Monitor the Execution

The agent will output logs to the console:
```
INFO  - Starting LinkedIn Job Application Workflow
INFO  - Initializing browser automation
INFO  - Performing LinkedIn login
INFO  - Navigating to LinkedIn jobs page
INFO  - Searching for jobs: Full Stack Engineer
INFO  - Extracted 5 jobs
INFO  - Processing job: Full Stack Engineer - TechCorp
INFO  - Job matches criteria, attempting to apply
INFO  - Applied to job - Status: SUCCESS
...
```

## Step 5: Check Results

After the agent completes, view the results:

```bash
# View the log file
cat applied_jobs_linkedin_$(date +%Y-%m-%d).txt

# Expected output:
# Job Title    Company Name    Application Date    Status
# Full Stack Engineer    TechCorp    2024-04-17    SUCCESS
# Senior Engineer        DataCorp    2024-04-17    SUCCESS
```

## Complete Example: One Command

```bash
#!/bin/bash

cd /Users/rsvnarayanas/Desktop/Work/Java/Spring-Boot-Apps/AI-Tasks/Apply-Jobs

# Set credentials and preferences
export LINKEDIN_USERNAME="your_email@gmail.com"
export LINKEDIN_PASSWORD="your_password"
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL,Docker"

# Build and run
./gradlew clean build -x test && \
java -jar build/libs/Apply-Jobs-0.0.1-SNAPSHOT.jar linkedin

# Show results
echo ""
echo "Results saved to:"
cat applied_jobs_linkedin_$(date +%Y-%m-%d).txt
```

## What the Agent Does

1. ✅ Opens Chrome browser
2. ✅ Logs into LinkedIn with your credentials
3. ✅ Navigates to LinkedIn Jobs section
4. ✅ Searches for "Full Stack Engineer" (or your JOB_TITLE)
5. ✅ Extracts the 5 most recent job postings
6. ✅ For each job:
   - Checks if title matches
   - Checks if it has 50%+ of your required skills
   - If it matches: Applies to the job
   - Fills out the Easy Apply form
   - Answers additional questions intelligently
   - Unchecks "Follow company" checkbox
   - Submits the application
7. ✅ Logs all results with timestamps
8. ✅ Generates a summary report

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Credentials not set" | Make sure LINKEDIN_USERNAME and LINKEDIN_PASSWORD are exported |
| "No recent search found" | The agent will perform a new search automatically |
| "ChromeDriver not found" | WebDriverManager will auto-download it |
| "LinkedIn login failed" | Verify username/password are correct |
| "No jobs found" | Try different JOB_TITLE or search manually on LinkedIn |

## Important Notes

⚠️ **Before Running:**
- ✓ LinkedIn account should be accessible
- ✓ No 2FA or manual verification required
- ✓ Chrome/Chromium browser must be installed
- ✓ Network connectivity must be available

⚠️ **Respect LinkedIn's Terms:**
- ✓ Only apply to jobs you're genuinely interested in
- ✓ The agent will rate-limit (10-30s between applications)
- ✓ Use responsibly and monitor the first run

## Files Created

```
Apply-Jobs/
├── build/
│   └── libs/
│       └── Apply-Jobs-0.0.1-SNAPSHOT.jar  (Runnable JAR)
├── src/main/java/
│   └── com/example/applyjobs/
│       ├── automation/           (Browser automation)
│       ├── config/               (Configuration loading)
│       ├── exception/            (Custom exceptions)
│       ├── linkedin/             (LinkedIn agent)
│       ├── logger/               (Result logging)
│       ├── matcher/              (Job matching)
│       ├── model/                (Data models)
│       └── ApplyJobsApplication.java
├── AGENTS.md                     (Developer guide)
├── IMPLEMENTATION_SUMMARY.md     (What was built)
├── LINKEDIN_AGENT_README.md      (Detailed usage)
├── QUICK_START.md                (This file)
└── run_linkedin_agent.sh         (Runner script)
```

## Support

For detailed information:
- **AGENTS.md** - Architecture and patterns
- **LINKEDIN_AGENT_README.md** - Complete guide
- **IMPLEMENTATION_SUMMARY.md** - What was implemented
- **.github/agents/** - Extensive documentation (18 files)

---

**Ready to go! Run the agent now:** 🚀

```bash
java -jar build/libs/Apply-Jobs-0.0.1-SNAPSHOT.jar linkedin
```

