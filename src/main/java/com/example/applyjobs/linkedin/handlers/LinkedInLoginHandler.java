package com.example.applyjobs.linkedin.handlers;

import com.example.applyjobs.automation.WaitHelper;
import com.example.applyjobs.exception.LoginException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedInLoginHandler {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInLoginHandler.class);
    private WebDriver driver;
    private WaitHelper waitHelper;

    private static final String LINKEDIN_LOGIN_URL = "https://www.linkedin.com/login";
    private static final By USERNAME_FIELD = By.id("username");
    private static final By PASSWORD_FIELD = By.id("password");
    private static final By LOGIN_BUTTON = By.xpath("//button[@type='submit']");
    private static final By FEED_INDICATOR = By.xpath("//a[contains(@href, '/feed/')]");

    public LinkedInLoginHandler(WebDriver driver) {
        this.driver = driver;
        this.waitHelper = new WaitHelper(driver);
    }

    /**
     * Perform LinkedIn login
     */
    public void login(String username, String password) {
        try {
            logger.info("Starting LinkedIn login");

            // Navigate to login page
            driver.get(LINKEDIN_LOGIN_URL);
            waitHelper.waitForPageLoad();

            logger.info("Entering username: {}", username);
            WebElement usernameField = waitHelper.waitForElementVisible(USERNAME_FIELD);
            usernameField.clear();
            usernameField.sendKeys(username);

            logger.info("Entering password");
            WebElement passwordField = waitHelper.waitForElementVisible(PASSWORD_FIELD);
            passwordField.clear();
            passwordField.sendKeys(password);

            logger.info("Clicking login button");
            WebElement loginButton = waitHelper.waitForElementClickable(LOGIN_BUTTON);
            loginButton.click();

            // Wait for page to load and check if login was successful
            waitHelper.waitForPageLoad();
            Thread.sleep(2000); // Additional wait for JavaScript to load

            // Check if we're on the feed (indicating successful login)
            try {
                waitHelper.waitForElementVisible(FEED_INDICATOR, 10);
                logger.info("Login successful - feed element found");
            } catch (Exception e) {
                logger.warn("Feed element not found, but continuing anyway");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Login interrupted", e);
            throw new LoginException("Login process was interrupted", e);
        } catch (Exception e) {
            logger.error("Login failed", e);
            throw new LoginException("Failed to login to LinkedIn", e);
        }
    }

    /**
     * Check if currently logged in
     */
    public boolean isLoggedIn() {
        try {
            return waitHelper.isElementPresent(FEED_INDICATOR);
        } catch (Exception e) {
            logger.warn("Could not determine login status", e);
            return false;
        }
    }
}

