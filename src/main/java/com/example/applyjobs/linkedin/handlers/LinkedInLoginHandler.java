package com.example.applyjobs.linkedin.handlers;

import com.example.applyjobs.automation.WaitHelper;
import com.example.applyjobs.exception.LoginException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class LinkedInLoginHandler {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInLoginHandler.class);
    private WebDriver driver;
    private WaitHelper waitHelper;

    private static final String LINKEDIN_LOGIN_URL = "https://www.linkedin.com/login";
    private static final By FEED_INDICATOR = By.xpath("//a[contains(@href, '/feed/')]");

    public LinkedInLoginHandler(WebDriver driver) {
        this.driver = driver;
        this.waitHelper = new WaitHelper(driver);
    }

    /**
     * Perform LinkedIn login with improved selectors and fallback mechanisms
     */
    public void login(String username, String password) {
        try {
            logger.info("Starting LinkedIn login process");

            // Navigate to LinkedIn feed first - this will test if session is valid
            // If session cookies are valid, we stay on feed page
            // If not, LinkedIn will redirect to login page
            logger.info("Navigating to LinkedIn feed to check session status...");
            driver.get("https://www.linkedin.com/feed/");
            Thread.sleep(3000);
            waitHelper.waitForPageLoad();
            Thread.sleep(2000);
            
            // Check if we're already logged in (persistent session worked)
            logger.info("Checking if already logged in with persistent session...");
            if (isLoggedIn()) {
                logger.info("Success! Session is valid - already logged in to LinkedIn");
                return;
            }

            // If we reach here, we need to login manually
            logger.info("Session not valid - need to enter credentials");
            
            // Wait for login form to fully render
            logger.info("Waiting for login form to fully load...");
            Thread.sleep(2000);
            
            // Wait for input fields
            try {
                WebDriverWait explicitWait = new WebDriverWait(driver, Duration.ofSeconds(20));
                explicitWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input")));
                logger.info("Input fields detected on page");
                Thread.sleep(2000);
            } catch (Exception e) {
                logger.warn("Timed out waiting for input fields to appear: {}", e.getMessage());
                inspectPageStructure();
                throw new LoginException("Login page inputs did not load after 20 seconds", e);
            }

            // Try to find and fill username field
            logger.info("Attempting to locate and fill username field");
            logger.info("username: {}", username);
            WebElement usernameField = findUsernameField();
            if (usernameField != null) {
                usernameField.clear();
                usernameField.sendKeys(username);
                logger.info("Username entered successfully");
            } else {
                logger.error("Could not find username field");
                inspectPageStructure();
                throw new LoginException("Username field not found on login page");
            }

            // Wait between fields
            Thread.sleep(1000);

            // Try to find and fill password field
            logger.info("Attempting to locate and fill password field");
            logger.info("password: {}", password);
            WebElement passwordField = findPasswordField();
            if (passwordField != null) {
                passwordField.clear();
                passwordField.sendKeys(password);
                logger.info("Password entered successfully");
            } else {
                logger.error("Could not find password field");
                throw new LoginException("Password field not found on login page");
            }

            // Wait before clicking login
            Thread.sleep(1000);

            // Find and click login button
            logger.info("Attempting to click login button");
            WebElement loginButton = findLoginButton();
            if (loginButton != null) {
                // Scroll into view first
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", loginButton);
                Thread.sleep(500);
                loginButton.click();
                logger.info("Login button clicked");
            } else {
                logger.error("Could not find login button");
                throw new LoginException("Login button not found on login page");
            }

            // Wait for page to load after login
            logger.info("Waiting for page to load after login submission");
            waitHelper.waitForPageLoad();
            Thread.sleep(3000);

            // Check if we're on the feed (indicating successful login)
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
                wait.until(ExpectedConditions.visibilityOfElementLocated(FEED_INDICATOR));
                logger.info("Login successful - feed element found");
            } catch (Exception e) {
                logger.warn("Feed element not found immediately, checking if login succeeded anyway");
                Thread.sleep(5000);
                if (!waitHelper.isElementPresent(FEED_INDICATOR)) {
                    logger.error("Could not verify login success");
                    throw new LoginException("Failed to verify login success");
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Login interrupted", e);
            throw new LoginException("Login process was interrupted", e);
        } catch (Exception e) {
            logger.error("Login failed", e);
            throw new LoginException("Failed to login to LinkedIn: " + e.getMessage(), e);
        }
    }

    /**
     * Find username field with multiple selector strategies
     * LinkedIn uses dynamic IDs like :r0:, so use position-based selection
     */
    private WebElement findUsernameField() {
        List<By> selectors = new ArrayList<>();
        // Primary: First text input on the page (position-based for dynamic IDs)
        selectors.add(By.xpath("(//input[@type='text'])[1]"));
        // Fallback: Try by ID if LinkedIn ever uses static IDs
        selectors.add(By.id("username"));
        selectors.add(By.name("session_key"));
        selectors.add(By.xpath("//input[@type='email']"));
        selectors.add(By.xpath("//input[@autocomplete='username']"));

        for (By selector : selectors) {
            try {
                logger.debug("Trying username selector: {}", selector);
                List<WebElement> elements = driver.findElements(selector);
                if (!elements.isEmpty()) {
                    WebElement element = elements.get(0);
                    logger.info("Found username field with selector: {} (id={}, type={})", 
                        selector, element.getAttribute("id"), element.getAttribute("type"));
                    return element;
                }
            } catch (Exception e) {
                logger.debug("Username selector failed: {} - {}", selector, e.getMessage());
            }
        }

        logger.warn("Could not find username field with any selector");
        return null;
    }

    /**
     * Find password field with multiple selector strategies
     * LinkedIn uses dynamic IDs like :r1:, so use position-based selection
     */
    private WebElement findPasswordField() {
        List<By> selectors = new ArrayList<>();
        // Primary: First password input on the page (position-based for dynamic IDs)
        selectors.add(By.xpath("(//input[@type='password'])[1]"));
        // Fallback: Try by ID if LinkedIn ever uses static IDs
        selectors.add(By.id("password"));
        selectors.add(By.name("session_password"));
        selectors.add(By.xpath("//input[@autocomplete='current-password']"));

        for (By selector : selectors) {
            try {
                logger.debug("Trying password selector: {}", selector);
                List<WebElement> elements = driver.findElements(selector);
                if (!elements.isEmpty()) {
                    WebElement element = elements.get(0);
                    logger.info("Found password field with selector: {} (id={}, type={})", 
                        selector, element.getAttribute("id"), element.getAttribute("type"));
                    return element;
                }
            } catch (Exception e) {
                logger.debug("Password selector failed: {} - {}", selector, e.getMessage());
            }
        }

        logger.warn("Could not find password field with any selector");
        return null;
    }

    /**
     * Find login button with multiple selector strategies
     */
    private WebElement findLoginButton() {
        List<By> selectors = new ArrayList<>();
        selectors.add(By.xpath("//button[contains(., 'Sign in') and not(contains(., 'Apple'))]"));
        selectors.add(By.xpath("//button[@type='submit']"));
        selectors.add(By.xpath("//button[contains(@aria-label, 'Sign in')]"));

        for (By selector : selectors) {
            try {
                logger.debug("Trying login button selector: {}", selector);
                List<WebElement> elements = driver.findElements(selector);
                if (!elements.isEmpty()) {
                    WebElement element = elements.get(0);
                    logger.info("Found login button with selector: {} (text={})", selector, element.getText());
                    return element;
                }
            } catch (Exception e) {
                logger.debug("Login button selector failed: {} - {}", selector, e.getMessage());
            }
        }

        logger.warn("Could not find login button with any selector");
        return null;
    }

    /**
     * Check if currently logged in by verifying URL and page content
     */
    public boolean isLoggedIn() {
        try {
            String currentUrl = driver.getCurrentUrl();
            String pageTitle = driver.getTitle();
            
            logger.debug("Checking login status - URL: {}, Title: {}", currentUrl, pageTitle);
            
            // If on feed page, we're logged in
            if (currentUrl.contains("/feed") || currentUrl.contains("/home")) {
                logger.info("Already logged in - detected feed/home page");
                return true;
            }
            
            // Fallback: check for feed indicator element
            if (waitHelper.isElementPresent(FEED_INDICATOR)) {
                logger.info("Already logged in - feed indicator element found");
                return true;
            }
            
            logger.info("Not logged in - on login page or unknown page");
            return false;
        } catch (Exception e) {
            logger.warn("Could not determine login status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Inspect and log the page structure for debugging
     */
    private void inspectPageStructure() {
        try {
            logger.warn("=== LinkedIn Login Page Structure Debug ===");

            // Log page title
            logger.warn("Page title: {}", driver.getTitle());
            logger.warn("Current URL: {}", driver.getCurrentUrl());

            // Log all input fields
            List<WebElement> inputs = driver.findElements(By.xpath("//input"));
            logger.warn("Total input fields found: {}", inputs.size());
            for (int i = 0; i < inputs.size() && i < 5; i++) {
                WebElement input = inputs.get(i);
                logger.warn("Input {}: id='{}', name='{}', type='{}', placeholder='{}'",
                    i,
                    input.getAttribute("id"),
                    input.getAttribute("name"),
                    input.getAttribute("type"),
                    input.getAttribute("placeholder"));
            }

            // Log all buttons
            List<WebElement> buttons = driver.findElements(By.xpath("//button"));
            logger.warn("Total button fields found: {}", buttons.size());
            for (int i = 0; i < buttons.size() && i < 5; i++) {
                WebElement button = buttons.get(i);
                logger.warn("Button {}: text='{}', type='{}'",
                    i,
                    button.getText(),
                    button.getAttribute("type"));
            }

            // Log page source length
            String pageSource = driver.getPageSource();
            logger.warn("Page source length: {} characters", pageSource.length());

            logger.warn("=== End Debug Info ===");
        } catch (Exception e) {
            logger.warn("Error during page structure inspection", e);
        }
    }
}

