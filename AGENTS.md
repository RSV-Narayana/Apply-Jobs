# AGENTS.md - AI Agent Developer Guide

This guide helps AI agents (GitHub Copilot, Claude, etc.) understand the Apply-Jobs codebase and contribute effectively.

## Project Overview

**Apply-Jobs** is a Spring Boot application that automates job applications across LinkedIn and Dice portals using Selenium WebDriver for browser automation. The system logs applied jobs and supports intelligent job matching based on title and skills.

**Tech Stack**: Java 25, Spring Boot 4.0.5, Selenium 4, Gradle  
**Key Dependency**: Selenium WebDriver for browser automation

## Architecture Essentials

### Core Components (Multi-file Understanding Required)

```
BrowserAutomationEngine
├─ Manages ChromeDriver initialization & lifecycle
├─ Pattern: Singleton with WebDriverWait configuration
└─ Key Methods: driver.findElement(), waitForElement()

Portal Agents (LinkedIn + Dice)
├─ LinkedInJobApplicationAgent: Entry point for LinkedIn automation
├─ DiceJobApplicationAgent: Entry point for Dice automation
└─ Shared Handlers: LoginHandler, JobExtractor, ApplicationHandler

Job Matching Pipeline
├─ JobMatcher: Matches jobs by title (exact) + skills (50%+ threshold)
├─ JobListing Model: Holds title, company, description, URL
└─ Scoring: Binary on title, percentage on skills

ResultLogger
├─ Writes to: applied_jobs_{PROVIDER}_{DATE}.txt
├─ Format: Tab-separated (title, company, date, status)
└─ Located in: project root or configured output directory
```

### Critical Data Flow

```
EnvironmentVariableLoader (reads credentials/preferences)
  ↓
Portal Agent Initialize (LoginHandler opens browser)
  ↓
SearchHandler (navigates to recent job search)
  ↓
JobExtractor (extracts latest 5 jobs from DOM)
  ↓
FOR EACH Job:
  ├─ JobMatcher → Check if title + skills match
  ├─ YES → ApplicationHandler (click apply, fill form, submit)
  ├─ Detect Easy Apply modal (LinkedIn) or direct form (Dice)
  ├─ Handle additional questions (text, checkbox, dropdown, textarea)
  ├─ Uncheck "Follow company" (LinkedIn review step)
  └─ ResultLogger → Log result with timestamp
  ↓
Close WebDriver (cleanup)
```

## Critical Workflows & Commands

### Local Development Build

```bash
# Build the project
./gradlew clean build

# Run tests
./gradlew test

# Run application
./gradlew bootRun

# Check dependencies
./gradlew dependencies
```

### Environment Variables (Required)

Agents must verify these are set before execution:
```bash
export LINKEDIN_USERNAME="email@example.com"
export LINKEDIN_PASSWORD="secure_password"
export DICE_USERNAME="dice_email@example.com"
export DICE_PASSWORD="dice_password"
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL,Docker"
export JOB_MATCHING_THRESHOLD=50  # Skill match percentage
export OUTPUT_DIRECTORY="/path/to/output"  # Optional
```

### Integration Testing Pattern

```java
// LinkedIn agent test example
LinkedInJobApplicationAgent agent = new LinkedInJobApplicationAgent();
agent.executeWorkflow();  // Runs full pipeline: login → search → apply → log

// Check results
File logFile = new File("applied_jobs_linkedin_" + LocalDate.now() + ".txt");
assertTrue(logFile.exists(), "Log file must be created");
```

## Project-Specific Conventions & Patterns

### 1. Exception Handling Pattern
```java
// Pattern: Graceful degradation, log warnings, continue
try {
    handleAdditionalQuestions();
} catch (NoSuchElementException e) {
    logger.warn("Questions not found, continuing: " + e.getMessage());
    // Do NOT rethrow - allow application to continue
}
```

### 2. XPath Selectors - Portal-Specific
```java
// LinkedIn: Easy Apply modal detection
//div[contains(@class, 'drawer-form')] //button[contains(text(), 'Next')]

// Dice: Job listing extraction  
//div[contains(@data-cy, 'job-card')] //a[@href*='/job/']

// Both: Question field detection (polymorphic)
//input[not(@type='hidden')] | //textarea | //select
```

### 3. Wait Strategies (Critical!)
```java
// Pattern: Use WebDriverWait with explicit waits, NOT sleep()
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
WebElement element = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("...")));

// Timeout values: 10-30 seconds between job applications (rate limiting)
Thread.sleep(new Random().nextInt(20000) + 10000);
```

### 4. Job Matching Algorithm
```java
// Title matching: Case-insensitive EXACT match
boolean titleMatches = jobTitle.toLowerCase().contains(expectedTitle.toLowerCase());

// Skills matching: At least 50% of required skills in description
int matchedCount = jobDescription.split("[,\\s]+").stream()
    .filter(skill -> jobDescription.toLowerCase().contains(skill.toLowerCase()))
    .count();
boolean skillsMatch = (matchedCount / totalSkills) >= 0.5;

// Apply only if: titleMatches AND skillsMatch
```

### 5. Result Logging Format
```
Tab-separated values (4 columns):
Job Title    | Company Name    | Application Date (YYYY-MM-DD) | Status
Full Stack Engineer | TechCorp | 2024-04-17 | SUCCESS

File naming: applied_jobs_{PROVIDER}_{DATE}.txt
Example: applied_jobs_linkedin_2024-04-17.txt
```

## File Organization & Key Paths

```
Apply-Jobs/
├─ src/main/java/com/example/applyjobs/
│  ├─ ApplyJobsApplication.java (Spring Boot entry point)
│  ├─ BrowserAutomationEngine.java (Selenium management)
│  ├─ models/
│  │  ├─ JobListing.java (job data model)
│  │  └─ ApplicationResult.java (result data model)
│  ├─ linkedin/
│  │  ├─ LinkedInJobApplicationAgent.java (main orchestrator)
│  │  ├─ handlers/ (LoginHandler, NavigationHandler, JobExtractor, ApplicationHandler)
│  │  └─ LinkedInQuestionHandler.java (additional questions logic)
│  ├─ dice/
│  │  ├─ DiceJobApplicationAgent.java (main orchestrator)
│  │  └─ handlers/ (LoginHandler, SearchHandler, JobExtractor, ApplicationHandler)
│  ├─ matching/
│  │  └─ JobMatcher.java (title + skills matching)
│  ├─ logging/
│  │  └─ ResultLogger.java (writes to file)
│  └─ config/
│     └─ EnvironmentVariableLoader.java (reads env vars)
├─ build.gradle (Gradle build - uses Spring Boot 4.0.5, Java 25)
├─ .github/agents/ (Extensive documentation - 18 MD files)
│  ├─ README.md (architecture & component overview)
│  ├─ QUICK_START.md (navigation guide)
│  ├─ LINKEDIN_AGENT.md (detailed implementation)
│  ├─ DICE_AGENT.md (detailed implementation)
│  ├─ BROWSER_AUTOMATION.md (Selenium patterns)
│  ├─ JOB_MATCHING.md (matching algorithm)
│  ├─ OUTPUT_LOGGING.md (result logging)
│  ├─ ENVIRONMENT_SETUP.md (env var configuration)
│  └─ ... (9 more reference documents)
└─ AGENTS.md (this file)
```

## Integration Points & Cross-Component Communication

### 1. Browser Lifecycle (Singleton Pattern)
```java
// Initialize once per agent
BrowserAutomationEngine engine = new BrowserAutomationEngine();
WebDriver driver = engine.initializeDriver();

// Use across handlers
LoginHandler login = new LinkedInLoginHandler(driver);
login.performLogin(username, password);

// Cleanup at end
engine.closeDriver();
```

### 2. Job Extraction to Matching
```java
// JobExtractor returns List<JobListing>
List<JobListing> jobs = jobExtractor.extractLatestJobListings(5);

// Each job passed to JobMatcher
for (JobListing job : jobs) {
    if (jobMatcher.isMatch(job.getTitle(), job.getDescription())) {
        applicationHandler.applyToJob(job);
    }
}
```

### 3. Application Result Logging
```java
// Every application stores result
ApplicationResult result = new ApplicationResult(
    jobTitle, 
    companyName, 
    LocalDate.now(),
    "SUCCESS" or "FAILED"
);

// ResultLogger appends to file
resultLogger.logResult(result);
```

## Common Pitfalls & Solutions

| Issue | Solution | Files to Check |
|-------|----------|-----------------|
| Tests fail with "WebDriver not found" | Set ChromeDriver path in BrowserAutomationEngine.initializeDriver() | BrowserAutomationEngine.java |
| Job matching too strict/loose | Adjust threshold in JobMatcher (currently 50% for skills) | JobMatcher.java, JOB_MATCHING.md |
| Questions not filled | Update XPath selectors for new LinkedIn/Dice UI | LinkedInQuestionHandler.java, BROWSER_AUTOMATION.md |
| Output file not created | Check OUTPUT_DIRECTORY exists, ResultLogger has write permissions | ResultLogger.java, OUTPUT_LOGGING.md |
| Login fails with 2FA | Add manual 2FA handling, use cookies persistence | LoginHandler.java, ENVIRONMENT_SETUP.md |
| Rate limiting errors | Increase delay between applications (currently 10-30s) | ApplicationHandler.java, config section |

## Dependency Management

### Current Dependencies (from build.gradle)
```gradle
implementation 'org.springframework.boot:spring-boot-starter-webmvc' // Web framework
implementation 'org.springframework.boot:spring-boot-starter-selenium' // WebDriver
testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

### Adding Dependencies
```bash
# Update build.gradle, then run:
./gradlew clean build  # This downloads and integrates new deps

# Example: Adding Selenium explicitly
# gradle:
# implementation 'org.seleniumhq.selenium:selenium-java:4.15.0'
```

## Testing Strategy

1. **Unit Tests**: JobMatcher, EnvironmentVariableLoader (no browser)
2. **Integration Tests**: Full agent workflows (requires browser, env vars)
3. **Manual Testing**: LinkedIn/Dice portal changes, captcha scenarios
4. **Log File Validation**: Check output_jobs files are created correctly

## Documentation Reference

For detailed implementation guidance:
- **System Architecture**: `.github/agents/README.md` (full 245 lines)
- **Navigation Guide**: `.github/agents/QUICK_START.md`
- **Implementation Details**: `.github/agents/IMPLEMENTATION_GUIDE.md`
- **Selenium Patterns**: `.github/agents/BROWSER_AUTOMATION.md` (8+ examples)
- **Matching Algorithm**: `.github/agents/JOB_MATCHING.md` (5+ code examples)

## AI Agent Workflow (Quick Start)

### Task: Add a new feature
1. **Understand scope** → Check which agent (LinkedIn/Dice) affected
2. **Review related patterns** → See similar implementations in same agent
3. **Check tests** → Run existing tests to establish baseline
4. **Implement** → Follow the pattern (handlers, error handling, logging)
5. **Test locally** → Run ./gradlew test, verify log file creation
6. **Reference docs** → Link to relevant .github/agents/XXX.md files in PR

### Task: Fix a bug
1. **Locate component** → Use file paths above to find code
2. **Understand flow** → Trace data through data flow diagram above
3. **Check existing logic** → Review similar patterns in same class
4. **Check docs** → See if pattern is documented in .github/agents/
5. **Apply fix** → Follow error handling pattern (log warning, graceful degradation)
6. **Validate** → Ensure log file still created, no regressions

---

**Last Updated**: April 17, 2026  
**Maintainer Docs**: See `.github/agents/README.md` and INDEX.md  
**Status**: Production-ready with extensive documentation

