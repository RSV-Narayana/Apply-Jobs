package com.example.applyjobs.linkedin;

import com.example.applyjobs.automation.BrowserAutomationEngine;
import com.example.applyjobs.automation.WaitHelper;
import com.example.applyjobs.config.EnvironmentVariableLoader;
import com.example.applyjobs.linkedin.handlers.LinkedInApplicationHandler;
import com.example.applyjobs.linkedin.handlers.LinkedInJobExtractor;
import com.example.applyjobs.linkedin.handlers.LinkedInJobValidationHandler;
import com.example.applyjobs.linkedin.handlers.LinkedInLoginHandler;
import com.example.applyjobs.linkedin.handlers.LinkedInNavigationHandler;
import com.example.applyjobs.matcher.JobMatcher;
import com.example.applyjobs.model.ApplicationResult;
import com.example.applyjobs.model.LinkedInJobListing;
import com.example.applyjobs.logger.ResultLogger;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class LinkedInJobApplicationAgent {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInJobApplicationAgent.class);
    private WebDriver driver;
    private WaitHelper waitHelper;
    private LinkedInLoginHandler loginHandler;
    private LinkedInNavigationHandler navigationHandler;
    private LinkedInJobExtractor jobExtractor;
    private LinkedInApplicationHandler applicationHandler;
    private LinkedInJobValidationHandler validationHandler;
    private JobMatcher jobMatcher;
    private ResultLogger resultLogger;
    private EnvironmentVariableLoader config;

    public LinkedInJobApplicationAgent() {
        this.config = new EnvironmentVariableLoader();
        this.resultLogger = new ResultLogger(config.getOutputDirectory(), "LinkedIn");
    }

    /**
     * Execute the full LinkedIn job application workflow
     */
    public void executeWorkflow() {
        try {
            logger.info("========== Starting LinkedIn Job Application Workflow ==========");

            // Initialize browser
            initializeBrowser();

            // Validate credentials
            config.validateLinkedInCredentials();

            // Login
            performLogin();

            // Navigate to jobs
            navigationHandler.navigateToJobsPage();

            Thread.sleep(5000);
            // Search or click recent search
            performJobSearch();

            Thread.sleep(10000);
            // Extract and process jobs
            processJobListings();

            Thread.sleep(10000);

            // Write summary
            resultLogger.writeSummary();

            logger.info("========== LinkedIn Workflow Completed Successfully ==========");

        } catch (Exception e) {
            logger.error("LinkedIn workflow failed", e);
            resultLogger.writeSummary();
            throw new RuntimeException("LinkedIn workflow failed", e);
        } finally {
            closeDriver();
        }
    }

    /**
     * Initialize browser and handlers
     */
    private void initializeBrowser() {
        try {
            logger.info("Initializing browser automation");

            BrowserAutomationEngine engine = BrowserAutomationEngine.getInstance();
            this.driver = engine.getDriver();
            this.waitHelper = new WaitHelper(driver);

            // Initialize handlers
            this.loginHandler = new LinkedInLoginHandler(driver);
            this.navigationHandler = new LinkedInNavigationHandler(driver);
            this.jobExtractor = new LinkedInJobExtractor(driver);
            this.applicationHandler = new LinkedInApplicationHandler(driver);
            this.validationHandler = new LinkedInJobValidationHandler(driver, config.getJobSkills());

            // Set validation handler in application handler
            this.applicationHandler.setValidationHandler(validationHandler);

            // Initialize job matcher
            this.jobMatcher = new JobMatcher(
                config.getJobTitle(),
                config.getJobSkills(),
                config.getMatchingThreshold()
            );

            logger.info("Browser initialization complete");
        } catch (Exception e) {
            logger.error("Failed to initialize browser", e);
            throw new RuntimeException("Browser initialization failed", e);
        }
    }

    /**
     * Perform LinkedIn login
     */
    private void performLogin() {
        try {
            logger.info("Performing LinkedIn login");
            loginHandler.login(config.getLinkedInUsername(), config.getLinkedInPassword());
            logger.info("Login successful");
        } catch (Exception e) {
            logger.error("Login failed", e);
            throw new RuntimeException("LinkedIn login failed", e);
        }
    }

    /**
     * Perform job search
     */
    private void performJobSearch() {
        try {
            Thread.sleep(5000);
            logger.info("Searching for jobs: {}", config.getJobTitle());

            // Try to click recent search first
            try {
                navigationHandler.clickRecentSearch(config.getJobTitle());
                logger.info("Found and clicked recent search");
            } catch (Exception e) {
                logger.info("No recent search found, performing new search");
                navigationHandler.performSearch(config.getJobTitle());
            }
        } catch (Exception e) {
            logger.error("Job search failed", e);
            throw new RuntimeException("Job search failed", e);
        }
    }

    /**
     * Extract and process job listings with comprehensive validation
     */
    private void processJobListings() {
        try {
            logger.info("Processing job listings");

            // Extract latest 10 jobs (configurable)
            List<LinkedInJobListing> jobs = jobExtractor.extractLatestJobListings(10);
            logger.info("Extracted {} jobs from LinkedIn", jobs.size());

            if (jobs.isEmpty()) {
                logger.warn("No jobs found to process");
                return;
            }

            // Apply to matching jobs
            int appliedCount = 0;
            int skippedCount = 0;
            int failedCount = 0;

            for (LinkedInJobListing job : jobs) {
                try {
                    logger.info("========== Processing job: {} - {} ==========", job.getTitle(), job.getCompany());

                    // Initial skill-based matching
                    if (!jobMatcher.isMatch(job)) {
                        logger.info("Job does not match initial criteria (title/skills), skipping");
                        skippedCount++;
                        continue;
                    }

                    logger.info("Job passed initial skill matching, proceeding to apply with validation");

                    // Apply to job (includes validation checks during application)
                    boolean success = applicationHandler.applyToJob(job);

                    if (success) {
                        logger.info("Successfully applied to job: {}", job.getTitle());
                        // Log only successful applications
                        ApplicationResult result = new ApplicationResult(
                            job.getTitle(),
                            job.getCompany(),
                            LocalDate.now(),
                            "SUCCESS"
                        );
                        resultLogger.logResult(result);
                        appliedCount++;
                    } else {
                        logger.warn("Application failed for job: {}", job.getTitle());
                        failedCount++;
                    }

                } catch (Exception e) {
                    logger.error("Exception while processing job: {}", job.getTitle(), e);
                    failedCount++;
                }

                // Add delay between job processing
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Job processing interrupted", e);
                    break;
                }
            }

            logger.info("========== Job Processing Summary ==========");
            logger.info("Total jobs extracted: {}", jobs.size());
            logger.info("Jobs applied: {}", appliedCount);
            logger.info("Jobs skipped (not matching): {}", skippedCount);
            logger.info("Jobs failed: {}", failedCount);

        } catch (Exception e) {
            logger.error("Failed to process job listings", e);
            throw new RuntimeException("Job processing failed", e);
        }
    }

    /**
     * Close browser and cleanup
     */
    private void closeDriver() {
        try {
            logger.info("Closing browser");
//            Thread.sleep(200000);
            BrowserAutomationEngine engine = BrowserAutomationEngine.getInstance();
            engine.closeDriver();
            logger.info("Browser closed successfully");
        } catch (Exception e) {
            logger.warn("Error closing browser", e);
        }
    }
}

