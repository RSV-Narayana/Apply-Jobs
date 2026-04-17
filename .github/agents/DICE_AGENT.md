# Dice Agent Implementation Guide

## Overview

The Dice Agent automates job searches and applications on Dice by:
1. Opening Chrome and navigating to Dice login
2. Authenticating with credentials from environment variables
3. Searching for "full stack engineer" positions
4. Extracting the latest job listings
5. Matching jobs against user skills
6. Applying to matching jobs
7. Logging results

## Dice Workflow Steps

### Step 1: Open Chrome & Navigate to Login

```java
public class DiceAgent {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private LoginHandler loginHandler;
    private static final Logger logger = LoggerFactory.getLogger(DiceAgent.class);
    
    private static final String DICE_LOGIN_URL = "https://www.dice.com/dashboard";
    private static final String DICE_SEARCH_URL = "https://www.dice.com/jobs";
    
    public DiceAgent() {
        BrowserAutomationEngine engine = new BrowserAutomationEngine();
        this.driver = engine.getDriver();
        this.waitHelper = new WaitHelper(driver);
        this.loginHandler = new LoginHandler(driver);
    }
    
    public void executeDiceJobSearch() {
        try {
            logger.info("Starting Dice job application workflow");
            
            // Step 1: Open Chrome and login
            performLogin();
            
            // Step 2: Search for full stack engineer
            searchForJobs();
            
            // Step 3: Extract and process jobs
            processJobListings();
            
            logger.info("Dice workflow completed successfully");
            
        } catch (Exception e) {
            logger.error("Dice workflow failed", e);
            throw new RuntimeException(e);
        }
    }
    
    public WebDriver getDriver() {
        return driver;
    }
}
```

### Step 2: Dice Login Flow

```java
public class DiceLoginHandler {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private static final Logger logger = LoggerFactory.getLogger(DiceLoginHandler.class);
    
    // Dice login locators
    private static final By EMAIL_FIELD = By.id("email");
    private static final By PASSWORD_FIELD = By.id("password");
    private static final By LOGIN_BUTTON = By.xpath("//button[contains(text(), 'Sign In') or contains(text(), 'Login')]");
    private static final By MY_PROFILE_LINK = By.xpath("//a[contains(@href, '/profile')] | //span[text()='Profile']");
    
    public DiceLoginHandler(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    public void login(String email, String password) {
        try {
            logger.info("Navigating to Dice login page");
            driver.get("https://www.dice.com/dashboard");
            
            // Wait for page to load
            waitHelper.waitForPageLoad();
            
            // Check if already logged in
            try {
                driver.findElement(MY_PROFILE_LINK);
                logger.info("Already logged in to Dice");
                return;
            } catch (NoSuchElementException e) {
                // Not logged in, proceed with login
            }
            
            logger.info("Entering Dice credentials");
            
            // Click login/sign in if needed to reveal form
            try {
                WebElement loginLink = waitHelper.waitForElementClickable(
                    By.xpath("//a[contains(text(), 'Sign In')] | //button[contains(text(), 'Sign In')]")
                );
                loginLink.click();
                Thread.sleep(1000);
            } catch (TimeoutException e) {
                // Form may already be visible
                logger.debug("Login form already visible");
            }
            
            // Enter email
            WebElement emailField = waitHelper.waitForElementVisible(EMAIL_FIELD);
            emailField.clear();
            emailField.sendKeys(email);
            
            // Enter password
            WebElement passwordField = waitHelper.waitForElementVisible(PASSWORD_FIELD);
            passwordField.clear();
            passwordField.sendKeys(password);
            
            logger.info("Clicking sign in button");
            
            // Click login button
            WebElement loginButton = waitHelper.waitForElementClickable(LOGIN_BUTTON);
            loginButton.click();
            
            // Wait for dashboard to load
            logger.info("Waiting for Dice dashboard to load");
            waitHelper.waitForElementVisible(MY_PROFILE_LINK);
            
            logger.info("Dice login successful");
            
        } catch (TimeoutException e) {
            logger.error("Dice login failed - timeout");
            throw new LoginException("Dice login failed: " + e.getMessage(), e);
        }
    }
}
```

### Step 3: Search for Jobs

```java
public class DiceJobSearchHandler {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private static final Logger logger = LoggerFactory.getLogger(DiceJobSearchHandler.class);
    
    // Dice search locators
    private static final By SEARCH_INPUT = By.xpath("//input[@placeholder='Search jobs' or @placeholder='Search']");
    private static final By SEARCH_BUTTON = By.xpath("//button[contains(text(), 'Search')] | //button[@aria-label='Search']");
    private static final By JOB_RESULTS_CONTAINER = By.xpath("//div[@class='card-container'] | //div[@data-test-id='jobResults']");
    
    public DiceJobSearchHandler(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    public void searchForFullStackEngineer() {
        try {
            logger.info("Navigating to Dice jobs search page");
            driver.get("https://www.dice.com/jobs");
            
            waitHelper.waitForPageLoad();
            
            logger.info("Searching for 'full stack engineer' jobs");
            
            // Enter search term
            WebElement searchInput = waitHelper.waitForElementVisible(SEARCH_INPUT);
            searchInput.clear();
            searchInput.sendKeys("full stack engineer");
            
            // Click search button
            WebElement searchButton = waitHelper.waitForElementClickable(SEARCH_BUTTON);
            searchButton.click();
            
            // Wait for results to load
            logger.info("Waiting for search results to load");
            waitHelper.waitForElementVisible(JOB_RESULTS_CONTAINER);
            
            // Additional wait for results to populate
            Thread.sleep(2000);
            
            logger.info("Job search results loaded");
            
        } catch (Exception e) {
            logger.error("Failed to search for jobs on Dice", e);
            throw new SearchException("Dice job search failed", e);
        }
    }
}

public class SearchException extends RuntimeException {
    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Step 4: Extract Job Listings

```java
public class DiceJobExtractor {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private static final Logger logger = LoggerFactory.getLogger(DiceJobExtractor.class);
    
    // Dice job listing locators
    private static final By JOB_CARDS = By.xpath(
        "//div[@data-test-id='job-card'] | //article[@class='card'] | //div[@class='job-card']"
    );
    private static final By JOB_TITLE = By.xpath(".//h3 | .//h2");
    private static final By COMPANY_NAME = By.xpath(".//h4 | .//p[@class='company']");
    private static final By JOB_LINK = By.xpath(".//a[@href]");
    private static final By JOB_DESCRIPTION = By.xpath(".//p[@class='description'] | .//div[@class='job-info']");
    
    public DiceJobExtractor(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    public List<DiceJobListing> extractLatestJobListings(int limit) {
        try {
            logger.info("Extracting latest {} job listings from Dice", limit);
            
            List<DiceJobListing> jobs = new ArrayList<>();
            
            // Get all job card elements
            List<WebElement> jobCards = driver.findElements(JOB_CARDS);
            
            logger.info("Found {} total job cards on Dice", jobCards.size());
            
            // Process up to limit jobs
            for (int i = 0; i < Math.min(jobCards.size(), limit); i++) {
                try {
                    WebElement jobCard = jobCards.get(i);
                    
                    // Scroll to element
                    ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView(true);",
                        jobCard
                    );
                    
                    Thread.sleep(500);
                    
                    DiceJobListing job = extractJobDetails(jobCard);
                    if (job != null) {
                        jobs.add(job);
                    }
                    
                } catch (Exception e) {
                    logger.warn("Failed to extract job {}: {}", i, e.getMessage());
                    continue;
                }
            }
            
            logger.info("Successfully extracted {} jobs from Dice", jobs.size());
            return jobs;
            
        } catch (Exception e) {
            logger.error("Error extracting job listings from Dice", e);
            throw new JobExtractionException("Failed to extract job listings", e);
        }
    }
    
    private DiceJobListing extractJobDetails(WebElement jobCard) {
        try {
            // Extract title
            String title = jobCard.findElement(JOB_TITLE).getText();
            
            // Extract company
            String company = "";
            try {
                company = jobCard.findElement(COMPANY_NAME).getText();
            } catch (NoSuchElementException e) {
                logger.debug("Company name not found for job: {}", title);
            }
            
            // Extract job URL
            String jobUrl = "";
            try {
                WebElement link = jobCard.findElement(JOB_LINK);
                jobUrl = link.getAttribute("href");
            } catch (NoSuchElementException e) {
                logger.debug("Job URL not found for: {}", title);
            }
            
            // Extract preview description
            String previewDescription = "";
            try {
                previewDescription = jobCard.findElement(JOB_DESCRIPTION).getText();
            } catch (NoSuchElementException e) {
                logger.debug("Description not found for: {}", title);
            }
            
            // Get full description by clicking job and reading details
            String fullDescription = extractFullDescription(jobUrl, previewDescription);
            
            DiceJobListing job = new DiceJobListing(title, company, jobUrl, fullDescription);
            
            logger.debug("Extracted job: {}", job);
            return job;
            
        } catch (Exception e) {
            logger.error("Error extracting job details", e);
            return null;
        }
    }
    
    private String extractFullDescription(String jobUrl, String previewDescription) {
        if (jobUrl == null || jobUrl.isEmpty()) {
            return previewDescription;
        }
        
        try {
            String currentUrl = driver.getCurrentUrl();
            
            // Navigate to job details page
            driver.get(jobUrl);
            waitHelper.waitForPageLoad();
            Thread.sleep(1000);
            
            // Extract full description
            StringBuilder fullDesc = new StringBuilder(previewDescription);
            
            // Look for job details section
            try {
                WebElement detailsSection = driver.findElement(
                    By.xpath("//div[@class='job-details'] | //section[contains(@class, 'details')] | //div[@id='job-description']")
                );
                fullDesc.append(" ").append(detailsSection.getText());
            } catch (NoSuchElementException e) {
                logger.debug("Could not find full details section");
            }
            
            // Go back to search results
            driver.navigate().back();
            waitHelper.waitForPageLoad();
            
            return fullDesc.toString();
            
        } catch (Exception e) {
            logger.debug("Could not extract full description: {}", e.getMessage());
            return previewDescription;
        }
    }
}

public class DiceJobListing {
    private String title;
    private String company;
    private String jobUrl;
    private String description;
    
    public DiceJobListing(String title, String company, String jobUrl, String description) {
        this.title = title;
        this.company = company;
        this.jobUrl = jobUrl;
        this.description = description;
    }
    
    // Getters
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getJobUrl() { return jobUrl; }
    public String getDescription() { return description; }
    
    @Override
    public String toString() {
        return String.format("%s - %s", title, company);
    }
}

public class JobExtractionException extends RuntimeException {
    public JobExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Step 5: Apply to Matching Jobs

```java
public class DiceApplicationHandler {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private static final Logger logger = LoggerFactory.getLogger(DiceApplicationHandler.class);
    
    // Dice apply button locators
    private static final By APPLY_BUTTON = By.xpath(
        "//button[contains(text(), 'Apply')] | //a[contains(text(), 'Apply Now')]"
    );
    private static final By APPLICATION_MODAL = By.xpath("//div[@role='dialog'] | //div[@class='modal']");
    private static final By SUBMIT_APPLICATION = By.xpath(
        "//button[contains(text(), 'Submit')] | //button[contains(text(), 'Send')]"
    );
    private static final By SUCCESS_MESSAGE = By.xpath(
        "//span[contains(text(), 'applied')] | //div[contains(text(), 'Application sent')]"
    );
    
    public DiceApplicationHandler(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    public boolean applyToJob(DiceJobListing job) {
        String previousUrl = driver.getCurrentUrl();
        try {
            logger.info("Attempting to apply to job: {}", job.getTitle());
            
            // Navigate to job URL
            driver.get(job.getJobUrl());
            waitHelper.waitForPageLoad();
            Thread.sleep(1000);
            
            // Find and click apply button
            WebElement applyButton = waitHelper.waitForElementClickable(APPLY_BUTTON);
            logger.info("Clicking apply button");
            applyButton.click();
            
            // Handle application form
            handleApplicationForm();
            
            logger.info("Job application submitted: {}", job.getTitle());
            return true;
            
        } catch (TimeoutException e) {
            logger.warn("Apply button not found or clickable for: {}", job.getTitle());
            return false;
        } catch (Exception e) {
            logger.error("Failed to apply to job: {}", job.getTitle(), e);
            return false;
        } finally {
            // Try to navigate back
            try {
                driver.navigate().back();
            } catch (Exception e) {
                logger.debug("Could not navigate back");
            }
        }
    }
    
    private void handleApplicationForm() {
        try {
            logger.debug("Handling Dice application form");
            
            // Dice may show modal or redirect to application form
            // Common fields: phone, cover letter, etc.
            
            // Check if modal appears
            try {
                WebElement modal = waitHelper.waitForElementVisible(APPLICATION_MODAL);
                logger.debug("Application modal appeared");
                
                // Dice may have optional fields that are pre-filled
                // Just submit if all required fields are filled
                
                List<WebElement> requiredFields = driver.findElements(
                    By.xpath("//input[@required] | //textarea[@required]")
                );
                
                logger.debug("Found {} required fields", requiredFields.size());
                
                // Try to submit
                try {
                    WebElement submitBtn = driver.findElement(SUBMIT_APPLICATION);
                    submitBtn.click();
                    logger.info("Application submitted via modal");
                } catch (NoSuchElementException e) {
                    logger.warn("Submit button not found in modal");
                }
                
            } catch (TimeoutException e) {
                logger.debug("No application modal - may auto-submit or require redirect");
            }
            
            // Wait briefly for confirmation
            Thread.sleep(1000);
            
        } catch (Exception e) {
            logger.warn("Error handling application form: {}", e.getMessage());
        }
    }
}
```

## Complete Dice Agent Workflow

```java
public class DiceJobApplicationAgent {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private DiceLoginHandler loginHandler;
    private DiceJobSearchHandler searchHandler;
    private DiceJobExtractor jobExtractor;
    private DiceApplicationHandler applicationHandler;
    private JobMatcher jobMatcher;
    private ResultLogger resultLogger;
    
    private static final Logger logger = LoggerFactory.getLogger(DiceJobApplicationAgent.class);
    private static final int JOB_LIMIT = 5; // Latest 5 jobs
    
    public DiceJobApplicationAgent() {
        initializeComponents();
    }
    
    private void initializeComponents() {
        BrowserAutomationEngine engine = new BrowserAutomationEngine();
        this.driver = engine.getDriver();
        this.waitHelper = new WaitHelper(driver);
        this.loginHandler = new DiceLoginHandler(driver, waitHelper);
        this.searchHandler = new DiceJobSearchHandler(driver, waitHelper);
        this.jobExtractor = new DiceJobExtractor(driver, waitHelper);
        this.applicationHandler = new DiceApplicationHandler(driver, waitHelper);
        
        // Initialize job matcher
        String jobTitle = System.getenv("JOB_TITLE");
        List<String> jobSkills = Arrays.asList(System.getenv("JOB_SKILLS").split(","));
        this.jobMatcher = new JobMatcher(jobTitle, jobSkills);
        
        // Initialize result logger
        this.resultLogger = new ResultLogger("dice");
    }
    
    public void executeWorkflow() {
        try {
            logger.info("=== Starting Dice Job Application Agent ===");
            
            // 1. Login
            String username = System.getenv("DICE_USERNAME");
            String password = System.getenv("DICE_PASSWORD");
            loginHandler.login(username, password);
            
            // 2. Search for jobs
            searchHandler.searchForFullStackEngineer();
            
            // 3. Extract job listings
            List<DiceJobListing> jobs = jobExtractor.extractLatestJobListings(JOB_LIMIT);
            logger.info("Extracted {} jobs from Dice", jobs.size());
            
            // 4. Process and apply to matching jobs
            int appliedCount = 0;
            for (DiceJobListing job : jobs) {
                if (jobMatcher.isMatch(job)) {
                    logger.info("Job matches criteria: {}", job.getTitle());
                    boolean applied = applicationHandler.applyToJob(job);
                    if (applied) {
                        appliedCount++;
                        resultLogger.logJob(job);
                    }
                } else {
                    logger.info("Job does not match criteria: {}", job.getTitle());
                }
            }
            
            logger.info("=== Dice Job Application Complete ===");
            logger.info("Applied to {} jobs", appliedCount);
            
        } catch (Exception e) {
            logger.error("Dice agent workflow failed", e);
            throw new RuntimeException(e);
        } finally {
            driver.quit();
        }
    }
    
    public static void main(String[] args) {
        DiceJobApplicationAgent agent = new DiceJobApplicationAgent();
        agent.executeWorkflow();
    }
}
```

## Dice-Specific Considerations

### Authentication
- Dice uses email + password login
- No 2FA typically required
- Session usually lasts longer than LinkedIn

### Job Search
- Search box is on main jobs page
- Results load dynamically with infinite scroll
- URLs are consistent (good for direct navigation)

### Application Process
- Dice may redirect to external application system
- Some jobs use "Easy Apply" style modal
- Others redirect to company career page
- Resume may be pre-filled for logged-in users

### Job Description Format
- Job card shows preview on list view
- Full description available on job details page
- Structured data in "About the Job" section

## Error Handling

### Common Dice Issues

| Issue | Solution |
|-------|----------|
| Login fails | Verify email format, check password |
| Search returns no results | Try broader search terms, check filters |
| Apply button redirects externally | This is normal, follow redirect to company site |
| Session expires after apply | Re-login and continue with next job |
| Dynamic content not loading | Increase wait times, scroll to ensure visibility |

## Integration with Main System

See [README.md](README.md) for how Dice agent integrates with overall system and [OUTPUT_LOGGING.md](OUTPUT_LOGGING.md) for result logging.

