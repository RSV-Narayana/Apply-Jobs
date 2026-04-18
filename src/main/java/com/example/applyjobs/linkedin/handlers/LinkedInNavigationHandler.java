package com.example.applyjobs.linkedin.handlers;

import com.example.applyjobs.automation.WaitHelper;
import com.example.applyjobs.exception.NavigationException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LinkedInNavigationHandler {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInNavigationHandler.class);
    private WebDriver driver;
    private WaitHelper waitHelper;

    private static final String LINKEDIN_JOBS_URL = "https://www.linkedin.com/jobs";

    public LinkedInNavigationHandler(WebDriver driver) {
        this.driver = driver;
        this.waitHelper = new WaitHelper(driver);
    }

    /**
     * Navigate to LinkedIn jobs page
     */
    public void navigateToJobsPage() {
        try {
            logger.info("Navigating to LinkedIn jobs page");
            driver.get(LINKEDIN_JOBS_URL);
            waitHelper.waitForPageLoad();
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NavigationException("Navigation interrupted", e);
        } catch (Exception e) {
            logger.error("Failed to navigate to jobs page", e);
            throw new NavigationException("Failed to navigate to jobs page", e);
        }
    }

    /**
     * Click on a recent job search by job title
     */
    public void clickRecentSearch(String jobTitle) {
        try {


//            System.out.println(By.xpath("//a[.//span[contains(text(), 'full stack engineer')]"));

            logger.info("Looking for recent search: {}", jobTitle);

            jobTitle = jobTitle.toLowerCase();
            // Find all recent searches
            List<WebElement> recentSearches = driver.findElements(
                    By.xpath("//div[@componentkey='JobsHomeATFModule_RecentJobSearchesModule']//a[.//span[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '"+jobTitle+"')]]")
            );

            logger.info("Recent Search Xpath WebElement: {}", recentSearches);

//            if (recentSearches.isEmpty()) {
//                logger.warn("No recent search found for: {}, trying alternative selector", jobTitle);
//                // Try alternative selector
//                recentSearches = driver.findElements(
//                    By.xpath("//span[contains(text(), '" + jobTitle + "')]")
//                );
//            }

            if (!recentSearches.isEmpty()) {
                WebElement searchElement = recentSearches.get(0);
                logger.info("Found recent search, clicking it");

                // Scroll into view
                ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView(true);", searchElement);

                Thread.sleep(10000);
                searchElement.click();

                // Wait for results to load
                waitHelper.waitForPageLoad();
                Thread.sleep(2000);

                logger.info("Recent search clicked and results loaded");
            } else {
                logger.warn("Could not find recent search for: {}", jobTitle);
                throw new NavigationException("Recent search not found: " + jobTitle);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NavigationException("Navigation interrupted", e);
        } catch (NavigationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to click recent search", e);
            throw new NavigationException("Failed to click recent search", e);
        }
    }

    /**
     * Perform a new job search
     */
    public void performSearch(String jobTitle) {
        try {
            logger.info("Performing new search for: {}", jobTitle);

            // Find search input
            By searchInputLocator = By.xpath("//input[@placeholder='Search jobs' or contains(@placeholder, 'Search')]");
            WebElement searchInput = waitHelper.waitForElementVisible(searchInputLocator);

            searchInput.clear();
            searchInput.sendKeys(jobTitle);

            // Find and click search button
            By searchButtonLocator = By.xpath("//button[contains(text(), 'Search') or @type='submit']");
            WebElement searchButton = waitHelper.waitForElementClickable(searchButtonLocator);
            searchButton.click();

            // Wait for results
            waitHelper.waitForPageLoad();
            Thread.sleep(2000);

            logger.info("Search completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NavigationException("Search interrupted", e);
        } catch (Exception e) {
            logger.error("Failed to perform search", e);
            throw new NavigationException("Failed to perform search", e);
        }
    }
}

