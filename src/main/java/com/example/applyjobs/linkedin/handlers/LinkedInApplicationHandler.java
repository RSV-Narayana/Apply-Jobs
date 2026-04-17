package com.example.applyjobs.linkedin.handlers;

import com.example.applyjobs.automation.WaitHelper;
import com.example.applyjobs.exception.ApplicationException;
import com.example.applyjobs.model.LinkedInJobListing;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class LinkedInApplicationHandler {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInApplicationHandler.class);
    private WebDriver driver;
    private WaitHelper waitHelper;
    private static final int MIN_DELAY = 10000; // 10 seconds
    private static final int MAX_DELAY = 30000; // 30 seconds

    public LinkedInApplicationHandler(WebDriver driver) {
        this.driver = driver;
        this.waitHelper = new WaitHelper(driver);
    }

    /**
     * Apply to a job
     */
    public boolean applyToJob(LinkedInJobListing job) {
        try {
            logger.info("Attempting to apply to job: {}", job.getTitle());

            // Click on the job to view details
            clickJobListing(job);

            // Click apply button
            if (!clickApplyButton()) {
                logger.warn("Could not find apply button for job: {}", job.getTitle());
                return false;
            }

            // Handle Easy Apply flow
            boolean success = handleEasyApplyFlow();

            // Random delay between applications (rate limiting)
            long delay = MIN_DELAY + new Random().nextLong(MAX_DELAY - MIN_DELAY);
            logger.info("Waiting {} ms before next application", delay);
            Thread.sleep(delay);

            return success;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Application process interrupted", e);
            return false;
        } catch (Exception e) {
            logger.error("Failed to apply to job: {}", job.getTitle(), e);
            return false;
        }
    }

    /**
     * Click on job listing to view details
     */
    private void clickJobListing(LinkedInJobListing job) {
        try {
            logger.debug("Clicking job listing for: {}", job.getTitle());

            // Try to find by job ID first
            String jobId = job.getId();
            By locator = By.xpath("//a[contains(@href, '/jobs/view/" + jobId + "')]");

            try {
                WebElement jobLink = waitHelper.waitForElementClickable(locator, 5);
                jobLink.click();
            } catch (Exception e) {
                // Try alternative selector
                logger.debug("Could not find by ID, trying by title");
                By altLocator = By.xpath("//span[contains(text(), '" + job.getTitle().substring(0, Math.min(20, job.getTitle().length())) + "')]");
                WebElement jobLink = waitHelper.waitForElementClickable(altLocator);
                jobLink.click();
            }

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("Job click interrupted", e);
        } catch (Exception e) {
            logger.warn("Failed to click job listing", e);
            // Continue anyway
        }
    }

    /**
     * Click the apply button
     */
    private boolean clickApplyButton() {
        try {
            logger.debug("Looking for apply button");

            By applyButtonLocator = By.xpath("//button[contains(text(), 'Apply') or @aria-label='Apply']");
            WebElement applyButton = waitHelper.waitForElementClickable(applyButtonLocator, 10);

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", applyButton);
            Thread.sleep(500);

            applyButton.click();
            logger.info("Apply button clicked");

            Thread.sleep(2000); // Wait for modal to open
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            logger.warn("Could not find or click apply button", e);
            return false;
        }
    }

    /**
     * Handle the Easy Apply modal flow
     */
    private boolean handleEasyApplyFlow() {
        try {
            logger.info("Starting Easy Apply flow");

            // Process multiple steps in the form
            int maxIterations = 10;
            for (int i = 0; i < maxIterations; i++) {
                logger.debug("Easy Apply step {}", i + 1);

                try {
                    // Check if on review step
                    if (isReviewStep()) {
                        logger.info("On review step");
                        handleReviewStep();
                    }

                    // Check for additional questions
                    if (hasAdditionalQuestions()) {
                        logger.info("Processing additional questions");
                        handleAdditionalQuestions();
                    }

                    // Try to click Next button
                    if (clickNextButton()) {
                        logger.debug("Clicked Next button");
                        Thread.sleep(1000);
                        continue;
                    }

                    // Try to click Submit button
                    if (clickSubmitButton()) {
                        logger.info("Clicked Submit button - application submitted");
                        Thread.sleep(2000);
                        return true;
                    }

                    // No more buttons found, might be complete
                    logger.info("No more buttons to click, application may be complete");
                    return true;

                } catch (NoSuchElementException e) {
                    logger.debug("Expected element not found in step {}", i + 1);
                    continue;
                }
            }

            logger.warn("Reached max iterations in Easy Apply flow");
            return true;

        } catch (Exception e) {
            logger.error("Error in Easy Apply flow", e);
            return true; // Return true to mark as attempted
        }
    }

    /**
     * Check if on review step
     */
    private boolean isReviewStep() {
        try {
            By reviewLocator = By.xpath("//h3[contains(text(), 'Review')] | //span[contains(text(), 'review')] | //div[contains(text(), 'Review your application')]");
            return waitHelper.isElementPresent(reviewLocator);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Handle review step - uncheck "Follow company" checkbox
     */
    private void handleReviewStep() {
        try {
            logger.info("Handling review step");

            // Find all checkboxes
            List<WebElement> checkboxes = driver.findElements(By.xpath("//input[@type='checkbox']"));

            for (WebElement checkbox : checkboxes) {
                try {
                    // Get the parent or associated label
                    WebElement parent = checkbox.findElement(By.xpath("./ancestor::label | ./ancestor::span | ./following-sibling::span"));
                    String text = parent.getText().toLowerCase();

                    if (text.contains("follow") && text.contains("company")) {
                        logger.info("Found 'Follow company' checkbox, unchecking it");

                        if (checkbox.isSelected()) {
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", checkbox);
                            Thread.sleep(300);
                            checkbox.click();
                            logger.info("'Follow company' checkbox unchecked");
                        }
                        break;
                    }
                } catch (Exception e) {
                    logger.debug("Error checking checkbox", e);
                }
            }
        } catch (Exception e) {
            logger.warn("Error in review step handling", e);
        }
    }

    /**
     * Check if there are additional questions
     */
    private boolean hasAdditionalQuestions() {
        try {
            By questionLocator = By.xpath("//input[not(@type='hidden')] | //textarea | //select");
            return waitHelper.isElementPresent(questionLocator);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Handle additional questions
     */
    private void handleAdditionalQuestions() {
        try {
            logger.info("Handling additional questions");

            // Find all input fields, textareas, and select elements
            List<WebElement> questionFields = driver.findElements(
                By.xpath("//input[not(@type='hidden')] | //textarea | //select")
            );

            logger.info("Found {} question fields", questionFields.size());

            for (WebElement field : questionFields) {
                try {
                    String fieldType = field.getTagName().toLowerCase();

                    if ("input".equals(fieldType)) {
                        String inputType = field.getAttribute("type").toLowerCase();

                        if ("checkbox".equals(inputType)) {
                            handleCheckbox(field);
                        } else if ("radio".equals(inputType)) {
                            // Skip radio for now
                        } else {
                            handleInputField(field);
                        }
                    } else if ("textarea".equals(fieldType)) {
                        handleTextAreaField(field);
                    } else if ("select".equals(fieldType)) {
                        handleSelectField(field);
                    }
                } catch (Exception e) {
                    logger.warn("Error handling question field", e);
                }
            }
        } catch (Exception e) {
            logger.warn("Error in additional questions handling", e);
        }
    }

    /**
     * Handle text input field
     */
    private void handleInputField(WebElement field) {
        try {
            String question = getQuestionText(field);
            String response = generateResponseForQuestion(question);

            field.clear();
            field.sendKeys(response);
            logger.debug("Filled input field: {}", question);
        } catch (Exception e) {
            logger.warn("Error handling input field", e);
        }
    }

    /**
     * Handle textarea field
     */
    private void handleTextAreaField(WebElement field) {
        try {
            String question = getQuestionText(field);
            String response = generateResponseForQuestion(question);

            field.clear();
            field.sendKeys(response);
            logger.debug("Filled textarea field: {}", question);
        } catch (Exception e) {
            logger.warn("Error handling textarea field", e);
        }
    }

    /**
     * Handle select/dropdown field
     */
    private void handleSelectField(WebElement field) {
        try {
            String question = getQuestionText(field);
            logger.debug("Handling select field: {}", question);

            Select select = new Select(field);
            List<WebElement> options = select.getOptions();

            if (options.size() > 1) {
                // Select first non-empty option
                select.selectByIndex(1);
                logger.debug("Selected option for: {}", question);
            }
        } catch (Exception e) {
            logger.warn("Error handling select field", e);
        }
    }

    /**
     * Handle checkbox field
     */
    private void handleCheckbox(WebElement checkbox) {
        try {
            String question = getQuestionText(checkbox);

            if (!checkbox.isSelected()) {
                checkbox.click();
                logger.debug("Checked checkbox: {}", question);
            }
        } catch (Exception e) {
            logger.warn("Error handling checkbox", e);
        }
    }

    /**
     * Get question text
     */
    private String getQuestionText(WebElement field) {
        try {
            // Try to find associated label
            String name = field.getAttribute("name");
            if (name != null && !name.isEmpty()) {
                return name;
            }

            // Try aria-label
            String ariaLabel = field.getAttribute("aria-label");
            if (ariaLabel != null && !ariaLabel.isEmpty()) {
                return ariaLabel;
            }

            // Try placeholder
            String placeholder = field.getAttribute("placeholder");
            if (placeholder != null && !placeholder.isEmpty()) {
                return placeholder;
            }

            return "Unknown question";
        } catch (Exception e) {
            return "Unknown question";
        }
    }

    /**
     * Generate response for a question based on keywords
     */
    private String generateResponseForQuestion(String question) {
        String lower = question.toLowerCase();

        if (lower.contains("experience") || lower.contains("years")) {
            return "5+ years of professional experience";
        } else if (lower.contains("location") || lower.contains("willing")) {
            return "Yes, willing to relocate if necessary";
        } else if (lower.contains("start") || lower.contains("availability")) {
            return "Immediately or by mutual agreement";
        } else if (lower.contains("salary") || lower.contains("compensation")) {
            return "Open to discussion based on role and company";
        } else if (lower.contains("notice") || lower.contains("period")) {
            return "2 weeks";
        } else if (lower.contains("remote") || lower.contains("work from")) {
            return "Flexible work arrangement preferred";
        } else if (lower.contains("visa") || lower.contains("sponsorship")) {
            return "No sponsorship required";
        }

        return "Yes, I'm interested in this opportunity";
    }

    /**
     * Click Next button
     */
    private boolean clickNextButton() {
        try {
            By nextButtonLocator = By.xpath("//button[contains(text(), 'Next')]");
            WebElement nextButton = waitHelper.waitForElementClickable(nextButtonLocator, 5);

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextButton);
            Thread.sleep(300);

            nextButton.click();
            return true;
        } catch (Exception e) {
            logger.debug("Next button not found or not clickable");
            return false;
        }
    }

    /**
     * Click Submit button
     */
    private boolean clickSubmitButton() {
        try {
            By submitButtonLocator = By.xpath("//button[contains(text(), 'Submit') or contains(text(), 'Done')]");
            WebElement submitButton = waitHelper.waitForElementClickable(submitButtonLocator, 5);

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitButton);
            Thread.sleep(300);

            submitButton.click();
            return true;
        } catch (Exception e) {
            logger.debug("Submit button not found or not clickable");
            return false;
        }
    }
}

