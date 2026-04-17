# Implementation Roadmap & Code Skeleton

This guide provides the code structure and scaffolding needed to implement the job application agent system.

## Java Package Structure

Create the following package structure in `src/main/java/com/example/applyjobs/`:

```
com/example/applyjobs/
├── agent/
│   ├── linkedin/
│   │   ├── LinkedInJobApplicationAgent.java
│   │   ├── LinkedInLoginHandler.java
│   │   ├── LinkedInNavigationHandler.java
│   │   ├── LinkedInJobExtractor.java
│   │   └── LinkedInApplicationHandler.java
│   ├── dice/
│   │   ├── DiceJobApplicationAgent.java
│   │   ├── DiceLoginHandler.java
│   │   ├── DiceJobSearchHandler.java
│   │   ├── DiceJobExtractor.java
│   │   └── DiceApplicationHandler.java
│   └── JobApplicationOrchestrator.java
├── automation/
│   ├── BrowserAutomationEngine.java
│   ├── WaitHelper.java
│   ├── ErrorHandler.java
│   └── DynamicContentHandler.java
├── matcher/
│   ├── JobMatcher.java
│   ├── SkillMatcher.java
│   └── ExperienceMatcher.java
├── model/
│   ├── JobListing.java
│   ├── LinkedInJobListing.java
│   └── DiceJobListing.java
├── logger/
│   ├── ResultLogger.java
│   └── JobApplicationTracker.java
├── config/
│   ├── EnvironmentVariableLoader.java
│   └── ApplicationConfig.java
└── exception/
    ├── LoginException.java
    ├── NavigationException.java
    ├── SearchException.java
    └── JobExtractionException.java
```

## Step 1: Create Exception Classes

```java
// src/main/java/com/example/applyjobs/exception/LoginException.java
package com.example.applyjobs.exception;

public class LoginException extends RuntimeException {
    public LoginException(String message) {
        super(message);
    }
    
    public LoginException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Repeat for: NavigationException, SearchException, JobExtractionException
```

## Step 2: Create Model Classes

```java
// src/main/java/com/example/applyjobs/model/JobListing.java
package com.example.applyjobs.model;

public abstract class JobListing {
    protected String title;
    protected String company;
    protected String location;
    protected String description;
    
    public JobListing(String title, String company, String location, String description) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
    }
    
    public abstract String getJobUrl();
    
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    
    @Override
    public String toString() {
        return String.format("%s - %s (%s)", title, company, location);
    }
}

// src/main/java/com/example/applyjobs/model/LinkedInJobListing.java
package com.example.applyjobs.model;

public class LinkedInJobListing extends JobListing {
    private String id;
    private String applyUrl;
    
    public LinkedInJobListing(String id, String title, String company, String location, String description) {
        super(title, company, location, description);
        this.id = id;
    }
    
    public String getId() { return id; }
    
    @Override
    public String getJobUrl() {
        return "https://www.linkedin.com/jobs/view/" + id;
    }
}

// src/main/java/com/example/applyjobs/model/DiceJobListing.java
package com.example.applyjobs.model;

public class DiceJobListing extends JobListing {
    private String jobUrl;
    
    public DiceJobListing(String title, String company, String jobUrl, String description) {
        super(title, company, "", description);
        this.jobUrl = jobUrl;
    }
    
    @Override
    public String getJobUrl() {
        return jobUrl;
    }
}
```

## Step 3: Create Configuration & Environment Loading

```java
// src/main/java/com/example/applyjobs/config/EnvironmentVariableLoader.java
package com.example.applyjobs.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EnvironmentVariableLoader {
    
    public static String getLinkedInUsername() {
        return System.getenv("LINKEDIN_USERNAME");
    }
    
    public static String getLinkedInPassword() {
        return System.getenv("LINKEDIN_PASSWORD");
    }
    
    public static String getDiceUsername() {
        return System.getenv("DICE_USERNAME");
    }
    
    public static String getDicePassword() {
        return System.getenv("DICE_PASSWORD");
    }
    
    public static String getJobTitle() {
        return System.getenv("JOB_TITLE");
    }
    
    public static List<String> getJobSkills() {
        String skillsStr = System.getenv("JOB_SKILLS");
        if (skillsStr == null || skillsStr.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(skillsStr.split(","));
    }
    
    public static void validateEnvironmentVariables() {
        String[] required = {
            "LINKEDIN_USERNAME", "LINKEDIN_PASSWORD",
            "DICE_USERNAME", "DICE_PASSWORD",
            "JOB_TITLE", "JOB_SKILLS"
        };
        
        for (String var : required) {
            if (System.getenv(var) == null || System.getenv(var).isEmpty()) {
                throw new IllegalStateException("Missing environment variable: " + var);
            }
        }
    }
}
```

## Step 4: Create Browser Automation Engine

```java
// src/main/java/com/example/applyjobs/automation/BrowserAutomationEngine.java
package com.example.applyjobs.automation;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserAutomationEngine {
    private WebDriver driver;
    private static final Logger logger = LoggerFactory.getLogger(BrowserAutomationEngine.class);
    
    public BrowserAutomationEngine() {
        initializeChromeDriver();
    }
    
    private void initializeChromeDriver() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        logger.info("Chrome WebDriver initialized");
    }
    
    public WebDriver getDriver() {
        return driver;
    }
    
    public void close() {
        if (driver != null) {
            driver.quit();
            logger.info("Chrome WebDriver closed");
        }
    }
}
```

## Step 5: Create Wait Helper

```java
// src/main/java/com/example/applyjobs/automation/WaitHelper.java
package com.example.applyjobs.automation;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class WaitHelper {
    private WebDriver driver;
    private static final int DEFAULT_TIMEOUT = 10;
    
    public WaitHelper(WebDriver driver) {
        this.driver = driver;
    }
    
    public WebElement waitForElement(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }
    
    public WebElement waitForElementVisible(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    
    public WebElement waitForElementClickable(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }
    
    public void waitForPageLoad() {
        new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
            .until(webDriver -> 
                ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete")
            );
    }
}
```

## Step 6: Create Job Matcher

```java
// src/main/java/com/example/applyjobs/matcher/JobMatcher.java
package com.example.applyjobs.matcher;

import com.example.applyjobs.model.JobListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;

public class JobMatcher {
    private String jobTitle;
    private List<String> jobSkills;
    private static final Logger logger = LoggerFactory.getLogger(JobMatcher.class);
    
    public JobMatcher(String jobTitle, List<String> jobSkills) {
        this.jobTitle = jobTitle.toLowerCase().trim();
        this.jobSkills = jobSkills.stream()
            .map(skill -> skill.toLowerCase().trim())
            .collect(Collectors.toList());
    }
    
    public boolean isMatch(JobListing job) {
        logger.debug("Checking match for job: {}", job.getTitle());
        
        boolean titleMatch = matchJobTitle(job);
        boolean skillsMatch = matchJobSkills(job);
        
        boolean result = titleMatch && skillsMatch;
        
        logger.info("Job: {} | Title: {} | Skills: {} | Match: {}",
            job.getTitle(), titleMatch, skillsMatch, result);
        
        return result;
    }
    
    private boolean matchJobTitle(JobListing job) {
        String text = (job.getTitle() + " " + job.getDescription()).toLowerCase();
        return text.contains(jobTitle);
    }
    
    private boolean matchJobSkills(JobListing job) {
        String description = job.getDescription().toLowerCase();
        
        for (String skill : jobSkills) {
            if (!description.contains(skill)) {
                logger.debug("Required skill missing: {}", skill);
                return false;
            }
        }
        
        return true;
    }
    
    public double calculateMatchScore(JobListing job) {
        double score = 0.0;
        double maxScore = jobSkills.size() + 1;
        
        if (matchJobTitle(job)) {
            score += 1.0;
        }
        
        String description = job.getDescription().toLowerCase();
        for (String skill : jobSkills) {
            if (description.contains(skill)) {
                score += 1.0;
            }
        }
        
        return score / maxScore;
    }
}
```

## Step 7: Create Result Logger

```java
// src/main/java/com/example/applyjobs/logger/ResultLogger.java
package com.example.applyjobs.logger;

import com.example.applyjobs.model.JobListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ResultLogger {
    private String provider;
    private String filePath;
    private PrintWriter writer;
    private int jobsApplied = 0;
    private LocalDateTime startTime;
    private static final Logger logger = LoggerFactory.getLogger(ResultLogger.class);
    
    public ResultLogger(String provider) {
        this.provider = provider.toLowerCase();
        this.startTime = LocalDateTime.now();
        initializeFile();
    }
    
    private void initializeFile() {
        try {
            File outputDir = new File("applied_jobs_logs");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            String filename = String.format("applied_jobs_%s_%s.txt", dateStr, provider);
            this.filePath = outputDir.getAbsolutePath() + File.separator + filename;
            
            this.writer = new PrintWriter(new FileWriter(filePath, true));
            
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
    
    public void logJob(JobListing job) {
        try {
            jobsApplied++;
            
            writer.printf("Job Applied: %d%n", jobsApplied);
            writer.printf("Date: %s%n", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            writer.printf("Title: %s%n", job.getTitle());
            writer.printf("Company: %s%n", job.getCompany());
            writer.printf("Location: %s%n", job.getLocation());
            writer.printf("URL: %s%n", job.getJobUrl());
            writer.printf("Status: SUCCESS%n");
            writer.println();
            
            writer.flush();
            
        } catch (Exception e) {
            logger.error("Failed to log job", e);
        }
    }
    
    public void writeSummary(List<String> appliedJobTitles, String jobTitle, List<String> jobSkills) {
        try {
            writer.println();
            writer.println("================================================================================");
            writer.println("SUMMARY");
            writer.println("================================================================================");
            
            writer.printf("Total Jobs Applied: %d%n", jobsApplied);
            writer.printf("Target Job Title: %s%n", jobTitle);
            writer.println();
            writer.println("Job Skills Used:");
            for (String skill : jobSkills) {
                writer.printf("- %s%n", skill);
            }
            
            writer.println("================================================================================");
            writer.flush();
            writer.close();
            
            logger.info("Summary written to: {}", filePath);
            
        } catch (Exception e) {
            logger.error("Failed to write summary", e);
        }
    }
}
```

## Step 8: Minimal LinkedIn Agent Example

```java
// src/main/java/com/example/applyjobs/agent/linkedin/LinkedInJobApplicationAgent.java
package com.example.applyjobs.agent.linkedin;

import com.example.applyjobs.automation.BrowserAutomationEngine;
import com.example.applyjobs.automation.WaitHelper;
import com.example.applyjobs.config.EnvironmentVariableLoader;
import com.example.applyjobs.matcher.JobMatcher;
import com.example.applyjobs.logger.ResultLogger;
import com.example.applyjobs.model.LinkedInJobListing;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LinkedInJobApplicationAgent {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private JobMatcher jobMatcher;
    private ResultLogger resultLogger;
    
    private static final Logger logger = LoggerFactory.getLogger(LinkedInJobApplicationAgent.class);
    private static final int JOB_LIMIT = 5;
    
    public LinkedInJobApplicationAgent() {
        BrowserAutomationEngine engine = new BrowserAutomationEngine();
        this.driver = engine.getDriver();
        this.waitHelper = new WaitHelper(driver);
        
        String jobTitle = EnvironmentVariableLoader.getJobTitle();
        List<String> jobSkills = EnvironmentVariableLoader.getJobSkills();
        this.jobMatcher = new JobMatcher(jobTitle, jobSkills);
        
        this.resultLogger = new ResultLogger("linkedin");
    }
    
    public void executeWorkflow() {
        try {
            logger.info("=== Starting LinkedIn Job Application Agent ===");
            
            // TODO: Implement steps
            // 1. Login
            // 2. Navigate to recent searches
            // 3. Click "full stack engineer" search
            // 4. Extract jobs
            // 5. Process and apply
            
            logger.info("=== LinkedIn Agent Complete ===");
            
        } catch (Exception e) {
            logger.error("LinkedIn agent failed", e);
            throw new RuntimeException(e);
        } finally {
            driver.quit();
        }
    }
    
    public static void main(String[] args) {
        LinkedInJobApplicationAgent agent = new LinkedInJobApplicationAgent();
        agent.executeWorkflow();
    }
}
```

## Step 9: Orchestrator (Run Both Agents)

```java
// src/main/java/com/example/applyjobs/agent/JobApplicationOrchestrator.java
package com.example.applyjobs.agent;

import com.example.applyjobs.agent.linkedin.LinkedInJobApplicationAgent;
import com.example.applyjobs.agent.dice.DiceJobApplicationAgent;
import com.example.applyjobs.config.EnvironmentVariableLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobApplicationOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(JobApplicationOrchestrator.class);
    
    public static void main(String[] args) {
        try {
            logger.info("====================================================");
            logger.info("  Job Application Agent Orchestrator Starting");
            logger.info("====================================================");
            
            // Validate environment
            EnvironmentVariableLoader.validateEnvironmentVariables();
            logger.info("Environment variables validated");
            
            // Run LinkedIn agent
            logger.info("Running LinkedIn agent...");
            LinkedInJobApplicationAgent linkedInAgent = new LinkedInJobApplicationAgent();
            linkedInAgent.executeWorkflow();
            
            logger.info("LinkedIn agent completed");
            
            // Wait between agents
            Thread.sleep(5000);
            
            // Run Dice agent
            logger.info("Running Dice agent...");
            DiceJobApplicationAgent diceAgent = new DiceJobApplicationAgent();
            diceAgent.executeWorkflow();
            
            logger.info("Dice agent completed");
            
            logger.info("====================================================");
            logger.info("  All agents completed successfully!");
            logger.info("====================================================");
            
        } catch (Exception e) {
            logger.error("Orchestrator failed", e);
            System.exit(1);
        }
    }
}
```

## Maven Dependencies Update

Update `pom.xml`:

```xml
<dependencies>
    <!-- ...existing dependencies... -->
    
    <!-- Selenium WebDriver -->
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.15.0</version>
    </dependency>
    
    <!-- WebDriver Manager -->
    <dependency>
        <groupId>io.github.bonigarcia</groupId>
        <artifactId>webdrivermanager</artifactId>
        <version>5.6.2</version>
    </dependency>
    
    <!-- Apache Commons -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
        <version>3.13.0</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

## Build & Run

```bash
# Build project
mvn clean package

# Run orchestrator
java -jar target/applyjobs-0.0.1-SNAPSHOT.jar \
  com.example.applyjobs.agent.JobApplicationOrchestrator
```

## Implementation Checklist

- [ ] Create exception classes
- [ ] Create model classes (JobListing, LinkedInJobListing, DiceJobListing)
- [ ] Create EnvironmentVariableLoader
- [ ] Create BrowserAutomationEngine
- [ ] Create WaitHelper
- [ ] Create JobMatcher
- [ ] Create ResultLogger
- [ ] Create LinkedInJobApplicationAgent stub
- [ ] Create DiceJobApplicationAgent stub
- [ ] Create JobApplicationOrchestrator
- [ ] Update pom.xml with dependencies
- [ ] Build and test structure
- [ ] Implement LinkedIn handlers (login, navigation, extraction, application)
- [ ] Implement Dice handlers (login, search, extraction, application)
- [ ] Full integration testing

## Next: See detailed implementation guides

- **LINKEDIN_AGENT.md** - Complete LinkedIn workflow
- **DICE_AGENT.md** - Complete Dice workflow
- **BROWSER_AUTOMATION.md** - Browser automation patterns
- **JOB_MATCHING.md** - Job matching logic
- **OUTPUT_LOGGING.md** - Logging implementation

