package com.example.applyjobs.automation;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class WaitHelper {
    private static final Logger logger = LoggerFactory.getLogger(WaitHelper.class);
    private WebDriver driver;
    private WebDriverWait wait;

    public WaitHelper(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    public WaitHelper(WebDriver driver, int timeoutSeconds) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    /**
     * Wait for element to be visible and return it
     */
    public WebElement waitForElementVisible(By locator) {
        try {
            logger.debug("Waiting for element to be visible: {}", locator);
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (Exception e) {
            logger.warn("Element not found or not visible: {}", locator);
            throw e;
        }
    }

    /**
     * Wait for element to be clickable and return it
     */
    public WebElement waitForElementClickable(By locator) {
        try {
            logger.debug("Waiting for element to be clickable: {}", locator);
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (Exception e) {
            logger.warn("Element not found or not clickable: {}", locator);
            throw e;
        }
    }

    /**
     * Wait for element to be present in DOM
     */
    public WebElement waitForElementPresent(By locator) {
        try {
            logger.debug("Waiting for element to be present: {}", locator);
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (Exception e) {
            logger.warn("Element not found: {}", locator);
            throw e;
        }
    }

    /**
     * Wait for page to load
     */
    public void waitForPageLoad() {
        try {
            logger.debug("Waiting for page to load");
            wait.until(driver -> {
                return ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("return document.readyState").equals("complete");
            });
        } catch (Exception e) {
            logger.warn("Page load wait timed out", e);
        }
    }

    /**
     * Wait for element to be invisible/disappear
     */
    public void waitForElementInvisible(By locator) {
        try {
            logger.debug("Waiting for element to be invisible: {}", locator);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (Exception e) {
            logger.warn("Element still visible or not found: {}", locator);
        }
    }

    /**
     * Check if element exists (non-blocking)
     */
    public boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait for element to be visible with custom timeout
     */
    public WebElement waitForElementVisible(By locator, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        try {
            logger.debug("Waiting for element (custom timeout {}s): {}", timeoutSeconds, locator);
            return customWait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (Exception e) {
            logger.warn("Element not found with custom timeout: {}", locator);
            throw e;
        }
    }

    /**
     * Wait for element to be clickable with custom timeout
     */
    public WebElement waitForElementClickable(By locator, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        try {
            logger.debug("Waiting for element to be clickable (custom timeout {}s): {}", timeoutSeconds, locator);
            return customWait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (Exception e) {
            logger.warn("Element not clickable with custom timeout: {}", locator);
            throw e;
        }
    }
}


