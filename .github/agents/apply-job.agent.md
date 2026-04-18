# Apply-Jobs Agent Documentation

**Project Name:** Apply-Jobs  
**Version:** 0.0.1-SNAPSHOT  
**Java Version:** 25  
**Spring Boot:** 4.0.5  
**Selenium:** 4.20.0  
**WebDriverManager:** 5.9.1  
**Build Tool:** Maven  
**Last Updated:** April 18, 2026

---

## 📋 Project Overview

**Apply-Jobs** is a Spring Boot automation agent that intelligently applies to job postings on LinkedIn (and Dice platforms) using Selenium WebDriver. The system extracts job listings, validates them against comprehensive criteria (employment type, salary, skills, experience), handles multi-step application forms, and logs results.

### Key Technologies
- **Java 25** - Latest Java version
- **Spring Boot 4.0.5** - Application framework
- **Selenium 4.20.0** - Browser automation
- **WebDriverManager 5.9.1** - Automatic ChromeDriver management
- **Maven** - Build and dependency management
- **SLF4J 2.0.9** - Logging framework (with Logback)
- **JUnit 5** - Testing framework

---

## 📁 Project Structure

```
Apply-Jobs/
├── src/main/
│   ├── java/com/example/applyjobs/
│   │   ├── ApplyJobsApplication.java                  (Spring Boot entry point)
│   │   ├── LinkedInJobApplicationRunner.java          (CLI runner)
│   │   │
│   │   ├── automation/
│   │   │   ├── BrowserAutomationEngine.java           (Singleton WebDriver)
│   │   │   └── WaitHelper.java                        (Wait utilities)
│   │   │
│   │   ├── config/
│   │   │   └── EnvironmentVariableLoader.java         (Configuration)
│   │   │
│   │   ├── exception/
│   │   │   ├── ApplicationException.java
│   │   │   ├── JobExtractionException.java
│   │   │   ├── LoginException.java
│   │   │   └── NavigationException.java
│   │   │
│   │   ├── linkedin/
│   │   │   ├── LinkedInJobApplicationAgent.java       (Main orchestrator)
│   │   │   └── handlers/
│   │   │       ├── LinkedInLoginHandler.java
│   │   │       ├── LinkedInNavigationHandler.java
│   │   │       ├── LinkedInJobExtractor.java
│   │   │       ├── LinkedInApplicationHandler.java
│   │   │       └── LinkedInJobValidationHandler.java  (NEW: Job validation)
│   │   │
│   │   ├── matcher/
│   │   │   └── JobMatcher.java                        (Skill matching)
│   │   │
│   │   ├── logger/
│   │   │   └── ResultLogger.java                      (Result logging)
│   │   │
│   │   └── model/
│   │       ├── JobListing.java                        (Base class)
│   │       ├── LinkedInJobListing.java
│   │       ├── ApplicationResult.java
│   │       └── DiceJobListing.java
│   │
│   └── resources/
│       └── application.properties
│
├── src/test/
│   └── java/com/example/applyjobs/
│       ├── ApplyJobsApplicationTests.java
│       └── linkedin/LinkedInJobApplicationAgentTest.java
│
├── pom.xml                                     (Maven config)
├── target/                                     (Build output)

### 1. **BrowserAutomationEngine** (Singleton)
**File:** `automation/BrowserAutomationEngine.java`

- Manages WebDriver lifecycle
- Initializes Chrome with specific flags
- Implements singleton pattern for single browser instance
- Uses WebDriverManager for automatic ChromeDriver download

**Key Methods:**
```java
getInstance()           // Get singleton instance
getDriver()            // Get WebDriver
initializeDriver()     // Initialize Chrome
closeDriver()          // Cleanup
```

### 2. **WaitHelper**
**File:** `automation/WaitHelper.java`

- Wraps WebDriverWait for explicit waits
- Provides common wait patterns
- Non-blocking element presence checks
- Default timeout: 10 seconds

**Key Methods:**
```java
waitForElementVisible(By)          // Wait for visibility
waitForElementClickable(By)        // Wait for clickability
waitForElementPresent(By)          // Wait for DOM presence
isElementPresent(By)               // Non-blocking check (returns boolean)
```

### 3. **EnvironmentVariableLoader**
**File:** `config/EnvironmentVariableLoader.java`

Reads configuration from environment variables with defaults.

**Environment Variables:**
```bash
# Required: LinkedIn Credentials
LINKEDIN_USERNAME       "user@example.com"
LINKEDIN_PASSWORD       "password"

# Required: Job Search
JOB_TITLE               "Full Stack Engineer"
JOB_SKILLS              "Java,Spring Boot,React" (comma-separated)

# Optional: Tuning
JOB_MATCHING_THRESHOLD  50                (percentage)
OUTPUT_DIRECTORY        "."               (default: current dir)
```

### 4. **LinkedInJobApplicationAgent** (Orchestrator)
**File:** `linkedin/LinkedInJobApplicationAgent.java`

Main workflow coordinator.

**Workflow:**
```
1. Initialize browser & handlers
2. Validate credentials
3. Perform login
4. Navigate to jobs page
5. Search for jobs
6. Extract latest 5 job listings
7. Process each job:
   - Match against criteria
   - Apply if match
   - Log result
8. Write summary & cleanup
```

### 5. **LinkedIn Handlers**

#### LinkedInLoginHandler
**File:** `linkedin/handlers/LinkedInLoginHandler.java`

- Navigates to LinkedIn login
- Enters username/password
- Waits for authentication
- Validates successful login

#### LinkedInNavigationHandler
**File:** `linkedin/handlers/LinkedInNavigationHandler.java`

- Navigates to LinkedIn Jobs page
- Searches for jobs
- Handles page transitions

#### LinkedInJobExtractor
**File:** `linkedin/handlers/LinkedInJobExtractor.java`

- Extracts job listings from page
- Parses title, company, location, description
- Returns `List<LinkedInJobListing>`
- Multi-level XPath fallback strategy

**XPath Selectors (in order):**
```xpath
//ul[@class='jobs-search__results-list']//li
//li[contains(@class, 'base-card')]
//div[@data-job-id]
//div[contains(@class, 'base-card')]
//article[contains(@class, 'job-')]
//a[contains(@href, '/jobs/view/')]
//*[@data-job-id]
```

#### LinkedInApplicationHandler
**File:** `linkedin/handlers/LinkedInApplicationHandler.java`

- Clicks apply button on job listings
- Handles Easy Apply modal workflow
- Processes additional questions
- Intelligently fills form fields based on type
- Implements 10-30 second random delays (rate limiting)

**Question Types Handled:**
- Text input fields
- Textarea fields
- Select dropdowns
- Checkbox fields

**Rate Limiting:**
```
MIN_DELAY: 10 seconds
MAX_DELAY: 30 seconds
Random delay between applications
```

#### LinkedInJobValidationHandler (NEW)
**File:** `linkedin/handlers/LinkedInJobValidationHandler.java`

Comprehensive job validation handler with 4-level criteria checking.

**Validation Criteria:**
1. **Employment Type:** Must be "Contract"
2. **Salary Range:** Minimum $60/hour
3. **Skills Match:** Job description contains required skills
4. **Experience:** Requires 10+ years of IT experience

**Returns:** `ValidationResult` object containing detailed pass/fail for each criterion

**Usage:**
```java
ValidationResult result = validationHandler.validateJob(jobListing);
if (result.isValid()) {
    // Proceed with application
}
```

### 6. **JobMatcher**
**File:** `matcher/JobMatcher.java`

Intelligent job matching algorithm.

**Matching Criteria:**
```
✓ Title Match: Job title contains search term (case-insensitive substring)
✓ Skills Match: At least 50% of required skills in description
✓ Result: Apply only if BOTH criteria met
```

### 7. **ResultLogger**
**File:** `logger/ResultLogger.java`

Logs application results to file and console.

**Output Format:**
```
Tab-separated values (4 columns):
Job Title | Company Name | Application Date (YYYY-MM-DD) | Status
```

**Filename Pattern:**
```
applied_jobs_{provider}_{YYYY-MM-DD}.txt
```

---

## 🚀 Running the Agent

### Prerequisites
- Java 25+
- Chrome or Chromium browser
- Valid LinkedIn account credentials
- Environment variables configured

### Setup Environment Variables

```bash
export LINKEDIN_USERNAME="your_email@example.com"
export LINKEDIN_PASSWORD="your_password"
export JOB_TITLE="Full Stack Engineer"
export JOB_SKILLS="Java,Spring Boot,React,PostgreSQL,Docker"
export JOB_MATCHING_THRESHOLD=50
export OUTPUT_DIRECTORY=$(pwd)
```

### Build with Maven

```bash
cd /Users/rsvnarayanas/Desktop/Work/Java/Spring-Boot-Apps/AI-Tasks/Apply-Jobs

# Clean and build with Maven
mvn clean package

# Run tests
mvn test

# Execute directly (without JAR)
mvn exec:java -Dexec.mainClass="com.example.applyjobs.LinkedInJobApplicationRunner"
```

### Run the Agent

```bash
# Method 1: Execute JAR directly
java -jar target/Apply-Jobs-0.0.1-SNAPSHOT-jar-with-dependencies.jar

# Method 2: Run CLI runner class
java -cp target/Apply-Jobs-0.0.1-SNAPSHOT.jar \
  com.example.applyjobs.LinkedInJobApplicationRunner

# Method 3: Using Maven exec plugin
mvn exec:java -Dexec.mainClass="com.example.applyjobs.LinkedInJobApplicationRunner"
```

### Verify Results

```bash
# Check today's application log
cat applied_jobs_linkedin_$(date +%Y-%m-%d).txt

# Example output:
# Job Title               Company Name        Application Date    Status
# Full Stack Engineer     TechCorp Inc        2026-04-18         SUCCESS
# Senior Java Dev         StartupX            2026-04-18         SUCCESS
```

---

## 🔑 Key Features

✅ **Comprehensive Job Validation**
- Employment type verification (Contract required)
- Salary threshold checking ($60/hour minimum)
- Skills-based filtering with configurable threshold
- Experience requirement validation (10+ years)

✅ **Intelligent Job Matching**
- Title substring matching (case-insensitive)
- Skills-based filtering (configurable % threshold)
- Multi-level validation before applying
- Only applies to genuinely matching jobs

✅ **Smart Form Automation**
- Auto-detects form field types (text, dropdown, checkbox, textarea)
- Context-aware field filling
- Handles Easy Apply multi-step workflows
- Intelligently answers additional questions
- Unchecks "Follow company" on review steps

✅ **Rate Limiting & Respect**
- 10-30 second random delays between applications
- Prevents LinkedIn rate limiting
- Respects ToS compliance

✅ **Comprehensive Logging**
- SLF4J with Logback for detailed debug logs
- Tab-separated result files with timestamps
- Successful applications tracked separately
- Summary statistics after completion
- File-based persistence (survives restarts)

✅ **Robust Error Handling**
- Custom exception hierarchy for different failure modes
- Non-critical failures don't halt workflow
- Graceful degradation for form variations
- Complete driver cleanup on exit

✅ **XPath Fallback Strategy**
- Multiple selector strategies per element type
- Automatic fallback on selector failure
- Handles frequent LinkedIn DOM changes
- Debug-friendly selector logging

---

## 🛠️ Configuration & Customization

### Environment Variables

**Required:**
```bash
export LINKEDIN_USERNAME="your_email@example.com"
export LINKEDIN_PASSWORD="your_password"
```

**Job Criteria:**
```bash
export JOB_TITLE="Full Stack Engineer"          # Target job title
export JOB_SKILLS="Java,Spring,PostgreSQL"     # Comma-separated skills
export JOB_MATCHING_THRESHOLD=50                # Min % of skills required (default: 50)
```

**Output:**
```bash
export OUTPUT_DIRECTORY="/path/to/logs"        # Where to save result files (default: current dir)
```

**Dice (Future Use):**
```bash
export DICE_USERNAME="dice_email@example.com"  # Dice credentials (not yet implemented)
export DICE_PASSWORD="dice_password"
```

### Adjust Validation Criteria

Edit in `LinkedInJobValidationHandler.validateJob()`:

```java
// Change salary threshold
private static final double MIN_SALARY_HOURLY = 60.0;

// Change experience requirement
private static final int REQUIRED_YEARS_EXPERIENCE = 10;

// Change employment type requirement
private static final String REQUIRED_EMPLOYMENT_TYPE = "Contract";
```

### Change Rate Limiting

Edit in `LinkedInApplicationHandler.java`:

```java
private static final int MIN_DELAY = 10000;   // 10 seconds (adjust as needed)
private static final int MAX_DELAY = 30000;   // 30 seconds (adjust as needed)
```

### Update XPath Selectors (LinkedIn DOM Changes)

If LinkedIn UI changes, update in `LinkedInJobExtractor.tryFindJobElements()`:

```java
List<String> xpathSelectors = new ArrayList<>();
xpathSelectors.add("//your-new-xpath-here");  // Add new selectors first (higher priority)
xpathSelectors.add("//ul[@class='jobs-search__results-list']//li");  // Existing fallbacks
xpathSelectors.add("//li[contains(@class, 'base-card')]");
```

---

## 🧪 Testing

### Run Tests with Maven

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=LinkedInJobApplicationAgentTest

# Run with detailed output
mvn test -X
```

### Test Files Location
- [src/test/java/com/example/applyjobs/](src/test/java/com/example/applyjobs/)

### Key Test Classes
- `ApplyJobsApplicationTests.java` - Spring Boot context and application startup
- `LinkedInJobApplicationAgentTest.java` - Configuration validation and credential checking

### Test Strategy
1. **Unit Tests:** JobMatcher, EnvironmentVariableLoader (no browser needed)
2. **Integration Tests:** Full agent workflow (requires actual browser)
3. **Manual Testing:** Verify against live LinkedIn UI before production
4. **Log Validation:** Verify output file format and content

### Test Limitations
- Most tests skip actual browser automation (Selenium WebDriver is real-world only)
- Integration tests require test LinkedIn credentials
- Full workflow testing is primarily manual due to Selenium's real-world nature

## 📋 Exception Hierarchy

```
Exception
├── ApplicationException
│   ├── LoginException
│   ├── NavigationException
│   ├── JobExtractionException
│   └── ApplicationException (general)
```

---

## 📝 Logging Configuration

**Logger Setup:**
```java
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);
```

**Log Levels:**
- **INFO:** Workflow progress, major milestones
- **WARN:** Non-critical issues (e.g., element not found)
- **ERROR:** Critical failures (login fail, extraction fail)
- **DEBUG:** Detailed diagnostic info (XPath attempts, retries)

---

## 📚 Dependencies (Maven)

Key dependencies declared in `pom.xml`:

```xml
<!-- Spring Boot 4.0.5 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>4.0.5</version>
</dependency>

<!-- Selenium WebDriver 4.20.0 -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.20.0</version>
</dependency>

<!-- WebDriverManager 5.9.1 -->
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.9.1</version>
</dependency>

<!-- Logging: SLF4J + Logback 2.0.9 -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.14</version>
</dependency>

<!-- Apache Commons Lang 3.13.0 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.13.0</version>
</dependency>

<!-- Testing: JUnit 5 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <version>4.0.5</version>
    <scope>test</scope>
</dependency>
```

### Maven Build Plugins

```xml
<!-- Spring Boot Maven Plugin -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
</plugin>

<!-- Maven Compiler Plugin (Java 25) -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>25</source>
        <target>25</target>
    </configuration>
</plugin>

<!-- Maven Surefire Plugin (Testing) -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
</plugin>

<!-- Maven Assembly Plugin (Uber JAR) -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
</plugin>
```

---

## 🎯 Common Patterns Used

### 1. Singleton Pattern (BrowserAutomationEngine)
```java
private static BrowserAutomationEngine instance;

public static synchronized BrowserAutomationEngine getInstance() {
    if (instance == null) {
        instance = new BrowserAutomationEngine();
    }
    return instance;
}
```

### 2. Handler Pattern (LoginHandler, NavigationHandler, etc.)
```java
public class LinkedInLoginHandler {
    private WebDriver driver;
    
    public LinkedInLoginHandler(WebDriver driver) {
        this.driver = driver;
    }
    
    public void login(String username, String password) {
        // Implementation
    }
}
```

### 3. Template Method Pattern (JobApplicationAgent)
```java
public void executeWorkflow() {
    initializeBrowser();
    performLogin();
    performJobSearch();
    processJobListings();
    cleanup();
}
```

### 4. Strategy Pattern (JobMatcher)
```java
public boolean isMatch(String title, String description) {
    return matchesTitleStrategy(title) && matchesSkillsStrategy(description);
}
```

---

## 🚨 Important Notes

⚠️ **Respect LinkedIn Terms of Service**
- Use responsibly
- Only apply to jobs matching your profile
- Monitor first few runs
- Respect rate limits

✅ **Best Practices**
- Start with 2-3 test jobs first to verify configuration
- Monitor output logs for unexpected behavior
- Adjust matching criteria if getting irrelevant applications
- Schedule runs during business hours when possible
- Store credentials in environment variables only, never in code
- Keep secrets out of version control (.gitignore)

🔐 **Security Recommendations**
- Use environment variables for credentials (never hardcode)
- Run on private machine or secure server only
- Don't commit apply-job.agent.md with credentials
- Consider using credential managers (1Password, AWS Secrets, etc.)
- Rotate LinkedIn password periodically

## 📖 Additional Resources

- **Architecture Details:** Review [AGENTS.md](../../AGENTS.md) for detailed patterns
- **Code Documentation:** Check source files for implementation comments
- **Log Analysis:** Enable DEBUG logging for detailed execution traces
- **XPath Debugging:** Use Chrome DevTools (F12) to test selectors
- **LinkedIn Changes:** Update XPaths in LinkedInJobExtractor if UI changes

## 🛠️ Debugging Tips

**Enable Debug Logging:**
```bash
# Check logs with DEBUG level output for detailed execution flow
java -jar target/Apply-Jobs-0.0.1-SNAPSHOT-jar-with-dependencies.jar 2>&1 | grep DEBUG
```

**Inspect XPath Failures:**
1. Open LinkedIn in browser with Chrome DevTools (F12)
2. Test XPath in Console: `$x('//your-xpath')`
3. Check page structure if selectors fail
4. Update XPath in LinkedInJobExtractor.java
5. Re-build and test

**Manual Verification:**
```bash
# Check if results file was created
ls -la applied_jobs_linkedin_*.txt

# View last 10 applications
tail -n 10 applied_jobs_linkedin_$(date +%Y-%m-%d).txt
```

## 📞 Support

For issues or questions:
1. Check logs for error messages
2. Verify environment variables are set correctly
3. Ensure LinkedIn credentials are accurate
4. Check if LinkedIn UI has changed (update XPaths if needed)
5. Verify Chrome/Chromium is installed and accessible
6. Confirm network connectivity to LinkedIn
7. Review README.md and AGENTS.md for additional guidance

---

**Status:** ✅ Production Ready  
**Last Updated:** April 18, 2026  
**Version:** 1.0  
**Maintainer:** AI Agent System  
**License:** Proprietary - AI-Tasks Project
