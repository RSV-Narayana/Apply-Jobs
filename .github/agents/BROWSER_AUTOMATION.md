# Browser Automation Guide

## Overview

This document covers Chrome automation patterns used by the Job Application Agent for login, navigation, and job application workflows.

## Technology Choice

### Selenium WebDriver (Recommended)
**Pros:**
- Mature, widely-used library
- Excellent documentation and community support
- Strong Java integration
- Handles most modern web apps
- Good compatibility with job portals

**Cons:**
- Slower than Playwright
- Heavier resource usage

**Best for:** LinkedIn and Dice (proven compatibility)

### Playwright for Java (Alternative)
**Pros:**
- Faster performance
- Better modern browser support
- Better debugging capabilities
- Automatic browser updates

**Cons:**
- Less mature than Selenium
- Smaller community
- Less proven with job portals

**Recommendation:** Start with **Selenium WebDriver**

## Dependencies

Add to `pom.xml`:
```xml
<!-- Selenium WebDriver -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.15.0</version>
</dependency>

<!-- WebDriverManager (auto-manages ChromeDriver) -->
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.6.2</version>
</dependency>

<!-- Apache Commons Lang (utility functions) -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.13.0</version>
</dependency>

<!-- Logging -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.9</version>
</dependency>
```

## Core Concepts

### 1. WebDriver Initialization

```java
public class BrowserAutomationEngine {
    private WebDriver driver;
    private static final Logger logger = LoggerFactory.getLogger(BrowserAutomationEngine.class);
    
    public BrowserAutomationEngine() {
        initializeChromeDriver();
    }
    
    private void initializeChromeDriver() {
        // Auto-manage ChromeDriver version
        WebDriverManager.chromedriver().setup();
        
        // Configure Chrome options
        ChromeOptions options = new ChromeOptions();
        
        // Optional: Run in headless mode (no GUI)
        // options.addArguments("--headless");
        
        // Disable notifications and popups
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        
        // Set window size
        options.addArguments("--window-size=1920,1080");
        
        // Disable image loading (faster)
        // options.addArguments("--blink-settings=imagesEnabled=false");
        
        // User data directory (for session persistence)
        // options.addArguments("user-data-dir=/tmp/chrome-profile");
        
        driver = new ChromeDriver(options);
        logger.info("Chrome WebDriver initialized");
    }
    
    public void close() {
        if (driver != null) {
            driver.quit();
            logger.info("Chrome WebDriver closed");
        }
    }
}
```

### 2. Wait Strategies

Use **Explicit Waits** instead of implicit waits (more reliable):

```java
public class WaitHelper {
    private WebDriver driver;
    private static final int DEFAULT_TIMEOUT = 10;
    
    public WaitHelper(WebDriver driver) {
        this.driver = driver;
    }
    
    // Wait for element to be present in DOM
    public WebElement waitForElement(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }
    
    // Wait for element to be visible
    public WebElement waitForElementVisible(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    
    // Wait for element to be clickable
    public WebElement waitForElementClickable(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }
    
    // Wait for element to disappear (loading spinner)
    public void waitForElementInvisible(By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }
    
    // Wait for page load
    public void waitForPageLoad() {
        new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
            .until(driver -> 
                ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState")
                    .equals("complete")
            );
    }
    
    // Custom wait with condition
    public void waitFor(Function<WebDriver, Boolean> condition) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        wait.until(condition);
    }
}
```

### 3. Login Flow Pattern

```java
public class LoginHandler {
    private WebDriver driver;
    private WaitHelper waitHelper;
    
    public LoginHandler(WebDriver driver) {
        this.driver = driver;
        this.waitHelper = new WaitHelper(driver);
    }
    
    public void loginToLinkedIn(String username, String password) {
        try {
            // Navigate to LinkedIn login
            driver.get("https://www.linkedin.com/login");
            
            // Wait for page to load
            waitHelper.waitForPageLoad();
            
            // Enter username
            WebElement usernameField = waitHelper.waitForElementVisible(
                By.id("username")
            );
            usernameField.clear();
            usernameField.sendKeys(username);
            
            // Enter password
            WebElement passwordField = waitHelper.waitForElementVisible(
                By.id("password")
            );
            passwordField.clear();
            passwordField.sendKeys(password);
            
            // Click login button
            WebElement loginButton = waitHelper.waitForElementClickable(
                By.xpath("//button[@type='submit']")
            );
            loginButton.click();
            
            // Wait for dashboard to load (sign that login was successful)
            waitHelper.waitForElementVisible(
                By.xpath("//a[contains(@href, '/feed')]")
            );
            
            logger.info("LinkedIn login successful");
            
        } catch (TimeoutException e) {
            throw new LoginException("LinkedIn login timeout - check credentials", e);
        }
    }
}

public class LoginException extends RuntimeException {
    public LoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 4. Dynamic Content Handling

```java
public class DynamicContentHandler {
    private WebDriver driver;
    
    public DynamicContentHandler(WebDriver driver) {
        this.driver = driver;
    }
    
    // Scroll to load more elements (infinite scroll)
    public void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript(
            "window.scrollTo(0, document.body.scrollHeight);"
        );
        Thread.sleep(1000); // Wait for content to load
    }
    
    // Scroll to element
    public void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView(true);",
            element
        );
    }
    
    // Wait for AJAX requests to complete
    public void waitForAjaxComplete() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(driver -> 
            (Long) ((JavascriptExecutor) driver)
                .executeScript("return jQuery.active == 0") == 0
        );
    }
    
    // Execute JavaScript
    public Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }
}
```

### 5. Error Handling & Recovery

```java
public class BrowserErrorHandler {
    private WebDriver driver;
    private static final int MAX_RETRIES = 3;
    
    public BrowserErrorHandler(WebDriver driver) {
        this.driver = driver;
    }
    
    // Retry logic for flaky actions
    public void executeWithRetry(Runnable action) {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                action.run();
                return;
            } catch (StaleElementReferenceException e) {
                retries++;
                if (retries >= MAX_RETRIES) throw e;
                Thread.sleep(1000);
            } catch (TimeoutException e) {
                retries++;
                if (retries >= MAX_RETRIES) throw e;
                Thread.sleep(1000);
            }
        }
    }
    
    // Handle alert dialogs
    public void handleAlert(String expectedText) {
        try {
            Alert alert = driver.switchTo().alert();
            if (alert.getText().contains(expectedText)) {
                alert.accept();
            }
        } catch (NoAlertPresentException e) {
            // Alert not present, continue
        }
    }
    
    // Switch to iframe
    public void switchToFrame(By frameLocator) {
        driver.switchTo().frame(driver.findElement(frameLocator));
    }
    
    // Switch back from iframe
    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }
    
    // Handle authentication prompts
    public void handleBasicAuth(String username, String password, String url) {
        // URL format: https://username:password@domain.com
        String authUrl = url.replace("://", "://" + username + ":" + password + "@");
        driver.get(authUrl);
    }
}
```

## Action Patterns

### Finding Elements Reliably

```java
public class ElementFinder {
    private WebDriver driver;
    private WaitHelper waitHelper;
    
    public ElementFinder(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    // Find element with multiple strategies
    public WebElement findElement(By... locators) {
        for (By locator : locators) {
            try {
                return waitHelper.waitForElementVisible(locator);
            } catch (TimeoutException e) {
                // Try next locator
            }
        }
        throw new NoSuchElementException("Element not found with any strategy");
    }
    
    // Find elements by text
    public WebElement findByText(String text) {
        return waitHelper.waitForElementVisible(
            By.xpath("//*[contains(text(), '" + text + "')]")
        );
    }
    
    // Find button by text
    public WebElement findButton(String buttonText) {
        return waitHelper.waitForElementVisible(
            By.xpath("//button[contains(text(), '" + buttonText + "')] | " +
                     "//a[@role='button'][contains(text(), '" + buttonText + "')]")
        );
    }
}
```

### Interacting with Elements

```java
public class ElementInteraction {
    private WebDriver driver;
    private WaitHelper waitHelper;
    
    public ElementInteraction(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    // Type text slowly (more reliable than sendKeys)
    public void typeText(By locator, String text) {
        WebElement element = waitHelper.waitForElementClickable(locator);
        element.clear();
        for (char c : text.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            try { Thread.sleep(50); } catch (InterruptedException e) { }
        }
    }
    
    // Click element with wait
    public void clickElement(By locator) {
        WebElement element = waitHelper.waitForElementClickable(locator);
        // Scroll to element if needed
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        element.click();
    }
    
    // Click using JavaScript (for stubborn elements)
    public void clickElementJS(By locator) {
        WebElement element = waitHelper.waitForElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }
    
    // Select from dropdown
    public void selectDropdown(By locator, String value) {
        Select dropdown = new Select(waitHelper.waitForElementVisible(locator));
        dropdown.selectByValue(value);
    }
    
    // Select dropdown by text
    public void selectDropdownByText(By locator, String text) {
        Select dropdown = new Select(waitHelper.waitForElementVisible(locator));
        dropdown.selectByVisibleText(text);
    }
}
```

## Best Practices

### 1. **Always Use Explicit Waits**
```java
// ❌ Bad
Thread.sleep(5000);

// ✅ Good
waitHelper.waitForElementVisible(By.id("element"));
```

### 2. **Catch Specific Exceptions**
```java
// ❌ Bad
try {
    // code
} catch (Exception e) { }

// ✅ Good
try {
    // code
} catch (TimeoutException e) {
    logger.error("Element not found within timeout");
} catch (StaleElementReferenceException e) {
    logger.error("Element is stale, retrying...");
}
```

### 3. **Use Page Object Model (Optional but Recommended)**
```java
public class LinkedInJobsPage {
    private WebDriver driver;
    private WaitHelper waitHelper;
    
    // Locators
    private By jobsTabLocator = By.xpath("//a[contains(@href, '/jobs')]");
    private By jobListingLocator = By.xpath("//li[@data-job-id]");
    private By applyButtonLocator = By.xpath("//button[contains(text(), 'Apply')]");
    
    public LinkedInJobsPage(WebDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }
    
    public void clickJobsTab() {
        waitHelper.waitForElementClickable(jobsTabLocator).click();
    }
    
    public List<WebElement> getJobListings() {
        return driver.findElements(jobListingLocator);
    }
    
    public void clickApplyButton() {
        waitHelper.waitForElementClickable(applyButtonLocator).click();
    }
}
```

### 4. **Resource Management**
```java
public class AgentRunner {
    public static void main(String[] args) {
        BrowserAutomationEngine engine = new BrowserAutomationEngine();
        try {
            // Use engine
            engine.performLinkedInApplications();
        } finally {
            // Always close, even if exception occurs
            engine.close();
        }
    }
}
```

### 5. **Logging & Debugging**
```java
public class BrowserLogger {
    private WebDriver driver;
    private static final Logger logger = LoggerFactory.getLogger(BrowserLogger.class);
    
    // Take screenshot on error
    public void takeScreenshot(String filename) {
        TakesScreenshot screenshot = (TakesScreenshot) driver;
        File source = screenshot.getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(source, new File("screenshots/" + filename + ".png"));
            logger.info("Screenshot saved: " + filename);
        } catch (IOException e) {
            logger.error("Failed to save screenshot", e);
        }
    }
    
    // Log page source
    public void logPageSource(String filename) {
        try {
            FileUtils.writeStringToFile(
                new File("logs/" + filename + ".html"),
                driver.getPageSource(),
                "UTF-8"
            );
        } catch (IOException e) {
            logger.error("Failed to log page source", e);
        }
    }
}
```

## Common Patterns

### Pattern: Login → Navigate → Extract Data → Apply → Repeat
```java
public class JobApplicationWorkflow {
    private BrowserAutomationEngine engine;
    
    public void executeWorkflow() {
        engine.openChrome();
        engine.login("username", "password");
        List<JobListing> jobs = engine.extractJobListings();
        
        for (JobListing job : jobs) {
            if (jobMatcher.isMatch(job)) {
                engine.applyToJob(job);
            }
        }
        
        engine.close();
    }
}
```

### Pattern: Wait for Element → Interact → Wait for Result
```java
public void applyToJob(JobListing job) {
    // 1. Wait for apply button to be clickable
    WebElement applyButton = waitHelper.waitForElementClickable(job.getApplyButtonLocator());
    
    // 2. Scroll into view and click
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", applyButton);
    applyButton.click();
    
    // 3. Wait for modal/form to appear
    waitHelper.waitForElementVisible(By.xpath("//div[@role='dialog']"));
    
    // 4. Fill form (if needed)
    // ...
    
    // 5. Submit
    waitHelper.waitForElementClickable(By.xpath("//button[text()='Submit']")).click();
    
    // 6. Wait for confirmation
    waitHelper.waitForElementVisible(By.xpath("//span[contains(text(), 'Applied')]"));
}
```

## Troubleshooting

### TimeoutException
- **Cause**: Element not found within timeout period
- **Solution**: Check if element locator is correct, increase timeout, check if page has loaded

### StaleElementReferenceException
- **Cause**: Element reference is no longer valid (DOM changed)
- **Solution**: Re-find element after action, use retry logic

### NoSuchElementException
- **Cause**: Element doesn't exist on page
- **Solution**: Check locator, verify page content, add waits

### Session Lost
- **Cause**: Browser crashed or connection lost
- **Solution**: Implement retry logic, restart WebDriver

See [LINKEDIN_AGENT.md](LINKEDIN_AGENT.md) and [DICE_AGENT.md](DICE_AGENT.md) for portal-specific patterns.

