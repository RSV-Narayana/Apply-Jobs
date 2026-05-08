# AI Agents Guide for Apply-Jobs

This document guides AI coding agents on critical patterns, architecture, and workflows for the Apply-Jobs project.

## Project Overview

**Apply-Jobs** is a Spring Boot-based AI automation agent that applies to job postings on LinkedIn and Dice platforms using Selenium WebDriver. The system extracts job listings, validates them against user criteria, handles multi-step application forms, and logs results.

## Architecture & Key Components

### Component Map

```
ApplyJobsApplication (Entry Point)
  ↓
LinkedInJobApplicationAgent (Main Workflow Orchestrator)
  ├─ LinkedInLoginHandler → Login authentication
  ├─ LinkedInNavigationHandler → Page navigation, job search
  ├─ LinkedInJobExtractor → Extract job listings with multi-condition validation
  ├─ LinkedInApplicationHandler → Click, fill forms, submit applications
  ├─ JobMatcher → Filter jobs by title and skills
  └─ ResultLogger → Log successful applications
```

### Critical Data Flow

1. **Extraction Phase**: `LinkedInJobExtractor` finds job elements via XPath, extracts details
2. **Validation Phase**: `JobMatcher` checks title/skills, then new validation layer checks employment type, salary, description match, and experience
3. **Application Phase**: `LinkedInApplicationHandler` clicks job, validates conditions, then navigates Easy Apply flow
4. **Logging Phase**: `ResultLogger` records only SUCCESSFUL applications, not attempts

## Essential Patterns & Conventions

### 1. XPath Selectors - Fallback Strategy Pattern

LinkedIn changes its DOM frequently. Always use **multiple XPath selectors with fallback logic**:

```java
// Pattern: Try primary selector, fall back to alternatives
List<String> xpathSelectors = new ArrayList<>();
xpathSelectors.add("//ul[@class='jobs-search__results-list']//li");  // Current (2024)
xpathSelectors.add("//li[contains(@class, 'base-card')]");           // Alternative
xpathSelectors.add("//div[@data-job-id]");                           // Attribute-based

for (String xpath : xpathSelectors) {
    try {
        List<WebElement> elements = driver.findElements(By.xpath(xpath));
        if (!elements.isEmpty()) {
            logger.info("Found {} elements using: {}", elements.size(), xpath);
            return elements;
        }
    } catch (Exception e) {
        logger.debug("XPath failed: {}", xpath);
    }
}
```

**Current 2026 XPaths to update** (in LinkedInJobExtractor):
- Job listings: LinkedIn's 2026 UI uses nested `article` or `li` tags with data attributes
- Job details panel: Right-side panel structure for viewing job details before applying
- Click target: Each job must be clickable to trigger details panel update

### 2. Wait & Synchronization Pattern

Use `WaitHelper` for reliable element interaction:

```java
WebElement element = waitHelper.waitForElementClickable(locator, timeoutSeconds);
// OR
boolean isPresent = waitHelper.isElementPresent(locator);
```

**Wait times matter**: 
- 1-3 sec: Small DOM updates
- 5-10 sec: Page navigation
- 10+ sec: Modal dialogs, form loads

### 3. Job Validation Conditions (NEW WORKFLOW)

Before applying, check in this order:
1. **Employment Type**: Must be "Contract" (extract from job details page)
2. **Salary**: Minimum $60/hour (extract from job details page)
3. **Skills Match**: Job description must contain JOB_SKILLS from config (use existing `JobMatcher`)
4. **Experience**: Job must require "10 or more" years of IT experience (parse from description)

These checks happen on the **job details page** (right panel) AFTER clicking the job.

### 4. Easy Apply Form Navigation Pattern

The Easy Apply modal has multiple steps. Each step may contain:
- Text inputs, textareas, select dropdowns, checkboxes
- Form validation
- "Next" buttons to advance
- "Submit" or "Done" button to complete

**Pattern**:
```java
for (int i = 0; i < maxIterations; i++) {
    // 1. Check if review step → uncheck "Follow company"
    if (isReviewStep()) {
        handleReviewStep();
    }
    
    // 2. Fill any question fields
    if (hasAdditionalQuestions()) {
        handleAdditionalQuestions();
    }
    
    // 3. Click Next to continue
    if (clickNextButton()) {
        Thread.sleep(1000); // Wait for form to load
        continue;
    }
    
    // 4. If no Next, try Submit
    if (clickSubmitButton()) {
        return true; // SUCCESS
    }
    
    // No buttons = form complete
    return true;
}
```

## Configuration & Environment Variables

Set these before running:

```bash
export LINKEDIN_USERNAME="your_email@example.com"
export LINKEDIN_PASSWORD="your_password"
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL"
export JOB_MATCHING_THRESHOLD="50"  # Percentage of skills that must match
export OUTPUT_DIRECTORY="/path/to/logs"
```

Load via `EnvironmentVariableLoader` class.

## Build & Execution

### Maven Build (Project is Maven-based)
```bash
cd /Users/rsvnarayanas/Desktop/Work/Java/Spring-Boot-Apps/AI-Tasks/Apply-Jobs
mvn clean package  # Creates JAR in target/
mvn exec:java -Dexec.mainClass="com.example.applyjobs.ApplyJobsApplication"
```

### Running the JAR
```bash
java -jar target/Apply-Jobs-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## Critical Implementation Details

### Result Logging
- **Only log SUCCESSFUL applications** (after Submit button is clicked successfully)
- `ApplicationResult` records: title, company, date, status ("SUCCESS", "FAILED", "ATTEMPTED")
- Avoid logging attempts that don't complete the application

### Threading & Delays
- Random delays between applications: 10-30 seconds (rate limiting for LinkedIn)
- Screenshot opportunity: Capture application confirmation before closing modal
- Interrupt handling: Thread interrupts must be properly propagated

### Error Handling
- Use custom exceptions: `ApplicationException`, `JobExtractionException`, `LoginException`, `NavigationException`
- Log at appropriate levels: INFO for major steps, DEBUG for selectors, WARN for recoverable issues
- Fallback mechanisms for every XPath selector

## Testing Approach

```bash
mvn test  # Run tests with Maven
```

Test files location: `src/test/java/com/example/applyjobs/`

Key test classes:
- `LinkedInJobApplicationAgentTest.java` → Test workflow orchestration
- Mock or skip actual browser interactions in tests

## Debugging Tips

1. **XPath not finding elements?** → Call `inspectPageStructure()` in LinkedInJobExtractor to log page state
2. **Form field identification?** → Check element attributes: `name`, `aria-label`, `placeholder`
3. **Modal not closing?** → Wait longer or scroll to find hidden buttons
4. **Application logged but not submitted?** → Verify `clickSubmitButton()` was called, not just attempted

## Performance Considerations

- Selenium operations are synchronous; each click/wait blocks execution
- Total runtime for 5 jobs with full Easy Apply: ~5-10 minutes
- Browser memory footprint: ~500MB-1GB (long-running jobs should periodically restart browser)

## Dependency Management

Key dependencies (see pom.xml):
- Selenium: 4.15.0
- Spring Boot: 4.0.5
- WebDriverManager: 5.6.2 (automatic ChromeDriver management)
- SLF4J/Logback: 2.0.9 (logging)

Avoid direct `WebDriver` management; use `BrowserAutomationEngine.getInstance()`.

---

**Last Updated**: 2026 | **Java Version**: 25

