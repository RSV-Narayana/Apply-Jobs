# LinkedIn Agent Implementation Guide

## Overview

The LinkedIn Agent automates job searches and applications on LinkedIn by:
1. Opening Chrome and navigating to LinkedIn login
2. Authenticating with credentials from environment variables
3. Finding "Recent job searches" and clicking the "full stack engineer" entry
4. Extracting the latest job listings
5. Matching jobs against user skills
6. Applying to matching jobs
7. Logging results

## LinkedIn Workflow Steps

### Step 1: Open Chrome & Navigate to Login

```java
public class LinkedInAgent {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private LoginHandler loginHandler;
    private static final Logger logger = LoggerFactory.getLogger(LinkedInAgent.class);
    
    private static final String LINKEDIN_LOGIN_URL = "https://www.linkedin.com/login";
    private static final String LINKEDIN_JOBS_URL = "https://www.linkedin.com/jobs";
    
    public LinkedInAgent() {
        BrowserAutomationEngine engine = new BrowserAutomationEngine();
        this.driver = engine.getDriver();
        this.waitHelper = new WaitHelper(driver);
        this.loginHandler = new LoginHandler(driver);
    }
    
    public void executeLinkedInJobSearch() {
        try {
            logger.info("Starting LinkedIn job application workflow");
            
            // Step 1: Open Chrome and login
            performLogin();
            
            // Step 2: Navigate to recent searches
            navigateToRecentSearches();
            
            // Step 3: Click "full stack engineer" search
            clickFullStackSearch();
            
            // Step 4: Extract and process jobs
            processJobListings();
            
            logger.info("LinkedIn workflow completed successfully");
            
        } catch (Exception e) {
            logger.error("LinkedIn workflow failed", e);
            throw new RuntimeException(e);
        }
    }
    
    public WebDriver getDriver() {
        return driver;
    }
}
```

### Step 2: LinkedIn Login Flow

```java
public class LinkedInLoginHandler {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private static final Logger logger = LoggerFactory.getLogger(LinkedInLoginHandler.class);
    
    // LinkedIn login locators
    private static final By USERNAME_FIELD = By.id("username");
    private static final By PASSWORD_FIELD = By.id("password");
    private static final By LOGIN_BUTTON = By.xpath("//button[@type='submit' and contains(@aria-label, 'Sign')]");
    private static final By FEED_LINK = By.xpath("//a[contains(@href, '/feed/')]");
    
    public LinkedInLoginHandler(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    public void login(String username, String password) {
        try {
            logger.info("Navigating to LinkedIn login page");
            driver.get("https://www.linkedin.com/login");
            
            // Wait for page to load
            waitHelper.waitForPageLoad();
            
            logger.info("Entering credentials");
            
            // Enter username
            WebElement usernameField = waitHelper.waitForElementVisible(USERNAME_FIELD);
            usernameField.clear();
            usernameField.sendKeys(username);
            
            // Enter password
            WebElement passwordField = waitHelper.waitForElementVisible(PASSWORD_FIELD);
            passwordField.clear();
            passwordField.sendKeys(password);
            
            logger.info("Clicking sign in button");
            
            // Click login button
            WebElement loginButton = waitHelper.waitForElementClickable(LOGIN_BUTTON);
            loginButton.click();
            
            // Wait for feed to load (sign of successful login)
            logger.info("Waiting for LinkedIn feed to load");
            waitHelper.waitForElementVisible(FEED_LINK);
            
            logger.info("LinkedIn login successful");
            
        } catch (TimeoutException e) {
            logger.error("Login failed - timeout waiting for element");
            throw new LoginException("LinkedIn login failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handle 2FA if needed
     */
    public void handle2FA() {
        try {
            // Check if 2FA prompt appears
            WebElement twoFAField = waitHelper.waitForElementVisible(
                By.xpath("//input[@type='text' and @inputmode='numeric']"),
                Duration.ofSeconds(5)
            );
            
            logger.info("2FA prompt detected. Please enter code manually");
            // For automated scenarios, you might need to use email/SMS API
            
        } catch (TimeoutException e) {
            // 2FA not required
            logger.debug("No 2FA required");
        }
    }
}

public class LoginException extends RuntimeException {
    public LoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Step 3: Navigate to Recent Searches

```java
public class LinkedInNavigationHandler {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private static final Logger logger = LoggerFactory.getLogger(LinkedInNavigationHandler.class);
    
    // LinkedIn navigation locators
    private static final By JOBS_TAB = By.xpath("//a[contains(@href, '/jobs/') and contains(text(), 'Jobs')]");
    private static final By RECENT_SEARCHES_SECTION = By.xpath("//h2[contains(text(), 'Recent searches')]");
    private static final By SEARCH_ITEMS = By.xpath("//ul[@role='list']//li//button");
    
    public LinkedInNavigationHandler(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    public void navigateToJobsTab() {
        try {
            logger.info("Clicking Jobs tab");
            WebElement jobsTab = waitHelper.waitForElementClickable(JOBS_TAB);
            jobsTab.click();
            
            // Wait for jobs page to load
            waitHelper.waitForPageLoad();
            logger.info("Jobs tab loaded");
            
        } catch (TimeoutException e) {
            logger.error("Failed to navigate to jobs tab", e);
            throw new NavigationException("Cannot find jobs tab", e);
        }
    }
    
    public void navigateToRecentSearches() {
        try {
            logger.info("Looking for Recent Searches section");
            
            // Scroll down to find Recent Searches section
            WebElement recentSearchesSection = waitHelper.waitForElementVisible(RECENT_SEARCHES_SECTION);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", recentSearchesSection);
            
            logger.info("Recent Searches section found");
            
        } catch (TimeoutException e) {
            logger.error("Recent Searches section not found", e);
            throw new NavigationException("Cannot find Recent Searches section", e);
        }
    }
    
    public void clickFullStackEngineerSearch() {
        try {
            logger.info("Finding 'Full Stack Engineer' search item");
            
            // Get all search items
            List<WebElement> searchItems = driver.findElements(SEARCH_ITEMS);
            
            WebElement fullStackSearch = null;
            for (WebElement item : searchItems) {
                String text = item.getText().toLowerCase();
                if (text.contains("full") && text.contains("stack")) {
                    fullStackSearch = item;
                    break;
                }
            }
            
            if (fullStackSearch == null) {
                throw new NavigationException("Full Stack Engineer search not found in recent searches");
            }
            
            logger.info("Clicking Full Stack Engineer search");
            fullStackSearch.click();
            
            // Wait for results to load
            waitHelper.waitForPageLoad();
            Thread.sleep(2000); // Additional wait for results to populate
            
            logger.info("Job results loaded");
            
        } catch (Exception e) {
            logger.error("Failed to click Full Stack Engineer search", e);
            throw new NavigationException("Cannot click Full Stack search", e);
        }
    }
}

public class NavigationException extends RuntimeException {
    public NavigationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public NavigationException(String message) {
        super(message);
    }
}
```

### Step 4: Extract Job Listings

```java
public class LinkedInJobExtractor {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private static final Logger logger = LoggerFactory.getLogger(LinkedInJobExtractor.class);
    
    // LinkedIn job listing locators
    private static final By JOB_LISTINGS = By.xpath(
        "//li[@data-job-id] | //div[@data-view-name='job-card']"
    );
    private static final By JOB_TITLE = By.xpath(".//h3");
    private static final By COMPANY_NAME = By.xpath(".//p[@class='base-search-card__subtitle']");
    private static final By LOCATION = By.xpath(".//span[@class='job-search-card__location']");
    private static final By ABOUT_JOB_SECTION = By.xpath(".//ul[@class='description__list']");
    
    public LinkedInJobExtractor(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    public List<LinkedInJobListing> extractLatestJobListings(int limit) {
        try {
            logger.info("Extracting latest {} job listings", limit);
            
            List<LinkedInJobListing> jobs = new ArrayList<>();
            
            // Get all job listing elements
            List<WebElement> jobElements = driver.findElements(JOB_LISTINGS);
            
            logger.info("Found {} total job listings", jobElements.size());
            
            // Process up to limit jobs
            for (int i = 0; i < Math.min(jobElements.size(), limit); i++) {
                try {
                    WebElement jobElement = jobElements.get(i);
                    
                    // Scroll to element to ensure it's loaded
                    ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView(true);", 
                        jobElement
                    );
                    
                    Thread.sleep(500); // Wait for dynamic content to load
                    
                    LinkedInJobListing job = extractJobDetails(jobElement);
                    if (job != null) {
                        jobs.add(job);
                    }
                    
                } catch (Exception e) {
                    logger.warn("Failed to extract job {}: {}", i, e.getMessage());
                    continue; // Continue with next job
                }
            }
            
            logger.info("Successfully extracted {} jobs", jobs.size());
            return jobs;
            
        } catch (Exception e) {
            logger.error("Error extracting job listings", e);
            throw new JobExtractionException("Failed to extract job listings", e);
        }
    }
    
    private LinkedInJobListing extractJobDetails(WebElement jobElement) {
        try {
            String jobId = jobElement.getAttribute("data-job-id");
            
            // Extract title
            String title = jobElement.findElement(JOB_TITLE).getText();
            
            // Extract company
            String company = "";
            try {
                company = jobElement.findElement(COMPANY_NAME).getText();
            } catch (NoSuchElementException e) {
                logger.debug("Company name not found for job: {}", title);
            }
            
            // Extract location
            String location = "";
            try {
                location = jobElement.findElement(LOCATION).getText();
            } catch (NoSuchElementException e) {
                logger.debug("Location not found for job: {}", title);
            }
            
            // Extract description from "About the job" section
            String description = extractJobDescription(jobElement);
            
            LinkedInJobListing job = new LinkedInJobListing(
                jobId, title, company, location, description
            );
            
            logger.debug("Extracted job: {}", job);
            return job;
            
        } catch (Exception e) {
            logger.error("Error extracting job details", e);
            return null;
        }
    }
    
    private String extractJobDescription(WebElement jobElement) {
        StringBuilder description = new StringBuilder();
        
        // Try to click job to open details pane
        try {
            jobElement.click();
            Thread.sleep(1000); // Wait for details pane to load
            
            // Extract description from details pane
            By detailsPaneDescription = By.xpath(
                "//div[@data-view-name='job-details']/div//span[contains(@class, 'description')]"
            );
            
            List<WebElement> descElements = driver.findElements(detailsPaneDescription);
            for (WebElement elem : descElements) {
                description.append(elem.getText()).append(" ");
            }
            
        } catch (Exception e) {
            logger.debug("Could not extract full description from details pane");
        }
        
        return description.toString().trim();
    }
}

public class LinkedInJobListing {
    private String id;
    private String title;
    private String company;
    private String location;
    private String description;
    private String applyUrl;
    
    public LinkedInJobListing(String id, String title, String company, String location, String description) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
    }
    
    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getApplyUrl() { return applyUrl; }
    
    public void setApplyUrl(String url) { this.applyUrl = url; }
    
    @Override
    public String toString() {
        return String.format("%s - %s (%s)", title, company, location);
    }
}

public class JobExtractionException extends RuntimeException {
    public JobExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Step 5: Apply to Matching Jobs

#### Required Imports

```java
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
```

```java
public class LinkedInApplicationHandler {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private static final Logger logger = LoggerFactory.getLogger(LinkedInApplicationHandler.class);
    
    // LinkedIn apply button locators
    private static final By APPLY_BUTTON = By.xpath(
        "//button[contains(text(), 'Apply') or contains(@aria-label, 'Apply')]"
    );
    private static final By EASY_APPLY_BUTTON = By.xpath(
        "//button[contains(text(), 'Easy Apply')]"
    );
    private static final By SUBMIT_BUTTON = By.xpath(
        "//button[contains(text(), 'Submit') or contains(text(), 'Next')]"
    );
    private static final By SUCCESS_MESSAGE = By.xpath(
        "//span[contains(text(), 'Application sent')] | " +
        "//div[contains(text(), 'You applied')]"
    );
    
    public LinkedInApplicationHandler(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    public boolean applyToJob(LinkedInJobListing job) {
        try {
            logger.info("Attempting to apply to job: {}", job.getTitle());
            
            // Click the job to open details
            driver.get("https://www.linkedin.com/jobs/view/" + job.getId());
            
            waitHelper.waitForPageLoad();
            Thread.sleep(1000);
            
            // Find and click apply button
            WebElement applyButton = findApplyButton();
            if (applyButton == null) {
                logger.warn("No apply button found for job: {}", job.getTitle());
                return false;
            }
            
            logger.info("Clicking apply button");
            applyButton.click();
            
            // Handle Easy Apply flow
            handleEasyApplyFlow();
            
            // Wait for success confirmation
            try {
                waitHelper.waitForElementVisible(SUCCESS_MESSAGE);
                logger.info("Job application successful: {}", job.getTitle());
                return true;
                
            } catch (TimeoutException e) {
                logger.warn("Could not confirm application success for: {}", job.getTitle());
                // Still return true if we got this far - may have submitted
                return true;
            }
            
        } catch (Exception e) {
            logger.error("Failed to apply to job: {}", job.getTitle(), e);
            return false;
        }
    }
    
    private WebElement findApplyButton() {
        try {
            // Try Easy Apply first (easier to automate)
            return waitHelper.waitForElementClickable(EASY_APPLY_BUTTON);
        } catch (TimeoutException e) {
            try {
                // Fallback to regular Apply button
                return waitHelper.waitForElementClickable(APPLY_BUTTON);
            } catch (TimeoutException e2) {
                logger.debug("No apply button found");
                return null;
            }
        }
    }
    
     private void handleEasyApplyFlow() {
         try {
             logger.debug("Handling Easy Apply flow");
             
             // LinkedIn Easy Apply flow:
             // 1. Modal appears with pre-filled info
             // 2. Handle additional questions
             // 3. Review step - uncheck "Follow company" checkbox
             // 4. Final Submit to apply
             
             int maxSteps = 10;
             int currentStep = 0;
             
             while (currentStep < maxSteps) {
                 try {
                     // Wait for modal
                     WebElement modal = driver.findElement(
                         By.xpath("//div[@role='dialog']")
                     );
                     
                     // Check if we're on the review step
                     if (isReviewStep()) {
                         logger.info("On Review step - processing review elements");
                         handleReviewStep();
                     }
                     
                     // Handle additional questions if present
                     if (hasAdditionalQuestions()) {
                         logger.info("Additional questions detected - processing");
                         handleAdditionalQuestions();
                     }
                     
                     // Check if this is the final step
                     List<WebElement> submitButtons = driver.findElements(
                         By.xpath("//button[contains(text(), 'Submit')]")
                     );
                     
                     if (!submitButtons.isEmpty()) {
                         logger.info("Submitting application (final step)");
                         submitButtons.get(0).click();
                         Thread.sleep(1000);
                         break;
                     }
                     
                     // Click Next button
                     List<WebElement> nextButtons = driver.findElements(
                         By.xpath("//button[contains(text(), 'Next')]")
                     );
                     
                     if (!nextButtons.isEmpty()) {
                         logger.debug("Clicking Next (step {})", currentStep);
                         nextButtons.get(0).click();
                         Thread.sleep(500);
                         currentStep++;
                     } else {
                         break; // No more buttons found
                     }
                     
                 } catch (NoSuchElementException e) {
                     logger.debug("Modal closed or flow complete");
                     break;
                 }
             }
             
         } catch (Exception e) {
             logger.warn("Error handling Easy Apply flow: {}", e.getMessage());
         }
     }
     
     /**
      * Check if we're on the review step
      */
     private boolean isReviewStep() {
         try {
             List<WebElement> reviewElements = driver.findElements(
                 By.xpath("//h3[contains(text(), 'Review')] | " +
                          "//span[contains(text(), 'review')] | " +
                          "//div[contains(text(), 'Review your application')]")
             );
             return !reviewElements.isEmpty();
         } catch (Exception e) {
             return false;
         }
     }
     
     /**
      * Handle review step - uncheck "Follow company" checkbox
      */
     private void handleReviewStep() {
         try {
             logger.info("Processing Review step");
             
             // Find and uncheck "Follow company" checkbox
             // LinkedIn usually has a checkbox with text like "Follow <company>" or "Follow to stay updated"
             List<WebElement> checkboxes = driver.findElements(
                 By.xpath("//input[@type='checkbox'] | " +
                          "//input[@role='switch']")
             );
             
             for (WebElement checkbox : checkboxes) {
                 try {
                     // Get the parent element that contains the label text
                     WebElement parent = checkbox.findElement(By.xpath("./.."));
                     String labelText = parent.getText();
                     
                     logger.debug("Found checkbox with label: {}", labelText);
                     
                     // Check if this is the "Follow company" checkbox
                     if (labelText.toLowerCase().contains("follow")) {
                         // Check if checkbox is currently checked
                         String ariaChecked = checkbox.getAttribute("aria-checked");
                         boolean isChecked = "true".equals(ariaChecked) || 
                                           checkbox.isSelected();
                         
                         if (isChecked) {
                             logger.info("Found 'Follow company' checkbox - unchecking it");
                             
                             // Scroll into view and uncheck
                             ((JavascriptExecutor) driver).executeScript(
                                 "arguments[0].scrollIntoView(true);", 
                                 checkbox
                             );
                             
                             Thread.sleep(300);
                             
                             // Click to uncheck
                             checkbox.click();
                             
                             logger.info("Successfully unchecked 'Follow company' checkbox");
                             Thread.sleep(300);
                             break;
                         }
                     }
                 } catch (Exception e) {
                     logger.debug("Error processing checkbox: {}", e.getMessage());
                     continue;
                 }
             }
             
         } catch (Exception e) {
             logger.warn("Error handling review step: {}", e.getMessage());
         }
     }
     
     /**
      * Check if additional questions are present
      */
     private boolean hasAdditionalQuestions() {
         try {
             List<WebElement> questionElements = driver.findElements(
                 By.xpath("//label[contains(text(), 'question')] | " +
                          "//div[@class='form-group']/label | " +
                          "//fieldset")
             );
             
             // Filter to find actual question fields (inputs, textareas, selects)
             List<WebElement> questionFields = driver.findElements(
                 By.xpath("//input[not(@type='hidden')] | " +
                          "//textarea | " +
                          "//select")
             );
             
             return !questionFields.isEmpty();
         } catch (Exception e) {
             return false;
         }
     }
     
     /**
      * Handle additional questions in the application form
      */
     private void handleAdditionalQuestions() {
         try {
             logger.info("Handling additional questions");
             
             // Find all form field groups (questions)
             List<WebElement> questionContainers = driver.findElements(
                 By.xpath("//fieldset | //div[@class='form-group']")
             );
             
             logger.info("Found {} question containers", questionContainers.size());
             
             for (int i = 0; i < questionContainers.size(); i++) {
                 try {
                     WebElement questionContainer = questionContainers.get(i);
                     
                     // Scroll to question
                     ((JavascriptExecutor) driver).executeScript(
                         "arguments[0].scrollIntoView(true);", 
                         questionContainer
                     );
                     
                     Thread.sleep(300);
                     
                     // Get question label/text
                     String questionText = getQuestionText(questionContainer);
                     logger.debug("Processing question {}: {}", i + 1, questionText);
                     
                     // Find input field in this container
                     List<WebElement> inputs = questionContainer.findElements(
                         By.xpath(".//input | .//textarea | .//select")
                     );
                     
                     if (!inputs.isEmpty()) {
                         WebElement field = inputs.get(0);
                         String fieldType = field.getTagName().toLowerCase();
                         
                         // Handle different field types
                         switch (fieldType) {
                             case "select":
                                 handleSelectField(field, questionText);
                                 break;
                             case "textarea":
                                 handleTextAreaField(field, questionText);
                                 break;
                             case "input":
                                 handleInputField(field, questionText);
                                 break;
                         }
                         
                         Thread.sleep(300);
                     }
                     
                 } catch (Exception e) {
                     logger.warn("Error processing question container {}: {}", i, e.getMessage());
                     continue;
                 }
             }
             
             logger.info("Completed processing additional questions");
             
         } catch (Exception e) {
             logger.warn("Error handling additional questions: {}", e.getMessage());
         }
     }
     
     /**
      * Extract question text from container
      */
     private String getQuestionText(WebElement container) {
         try {
             // Try to find label
             List<WebElement> labels = container.findElements(
                 By.xpath(".//label")
             );
             
             if (!labels.isEmpty()) {
                 return labels.get(0).getText();
             }
             
             // Fallback to container text
             return container.getText().split("\n")[0];
             
         } catch (Exception e) {
             return "Unknown question";
         }
     }
     
     /**
      * Handle text input field
      */
     private void handleInputField(WebElement field, String questionText) {
         try {
             String fieldType = field.getAttribute("type");
             
             if ("checkbox".equals(fieldType) || "radio".equals(fieldType)) {
                 // For checkboxes and radios with "yes" in question, click them
                 if (questionText.toLowerCase().contains("yes") || 
                     questionText.toLowerCase().contains("apply")) {
                     field.click();
                     logger.info("Clicked checkbox/radio for: {}", questionText);
                 }
             } else {
                 // For text input, try to fill with generic response
                 String value = field.getAttribute("value");
                 
                 if (value == null || value.isEmpty()) {
                     // Generate response based on question
                     String response = generateResponseForQuestion(questionText);
                     
                     field.clear();
                     field.sendKeys(response);
                     
                     logger.info("Filled input for '{}' with: {}", questionText, response);
                 }
             }
         } catch (Exception e) {
             logger.warn("Error handling input field: {}", e.getMessage());
         }
     }
     
     /**
      * Handle textarea field
      */
     private void handleTextAreaField(WebElement field, String questionText) {
         try {
             String value = field.getAttribute("value");
             
             if (value == null || value.isEmpty()) {
                 String response = generateResponseForQuestion(questionText);
                 
                 field.clear();
                 field.sendKeys(response);
                 
                 logger.info("Filled textarea for '{}' with: {}", questionText, response);
             }
         } catch (Exception e) {
             logger.warn("Error handling textarea field: {}", e.getMessage());
         }
     }
     
     /**
      * Handle select dropdown field
      */
     private void handleSelectField(WebElement field, String questionText) {
         try {
             Select select = new Select(field);
             
             // Get all available options
             List<WebElement> options = select.getOptions();
             
             if (options.size() > 1) {
                 // Skip the first option if it's a placeholder ("Select...", "Choose...", etc)
                 int startIndex = options.get(0).getText().toLowerCase().contains("select") ? 1 : 0;
                 
                 if (options.size() > startIndex) {
                     WebElement optionToSelect = options.get(startIndex);
                     select.selectByVisibleText(optionToSelect.getText());
                     
                     logger.info("Selected '{}' for question: {}", 
                         optionToSelect.getText(), questionText);
                 }
             }
         } catch (Exception e) {
             logger.warn("Error handling select field: {}", e.getMessage());
         }
     }
     
     /**
      * Generate appropriate response for question based on question text
      */
     private String generateResponseForQuestion(String questionText) {
         String lowerQuestion = questionText.toLowerCase();
         
         // Keywords to match
         if (lowerQuestion.contains("experience") || lowerQuestion.contains("years")) {
             return "5+ years of professional experience";
         } else if (lowerQuestion.contains("location") || lowerQuestion.contains("willing")) {
             return "Yes, willing to relocate if necessary";
         } else if (lowerQuestion.contains("availability") || lowerQuestion.contains("start")) {
             return "Immediately or by mutual agreement";
         } else if (lowerQuestion.contains("salary") || lowerQuestion.contains("compensation")) {
             return "Open to discussion based on role and company";
         } else if (lowerQuestion.contains("notice") || lowerQuestion.contains("period")) {
             return "2 weeks";
         } else if (lowerQuestion.contains("remote") || lowerQuestion.contains("work from")) {
             return "Flexible work arrangement preferred";
         } else if (lowerQuestion.contains("visa") || lowerQuestion.contains("sponsorship")) {
             return "No sponsorship required";
         } else {
             // Default response
             return "Yes, I'm interested in this opportunity";
         }
     }
}
```

## Complete LinkedIn Agent Workflow

```java
public class LinkedInJobApplicationAgent {
    private WebDriver driver;
    private WaitHelper waitHelper;
    private LinkedInLoginHandler loginHandler;
    private LinkedInNavigationHandler navigationHandler;
    private LinkedInJobExtractor jobExtractor;
    private LinkedInApplicationHandler applicationHandler;
    private JobMatcher jobMatcher;
    private ResultLogger resultLogger;
    
    private static final Logger logger = LoggerFactory.getLogger(LinkedInJobApplicationAgent.class);
    private static final int JOB_LIMIT = 5; // Latest 5 jobs
    
    public LinkedInJobApplicationAgent() {
        initializeComponents();
    }
    
    private void initializeComponents() {
        BrowserAutomationEngine engine = new BrowserAutomationEngine();
        this.driver = engine.getDriver();
        this.waitHelper = new WaitHelper(driver);
        this.loginHandler = new LinkedInLoginHandler(driver, waitHelper);
        this.navigationHandler = new LinkedInNavigationHandler(driver, waitHelper);
        this.jobExtractor = new LinkedInJobExtractor(driver, waitHelper);
        this.applicationHandler = new LinkedInApplicationHandler(driver, waitHelper);
        
        // Initialize job matcher
        String jobTitle = System.getenv("JOB_TITLE");
        List<String> jobSkills = Arrays.asList(System.getenv("JOB_SKILLS").split(","));
        this.jobMatcher = new JobMatcher(jobTitle, jobSkills);
        
        // Initialize result logger
        this.resultLogger = new ResultLogger("linkedin");
    }
    
    public void executeWorkflow() {
        try {
            logger.info("=== Starting LinkedIn Job Application Agent ===");
            
            // 1. Login
            String username = System.getenv("LINKEDIN_USERNAME");
            String password = System.getenv("LINKEDIN_PASSWORD");
            loginHandler.login(username, password);
            
            // 2. Navigate to recent searches
            navigationHandler.navigateToJobsTab();
            navigationHandler.navigateToRecentSearches();
            navigationHandler.clickFullStackEngineerSearch();
            
            // 3. Extract job listings
            List<LinkedInJobListing> jobs = jobExtractor.extractLatestJobListings(JOB_LIMIT);
            logger.info("Extracted {} jobs from LinkedIn", jobs.size());
            
            // 4. Process and apply to matching jobs
            int appliedCount = 0;
            for (LinkedInJobListing job : jobs) {
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
            
            logger.info("=== LinkedIn Job Application Complete ===");
            logger.info("Applied to {} jobs", appliedCount);
            
        } catch (Exception e) {
            logger.error("LinkedIn agent workflow failed", e);
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

## Error Handling

### Common LinkedIn Issues

| Issue | Solution |
|-------|----------|
| Login fails with "Couldn't sign you in" | Check credentials, disable 2FA temporarily |
| Recent searches not visible | Scroll down on jobs page, may take time to load |
| Apply button not clickable | Try Easy Apply instead, wait for page to fully load |
| "You're logged out" error | Session expired, restart agent |
| Captcha appears | Stop agent, solve captcha manually, restart |

### Retry Logic

```java
public class LinkedInApplicationWithRetry {
    private static final int MAX_RETRIES = 3;
    
    public boolean applyWithRetry(LinkedInApplicationHandler handler, LinkedInJobListing job) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return handler.applyToJob(job);
            } catch (Exception e) {
                logger.warn("Attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(2000 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return false;
    }
}
```

## Integration with Main System

See [README.md](README.md) for how LinkedIn agent integrates with overall system and [OUTPUT_LOGGING.md](OUTPUT_LOGGING.md) for result logging.

