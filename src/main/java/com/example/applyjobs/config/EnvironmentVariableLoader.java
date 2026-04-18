package com.example.applyjobs.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentVariableLoader {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentVariableLoader.class);

    private String linkedInUsername;
    private String linkedInPassword;
    private String diceUsername;
    private String dicePassword;
    private String jobTitle;
    private List<String> jobSkills;
    private int matchingThreshold;
    private String outputDirectory;

    public EnvironmentVariableLoader() {
        loadEnvironmentVariables();
    }

    private void loadEnvironmentVariables() {
        this.linkedInUsername = getEnvironmentVariable("LINKEDIN_USERNAME", "");
        this.linkedInPassword = getEnvironmentVariable("LINKEDIN_PASSWORD", "");
        this.diceUsername = getEnvironmentVariable("DICE_USERNAME", "");
        this.dicePassword = getEnvironmentVariable("DICE_PASSWORD", "");
        this.jobTitle = getEnvironmentVariable("JOB_TITLE", "Full Stack Engineer");

        String skillsEnv = getEnvironmentVariable("JOB_SKILLS", "Java,Spring Boot,React,PostgreSQL");
        this.jobSkills = Arrays.stream(skillsEnv.split(","))
            .map(String::trim)
            .collect(Collectors.toList());

        String threshold = getEnvironmentVariable("JOB_MATCHING_THRESHOLD", "50");
        this.matchingThreshold = Integer.parseInt(threshold);

        this.outputDirectory = getEnvironmentVariable("OUTPUT_DIRECTORY", System.getProperty("user.dir"));

        // Log loaded configuration (mask sensitive values)
        logger.info("===== Environment Variables Loaded =====");
        logger.info("LINKEDIN_USERNAME: {}", linkedInUsername.isEmpty() ? "NOT SET" : "***");
        logger.info("LINKEDIN_PASSWORD: {}", linkedInPassword.isEmpty() ? "NOT SET" : "***");
        logger.info("JOB_TITLE: {}", jobTitle);
        logger.info("JOB_SKILLS: {}", jobSkills);
        logger.info("JOB_MATCHING_THRESHOLD: {}%", matchingThreshold);
        logger.info("OUTPUT_DIRECTORY: {}", outputDirectory);
        logger.info("=========================================");
    }

    private String getEnvironmentVariable(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    public String getLinkedInUsername() {
        return linkedInUsername;
    }

    public String getLinkedInPassword() {
        return linkedInPassword;
    }

    public String getDiceUsername() {
        return diceUsername;
    }

    public String getDicePassword() {
        return dicePassword;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public List<String> getJobSkills() {
        return jobSkills;
    }

    public int getMatchingThreshold() {
        return matchingThreshold;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void validateLinkedInCredentials() {
        if (linkedInUsername.isEmpty() || linkedInPassword.isEmpty()) {
            logger.error("LinkedIn credentials validation failed!");
            if (linkedInUsername.isEmpty()) {
                logger.error("  - LINKEDIN_USERNAME is not set");
            }
            if (linkedInPassword.isEmpty()) {
                logger.error("  - LINKEDIN_PASSWORD is not set");
            }
            logger.error("Set these environment variables before running:");
            logger.error("  export LINKEDIN_USERNAME='your_email@example.com'");
            logger.error("  export LINKEDIN_PASSWORD='your_password'");
            throw new IllegalArgumentException("LinkedIn credentials not set. Set LINKEDIN_USERNAME and LINKEDIN_PASSWORD environment variables.");
        }
    }

    public void validateDiceCredentials() {
        if (diceUsername.isEmpty() || dicePassword.isEmpty()) {
            throw new IllegalArgumentException("Dice credentials not set. Set DICE_USERNAME and DICE_PASSWORD environment variables.");
        }
    }
}

