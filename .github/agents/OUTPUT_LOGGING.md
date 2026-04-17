# Output Logging Guide

## Overview

The Job Application Agent logs all applied jobs to timestamped text files with provider information (LinkedIn, Dice). This guide covers file format, logging mechanisms, and result tracking.

## Output File Format

### File Naming Convention

```
applied_jobs_YYYY-MM-DD_PROVIDER.txt
```

**Example:**
```
applied_jobs_2026-04-16_linkedin.txt
applied_jobs_2026-04-16_dice.txt
```

**Components:**
- `applied_jobs` - Fixed prefix
- `YYYY-MM-DD` - Current date in ISO format
- `PROVIDER` - Portal name in lowercase (linkedin, dice)
- `.txt` - Text file extension

### File Content Format

```
================================================================================
JOB APPLICATION RESULTS - LINKEDIN
Generated: 2026-04-16 14:30:45
================================================================================

Job Applied: 1
Date: 2026-04-16 14:30:45
Title: Full Stack Engineer
Company: Tech Company Inc.
Location: San Francisco, CA
URL: https://www.linkedin.com/jobs/view/123456789
Match Score: 1.0 (100%)
Status: SUCCESS

Job Applied: 2
Date: 2026-04-16 14:31:15
Title: Full Stack Software Engineer
Company: Innovation Corp
Location: New York, NY
URL: https://www.linkedin.com/jobs/view/987654321
Match Score: 0.95 (95%)
Status: SUCCESS

Job Failed: 1
Date: 2026-04-16 14:32:00
Title: Senior Full Stack Developer
Company: Enterprise Solutions Ltd
Location: Remote
URL: https://www.linkedin.com/jobs/view/555666777
Match Score: 0.85 (85%)
Status: FAILED - Apply button not found

================================================================================
SUMMARY
================================================================================
Total Jobs Processed: 5
Total Jobs Applied: 2
Total Jobs Failed: 1
Total Jobs Skipped: 2
Success Rate: 40%
Duration: 5 minutes 23 seconds

Job Skills Used for Matching:
- Java
- Spring Boot
- React
- PostgreSQL
- Docker

Target Job Title: Full Stack Engineer

================================================================================
```

## Java Implementation

### Result Logger Class

```java
public class ResultLogger {
    private String provider; // "linkedin" or "dice"
    private String filePath;
    private PrintWriter writer;
    private int jobsApplied = 0;
    private int jobsFailed = 0;
    private int jobsSkipped = 0;
    private LocalDateTime startTime;
    private static final Logger logger = LoggerFactory.getLogger(ResultLogger.class);
    
    public ResultLogger(String provider) {
        this.provider = provider.toLowerCase();
        this.startTime = LocalDateTime.now();
        initializeFile();
    }
    
    private void initializeFile() {
        try {
            // Create output directory if it doesn't exist
            File outputDir = new File("applied_jobs_logs");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // Generate filename with date
            String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            String filename = String.format("applied_jobs_%s_%s.txt", dateStr, provider);
            this.filePath = outputDir.getAbsolutePath() + File.separator + filename;
            
            // Initialize writer
            this.writer = new PrintWriter(new FileWriter(filePath, true)); // append mode
            
            // Write header if file is new
            if (new File(filePath).length() == 0) {
                writeHeader();
            }
            
            logger.info("Initialized result logger: {}", filePath);
            
        } catch (IOException e) {
            logger.error("Failed to initialize result logger", e);
            throw new RuntimeException(e);
        }
    }
    
    private void writeHeader() {
        writer.println("================================================================================");
        writer.printf("JOB APPLICATION RESULTS - %s%n", provider.toUpperCase());
        writer.printf("Generated: %s%n", LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        ));
        writer.println("================================================================================");
        writer.println();
    }
    
    /**
     * Log a successful job application
     */
    public void logJob(JobListing job) {
        logJob(job, true);
    }
    
    /**
     * Log a job application with status
     */
    public void logJob(JobListing job, boolean success) {
        try {
            jobsApplied++;
            
            writer.printf("Job Applied: %d%n", jobsApplied);
            writer.printf("Date: %s%n", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            writer.printf("Title: %s%n", job.getTitle());
            writer.printf("Company: %s%n", job.getCompany());
            writer.printf("Location: %s%n", job.getLocation());
            
            // Include job URL if available
            if (job instanceof LinkedInJobListing) {
                LinkedInJobListing linkedinJob = (LinkedInJobListing) job;
                writer.printf("URL: https://www.linkedin.com/jobs/view/%s%n", linkedinJob.getId());
            } else if (job instanceof DiceJobListing) {
                DiceJobListing diceJob = (DiceJobListing) job;
                writer.printf("URL: %s%n", diceJob.getJobUrl());
            }
            
            writer.printf("Status: %s%n", success ? "SUCCESS" : "FAILED");
            writer.println();
            
            writer.flush();
            
        } catch (Exception e) {
            logger.error("Failed to log job", e);
        }
    }
    
    /**
     * Log a failed application
     */
    public void logFailedJob(JobListing job, String reason) {
        try {
            jobsFailed++;
            
            writer.printf("Job Failed: %d%n", jobsFailed);
            writer.printf("Date: %s%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            writer.printf("Title: %s%n", job.getTitle());
            writer.printf("Company: %s%n", job.getCompany());
            writer.printf("Location: %s%n", job.getLocation());
            writer.printf("Reason: %s%n", reason);
            writer.printf("Status: FAILED%n");
            writer.println();
            
            writer.flush();
            
        } catch (Exception e) {
            logger.error("Failed to log failed job", e);
        }
    }
    
    /**
     * Log a skipped job
     */
    public void logSkippedJob(JobListing job, String reason) {
        jobsSkipped++;
        logger.debug("Skipped job: {} - {}", job.getTitle(), reason);
    }
    
    /**
     * Write final summary
     */
    public void writeSummary(List<String> appliedJobTitles, String jobTitle, List<String> jobSkills) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            
            writer.println();
            writer.println("================================================================================");
            writer.println("SUMMARY");
            writer.println("================================================================================");
            
            int totalProcessed = jobsApplied + jobsFailed + jobsSkipped;
            double successRate = totalProcessed > 0 ? 
                (double) jobsApplied / totalProcessed * 100 : 0;
            
            writer.printf("Total Jobs Processed: %d%n", totalProcessed);
            writer.printf("Total Jobs Applied: %d%n", jobsApplied);
            writer.printf("Total Jobs Failed: %d%n", jobsFailed);
            writer.printf("Total Jobs Skipped: %d%n", jobsSkipped);
            writer.printf("Success Rate: %.1f%%%n", successRate);
            
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.getSeconds() % 60;
            writer.printf("Duration: %d hours %d minutes %d seconds%n", hours, minutes, seconds);
            
            writer.println();
            writer.println("Job Skills Used for Matching:");
            for (String skill : jobSkills) {
                writer.printf("- %s%n", skill);
            }
            
            writer.println();
            writer.printf("Target Job Title: %s%n", jobTitle);
            
            writer.println();
            writer.println("Applied Job Titles:");
            for (String title : appliedJobTitles) {
                writer.printf("- %s%n", title);
            }
            
            writer.println();
            writer.println("================================================================================");
            
            writer.flush();
            writer.close();
            
            logger.info("Summary written to: {}", filePath);
            logger.info("Jobs Applied: {}, Failed: {}, Skipped: {}", 
                jobsApplied, jobsFailed, jobsSkipped);
            
        } catch (Exception e) {
            logger.error("Failed to write summary", e);
        }
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public int getJobsApplied() {
        return jobsApplied;
    }
}
```

### Enhanced Result Tracker

```java
public class JobApplicationTracker {
    private ResultLogger resultLogger;
    private List<String> appliedJobTitles = new ArrayList<>();
    private List<String> failedJobs = new ArrayList<>();
    private Map<String, String> failureReasons = new HashMap<>();
    private String jobTitle;
    private List<String> jobSkills;
    
    public JobApplicationTracker(ResultLogger resultLogger, String jobTitle, List<String> jobSkills) {
        this.resultLogger = resultLogger;
        this.jobTitle = jobTitle;
        this.jobSkills = jobSkills;
    }
    
    public void trackSuccessfulApplication(JobListing job) {
        appliedJobTitles.add(job.getTitle());
        resultLogger.logJob(job, true);
    }
    
    public void trackFailedApplication(JobListing job, String reason) {
        failedJobs.add(job.getTitle());
        failureReasons.put(job.getTitle(), reason);
        resultLogger.logFailedJob(job, reason);
    }
    
    public void trackSkippedJob(JobListing job, String reason) {
        resultLogger.logSkippedJob(job, reason);
    }
    
    public void finalizeAndClose() {
        resultLogger.writeSummary(appliedJobTitles, jobTitle, jobSkills);
    }
    
    public int getTotalApplied() {
        return appliedJobTitles.size();
    }
}
```

## Integration with Agents

### In LinkedInJobApplicationAgent

```java
public void executeWorkflow() {
    JobApplicationTracker tracker = new JobApplicationTracker(resultLogger, jobTitle, jobSkills);
    
    try {
        // ... login, search, extract jobs ...
        
        for (LinkedInJobListing job : jobs) {
            if (jobMatcher.isMatch(job)) {
                boolean applied = applicationHandler.applyToJob(job);
                if (applied) {
                    tracker.trackSuccessfulApplication(job);
                } else {
                    tracker.trackFailedApplication(job, "Apply button not clickable");
                }
            } else {
                tracker.trackSkippedJob(job, "Does not match job criteria");
            }
        }
        
    } finally {
        tracker.finalizeAndClose();
    }
}
```

### In DiceJobApplicationAgent

```java
public void executeWorkflow() {
    JobApplicationTracker tracker = new JobApplicationTracker(resultLogger, jobTitle, jobSkills);
    
    try {
        // ... login, search, extract jobs ...
        
        for (DiceJobListing job : jobs) {
            if (jobMatcher.isMatch(job)) {
                boolean applied = applicationHandler.applyToJob(job);
                if (applied) {
                    tracker.trackSuccessfulApplication(job);
                } else {
                    tracker.trackFailedApplication(job, "Application form error");
                }
            } else {
                tracker.trackSkippedJob(job, "Skill match failed");
            }
        }
        
    } finally {
        tracker.finalizeAndClose();
    }
}
```

## Output File Location

Default location:
```
applied_jobs_logs/
├── applied_jobs_2026-04-16_linkedin.txt
├── applied_jobs_2026-04-16_dice.txt
├── applied_jobs_2026-04-17_linkedin.txt
└── applied_jobs_2026-04-17_dice.txt
```

### Custom Output Directory

```java
public class ResultLogger {
    private static final String OUTPUT_DIR = System.getProperty("user.home") + "/job_applications";
    
    // Or configure via environment variable
    private static final String OUTPUT_DIR = 
        System.getenv("JOB_LOGS_DIR") != null ? 
        System.getenv("JOB_LOGS_DIR") : 
        "applied_jobs_logs";
}
```

## Viewing Results

### View Recent Applications

```bash
# Show latest application results
cat applied_jobs_logs/applied_jobs_$(date +%Y-%m-%d)_linkedin.txt

# Count total applications today
grep "Job Applied:" applied_jobs_logs/applied_jobs_$(date +%Y-%m-%d)_*.txt | wc -l

# View all Dice applications this week
ls -lt applied_jobs_logs/applied_jobs_*_dice.txt | head -7
```

### Parse and Analyze Results

```bash
#!/bin/bash
# analyze_applications.sh

echo "=== Job Application Summary ==="
echo ""

for file in applied_jobs_logs/applied_jobs_*.txt; do
    if [ -f "$file" ]; then
        provider=$(basename "$file" | grep -o '[a-z]*\.txt$' | sed 's/.txt//')
        applied=$(grep -c "^Job Applied:" "$file" || echo "0")
        failed=$(grep -c "^Job Failed:" "$file" || echo "0")
        
        echo "Provider: $provider"
        echo "  Applied: $applied"
        echo "  Failed: $failed"
        echo ""
    fi
done
```

## Backup and Archive

### Archive Old Logs

```bash
#!/bin/bash
# archive_old_logs.sh

ARCHIVE_DIR="applied_jobs_logs/archive"
mkdir -p "$ARCHIVE_DIR"

# Archive logs older than 30 days
find applied_jobs_logs -name "applied_jobs_*.txt" -mtime +30 -exec mv {} "$ARCHIVE_DIR" \;

echo "Archived old application logs"
```

### Compress Logs

```bash
# Create compressed backup
tar -czf applied_jobs_logs_backup_$(date +%Y-%m-%d).tar.gz applied_jobs_logs/

# Remove old backups (keep last 3)
ls -t applied_jobs_logs_backup_*.tar.gz | tail -n +4 | xargs rm -f
```

## Advanced Logging Features

### Email Notifications

```java
public class EmailNotifier {
    private String recipientEmail;
    
    public void sendApplicationSummary(String filePath, int jobsApplied) {
        try {
            String subject = "Job Application Summary: " + LocalDate.now();
            String body = String.format(
                "Successfully applied to %d jobs. Details in attached file.",
                jobsApplied
            );
            
            // Send email with log file attached
            // Implementation depends on mail library (JavaMail, SendGrid, etc.)
            
        } catch (Exception e) {
            logger.error("Failed to send notification email", e);
        }
    }
}
```

### Database Logging

```java
public class DatabaseResultLogger {
    private DataSource dataSource;
    
    public void logApplicationToDatabase(JobListing job, String provider, boolean success) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO job_applications " +
                        "(job_title, company, provider, applied_date, status) " +
                        "VALUES (?, ?, ?, ?, ?)";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, job.getTitle());
            stmt.setString(2, job.getCompany());
            stmt.setString(3, provider);
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            stmt.setString(5, success ? "SUCCESS" : "FAILED");
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Failed to log to database", e);
        }
    }
}
```

## File Retention Policy

Recommended:
- **Keep files for 90 days** in `applied_jobs_logs/`
- **Archive files 30-90 days old** to `applied_jobs_logs/archive/`
- **Delete files older than 90 days** or keep in secure backup

See [README.md](README.md) for integration with overall system architecture.

