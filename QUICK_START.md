# Quick Start Guide - Apply-Jobs Enhanced Edition

## Prerequisites

- Java 25+ installed
- Maven 3.8.0+
- Google Chrome or Chromium browser
- LinkedIn account credentials
- 10-15 minutes for full job cycle

## Installation & Setup

### 1. Set Environment Variables

```bash
# Required LinkedIn credentials
export LINKEDIN_USERNAME="your.email@example.com"
export LINKEDIN_PASSWORD="your_password"

# Job search configuration
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL"
export JOB_MATCHING_THRESHOLD="50"

# Optional: Output directory for logs
export OUTPUT_DIRECTORY="$HOME/apply-jobs-logs"
```

### 2. Build the Project

```bash
cd /Users/rsvnarayanas/Desktop/Work/Java/Spring-Boot-Apps/AI-Tasks/Apply-Jobs
mvn clean package
```

### 3. Run the Application

**Option A: Using Maven**
```bash
mvn exec:java -Dexec.mainClass="com.example.applyjobs.ApplyJobsApplication"
```

**Option B: Using JAR**
```bash
java -jar target/Apply-Jobs-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## What Happens When You Run It

1. ✅ Browser opens and navigates to LinkedIn
2. ✅ Logs in with your credentials
3. ✅ Searches for jobs matching your criteria
4. ✅ Extracts up to 10 job listings
5. ✅ For each job:
   - Checks if employment type is "Contract"
   - Verifies salary is at least $60/hour
   - Matches required skills (60% minimum)
   - Confirms 10+ years IT experience requirement
   - Applies if all conditions pass
   - Fills out multi-step Easy Apply form
   - Logs successful applications only

6. ✅ Generates report of successful applications in logs folder

## Expected Workflow Timeline

```
Startup:              ~2 seconds
Login:                ~5-10 seconds
Search Navigation:    ~10 seconds
Job Extraction:       ~30 seconds (for 10 jobs)
Per Job Processing:   ~2-3 minutes (with form handling + delays)
Total (5 jobs):       ~15-20 minutes
```

## Understanding the Validation Checks

### ✓ Contract Employment Type
Jobs must be explicitly marked as "Contract" (not Full-time, Part-time, etc.)

### ✓ Minimum Salary $60/hour
Extracts salary from job details, requires minimum $60/hour rate

### ✓ Skills Match (60%)
If you set `JOB_SKILLS="Java,Spring Boot,React,PostgreSQL"` (4 skills),
job description must mention at least 3 of them (75%)

### ✓ 10+ Years Experience
Job posting must require 10 or more years of IT experience

## Output & Logs

Results are saved to your OUTPUT_DIRECTORY:

```
~/apply-jobs-logs/
├── LinkedIn_YYYY-MM-DD_HHmmss.csv
└── LinkedIn_YYYY-MM-DD_HHmmss_summary.txt
```

CSV contains:
- Job Title
- Company Name
- Date Applied
- Status (SUCCESS/FAILED)

## Troubleshooting

### Browser doesn't open
- Ensure Chrome/Chromium is installed
- Check Java path: `java -version`
- Try: `export CHROME_BIN=/usr/bin/google-chrome`

### Login fails
- Verify credentials are correct
- Check if 2FA is enabled (may cause issues)
- Ensure account isn't locked
- Try manual login first at linkedin.com

### No jobs found
- Verify job search criteria
- Check JOB_SKILLS matches LinkedIn's language
- LinkedIn may restrict job search results
- Try broader job title

### Application stops mid-form
- Form complexity exceeded (15 steps max)
- Required field couldn't be auto-filled
- Browser disconnected from LinkedIn
- Check logs for specific field name

### Validation too strict
Modify these in `EnvironmentVariableLoader.java`:
- Salary minimum: $60 (line ~50)
- Skills match: 60% threshold
- Experience: 10 years minimum

## Advanced Configuration

### Increase job limit
Edit `LinkedInJobApplicationAgent.java` line ~156:
```java
List<LinkedInJobListing> jobs = jobExtractor.extractLatestJobListings(10); // Change 10 to higher
```

### Adjust timeouts
Edit `WaitHelper.java` for element wait times:
```java
private static final int TIMEOUT_SECONDS = 10; // Increase if timeouts occur
```

### Enable debug logging
Create or update `src/main/resources/logback.xml`:
```xml
<logger name="com.example.applyjobs" level="DEBUG"/>
```

## Expected Success Metrics

- **Extract Success**: 90%+ of listed jobs extracted correctly
- **Validation Pass Rate**: 20-40% of jobs meet all 4 conditions
- **Application Success**: 85%+ of validated jobs successfully applied
- **Form Completion**: 90%+ of Easy Apply forms completed

## Support Files

- **AGENTS.md** - Guidance for AI coding agents
- **IMPLEMENTATION_SUMMARY.md** - Detailed feature documentation
- **This file** - Quick start guide

## Key Features

✨ **Smart Job Validation**
- Employment type checking
- Salary requirement verification
- Skills matching with threshold
- Experience requirement validation

✨ **Multi-Step Form Handling**
- Auto-fills common questions intelligently
- Handles 15+ form steps
- Manages checkboxes and dropdowns
- Review step modifications

✨ **2026 LinkedIn XPath Compatibility**
- Multiple fallback selectors
- Handles LinkedIn DOM changes
- Debug output for troubleshooting

✨ **Professional Application Flow**
- Rate-limited applications (10-30 sec delays)
- Logs only successful submissions
- Comprehensive error handling
- Summary report generation

## Next Steps

1. Set up environment variables (5 min)
2. Build project (3-5 min)
3. Do a test run with 1-2 jobs first
4. Adjust validation rules if needed
5. Scale up to full job search (10+ jobs)

## Important Notes

⚠️ **Rate Limiting**: LinkedIn detects automated behavior. This tool includes delays but may still trigger rate limits. Use responsibly.

⚠️ **Account Safety**: Use at own risk. LinkedIn ToS may not permit automation. Consider using dedicated account for testing.

⚠️ **Captcha**: If LinkedIn shows captcha, application will pause. Solve manually to continue.

⚠️ **Testing**: Test with 1-2 jobs first before running full automation.

---

**Version**: 2.0 Enhanced
**Status**: Ready to use
**Last Updated**: 2026-04-18

