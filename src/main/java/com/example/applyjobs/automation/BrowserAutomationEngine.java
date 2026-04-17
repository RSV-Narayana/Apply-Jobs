package com.example.applyjobs.automation;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class BrowserAutomationEngine {
    private static final Logger logger = LoggerFactory.getLogger(BrowserAutomationEngine.class);
    private static BrowserAutomationEngine instance;
    private WebDriver driver;
    private WebDriverWait wait;

    private BrowserAutomationEngine() {
        initializeChromeDriver();
    }

    public static synchronized BrowserAutomationEngine getInstance() {
        if (instance == null) {
            instance = new BrowserAutomationEngine();
        }
        return instance;
    }

    private void initializeChromeDriver() {
        try {
            logger.info("Initializing ChromeDriver");

            // Auto-manage ChromeDriver version
            WebDriverManager.chromedriver().setup();

            // Configure Chrome options
            ChromeOptions options = new ChromeOptions();

            // Optional: Run in headless mode (uncomment to enable)
            // options.addArguments("--headless");

            // Disable notifications and popups
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-popup-blocking");

            // Set window size
            options.addArguments("--window-size=1920,1080");

            // Disable image loading for faster performance
            options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);

            // Initialize WebDriver
            this.driver = new ChromeDriver(options);

            // Set implicit wait
            this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            // Set WebDriverWait
            this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            logger.info("ChromeDriver initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize ChromeDriver", e);
            throw new RuntimeException("Failed to initialize ChromeDriver", e);
        }
    }

    public WebDriver getDriver() {
        return driver;
    }

    public WebDriverWait getWait() {
        return wait;
    }

    public void closeDriver() {
        try {
            if (driver != null) {
                logger.info("Closing WebDriver");
                driver.quit();
                driver = null;
                instance = null;
            }
        } catch (Exception e) {
            logger.warn("Error closing WebDriver", e);
        }
    }

    public void navigateTo(String url) {
        try {
            logger.info("Navigating to: {}", url);
            driver.get(url);
        } catch (Exception e) {
            logger.error("Failed to navigate to {}", url, e);
            throw new RuntimeException("Navigation failed", e);
        }
    }
}

