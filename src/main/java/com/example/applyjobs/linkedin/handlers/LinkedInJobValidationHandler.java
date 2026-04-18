package com.example.applyjobs.linkedin.handlers;

import com.example.applyjobs.automation.WaitHelper;
import com.example.applyjobs.model.LinkedInJobListing;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates job postings against specific criteria:
 * 1. Employment Type must be "Contract"
 * 2. Minimum salary of $60
 * 3. Job description matches required skills
 * 4. Overall IT Experience requirement is 10 or more years
 */
public class LinkedInJobValidationHandler {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInJobValidationHandler.class);
    private WebDriver driver;
    private WaitHelper waitHelper;
    private List<String> requiredSkills;

    public LinkedInJobValidationHandler(WebDriver driver, List<String> requiredSkills) {
        this.driver = driver;
        this.waitHelper = new WaitHelper(driver);
        this.requiredSkills = requiredSkills;
    }

    /**
     * Validate job against all conditions
     * Should be called after clicking on a job to view details on the right panel
     */
    public ValidationResult validateJob(LinkedInJobListing job) {
        try {
            logger.info("Validating job: {} from {}", job.getTitle(), job.getCompany());

            ValidationResult result = new ValidationResult(job.getId());

            // Check employment type
            result.employmentTypeValid = validateEmploymentType(job);
            logger.info("Employment Type Check: {}", result.employmentTypeValid);

            // Check salary
            result.salaryValid = validateMinimumSalary(job);
            logger.info("Salary Check (min $60): {}", result.salaryValid);

            // Check skills match
            result.skillsValid = validateSkillsMatch(job);
            logger.info("Skills Match Check: {}", result.skillsValid);

            // Check experience requirement
            result.experienceValid = validateExperienceRequirement(job);
            logger.info("Experience Requirement Check (10+ years): {}", result.experienceValid);

            // Overall validation
            result.isValid = result.employmentTypeValid && result.salaryValid &&
                             result.skillsValid && result.experienceValid;

            logger.info("Job Validation Complete: {} - Valid: {}", job.getTitle(), result.isValid);
            return result;

        } catch (Exception e) {
            logger.error("Error validating job: {}", job.getTitle(), e);
            return new ValidationResult(job.getId());
        }
    }

    /**
     * Condition 1: Check if employment type is "Contract"
     */
    private boolean validateEmploymentType(LinkedInJobListing job) {
        try {
            logger.debug("Checking employment type...");

            // Try multiple XPath selectors for employment type in job details panel
            String[] employmentTypeXpaths = {
                "//span[contains(text(), 'Contract')]",
                "//span[contains(text(), 'contract')]",
                "//div[contains(@class, 'employment-type')]//span",
                "//li[contains(text(), 'Contract')]",
                "//span[contains(., 'Contract')]",
                "//div[@data-job-id]//span[contains(., 'Contract')]"
            };

            String jobDetailsText = getJobDetailsText();

            for (String xpath : employmentTypeXpaths) {
                try {
                    WebElement element = driver.findElement(By.xpath(xpath));
                    String text = element.getText().toLowerCase();
                    if (text.contains("contract")) {
                        logger.info("Found Contract employment type");
                        job.setEmploymentType("Contract");
                        return true;
                    }
                } catch (Exception e) {
                    logger.debug("Employment type xpath failed: {}", xpath);
                }
            }

            // Also check in description/full text
            if (jobDetailsText.contains("contract") || jobDetailsText.contains("Contract")) {
                logger.info("Found 'Contract' in job details text");
                job.setEmploymentType("Contract");
                return true;
            }

            logger.warn("Employment type 'Contract' not found");
            return false;

        } catch (Exception e) {
            logger.warn("Error validating employment type", e);
            return false;
        }
    }

    /**
     * Condition 2: Check if minimum salary is $60 or more per hour
     */
    private boolean validateMinimumSalary(LinkedInJobListing job) {
        try {
            logger.debug("Checking minimum salary (minimum $60)...");

            String jobDetailsText = getJobDetailsText();
            logger.debug("Job details text length: {}", jobDetailsText.length());

            // Look for salary patterns like $60, $65/hr, $60-$80, etc.
            Pattern salaryPattern = Pattern.compile("\\$(\\d+)(?:\\s*-\\s*\\$(\\d+))?(?:/hr|/hour|per hour)?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = salaryPattern.matcher(jobDetailsText);

            double maxSalaryFound = 0;

            while (matcher.find()) {
                try {
                    String minStr = matcher.group(1);
                    String maxStr = matcher.group(2);

                    double min = Double.parseDouble(minStr);
                    double max = maxStr != null ? Double.parseDouble(maxStr) : min;

                    logger.debug("Found salary range: ${}-${}", min, max);

                    if (max > maxSalaryFound) {
                        maxSalaryFound = max;
                    }

                    job.setMinSalary(min);
                    job.setMaxSalary(max);
                    job.setSalaryCurrency("USD");
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse salary numbers", e);
                }
            }

            if (maxSalaryFound >= 60) {
                logger.info("Salary valid: ${} >= $60", maxSalaryFound);
                return true;
            } else if (maxSalaryFound > 0) {
                logger.warn("Salary too low: ${} < $60", maxSalaryFound);
                return false;
            } else {
                logger.warn("No salary information found in job details");
                return false;
            }

        } catch (Exception e) {
            logger.warn("Error validating salary", e);
            return false;
        }
    }

    /**
     * Condition 3: Check if job description contains required skills
     */
    private boolean validateSkillsMatch(LinkedInJobListing job) {
        try {
            logger.debug("Checking skills match...");

            String jobDetailsText = getJobDetailsText().toLowerCase();

            // Also check in job description
            String description = job.getDescription().toLowerCase();
            String combinedText = jobDetailsText + " " + description;

            int skillsMatched = 0;
            for (String skill : requiredSkills) {
                if (combinedText.contains(skill.toLowerCase())) {
                    skillsMatched++;
                    logger.debug("Skill found: {}", skill);
                }
            }

            double matchPercentage = (skillsMatched * 100.0) / requiredSkills.size();
            logger.info("Skills match: {}/{} ({}%)", skillsMatched, requiredSkills.size(), (int)matchPercentage);

            // Require at least 60% skill match
            return matchPercentage >= 60;

        } catch (Exception e) {
            logger.warn("Error validating skills", e);
            return false;
        }
    }

    /**
     * Condition 4: Check if job requires 10 or more years of IT experience
     */
    private boolean validateExperienceRequirement(LinkedInJobListing job) {
        try {
            logger.debug("Checking IT experience requirement (10+ years)...");

            String jobDetailsText = getJobDetailsText();

            // Look for experience patterns like "10+ years", "10 years", "10years experience", etc.
            Pattern experiencePattern = Pattern.compile("(\\d+)\\+?\\s*(?:years?|yrs)\\s+(?:of\\s+)?(?:experience|exp)?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = experiencePattern.matcher(jobDetailsText);

            int minExperienceFound = 0;

            while (matcher.find()) {
                try {
                    int years = Integer.parseInt(matcher.group(1));
                    logger.debug("Found experience requirement: {} years", years);

                    if (years > minExperienceFound) {
                        minExperienceFound = years;
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse experience years", e);
                }
            }

            job.setYearsExperienceRequired(minExperienceFound);

            if (minExperienceFound >= 10) {
                logger.info("Experience requirement valid: {} years >= 10 years", minExperienceFound);
                return true;
            } else if (minExperienceFound > 0) {
                logger.warn("Experience requirement too low: {} years < 10 years", minExperienceFound);
                return false;
            } else {
                logger.warn("Could not determine experience requirement from job details");
                // Return true if not specified (assume it's flexible)
                return true;
            }

        } catch (Exception e) {
            logger.warn("Error validating experience requirement", e);
            return false;
        }
    }

    /**
     * Get the visible text from the job details panel (right side)
     */
    private String getJobDetailsText() {
        try {
            // Try to get text from the job details section on the right panel
            String[] detailsXpaths = {
                "//main//section[contains(@class, 'details')]",
                "//div[@data-job-details-container]",
                "//div[contains(@class, 'job-details')]",
                "//article[contains(@class, 'job')]",
                "//div[contains(@class, 'show-details')]"
            };

            for (String xpath : detailsXpaths) {
                try {
                    WebElement detailsElement = driver.findElement(By.xpath(xpath));
                    String text = detailsElement.getText();
                    if (!text.isEmpty()) {
                        logger.debug("Extracted job details from: {}", xpath);
                        return text;
                    }
                } catch (Exception e) {
                    logger.debug("Could not get details from: {}", xpath);
                }
            }

            // Fallback: Get all text from main content area
            WebElement main = driver.findElement(By.xpath("//main"));
            String mainText = main.getText();
            logger.debug("Using main content area text ({} chars)", mainText.length());
            return mainText;

        } catch (Exception e) {
            logger.warn("Error retrieving job details text", e);
            return "";
        }
    }

    /**
     * Validation result container
     */
    public static class ValidationResult {
        public String jobId;
        public boolean employmentTypeValid;
        public boolean salaryValid;
        public boolean skillsValid;
        public boolean experienceValid;
        public boolean isValid;

        public ValidationResult(String jobId) {
            this.jobId = jobId;
            this.employmentTypeValid = false;
            this.salaryValid = false;
            this.skillsValid = false;
            this.experienceValid = false;
            this.isValid = false;
        }

        public String getSummary() {
            return String.format(
                "Job %s - Employment: %s, Salary: %s, Skills: %s, Experience: %s, Overall: %s",
                jobId, employmentTypeValid, salaryValid, skillsValid, experienceValid, isValid
            );
        }
    }
}

