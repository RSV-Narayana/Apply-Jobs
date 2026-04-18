package com.example.applyjobs.automation;

import java.util.logging.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    
    // Persistent Chrome profile directory for session maintenance
    private static final String CHROME_PROFILE_DIR = System.getProperty("user.home") + "/.applyjobs-chrome-profile";

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
            logger.info("Initializing ChromeDriver with persistent session");

            // Create persistent Chrome profile directory if it doesn't exist
            Path profilePath = Paths.get(CHROME_PROFILE_DIR);
            if (!Files.exists(profilePath)) {
                Files.createDirectories(profilePath);
                logger.info("Created Chrome profile directory: {}", CHROME_PROFILE_DIR);
            } else {
                logger.info("Using existing Chrome profile directory: {}", CHROME_PROFILE_DIR);
            }

            // Suppress CDP version warnings
            java.util.logging.Logger.getLogger("org.openqa.selenium.devtools").setLevel(Level.OFF);
            java.util.logging.Logger.getLogger("org.openqa.selenium.chromium").setLevel(Level.OFF);

            // Auto-manage ChromeDriver version
            WebDriverManager.chromedriver().setup();

            // Configure Chrome options
            ChromeOptions options = new ChromeOptions();

            // IMPORTANT: Use persistent user data directory to maintain session
            // This keeps cookies, authentication tokens, and session data between runs
            options.addArguments("--user-data-dir=" + CHROME_PROFILE_DIR);

            // Add realistic user agent to avoid bot detection
            options.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36");

            // Optional: Run in headless mode (uncomment to enable)
            // options.addArguments("--headless");

            // Disable notifications and popups
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-popup-blocking");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-sync");

            // Set window size
            options.addArguments("--window-size=1920,1080");

            // Use NORMAL page load strategy to wait for full page load including React components
            // EAGER loads too early before JavaScript frameworks initialize
            options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.NORMAL);

            // Add experimental options to avoid detection
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.setExperimentalOption("useAutomationExtension", false);

            // Initialize WebDriver
            this.driver = new ChromeDriver(options);

            // Set implicit wait
            this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            // Set page load timeout to be more lenient
            this.driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

            // Set WebDriverWait
            this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            logger.info("ChromeDriver initialized successfully with persistent session");
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
                try {
                    driver.quit();
                    logger.info("WebDriver quit successfully");
                } catch (Exception e) {
                    logger.warn("WebDriver quit threw exception (may still be closing): {}", e.getMessage());
                }
                driver = null;
                instance = null;
                
                // macOS-specific: Forcefully kill Chrome processes to remove from Dock
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    logger.info("macOS detected: Force killing Chrome processes");
                    try {
                        // Give Chrome a moment to gracefully shut down
                        Thread.sleep(500);
                        
                        // Kill any remaining Chrome processes
                        Runtime.getRuntime().exec("pkill -9 -f 'Google Chrome'").waitFor();
                        Runtime.getRuntime().exec("pkill -9 -f 'Chromium'").waitFor();
                        Runtime.getRuntime().exec("pkill -9 'chrome'").waitFor();
                        
                        logger.info("Chrome processes terminated successfully");
                        
                        // Additional wait to ensure Dock is updated
                        Thread.sleep(500);
                    } catch (Exception e) {
                        logger.debug("Failed to force kill Chrome (may have already exited): {}", e.getMessage());
                    }
                }
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

