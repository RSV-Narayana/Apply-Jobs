package com.example.applyjobs.linkedin.handlers;

import com.example.applyjobs.automation.WaitHelper;
import com.example.applyjobs.exception.JobExtractionException;
import com.example.applyjobs.model.LinkedInJobListing;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LinkedInJobExtractor {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInJobExtractor.class);
    private WebDriver driver;
    private WaitHelper waitHelper;

    public LinkedInJobExtractor(WebDriver driver) {
        this.driver = driver;
        this.waitHelper = new WaitHelper(driver);
    }

    /**
     * Extract latest job listings from current page
     */
    public List<LinkedInJobListing> extractLatestJobListings(int limit) {
        try {
            logger.info("Extracting latest {} job listings", limit);

            List<LinkedInJobListing> jobs = new ArrayList<>();

            // Find all job listing elements
            By jobListingLocator = By.xpath("//div[contains(@class, 'base-card')]");
            List<WebElement> jobElements = driver.findElements(jobListingLocator);

            logger.info("Found {} job listings on page", jobElements.size());

            // Extract up to 'limit' jobs
            for (int i = 0; i < Math.min(limit, jobElements.size()); i++) {
                try {
                    WebElement jobElement = jobElements.get(i);

                    // Scroll into view
                    ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("arguments[0].scrollIntoView(true);", jobElement);

                    Thread.sleep(500);

                    LinkedInJobListing job = extractJobDetails(jobElement);
                    if (job != null) {
                        jobs.add(job);
                        logger.info("Extracted job {}: {}", i + 1, job.getTitle());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to extract job at index {}", i, e);
                    // Continue with next job
                }
            }

            logger.info("Successfully extracted {} jobs", jobs.size());
            return jobs;

        } catch (Exception e) {
            logger.error("Failed to extract job listings", e);
            throw new JobExtractionException("Failed to extract job listings", e);
        }
    }

    /**
     * Extract details from a single job element
     */
    private LinkedInJobListing extractJobDetails(WebElement jobElement) {
        try {
            // Extract job ID from data attribute or URL
            String jobId = jobElement.getAttribute("data-job-id");
            if (jobId == null || jobId.isEmpty()) {
                // Try to extract from link href
                try {
                    WebElement link = jobElement.findElement(By.xpath(".//a[contains(@href, '/jobs/view/')]"));
                    String href = link.getAttribute("href");
                    jobId = href.replaceAll(".*/(\\d+).*", "$1");
                } catch (Exception e) {
                    logger.debug("Could not extract job ID");
                    jobId = String.valueOf(System.currentTimeMillis());
                }
            }

            // Extract title
            String title = "";
            try {
                title = jobElement.findElement(By.xpath(".//span[@aria-hidden='true']"))
                    .getText();
            } catch (Exception e) {
                logger.debug("Could not extract title");
            }

            // Extract company name
            String company = "";
            try {
                company = jobElement.findElement(By.xpath(".//a[@data-tracking-control-name='public_jobs_company-name-link']"))
                    .getText();
            } catch (Exception e) {
                logger.debug("Could not extract company");
            }

            // Extract location
            String location = "";
            try {
                location = jobElement.findElement(By.xpath(".//span[contains(text(), 'in ')]"))
                    .getText();
            } catch (Exception e) {
                logger.debug("Could not extract location");
            }

            // Extract description/summary
            String description = "";
            try {
                description = jobElement.findElement(By.xpath(".//p[@class='base-serp-card__subtitle']"))
                    .getText();
            } catch (Exception e) {
                logger.debug("Could not extract description");
            }

            if (title.isEmpty()) {
                logger.warn("Could not extract job title, skipping");
                return null;
            }

            return new LinkedInJobListing(jobId, title, company, location, description);

        } catch (Exception e) {
            logger.warn("Failed to extract job details", e);
            return null;
        }
    }
}

