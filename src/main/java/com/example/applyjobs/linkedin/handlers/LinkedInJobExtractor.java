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

            // Try multiple XPath selectors for robustness (LinkedIn UI changes frequently)
            List<WebElement> jobElements = tryFindJobElements();

            if (jobElements.isEmpty()) {
                logger.warn("No job elements found with standard selectors, trying fallback methods");
                jobElements = tryFallbackJobElements();
            }

            logger.info("Found {} job listings on page", jobElements.size());

            // Extract up to 'limit' jobs
            for (int i = 0; i < Math.min(limit, jobElements.size()); i++) {
                try {
                    WebElement jobElement = jobElements.get(i);

                    // Scroll into view
                    ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("arguments[0].scrollIntoView(true);", jobElement);

                    Thread.sleep(5000);

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
            logger.debug("Extracting job details from element");

            // Extract job ID (try multiple attributes)
            String jobId = extractJobId(jobElement);

            // Extract title
            String title = extractJobTitle(jobElement);

            // Extract company name
            String company = extractCompanyName(jobElement);

            // Extract location
            String location = extractLocation(jobElement);

            // Extract description/summary
            String description = extractDescription(jobElement);

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

    /**
     * Extract job ID using multiple strategies
     */
    private String extractJobId(WebElement jobElement) {
        try {
            // Strategy 1: data-occludable-job-id attribute
            String jobId = jobElement.getAttribute("data-occludable-job-id");
            if (jobId != null && !jobId.isEmpty()) {
                logger.debug("Found job ID via data-occludable-job-id: {}", jobId);
                return jobId;
            }

            // Strategy 2: data-job-id attribute
            jobId = jobElement.getAttribute("data-job-id");
            if (jobId != null && !jobId.isEmpty()) {
                logger.debug("Found job ID via data-job-id: {}", jobId);
                return jobId;
            }

            // Strategy 3: Extract from href
            try {
                WebElement link = jobElement.findElement(By.xpath(".//a[contains(@href, '/jobs/view/')]"));
                String href = link.getAttribute("href");
                jobId = href.replaceAll(".*/(\\d+).*", "$1");
                if (!jobId.isEmpty() && jobId.matches("\\d+")) {
                    logger.debug("Found job ID from href: {}", jobId);
                    return jobId;
                }
            } catch (Exception e) {
                logger.debug("Could not extract from href");
            }

            // Fallback: Use timestamp as unique ID
            jobId = String.valueOf(System.currentTimeMillis());
            logger.debug("Using timestamp as job ID: {}", jobId);
            return jobId;
        } catch (Exception e) {
            logger.debug("Error extracting job ID", e);
            return String.valueOf(System.currentTimeMillis());
        }
    }

    /**
     * Extract job title using multiple selectors
     */
    private String extractJobTitle(WebElement jobElement) {
        String[] titleXpaths = {
            ".//span[@aria-hidden='true']",
            ".//h3",
            ".//h2",
            ".//a[contains(@href, '/jobs/view/')]/span",
            ".//div[contains(@class, 'title')]",
            ".//span[contains(@class, 'title')]"
        };

        for (String xpath : titleXpaths) {
            try {
                String title = jobElement.findElement(By.xpath(xpath)).getText();
                if (!title.isEmpty()) {
                    logger.debug("Found title: {}", title);
                    return title;
                }
            } catch (Exception e) {
                logger.debug("Title xpath failed: {}", xpath);
            }
        }

        logger.debug("Could not extract title");
        return "";
    }

    /**
     * Extract company name using multiple selectors
     */
    private String extractCompanyName(WebElement jobElement) {
        String[] companyXpaths = {
            ".//a[@data-tracking-control-name='public_jobs_company-name-link']",
            ".//span[contains(@class, 'company-name')]",
            ".//div[contains(@class, 'company')]//span",
            ".//a[contains(@href, '/company/')]",
            ".//span[contains(text(), 'Company')]/.."
        };

        for (String xpath : companyXpaths) {
            try {
                String company = jobElement.findElement(By.xpath(xpath)).getText();
                if (!company.isEmpty()) {
                    logger.debug("Found company: {}", company);
                    return company;
                }
            } catch (Exception e) {
                logger.debug("Company xpath failed: {}", xpath);
            }
        }

        logger.debug("Could not extract company");
        return "";
    }

    /**
     * Extract location using multiple selectors
     */
    private String extractLocation(WebElement jobElement) {
        String[] locationXpaths = {
            ".//span[contains(text(), 'in ')]",
            ".//span[contains(@class, 'location')]",
            ".//div[contains(@class, 'location')]",
            ".//span[contains(text(), 'Remote')] | .//span[contains(text(), 'Hybrid')]"
        };

        for (String xpath : locationXpaths) {
            try {
                String location = jobElement.findElement(By.xpath(xpath)).getText();
                if (!location.isEmpty()) {
                    logger.debug("Found location: {}", location);
                    return location;
                }
            } catch (Exception e) {
                logger.debug("Location xpath failed: {}", xpath);
            }
        }

        logger.debug("Could not extract location");
        return "";
    }

    /**
     * Extract job description/summary using multiple selectors
     */
    private String extractDescription(WebElement jobElement) {
        String[] descriptionXpaths = {
            ".//p[@class='base-serp-card__subtitle']",
            ".//div[contains(@class, 'subtitle')]",
            ".//span[contains(@class, 'subtitle')]",
            ".//p[contains(@class, 'description')]",
            ".//div[@data-view-name='job-details']//p",
            ".//span[@class='job-search-card__snippet']"
        };

        for (String xpath : descriptionXpaths) {
            try {
                String description = jobElement.findElement(By.xpath(xpath)).getText();
                if (!description.isEmpty()) {
                    logger.debug("Found description: {}", description.substring(0, Math.min(50, description.length())));
                    return description;
                }
            } catch (Exception e) {
                logger.debug("Description xpath failed: {}", xpath);
            }
        }

        logger.debug("Could not extract description");
        return "";
    }

    /**
     * Try to find job elements using current LinkedIn selectors
     */
    private List<WebElement> tryFindJobElements() {
        // LinkedIn job listings are typically in ul > li elements with these classes
        // Try multiple selectors as LinkedIn UI changes frequently

        List<String> xpathSelectors = new ArrayList<>();

        // Selector 1: Current LinkedIn structure (as of 2024)
        xpathSelectors.add("//ul[@class='jobs-search__results-list']//li");

        // Selector 2: Alternative with data attributes
        xpathSelectors.add("//li[contains(@class, 'base-card')]");

        // Selector 3: Using aria-label
        xpathSelectors.add("//div[@data-job-id]");

        // Selector 4: Job card containers
        xpathSelectors.add("//div[contains(@class, 'base-card') and contains(@class, 'rounded-lg')]");

        // Selector 5: Fallback generic selector
        xpathSelectors.add("//article[contains(@class, 'job-')]");

        for (String xpath : xpathSelectors) {
            try {
                List<WebElement> elements = driver.findElements(By.xpath(xpath));
                if (!elements.isEmpty()) {
                    logger.info("Found {} job elements using selector: {}", elements.size(), xpath);
                    return elements;
                }
            } catch (Exception e) {
                logger.debug("XPath selector failed: {}", xpath);
            }
        }

        return new ArrayList<>();
    }

    /**
     * Try fallback methods to find job elements
     */
    private List<WebElement> tryFallbackJobElements() {
        logger.info("Attempting fallback job element discovery...");

        List<WebElement> elements = new ArrayList<>();

        // Try to find by looking for job posting patterns
        try {
            // Look for links that point to job postings
            List<WebElement> jobLinks = driver.findElements(By.xpath("//a[contains(@href, '/jobs/view/')]"));
            logger.info("Found {} job links via href pattern", jobLinks.size());

            if (!jobLinks.isEmpty()) {
                return jobLinks;
            }
        } catch (Exception e) {
            logger.debug("Job link search failed", e);
        }

        try {
            // Look for any element with data-job-id attribute
            List<WebElement> dataJobElements = driver.findElements(By.xpath("//*[@data-job-id]"));
            logger.info("Found {} elements with data-job-id attribute", dataJobElements.size());

            if (!dataJobElements.isEmpty()) {
                return dataJobElements;
            }
        } catch (Exception e) {
            logger.debug("data-job-id search failed", e);
        }

        // Inspect page structure for debugging
        inspectPageStructure();

        return elements;
    }

    /**
     * Inspect and log the page structure to help with debugging
     */
    private void inspectPageStructure() {
        try {
            logger.warn("=== LinkedIn Page Structure Debug ===");

            // Get all list items and log their HTML structure
            List<WebElement> allListItems = driver.findElements(By.xpath("//li"));
            logger.warn("Total <li> elements on page: {}", allListItems.size());

            // Get all divs with class containing 'card'
            List<WebElement> cardDivs = driver.findElements(By.xpath("//div[contains(@class, 'card')]"));
            logger.warn("Total divs with 'card' in class: {}", cardDivs.size());

            // Get all elements with data-job-id
            List<WebElement> jobDataElements = driver.findElements(By.xpath("//*[@data-job-id]"));
            logger.warn("Total elements with data-job-id: {}", jobDataElements.size());

            // Log sample HTML of page container
            try {
                WebElement container = driver.findElement(By.xpath("//main"));
                String html = container.getAttribute("innerHTML");
                logger.warn("Main container HTML length: {} characters", html.length());

                // Log first 500 chars of HTML for inspection
                if (html.length() > 500) {
                    logger.warn("HTML Preview (first 500 chars): {}", html.substring(0, 500));
                }
            } catch (Exception e) {
                logger.debug("Could not access main container");
            }

            logger.warn("=== End Debug Info ===");
        } catch (Exception e) {
            logger.warn("Error during page structure inspection", e);
        }
    }
}

