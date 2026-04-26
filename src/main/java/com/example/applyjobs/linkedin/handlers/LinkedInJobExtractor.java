package com.example.applyjobs.linkedin.handlers;

import com.example.applyjobs.automation.WaitHelper;
import com.example.applyjobs.exception.JobExtractionException;
import com.example.applyjobs.model.LinkedInJobListing;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
            
            // IMPORTANT: Wait for job list to fully load before extracting
            logger.info("Waiting for job elements to load on page...");
            Thread.sleep(10000); // Initial wait for page structure
            waitHelper.waitForPageLoad();
            Thread.sleep(10000); // Additional wait for JavaScript rendering
            
            // Wait for at least one job element to be present
            try {
                org.openqa.selenium.support.ui.WebDriverWait wait = 
                    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30));
                wait.until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.xpath("//li[contains(@class, 'ember-view')]")
                ));
                logger.info("Job elements detected, proceeding with extraction");
            } catch (Exception e) {
                logger.warn("Timeout waiting for job elements: {}", e.getMessage());
                inspectPageForDebug();
            }

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

                    Thread.sleep(2000);

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

            // Log HTML structure for debugging
            try {
                String outerHtml = ((JavascriptExecutor) driver).executeScript(
                    "return arguments[0].outerHTML;", jobElement).toString();
                logger.info("=== JOB ELEMENT HTML (first 500 chars) ===");
                logger.info(outerHtml.substring(0, Math.min(500, outerHtml.length())));
                logger.info("=== END HTML ===");
            } catch (Exception e) {
                logger.debug("Could not log HTML structure", e);
            }

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
     * Extract job title using multiple selectors (adapted for minified classes)
     */
    private String extractJobTitle(WebElement jobElement) {
        String[] titleXpaths = {
            // Strategy 1: Look for links to job view pages
            ".//a[contains(@href, '/jobs/view/')]",
            // Strategy 2: Look for first h3 or h2 heading
            ".//h3[1]",
            ".//h2[1]",
            // Strategy 3: Look for span with aria-hidden (title text)
            ".//span[@aria-hidden='true']",
            // Strategy 4: Look for strong tags (often used for titles)
            ".//strong",
            // Strategy 5: Look for any text in immediate divs/spans
            ".//div[1]//span[1]",
            // Strategy 6: Generic text containers
            ".//span[string-length(text()) > 5][1]"
        };

        for (String xpath : titleXpaths) {
            try {
                WebElement titleElement = jobElement.findElement(By.xpath(xpath));
                String title = titleElement.getText().trim();
                if (!title.isEmpty() && title.length() > 2) {
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
     * Extract company name using multiple selectors (adapted for minified classes)
     */
    private String extractCompanyName(WebElement jobElement) {
        String[] companyXpaths = {
            // Strategy 1: Look for company data attribute
            ".//a[@data-tracking-control-name='public_jobs_company-name-link']",
            // Strategy 2: Look for company links
            ".//a[contains(@href, '/company/')]",
            // Strategy 3: Second span element (often company)
            ".//span[2]",
            // Strategy 4: Span after a job title link
            ".//a[contains(@href, '/jobs/view/')]/../following-sibling::*//span[1]",
            // Strategy 5: Look for medium-length text (company names)
            ".//span[string-length(text()) > 2 and string-length(text()) < 100][2]"
        };

        for (String xpath : companyXpaths) {
            try {
                WebElement companyElement = jobElement.findElement(By.xpath(xpath));
                String company = companyElement.getText().trim();
                // Validate it's not a job title (job titles are longer)
                if (!company.isEmpty() && company.length() < 100 && !company.contains("Jr") && !company.contains("Sr")) {
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
     * Extract location using multiple selectors (adapted for minified classes)
     */
    private String extractLocation(WebElement jobElement) {
        String[] locationXpaths = {
            // Strategy 1: Look for Remote/Hybrid keywords
            ".//span[contains(text(), 'Remote')] | .//span[contains(text(), 'Hybrid')] | .//span[contains(text(), 'On-site')]",
            // Strategy 2: Look for location text patterns
            ".//span[contains(text(), 'in ')]",
            // Strategy 3: Look for city names (US states)
            ".//span[contains(text(), 'CA') or contains(text(), 'NY') or contains(text(), 'TX') or contains(text(), 'FL')]",
            // Strategy 4: Third span element (often location)
            ".//span[3]",
            // Strategy 5: Spans with location-like text
            ".//span[string-length(text()) < 50 and (contains(text(), ',') or contains(text(), 'USA'))]"
        };

        for (String xpath : locationXpaths) {
            try {
                WebElement locationElement = jobElement.findElement(By.xpath(xpath));
                String location = locationElement.getText().trim();
                if (!location.isEmpty() && location.length() < 50) {
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
     * Extract job description/summary using multiple selectors (adapted for minified classes)
     */
    private String extractDescription(WebElement jobElement) {
        String[] descriptionXpaths = {
            // Strategy 1: Look for paragraph tags
            ".//p[1]",
            // Strategy 2: Look for description in divs
            ".//div[contains(text(), 'Senior') or contains(text(), 'junior') or contains(text(), 'experience')]",
            // Strategy 3: Look for text patterns (longer text)
            ".//span[string-length(text()) > 20][1]",
            // Strategy 4: Data attributes containing job snippet
            ".//*[@data-view-name='job-details']//p",
            // Strategy 5: Any element with reasonable description length
            ".//div[string-length(.) > 30 and string-length(.) < 500][1]"
        };

        for (String xpath : descriptionXpaths) {
            try {
                WebElement descElement = jobElement.findElement(By.xpath(xpath));
                String description = descElement.getText().trim();
                if (!description.isEmpty() && description.length() > 20) {
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
     * Try to find job elements using 2026 LinkedIn selectors
     * LinkedIn frequently updates its DOM structure, so multiple fallbacks are essential
     */
    private List<WebElement> tryFindJobElements() {
        List<String> xpathSelectors = new ArrayList<>();

        logger.info("Current Page URL in the Try Find Job Elements: {} ", driver.getCurrentUrl());

        // LinkedIn 2026 selectors (updated structure - using minified class names)
        // Selector 0: Target the exact minified classes we found (_96b8567e _20f8d793)
        xpathSelectors.add("//li[contains(@class, '_96b8567e')]");
        
        // Selector 1: Any li with hashed class name pattern (starts with underscore)
        xpathSelectors.add("//li[starts-with(@class, '_')]");
        
        // Selector 2: Li elements in job list container (by data attributes)
        xpathSelectors.add("//li[@data-job-id or @data-occludable-job-id]");
        
        // Selector 3: Primary - li elements with ember-view class (fallback for older versions)
        xpathSelectors.add("//li[contains(@class, 'ember-view')]");
        
        // Selector 4: With ul container
        xpathSelectors.add("//ul//li[contains(@class, 'ember-view')]");
        
        // Selector 5: Alternative - li elements with base-card class
        xpathSelectors.add("//li[contains(@class, 'base-card')]");

        // Selector 6: Article elements (2026 structure update)
        xpathSelectors.add("//article[contains(@data-job-id, '')]");

        // Selector 7: Div elements with data-job-id attribute (2026 update)
        xpathSelectors.add("//div[contains(@class, 'base-card') and contains(@class, 'rounded-lg')]");

        // Selector 8: Generic base-search-card elements
        xpathSelectors.add("//div[contains(@class, 'base-search-card')]");
        
        logger.info("Trying {} XPath selectors to find job elements", xpathSelectors.size());

        for (String xpath : xpathSelectors) {
            try {
                logger.debug("Trying XPath selector: {}", xpath);
                List<WebElement> elements = driver.findElements(By.xpath(xpath));
                logger.info("XPath '{}' found {} elements", xpath, elements.size());
                
                if (!elements.isEmpty()) {
                    logger.info("SUCCESS! Found {} job elements using selector: {}", elements.size(), xpath);
                    // Log first element's HTML for debugging
                    try {
                        String firstElementTag = elements.get(0).getTagName();
                        String firstElementClass = elements.get(0).getAttribute("class");
                        String firstElementDataJobId = elements.get(0).getAttribute("data-job-id");
                        logger.info("First element: tag={}, class={}, data-job-id={}", 
                                   firstElementTag, firstElementClass, firstElementDataJobId);
                    } catch (Exception e) {
                        logger.debug("Could not log first element details", e);
                    }
                    return elements;
                }
            } catch (Exception e) {
                logger.debug("XPath selector failed: {} - {}", xpath, e.getMessage());
            }
        }
        
        logger.warn("All XPath selectors failed to find job elements!");
        return new ArrayList<>();
    }
    
    /**
     * Debug helper to inspect page structure
     */
    private void inspectPageForDebug() {
        try {
            logger.warn("=== Page Structure Debug Info ===");
            logger.warn("Current URL: {}", driver.getCurrentUrl());
            logger.warn("Page title: {}", driver.getTitle());
            
            // Count all li elements
            List<WebElement> allLi = driver.findElements(By.tagName("li"));
            logger.warn("Total <li> elements found: {}", allLi.size());
            
            // Count li elements with ember-view class
            List<WebElement> emberViewItems = driver.findElements(By.xpath("//li[contains(@class, 'ember-view')]"));
            logger.warn("Total <li> elements with 'ember-view' class: {}", emberViewItems.size());
            
            // Count li elements with hashed classes (starting with _)
            List<WebElement> hashedItems = driver.findElements(By.xpath("//li[starts-with(@class, '_')]"));
            logger.warn("Total <li> elements with hashed classes (starts with _): {}", hashedItems.size());
            
            // Log details about first few li elements with all attributes
            logger.warn("Detailed info about first 5 <li> elements:");
            for (int i = 0; i < Math.min(5, allLi.size()); i++) {
                String classes = allLi.get(i).getAttribute("class");
                String dataJobId = allLi.get(i).getAttribute("data-job-id");
                String dataOccludableJobId = allLi.get(i).getAttribute("data-occludable-job-id");
                String id = allLi.get(i).getAttribute("id");
                logger.warn("  Li[{}]: class='{}', data-job-id='{}', data-occludable-job-id='{}', id='{}'", 
                           i, classes, dataJobId, dataOccludableJobId, id);
            }
            
            // Check for parent containers
            List<WebElement> containers = driver.findElements(By.xpath("//div[contains(@class, 'scaffold-layout__list')]"));
            logger.warn("Found {} containers with 'scaffold-layout__list' class", containers.size());
            
            // Check for any job-list containers
            List<WebElement> jobListDivs = driver.findElements(By.xpath("//div[contains(@class, 'jobs-search__results')]"));
            logger.warn("Found {} divs with 'jobs-search__results' class", jobListDivs.size());
            
            logger.warn("=== End Debug Info ===");
        } catch (Exception e) {
            logger.warn("Error during page inspection: {}", e.getMessage());
        }
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

