# Job Matching & Application Logging Guide

## Overview

This guide covers the implementation of job matching against user criteria and logging applied jobs to a results file with date and provider name.

## Job Matching Implementation

### JobMatcher Class

```java
public class JobMatcher {
    private String jobTitle;
    private List<String> jobSkills;
    private static final Logger logger = LoggerFactory.getLogger(JobMatcher.class);
    
    // Matching thresholds
    private static final double TITLE_MATCH_THRESHOLD = 0.6;
    private static final double SKILLS_MATCH_THRESHOLD = 0.5;
    
    public JobMatcher(String jobTitle, List<String> jobSkills) {
        this.jobTitle = jobTitle != null ? jobTitle.toLowerCase().trim() : "";
        this.jobSkills = jobSkills != null ? 
            jobSkills.stream()
                .map(s -> s.toLowerCase().trim())
                .collect(Collectors.toList()) : 
            new ArrayList<>();
        
        logger.info("Initialized JobMatcher - Title: {}, Skills: {}", jobTitle, jobSkills);
    }
    
    /**
     * Check if a job matches user criteria
     * @param job The job listing to check
     * @return true if job matches criteria
     */
    public boolean isMatch(LinkedInJobListing job) {
        try {
            // Extract job details
            String jobDescription = job.getDescription().toLowerCase();
            String listedJobTitle = job.getTitle().toLowerCase();
            
            // Check job title match
            boolean titleMatch = checkTitleMatch(listedJobTitle);
            
            // Check skills match
            int matchedSkills = countMatchedSkills(jobDescription);
            double skillMatchPercentage = calculateSkillMatchPercentage(matchedSkills);
            
            // Log matching details
            logger.debug("Job: {} - Title Match: {}, Skill Match: {}%", 
                job.getTitle(), titleMatch, String.format("%.1f", skillMatchPercentage * 100));
            
            // Both title and skills should match
            boolean matches = titleMatch && skillMatchPercentage >= SKILLS_MATCH_THRESHOLD;
            
            if (matches) {
                logger.info("✓ Job MATCHES criteria: {}", job.getTitle());
            } else {
                logger.info("✗ Job does NOT match criteria: {}", job.getTitle());
            }
            
            return matches;
            
        } catch (Exception e) {
            logger.error("Error checking job match: {}", e.getMessage());
            return false; // Be conservative - reject on error
        }
    }
    
    /**
     * Check if job title matches user's target job title
     */
    private boolean checkTitleMatch(String listedJobTitle) {
        if (jobTitle == null || jobTitle.isEmpty()) {
            logger.debug("No target job title configured - skipping title match");
            return true; // No title filter configured
        }
        
        // Direct match
        if (listedJobTitle.contains(jobTitle)) {
            return true;
        }
        
        // Partial match for common variations
        String[] titleKeywords = jobTitle.split(" ");
        int matchedKeywords = 0;
        
        for (String keyword : titleKeywords) {
            if (keyword.length() > 2 && listedJobTitle.contains(keyword)) {
                matchedKeywords++;
            }
        }
        
        double matchPercentage = (double) matchedKeywords / titleKeywords.length;
        return matchPercentage >= TITLE_MATCH_THRESHOLD;
    }
    
    /**
     * Count how many user skills are mentioned in job description
     */
    private int countMatchedSkills(String jobDescription) {
        int count = 0;
        
        for (String skill : jobSkills) {
            if (skill.length() < 2) {
                continue; // Skip very short skills
            }
            
            // Create multiple search patterns
            String[] patterns = new String[]{
                " " + skill + " ",           // Word boundary
                " " + skill + ",",           // Followed by comma
                " " + skill + ".",           // Followed by period
                " " + skill + "/",           // Followed by slash
                "(" + skill,                 // Followed by parenthesis
                skill + ")",                 // In parenthesis
                skill + "."                  // At end of sentence
            };
            
            for (String pattern : patterns) {
                if (jobDescription.contains(pattern)) {
                    count++;
                    logger.debug("Found skill match: {}", skill);
                    break; // Count each skill only once
                }
            }
        }
        
        return count;
    }
    
    /**
     * Calculate percentage of skills matched
     */
    private double calculateSkillMatchPercentage(int matchedSkills) {
        if (jobSkills.isEmpty()) {
            return 1.0; // No skills configured - accept all
        }
        
        return (double) matchedSkills / jobSkills.size();
    }
    
    /**
     * Get matching details for logging
     */
    public Map<String, Object> getMatchingDetails(LinkedInJobListing job) {
        Map<String, Object> details = new HashMap<>();
        
        String jobDescription = job.getDescription().toLowerCase();
        int matchedSkills = countMatchedSkills(jobDescription);
        
        details.put("job_id", job.getId());
        details.put("job_title", job.getTitle());
        details.put("company", job.getCompany());
        details.put("location", job.getLocation());
        details.put("skills_matched", matchedSkills);
        details.put("skills_required", jobSkills.size());
        details.put("match_percentage", calculateSkillMatchPercentage(matchedSkills) * 100);
        details.put("timestamp", System.currentTimeMillis());
        
        return details;
    }
}
```

## Application Logging

### ResultLogger Class

```java
public class ResultLogger {
    private String provider; // "linkedin", "dice", etc.
    private String logFilePath;
    private static final Logger logger = LoggerFactory.getLogger(ResultLogger.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public ResultLogger(String provider) {
        this.provider = provider;
        this.logFilePath = createLogFilePath();
        logger.info("Initialized ResultLogger - Provider: {}, File: {}", provider, logFilePath);
    }
    
    /**
     * Create log file name with date and provider
     * Format: applied_jobs_linkedin_2024-04-17.txt
     */
    private String createLogFilePath() {
        try {
            String homeDir = System.getProperty("user.home");
            String projectDir = System.getenv("PROJECT_DIR");
            
            // Use project directory if available, otherwise use home directory
            String baseDir = projectDir != null ? projectDir : homeDir;
            
            String date = DATE_FORMAT.format(new Date());
            String filename = String.format("applied_jobs_%s_%s.txt", provider, date);
            
            return baseDir + File.separator + filename;
            
        } catch (Exception e) {
            logger.error("Error creating log file path", e);
            return "applied_jobs_" + provider + ".txt";
        }
    }
    
    /**
     * Log a successfully applied job
     */
    public void logJob(LinkedInJobListing job) {
        logJob(job, null, null);
    }
    
    /**
     * Log a successfully applied job with additional details
     */
    public void logJob(LinkedInJobListing job, Map<String, Object> matchDetails, String notes) {
        try {
            StringBuilder logEntry = new StringBuilder();
            
            // Header
            logEntry.append("\n");
            logEntry.append("=".repeat(80)).append("\n");
            logEntry.append(String.format("Job Application - %s\n", TIMESTAMP_FORMAT.format(new Date())));
            logEntry.append("=".repeat(80)).append("\n");
            
            // Job Details
            logEntry.append(String.format("Provider: %s\n", provider.toUpperCase()));
            logEntry.append(String.format("Job ID: %s\n", job.getId()));
            logEntry.append(String.format("Title: %s\n", job.getTitle()));
            logEntry.append(String.format("Company: %s\n", job.getCompany()));
            logEntry.append(String.format("Location: %s\n", job.getLocation()));
            logEntry.append(String.format("URL: https://www.linkedin.com/jobs/view/%s\n", job.getId()));
            
            // Matching Details
            if (matchDetails != null && !matchDetails.isEmpty()) {
                logEntry.append("\n--- Matching Details ---\n");
                logEntry.append(String.format("Skills Matched: %d/%d\n", 
                    matchDetails.get("skills_matched"),
                    matchDetails.get("skills_required")));
                logEntry.append(String.format("Match Percentage: %.1f%%\n", 
                    matchDetails.get("match_percentage")));
            }
            
            // Additional Notes
            if (notes != null && !notes.isEmpty()) {
                logEntry.append("\n--- Notes ---\n");
                logEntry.append(notes).append("\n");
            }
            
            logEntry.append("\n");
            
            // Write to file
            writeToLogFile(logEntry.toString());
            
            logger.info("Job logged successfully: {}", job.getTitle());
            
        } catch (Exception e) {
            logger.error("Error logging job", e);
        }
    }
    
    /**
     * Write entry to log file
     */
    private void writeToLogFile(String content) {
        try {
            Path filePath = Paths.get(logFilePath);
            
            // Create parent directories if they don't exist
            Files.createDirectories(filePath.getParent());
            
            // Append to file (create if doesn't exist)
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            logger.error("Error writing to log file: {}", logFilePath, e);
            
            // Fallback: write to console
            System.out.println("=== LOG OUTPUT (File write failed) ===");
            System.out.println(content);
            System.out.println("===================================");
        }
    }
    
    /**
     * Log summary of all applications
     */
    public void logSummary(int totalJobs, int appliedJobs, int matchedJobs) {
        try {
            StringBuilder summary = new StringBuilder();
            summary.append("\n").append("=".repeat(80)).append("\n");
            summary.append(String.format("APPLICATION SUMMARY - %s\n", TIMESTAMP_FORMAT.format(new Date())));
            summary.append("=".repeat(80)).append("\n");
            summary.append(String.format("Total Jobs Processed: %d\n", totalJobs));
            summary.append(String.format("Jobs Matched: %d\n", matchedJobs));
            summary.append(String.format("Jobs Applied: %d\n", appliedJobs));
            summary.append(String.format("Success Rate: %.1f%%\n", 
                totalJobs > 0 ? (double) appliedJobs / totalJobs * 100 : 0));
            summary.append(String.format("Provider: %s\n", provider.toUpperCase()));
            summary.append(String.format("Log File: %s\n", logFilePath));
            summary.append("=".repeat(80)).append("\n\n");
            
            writeToLogFile(summary.toString());
            
            logger.info("Summary logged: Applied to {} out of {} jobs", appliedJobs, totalJobs);
            
        } catch (Exception e) {
            logger.error("Error logging summary", e);
        }
    }
    
    /**
     * Get the log file path
     */
    public String getLogFilePath() {
        return logFilePath;
    }
    
    /**
     * Read and display log file contents
     */
    public String readLogFile() {
        try {
            return new String(Files.readAllBytes(Paths.get(logFilePath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Error reading log file", e);
            return "";
        }
    }
}
```

## Integration in Main Workflow

```java
public class LinkedInJobApplicationAgent {
    // ... existing fields ...
    private JobMatcher jobMatcher;
    private ResultLogger resultLogger;
    
    public void executeWorkflow() {
        try {
            logger.info("=== Starting LinkedIn Job Application Agent ===");
            
            // ... login and navigation ...
            
            // Extract job listings (limit to 5 latest)
            List<LinkedInJobListing> jobs = jobExtractor.extractLatestJobListings(5);
            logger.info("Extracted {} jobs from LinkedIn", jobs.size());
            
            int appliedCount = 0;
            int matchedCount = 0;
            
            // Process and apply to matching jobs
            for (LinkedInJobListing job : jobs) {
                if (jobMatcher.isMatch(job)) {
                    matchedCount++;
                    logger.info("Job matches criteria: {}", job.getTitle());
                    
                    // Get matching details for logging
                    Map<String, Object> matchDetails = jobMatcher.getMatchingDetails(job);
                    
                    // Apply to the job
                    boolean applied = applicationHandler.applyToJob(job);
                    
                    if (applied) {
                        appliedCount++;
                        
                        // Log the successful application
                        resultLogger.logJob(job, matchDetails, "Successfully applied to job");
                        
                        logger.info("✓ Applied to job: {}", job.getTitle());
                    } else {
                        resultLogger.logJob(job, matchDetails, "Application failed");
                        logger.warn("✗ Failed to apply to job: {}", job.getTitle());
                    }
                } else {
                    logger.info("Job does not match criteria: {}", job.getTitle());
                }
            }
            
            // Log summary
            resultLogger.logSummary(jobs.size(), appliedCount, matchedCount);
            
            logger.info("=== LinkedIn Job Application Complete ===");
            logger.info("Applied to {} out of {} jobs", appliedCount, matchedCount);
            logger.info("Results logged to: {}", resultLogger.getLogFilePath());
            
        } catch (Exception e) {
            logger.error("LinkedIn agent workflow failed", e);
            throw new RuntimeException(e);
        } finally {
            driver.quit();
        }
    }
}
```

## Log File Format

### Example Output

```
================================================================================
Job Application - 2024-04-17 14:32:15
================================================================================
Provider: LINKEDIN
Job ID: 3921547832
Title: Full Stack Engineer
Company: Google LLC
Location: Mountain View, CA, USA
URL: https://www.linkedin.com/jobs/view/3921547832

--- Matching Details ---
Skills Matched: 4/5
Match Percentage: 80.0%

--- Notes ---
Successfully applied to job

================================================================================
Job Application - 2024-04-17 14:35:42
================================================================================
Provider: LINKEDIN
Job ID: 3921548940
Title: Senior Full Stack Developer
Company: Amazon
Location: Seattle, WA, USA
URL: https://www.linkedin.com/jobs/view/3921548940

--- Matching Details ---
Skills Matched: 5/5
Match Percentage: 100.0%

--- Notes ---
Successfully applied to job

================================================================================
APPLICATION SUMMARY - 2024-04-17 15:00:00
================================================================================
Total Jobs Processed: 5
Jobs Matched: 4
Jobs Applied: 4
Success Rate: 80.0%
Provider: LINKEDIN
Log File: /Users/username/applied_jobs_linkedin_2024-04-17.txt
================================================================================
```

## Environment Variables

```bash
# Job Matching Configuration
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL,Docker,Kubernetes"

# LinkedIn Credentials
export LINKEDIN_USERNAME="your_email@example.com"
export LINKEDIN_PASSWORD="your_password"

# Optional: Results Directory
export RESULTS_DIR="/path/to/results"
export PROJECT_DIR="/path/to/project"
```

## Required Imports

```java
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

## Best Practices

1. **Always Log Attempts**: Log both successful and failed applications
2. **Include Details**: Capture skills matched, match percentage, and timestamps
3. **Daily Files**: Create new files daily with date in filename
4. **Readable Format**: Use clear formatting with separators for readability
5. **Backup Data**: Keep log files as audit trail of all applications
6. **Error Handling**: Fallback to console output if file write fails

## File Naming Convention

- **Format**: `applied_jobs_{provider}_{YYYY-MM-DD}.txt`
- **Example**: `applied_jobs_linkedin_2024-04-17.txt`
- **Example**: `applied_jobs_dice_2024-04-17.txt`

## Performance Considerations

- Log operations are I/O bound but minimal impact
- Batching writes could improve performance for high-volume applications
- Consider logging to database for larger datasets

